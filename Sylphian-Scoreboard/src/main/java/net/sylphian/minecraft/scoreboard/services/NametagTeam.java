package net.sylphian.minecraft.scoreboard.services;

import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds the state for a single nametag team — the prefix shown above a player's
 * head and the set of player name entries assigned to that team.
 */
public class NametagTeam {

    private Component prefix;
    private final Set<String> entries = new HashSet<>();

    /**
     * Constructs a new NametagTeam.
     *
     * @param prefix the Component prefix shown before the player's name
     */
    public NametagTeam(Component prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix Component shown before the player's name.
     *
     * @return the current prefix
     */
    public Component prefix() { return prefix; }

    /**
     * Returns the mutable set of player name entries assigned to this team.
     *
     * @return the entries set
     */
    public Set<String> entries() { return entries; }

    /**
     * Updates the prefix for this team.
     *
     * @param prefix the new prefix
     */
    public void setPrefix(Component prefix) { this.prefix = prefix; }
}