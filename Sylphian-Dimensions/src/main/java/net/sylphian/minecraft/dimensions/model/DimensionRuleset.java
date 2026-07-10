package net.sylphian.minecraft.dimensions.model;

/**
 * The rules enforced inside a dimension. Every field is set per dimension in
 * config; missing keys fall back to {@link #DEFAULTS}.
 *
 * <p>{@code claimingEnabled} is only enforced when Sylphian-Clans is present.</p>
 *
 * @param pvpEnabled       whether players can damage each other
 * @param buildingEnabled  whether players can place and break blocks
 * @param naturalSpawning  whether vanilla environmental creature spawning is allowed; false blocks natural, patrol, raid, reinforcement, jockey, mount, trap, nether-portal, and slime-split spawns while leaving player- and plugin-initiated spawns untouched
 * @param claimingEnabled  whether clans can claim chunks here
 * @param damageEnabled    whether players can take damage at all; false makes death impossible
 * @param keepInventory    whether players keep their inventory and experience on death
 * @param loginRedirect    whether players who log out here are sent to the hub on their next join
 * @param deathLossChance  chance per gathered item to be lost on death, 0.0 to 1.0
 */
public record DimensionRuleset(
        boolean pvpEnabled,
        boolean buildingEnabled,
        boolean naturalSpawning,
        boolean claimingEnabled,
        boolean damageEnabled,
        boolean keepInventory,
        boolean loginRedirect,
        double deathLossChance) {

    /** Fallback values for keys missing from a dimension's config block. */
    public static final DimensionRuleset DEFAULTS =
            new DimensionRuleset(false, false, true, false, true, true, false, 0.0);

    /**
     * Returns a one-line MiniMessage summary of every rule, for admin tooling.
     *
     * @return the MiniMessage rule summary
     */
    public String describe() {
        return "pvp " + flag(pvpEnabled)
                + "<gray>, building " + flag(buildingEnabled)
                + "<gray>, natural-spawning " + flag(naturalSpawning)
                + "<gray>, claiming " + flag(claimingEnabled)
                + "<gray>, damage " + flag(damageEnabled)
                + "<gray>, keep-inventory " + flag(keepInventory)
                + "<gray>, login-redirect " + flag(loginRedirect)
                + "<gray>, death-loss <white>" + deathLossChance;
    }

    private static String flag(boolean enabled) {
        return enabled ? "<green>on" : "<red>off";
    }
}
