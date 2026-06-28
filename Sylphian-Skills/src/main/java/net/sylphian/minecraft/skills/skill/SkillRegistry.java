package net.sylphian.minecraft.skills.skill;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds all registered {@link Skill} instances.
 *
 * <p>Skills are stored in insertion order so iteration (e.g. for display)
 * is deterministic. Skills are registered and unregistered dynamically as
 * owning plugins enable and disable.</p>
 */
public final class SkillRegistry {

    private final Map<String, Skill> skills = new LinkedHashMap<>();

    /**
     * Registers a skill. The skill's ID must be unique.
     *
     * @param skill the skill to register
     * @throws IllegalArgumentException if a skill with the same ID is already registered
     */
    public void register(Skill skill) {
        if (skills.containsKey(skill.getId())) {
            throw new IllegalArgumentException("Skill already registered: " + skill.getId());
        }
        skills.put(skill.getId(), skill);
    }

    /**
     * Looks up a skill by its ID.
     *
     * @param id the skill identifier
     * @return the skill, or empty if not found
     */
    public Optional<Skill> get(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    /**
     * Removes a skill from the registry. Bukkit automatically unregisters the
     * skill's event listeners when the owning plugin unloads.
     *
     * @param skillId the skill identifier to remove
     */
    public void unregister(String skillId) {
        skills.remove(skillId);
    }

    /**
     * @return an unmodifiable view of all registered skills in insertion order
     */
    public Collection<Skill> all() {
        return Collections.unmodifiableCollection(skills.values());
    }
}
