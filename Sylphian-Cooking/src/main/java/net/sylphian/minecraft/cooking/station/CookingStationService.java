package net.sylphian.minecraft.cooking.station;

import net.sylphian.minecraft.cooking.config.CookingConfig;
import net.sylphian.minecraft.cooking.event.CookingCompleteEvent;
import net.sylphian.minecraft.cooking.event.CookingStartEvent;
import net.sylphian.minecraft.cooking.event.CookingXpEvent;
import net.sylphian.minecraft.cooking.gui.CookingStationGui;
import net.sylphian.minecraft.cooking.mastery.MasteryAccessor;
import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.quality.QualityRoller;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages the lifecycle of all active cooking stations.
 *
 * <p>Stations are loaded from PDC on first interaction and held in an in-memory map
 * for the duration of their activity. The service runs a tick loop every 20 game
 * ticks (once per second) that advances cook progress and consumes fuel for any
 * station that has a matching recipe and remaining fuel.</p>
 *
 * <p>When a station becomes fully idle (no ingredients, no fuel, no output, no
 * viewers) it is evicted from memory and its PDC is cleared.</p>
 *
 * <p>At the start of each new cook cycle the service fires {@link CookingStartEvent},
 * allowing skill listeners to reduce the effective cook time. When a recipe completes
 * it fires {@link CookingCompleteEvent}, allowing skill listeners to award XP, set a
 * quality tier, and set a bonus output item to be dropped at the station.</p>
 *
 * <p>Output is distributed across five independent output slots. The service finds
 * the first slot with a stackable identical item, or the first empty slot. If all
 * five are full with incompatible items, cooking is blocked until the player takes
 * some output. Items produced while output is full are dropped naturally at the
 * station block.</p>
 */
public class CookingStationService {

    /** Ticks between each processing cycle (20 = once per second). */
    private static final int TICK_INTERVAL = 20;

    /** Minimum cook time in ticks that skills may reduce a cycle to. */
    private static final int MIN_COOK_TIME = 20;

    private final JavaPlugin plugin;
    private final CookingStationGui gui;

    private List<CookingRecipe> recipes;
    private Map<Material, Integer> fuels;
    private volatile CookingConfig config;

    /** Provides a player's cooking skill level; defaults to 0 (no level bonus) when Skills is absent. */
    private Function<UUID, Integer> levelProvider = _ -> 0;

    /** Provides mastery counts; defaults to no-op until {@link net.sylphian.minecraft.cooking.mastery.CookingMasteryManager} is wired in. */
    private MasteryAccessor masteryAccessor = new MasteryAccessor() {
        public int getCount(UUID playerUuid, String recipeId) { return 0; }
        public void increment(UUID playerUuid, String recipeId) {}
    };

    /** Active stations keyed by their block's location. */
    private final Map<Location, CookingStationState> stations = new HashMap<>();

    /** Tracks which location each viewing player has open. */
    private final Map<UUID, Location> playerStationMap = new HashMap<>();

    private BukkitRunnable tickTask;

    /**
     * @param plugin  the owning plugin
     * @param recipes all loaded cooking recipes
     * @param fuels   map of fuel material to burn time in ticks
     * @param gui     the GUI factory used to open and update station inventories
     * @param config  the core cooking config (quality weights, formats, mastery)
     */
    public CookingStationService(JavaPlugin plugin, List<CookingRecipe> recipes,
                                 Map<Material, Integer> fuels, CookingStationGui gui,
                                 CookingConfig config) {
        this.plugin = plugin;
        this.recipes = new ArrayList<>(recipes);
        this.fuels = new EnumMap<>(fuels);
        this.gui = gui;
        this.config = config;
    }

    /**
     * Sets the provider used to look up a player's cooking skill level.
     * Call from {@code SkillsBridge} once Sylphian-Skills is confirmed present.
     *
     * @param levelProvider function returning the skill level for a given UUID
     */
    public void setLevelProvider(Function<UUID, Integer> levelProvider) {
        this.levelProvider = levelProvider;
    }

    /**
     * Sets the mastery accessor used to read and record per-player recipe cook counts.
     * Call from {@code SylphianCooking.onEnable()} after the mastery manager is ready.
     *
     * @param masteryAccessor the mastery accessor
     */
    public void setMasteryAccessor(MasteryAccessor masteryAccessor) {
        this.masteryAccessor = masteryAccessor;
    }

