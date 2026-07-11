package net.sylphian.minecraft.entities.config;

import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.entities.config.EntityProperties.PotionSpec;
import net.sylphian.minecraft.entities.util.EntityBuilder;
import net.sylphian.minecraft.entities.util.EquipmentEntry;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * An immutable entity definition parsed from {@code config.yml}. Each definition
 * is a recipe for a fresh entity; {@link #builder()} produces a configured
 * {@link EntityBuilder} whose {@code spawn} puts one instance into the world.
 *
 * @param id         the entity ID, unique within the provider namespace
 * @param type       the base vanilla entity type
 * @param name       the custom name, or null for none
 * @param health     the max health, or null to leave vanilla
 * @param damage     the attack damage, or null to leave vanilla
 * @param armor      the armor value, or null to leave vanilla
 * @param equipment  items to equip with optional drop chance, keyed by slot
 * @param properties the spawn-time behavioural properties
 */
public record EntityDefinition(
        String id,
        EntityType type,
        @Nullable Component name,
        @Nullable Double health,
        @Nullable Double damage,
        @Nullable Double armor,
        Map<EquipmentSlot, EquipmentEntry> equipment,
        EntityProperties properties) {

    /**
     * Builds a configured {@link EntityBuilder} for this definition. Each call
     * returns a fresh builder, so one definition can spawn many instances.
     *
     * @return the builder with every configured value applied
     */
    public EntityBuilder builder() {
        EntityBuilder builder = new EntityBuilder(type);

        if (name != null) builder.name(name).nameVisible(properties.nameVisible());
        if (health != null) builder.health(health);
        if (damage != null) builder.damage(damage);
        if (armor != null) builder.armor(armor);

        builder.glowing(properties.glowing())
                .baby(properties.baby())
                .persistent(properties.persistent());

        for (PotionSpec effect : properties.potionEffects()) {
            builder.potionEffect(effect.type(), effect.amplifier(), effect.durationTicks());
        }

        equipment.forEach((slot, entry) -> builder.equipment(slot, entry.item(), entry.dropChance()));
        return builder;
    }
}
