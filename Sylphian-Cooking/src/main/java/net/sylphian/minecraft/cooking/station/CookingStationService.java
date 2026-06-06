package net.sylphian.minecraft.cooking.station;

import net.sylphian.minecraft.cooking.gui.CookingStationGui;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

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
 */
public class CookingStationService {

    /** Ticks between each processing cycle (20 = once per second). */
    private static final int TICK_INTERVAL = 20;

    private final JavaPlugin plugin;
    private final CookingStationGui gui;

    private List<CookingRecipe> recipes;
    private Map<Material, Integer> fuels;

    /** Active stations keyed by their block's location. */
    private final Map<Location, CookingStationState> stations = new HashMap<>();

    /** Tracks which location each viewing player has open. */
    private final Map<UUID, Location> playerStationMap = new HashMap<>();

    private BukkitRunnable tickTask;

    /**
     * Constructs a new CookingStationService.
     *
     * @param plugin  the owning plugin
     * @param recipes all loaded cooking recipes
     * @param fuels   map of fuel material → burn time in ticks
     * @param gui     the GUI factory used to open and update station inventories
     */
    public CookingStationService(JavaPlugin plugin, List<CookingRecipe> recipes,
                                 Map<Material, Integer> fuels, CookingStationGui gui) {
        this.plugin = plugin;
        this.recipes = new ArrayList<>(recipes);
        this.fuels = new EnumMap<>(fuels);
        this.gui = gui;
    }

