package com.songoda.epichoppers.boost;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoostManagerTest {

    @Test
    void getBoostReturnsNullForUnregisteredPlayer() {
        BoostManager manager = new BoostManager();
        assertNull(manager.getBoost(UUID.randomUUID()));
    }

    @Test
    void getBoostReturnsNullForNullPlayer() {
        BoostManager manager = new BoostManager();
        assertNull(manager.getBoost(null));
    }

    @Test
    void addedBoostIsReturnedByGetBoostWhileStillActive() {
        BoostManager manager = new BoostManager();
        UUID player = UUID.randomUUID();
        BoostData data = new BoostData(2, System.currentTimeMillis() + 60_000, player);

        manager.addBoostToPlayer(data);

        assertSame(data, manager.getBoost(player));
        assertTrue(manager.getBoosts().contains(data));
    }

    @Test
    void expiredBoostIsAutoRemovedOnLookupButStillReturnedOnce() {
        BoostManager manager = new BoostManager();
        UUID player = UUID.randomUUID();
        // endTime already in the past.
        BoostData expired = new BoostData(2, System.currentTimeMillis() - 1_000, player);

        manager.addBoostToPlayer(expired);

        // getBoost still hands back the expired BoostData this one time...
        BoostData result = manager.getBoost(player);
        assertNotNull(result);
        assertSame(expired, result);

        // ...but has removed it from the registry as a side effect, so a
        // second lookup for the same player now finds nothing.
        assertFalse(manager.getBoosts().contains(expired));
        assertNull(manager.getBoost(player));
    }

    @Test
    void removeBoostFromPlayerRemovesIt() {
        BoostManager manager = new BoostManager();
        UUID player = UUID.randomUUID();
        BoostData data = new BoostData(1, System.currentTimeMillis() + 60_000, player);

        manager.addBoostToPlayer(data);
        manager.removeBoostFromPlayer(data);

        assertNull(manager.getBoost(player));
        assertTrue(manager.getBoosts().isEmpty());
    }

    @Test
    void getBoostsIsUnmodifiable() {
        BoostManager manager = new BoostManager();
        assertTrue(manager.getBoosts().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> manager.getBoosts().add(new BoostData(1, 1L, UUID.randomUUID())));
    }
}