    /** Starts the background tick loop. Call from {@link JavaPlugin#onEnable()}. */
    public void start() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() { tickAll(); }
        };
        tickTask.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    /** Stops the tick loop and persists all active station states to PDC. */
    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (Map.Entry<Location, CookingStationState> entry : stations.entrySet()) {
            CookingStationPdc.save(entry.getKey().getBlock(), entry.getValue());
        }
        stations.clear();
        playerStationMap.clear();
    }

    /**
     * Opens the cooking station GUI for the given player at the given block.
     * Loads PDC state if this is the first time the station is being accessed.
     */
    public void openStation(Player player, Block block) {
        Location location = block.getLocation();
        CookingStationState state = stations.computeIfAbsent(location, _ -> CookingStationPdc.load(block));

        updateActiveRecipe(state);

        Inventory inventory = gui.build(state, block.getType());
        player.openInventory(inventory);

        state.addViewer(player.getUniqueId());
        playerStationMap.put(player.getUniqueId(), location);
    }

    /**
     * Flushes the current contents of an open GUI inventory directly into the in-memory
     * station state. Call this before {@link #closeStation} to guarantee that any items
     * moved in the same tick as the close are captured before the PDC write.
     */
    public void syncFromInventory(Player player, Inventory inv) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null) return;

        for (int i = 0; i < CookingStationGui.INGREDIENT_SLOTS.length; i++) {
            ItemStack item = inv.getItem(CookingStationGui.INGREDIENT_SLOTS[i]);
            state.setIngredient(i, realItem(item));
        }

        ItemStack fuel = inv.getItem(CookingStationGui.FUEL_SLOT);
        state.setFuel(realItem(fuel));

        for (int i = 0; i < CookingStationGui.OUTPUT_SLOTS.length; i++) {
            ItemStack out = inv.getItem(CookingStationGui.OUTPUT_SLOTS[i]);
            state.setOutput(i, realItem(out));
        }

        updateActiveRecipe(state);
    }

    /**
     * Syncs only the output slots from an open GUI inventory into the station state.
     * Call this after a player takes an item from an output slot.
     *
     * @param player the player who interacted with an output slot
     * @param inv    the open GUI inventory
     */
    public void syncOutputSlots(Player player, Inventory inv) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null) return;

        for (int i = 0; i < CookingStationGui.OUTPUT_SLOTS.length; i++) {
            ItemStack out = inv.getItem(CookingStationGui.OUTPUT_SLOTS[i]);
            state.setOutput(i, realItem(out));
        }
    }

    /** @return null if the item is null, air, or a GUI placeholder */
    private @Nullable ItemStack realItem(@Nullable ItemStack item) {
        return (item != null && !item.getType().isAir() && !CookingStationGui.isPlaceholder(item))
                ? item.clone() : null;
    }

    /** Called when a player closes their cooking station GUI. Saves state to PDC and evicts if idle. */
    public void closeStation(Player player) {
        Location location = playerStationMap.remove(player.getUniqueId());
        if (location == null) return;

        CookingStationState state = stations.get(location);
        if (state == null) return;

        state.removeViewer(player.getUniqueId());

        Block block = location.getBlock();
        CookingStationPdc.save(block, state);

        if (state.getViewers().isEmpty() && state.isEmpty() && state.getCookProgress() == 0) {
            stations.remove(location);
            CookingStationPdc.clear(block);
        }
    }

    /** Returns the location of the station currently open for the given player, or null. */
    public @Nullable Location getOpenStationLocation(UUID uuid) {
        return playerStationMap.get(uuid);
    }

    /** Returns the in-memory state of the station at the given location, or null. */
    public @Nullable CookingStationState getState(Location location) {
        return stations.get(location);
    }

    /**
     * Handles the destruction of a cooking station block.
     * Closes any open GUIs, drops stored items, removes in-memory state, and clears PDC.
     */
    public void destroyStation(Block block) {
        Location location = block.getLocation();

        CookingStationState state = stations.remove(location);
        if (state != null) {
            for (UUID uuid : new HashSet<>(state.getViewers())) {
                playerStationMap.remove(uuid);
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) p.closeInventory();
            }
            dropAllItems(block, state);
        } else {
            CookingStationState pdcState = CookingStationPdc.load(block);
            if (!pdcState.isEmpty()) dropAllItems(block, pdcState);
        }

        CookingStationPdc.clear(block);
    }

    /** Applies a change to an ingredient slot of the station the player has open. */
    public void setIngredient(Player player, int ingredientIdx, @Nullable ItemStack item) {
        mutateState(player, state -> {
            state.setIngredient(ingredientIdx, item);
            updateActiveRecipe(state);
        });
    }

    /** Applies a change to the fuel slot of the station the player has open. */
    public void setFuel(Player player, @Nullable ItemStack item) {
        mutateState(player, state -> state.setFuel(item));
    }

    /** Returns true if the given item is a recognised fuel. */
    public boolean isValidFuel(@Nullable ItemStack item) {
        return item != null && !item.getType().isAir() && fuels.containsKey(item.getType());
    }

    /**
     * Immediately completes the current recipe at the station the given player has open,
     * attributing the completion to {@code activatorUuid}. Used by active cooking abilities.
     */
    public void rushCook(Player player, UUID activatorUuid) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null || state.getActiveRecipe() == null) return;
        state.setLastInteractor(activatorUuid);
        finishCooking(location, state, state.getActiveRecipe());
    }

    private void tickAll() {
        for (Map.Entry<Location, CookingStationState> entry : new ArrayList<>(stations.entrySet())) {
            tickStation(entry.getKey(), entry.getValue());
        }
    }

    private void tickStation(Location location, CookingStationState state) {
        CookingRecipe recipe = state.getActiveRecipe();

        if (recipe == null) {
            if (state.getCookProgress() > 0) {
                state.setCookProgress(0);
                state.setEffectiveCookTime(0);
                refreshViewers(location, state);
            }
            return;
        }

        if (!hasOutputCapacity(state, recipe)) return;

        if (state.getEffectiveCookTime() == 0) {
            int baseTime = recipe.cookTime();
            if (state.getLastInteractor() != null) {
                CookingStartEvent startEvent = new CookingStartEvent(
                        location, recipe, state.getLastInteractor(), baseTime);
                plugin.getServer().getPluginManager().callEvent(startEvent);
                state.setEffectiveCookTime(Math.max(MIN_COOK_TIME, startEvent.getEffectiveCookTime()));
            } else {
                state.setEffectiveCookTime(baseTime);
            }
        }

        if (state.getFuelRemaining() <= 0) {
            if (!consumeFuel(state)) {
                if (state.getCookProgress() > 0) {
                    state.setCookProgress(0);
                    refreshViewers(location, state);
                }
                return;
            }
        }

        state.setFuelRemaining(state.getFuelRemaining() - TICK_INTERVAL);
        state.setCookProgress(state.getCookProgress() + TICK_INTERVAL);

        if (state.getCookProgress() >= state.getEffectiveCookTime()) {
            finishCooking(location, state, recipe);
        } else {
            refreshViewers(location, state);
        }
    }

    /**
     * Returns true if the station can accept at least one more output item from the given recipe.
     * A slot is usable if it is empty, or holds an item of the same material that isn't at max stack.
     */
    private boolean hasOutputCapacity(CookingStationState state, CookingRecipe recipe) {
        Material outputType = recipe.output().getType();
        int outputAmount = recipe.output().getAmount();
        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            ItemStack existing = state.getOutput(i);
            if (existing == null || existing.getType().isAir()) return true;
            if (existing.getType() == outputType
                    && existing.getAmount() + outputAmount <= existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeFuel(CookingStationState state) {
        ItemStack fuelItem = state.getFuel();
        if (fuelItem == null || fuelItem.getType().isAir()) return false;

        Integer burnTime = fuels.get(fuelItem.getType());
        if (burnTime == null) return false;

        if (fuelItem.getAmount() > 1) {
            fuelItem.setAmount(fuelItem.getAmount() - 1);
        } else {
            state.setFuel(null);
        }

        state.setFuelRemaining(state.getFuelRemaining() + burnTime);
        return true;
    }

    /**
     * Completes a cook cycle: fires {@link CookingCompleteEvent} to collect passive
     * contributions, rolls quality, places the formatted output, drops any bonus drop,
     * decrements matched ingredients, resets progress, and refreshes viewer GUIs.
     *
     * <p>Quality is always rolled, regardless of whether Sylphian-Skills is loaded.
     * If no interactor is recorded, level is treated as 0 and no passive shifts apply.</p>
     *
     * <p>Output slot selection: first slot with an identical stackable item; otherwise
     * the first empty slot; otherwise drop the item naturally at the station.</p>
     */
    private void finishCooking(Location location, CookingStationState state, CookingRecipe recipe) {
        CookingConfig cfg = config; // snapshot for consistent reads
        UUID interactor = state.getLastInteractor();

        // Collect passive contributions from Skills (or other listeners).
        EnumMap<CookingQuality, Double> qualityShifts = new EnumMap<>(CookingQuality.class);
        double xpMultiplier = 1.0;
        ItemStack bonusOutput = null;

        if (interactor != null) {
            CookingCompleteEvent completeEvent =
                    new CookingCompleteEvent(location, recipe, interactor);
            plugin.getServer().getPluginManager().callEvent(completeEvent);
            qualityShifts.putAll(completeEvent.getQualityShifts());
            xpMultiplier  = completeEvent.getXpMultiplier();
            bonusOutput   = completeEvent.getBonusOutput();
        }

        // Apply mastery bonus if the player has cooked this recipe enough times.
        if (interactor != null && masteryAccessor.getCount(interactor, recipe.id()) >= cfg.masteryThreshold()) {
            qualityShifts.merge(CookingQuality.PERFECT, cfg.masteryBonus(), Double::sum);
        }

        // Roll quality; always happens, Skills is not required.
        int level     = interactor != null ? levelProvider.apply(interactor) : 0;
        int slotCount = recipe.ingredients().size();
        CookingQuality quality = QualityRoller.roll(cfg.baseWeights(), level, slotCount, qualityShifts, cfg.levelBonus(), cfg.slotBonus());

        ItemStack outputItem = quality.applyTo(recipe.output(), cfg.formatFor(quality));
        state.setLastQuality(quality);
        placeOutput(location, state, outputItem);

        if (bonusOutput != null) {
            dropItem(location.clone().add(0.5, 0.5, 0.5), bonusOutput);
        }

        // Record this cook in mastery (cache + async DB write).
        if (interactor != null) {
            masteryAccessor.increment(interactor, recipe.id());
        }

        // Fire XP event so Skills (or any other listener) can award XP.
        if (interactor != null) {
            plugin.getServer().getPluginManager().callEvent(
                    new CookingXpEvent(interactor, recipe, quality, xpMultiplier));
        }

        // Decrement matched ingredients.
        boolean[] consumed = new boolean[CookingStationState.INGREDIENT_COUNT];
        for (var spec : recipe.ingredients()) {
            for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
                if (!consumed[i] && spec.matches(state.getIngredient(i))) {
                    ItemStack ing = state.getIngredient(i);
                    if (ing.getAmount() > 1) {
                        ing.setAmount(ing.getAmount() - 1);
                    } else {
                        state.setIngredient(i, null);
                    }
                    consumed[i] = true;
                    break;
                }
            }
        }

        state.setEffectiveCookTime(0);
        state.setCookProgress(0);
        updateActiveRecipe(state);
        refreshViewers(location, state);
    }

    /**
     * Places {@code outputItem} into the best available output slot.
     * Prefers a slot already holding an identical (isSimilar) item that isn't full.
     * Falls back to the first empty slot.
     * Drops naturally at the station if all slots are occupied.
     */
    private void placeOutput(Location location, CookingStationState state, ItemStack outputItem) {
        // Pass 1: find a slot with an identical stackable item.
        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            ItemStack existing = state.getOutput(i);
            if (existing != null && !existing.getType().isAir()
                    && existing.isSimilar(outputItem)
                    && existing.getAmount() + outputItem.getAmount() <= existing.getMaxStackSize()) {
                existing.setAmount(existing.getAmount() + outputItem.getAmount());
                return;
            }
        }
        // Pass 2: find an empty slot.
        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            ItemStack existing = state.getOutput(i);
            if (existing == null || existing.getType().isAir()) {
                state.setOutput(i, outputItem.clone());
                return;
            }
        }
        // All slots occupied: drop the item so it's not lost.
        dropItem(location.clone().add(0.5, 0.5, 0.5), outputItem);
    }

    private void updateActiveRecipe(CookingStationState state) {
        ItemStack[] snapshot = state.ingredientSnapshot();
        CookingRecipe matched = recipes.stream()
                .filter(r -> r.matches(snapshot))
                .findFirst()
                .orElse(null);

        if (!Objects.equals(matched, state.getActiveRecipe())) {
            state.setActiveRecipe(matched);
            state.setCookProgress(0);
            state.setEffectiveCookTime(0);
        }
    }

    private void refreshViewers(Location location, CookingStationState state) {
        for (UUID uuid : state.getViewers()) {
            Player viewer = plugin.getServer().getPlayer(uuid);
            if (viewer != null && viewer.isOnline()) {
                gui.update(viewer.getOpenInventory().getTopInventory(), state);
            }
        }
    }

    private void dropAllItems(Block block, CookingStationState state) {
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            dropItem(drop, state.getIngredient(i));
        }
        dropItem(drop, state.getFuel());
        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            dropItem(drop, state.getOutput(i));
        }
    }

    private void dropItem(Location location, @Nullable ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            Objects.requireNonNull(location.getWorld()).dropItemNaturally(location, item);
        }
    }

    private void mutateState(Player player, Consumer<CookingStationState> mutation) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null) return;
        state.setLastInteractor(player.getUniqueId());
        mutation.accept(state);
        refreshViewers(location, state);
    }

    /** Replaces the recipe list, fuel list, and config used for future ticks. */
    public void reload(List<CookingRecipe> newRecipes, Map<Material, Integer> newFuels, CookingConfig newConfig) {
        this.recipes = new ArrayList<>(newRecipes);
        this.fuels = new EnumMap<>(newFuels);
        this.config = newConfig;
        stations.values().forEach(this::updateActiveRecipe);
    }
}
