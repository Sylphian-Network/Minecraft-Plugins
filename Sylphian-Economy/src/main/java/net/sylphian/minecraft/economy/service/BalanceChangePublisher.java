package net.sylphian.minecraft.economy.service;

import java.util.UUID;

/** Announces balance changes. */
@FunctionalInterface
public interface BalanceChangePublisher {

    /**
     * Announces that a player's balance has changed.
     *
     * @param playerId the affected player's UUID
     */
    void publish(UUID playerId);
}