    /**
     * Starts the background tick loop. Call from {@link JavaPlugin#onEnable()}.
     */
    public void start() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        };
        tickTask.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    /**
     * Stops the tick loop and persists all active station states to PDC.
     * Call from {@link JavaPlugin#onDisable()}.
     */
    public void shutdown() {
        if (tickTask != null) tickTask.cancel();

        for (Map.Entry<Location, CookingStationState> entry : stations.entrySet()) {
            Block block = entry.getKey().getBlock();
            CookingStationPdc.save(block, entry.getValue());
        }
        stations.clear();
        playerStationMap.clear();
    }

    /**
     * Opens the cooking station GUI for the given player at the given block.
     * Loads PDC state if this is the first time the station is being accessed.
     *
     * @param player the player opening the station
     * @param block  the furnace or campfire block
     */
    public void openStation(Player player, Block block) {
        Location location = block.getLocation();
        CookingStationState state = stations.computeIfAbsent(location,
                loc -> CookingStationPdc.load(block));

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
     *
     * <p>This exists because deferred 1-tick syncs scheduled during click handling race
     * against {@code InventoryCloseEvent}: the close fires on the same tick as the click,
     * so a pending sync task may not have run yet when the state is persisted.</p>
     *
     * @param player the player closing the station
     * @param inv    the station's GUI inventory at close time
     */
    public void syncFromInventory(Player player, Inventory inv) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null) return;

        for (int i = 0; i < CookingStationGui.INGREDIENT_SLOTS.length; i++) {
            ItemStack item = inv.getItem(CookingStationGui.INGREDIENT_SLOTS[i]);
            state.setIngredient(i, (isPresent(item) && !CookingStationGui.isPlaceholder(item)) ? item.clone() : null);
        }

        ItemStack fuel = inv.getItem(CookingStationGui.FUEL_SLOT);
        state.setFuel((isPresent(fuel) && !CookingStationGui.isPlaceholder(fuel)) ? fuel.clone() : null);

        ItemStack output = inv.getItem(CookingStationGui.OUTPUT_SLOT);
        state.setOutput((isPresent(output) && !CookingStationGui.isPlaceholder(output)) ? output.clone() : null);

        updateActiveRecipe(state);
    }

    private static boolean isPresent(ItemStack item) {
        return item != null && !item.getType().isAir();
    }

    /**
     * Called when a player closes their cooking station GUI.
     * Saves state to PDC and evicts the station from memory if it is now idle.
     *
     * @param player the player who closed the GUI
     */
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

    /**
     * Returns the location of the station currently open for the given player,
     * or null if they have no station open.
     */
    public Location getOpenStationLocation(UUID uuid) {
        return playerStationMap.get(uuid);
    }

    /**
     * Returns the in-memory state of the station at the given location, or null.
     */
    public CookingStationState getState(Location location) {
        return stations.get(location);
    }

    /**
     * Handles the destruction of a cooking station block.
     * Closes any open GUIs, drops stored items at the block's location,
     * removes in-memory state, and clears PDC.
     *
     * @param block the block being destroyed
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
            if (!pdcState.isEmpty()) {
                dropAllItems(block, pdcState);
            }
        }

        CookingStationPdc.clear(block);
    }

    /**
     * Applies a change to an ingredient slot of the station currently open for the player.
     *
     * @param player        the player interacting
     * @param ingredientIdx ingredient slot index (0–4)
     * @param item          the new item (null to clear)
     */
    public void setIngredient(Player player, int ingredientIdx, ItemStack item) {
        mutateState(player, state -> {
            state.setIngredient(ingredientIdx, item);
            updateActiveRecipe(state);
        });
    }

    /**
     * Applies a change to the fuel slot of the station currently open for the player.
     */
    public void setFuel(Player player, ItemStack item) {
        mutateState(player, state -> state.setFuel(item));
    }

    /**
     * Clears the output slot of the station currently open for the player.
     */
    public void clearOutput(Player player) {
        mutateState(player, state -> state.setOutput(null));
    }

    /**
     * Returns true if the given item is a recognised fuel.
     */
    public boolean isValidFuel(ItemStack item) {
        return item != null && !item.getType().isAir() && fuels.containsKey(item.getType());
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
                refreshViewers(location, state);
            }
            return;
        }

        ItemStack currentOutput = state.getOutput();
        if (currentOutput != null && !currentOutput.getType().isAir()) {
            if (!currentOutput.isSimilar(recipe.output())
                    || currentOutput.getAmount() + recipe.output().getAmount()
                    > currentOutput.getMaxStackSize()) {
                return;
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

        if (state.getCookProgress() >= recipe.cookTime()) {
            finishCooking(location, state, recipe);
        } else {
            refreshViewers(location, state);
        }
    }

    /**
     * Consumes one unit of fuel from the fuel slot and adds its burn time.
     *
     * @return true if fuel was consumed
     */
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
     * Completes a cook cycle: produces output, decrements matched ingredients,
     * resets progress, and refreshes all viewer GUIs.
     */
    private void finishCooking(Location location, CookingStationState state, CookingRecipe recipe) {
        ItemStack existing = state.getOutput();
        if (existing == null || existing.getType().isAir()) {
            state.setOutput(recipe.output().clone());
        } else {
            existing.setAmount(existing.getAmount() + recipe.output().getAmount());
        }

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

        state.setCookProgress(0);
        updateActiveRecipe(state);
        refreshViewers(location, state);
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
        dropItem(drop, state.getOutput());
    }

    private void dropItem(Location location, ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            Objects.requireNonNull(location.getWorld()).dropItemNaturally(location, item);
        }
    }

    private void mutateState(Player player, Consumer<CookingStationState> mutation) {
        Location location = playerStationMap.get(player.getUniqueId());
        if (location == null) return;
        CookingStationState state = stations.get(location);
        if (state == null) return;
        mutation.accept(state);
    }

    /**
     * Replaces the recipe and fuel lists used for future ticks.
     *
     * @param newRecipes updated recipe list
     * @param newFuels   updated fuel map
     */
    public void reload(List<CookingRecipe> newRecipes, Map<Material, Integer> newFuels) {
        this.recipes = new ArrayList<>(newRecipes);
        this.fuels = new EnumMap<>(newFuels);
        stations.values().forEach(this::updateActiveRecipe);
    }
}
