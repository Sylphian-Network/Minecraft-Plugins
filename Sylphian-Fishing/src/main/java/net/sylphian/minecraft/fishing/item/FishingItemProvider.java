package net.sylphian.minecraft.fishing.item;

import net.sylphian.minecraft.core.item.ItemProvider;
import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.fish.LootEntry;
import net.sylphian.minecraft.fishing.services.bait.BaitItem;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exposes Sylphian-Fishing items to the cross-plugin item registry.
 *
 * <p>Item IDs follow the convention {@code category/item-id}:</p>
 * <ul>
 *   <li>{@code bait/ocean_bait} — a bait item</li>
 *   <li>{@code fish/common_cod} — a fish item</li>
 * </ul>
 */
public class FishingItemProvider implements ItemProvider {

    private final SylphianFishing plugin;

    public FishingItemProvider(SylphianFishing plugin) {
        this.plugin = plugin;
    }

    @Override
    public String namespace() { return "sylphian-fishing"; }

    @Override
    public Optional<ItemStack> provide(String itemId) {
        int slash = itemId.indexOf('/');
        if (slash == -1) return Optional.empty();

        String category = itemId.substring(0, slash);
        String id       = itemId.substring(slash + 1);

        return switch (category) {
            case "bait" -> {
                BaitConfig bait = plugin.getBaitZoneService().getBaitConfig(id);
                yield bait != null
                        ? Optional.of(BaitItem.create(bait, plugin))
                        : Optional.empty();
            }
            case "fish" -> {
                Optional<LootEntry> entry = plugin.getLootService().getEntry(id);
                if (entry.isEmpty() || entry.get().externalItemId() != null) yield Optional.empty();
                yield Optional.of(plugin.getLootService().buildItemStack(entry.get()));
            }
            default -> Optional.empty();
        };
    }

    @Override
    public Set<String> itemIds() {
        Set<String> baits = plugin.getBaitZoneService().getBaitIds().stream()
                .map(id -> "bait/" + id)
                .collect(Collectors.toSet());
        Set<String> fish = plugin.getLootService().getItemEntryIds().stream()
                .map(id -> "fish/" + id)
                .collect(Collectors.toSet());
        return Stream.concat(baits.stream(), fish.stream()).collect(Collectors.toSet());
    }
}