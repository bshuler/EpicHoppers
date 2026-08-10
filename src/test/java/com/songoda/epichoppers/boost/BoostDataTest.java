package com.songoda.epichoppers.boost;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BoostDataTest {

    @Test
    void gettersReturnConstructorValues() {
        UUID player = UUID.randomUUID();
        BoostData data = new BoostData(2, 1_000L, player);

        assertEquals(2, data.getMultiplier());
        assertEquals(1_000L, data.getEndTime());
        assertEquals(player, data.getPlayer());
    }

    @Test
    void equalsAndHashCodeAreBasedOnAllThreeFields() {
        UUID player = UUID.randomUUID();
        BoostData a = new BoostData(2, 1_000L, player);
        BoostData b = new BoostData(2, 1_000L, player);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsIsTrueForSameInstance() {
        BoostData a = new BoostData(1, 1L, UUID.randomUUID());
        assertEquals(a, a);
    }

    @Test
    void equalsIsFalseForNonBoostDataObject() {
        BoostData a = new BoostData(1, 1L, UUID.randomUUID());
        assertNotEquals(a, "not a BoostData");
    }

    @Test
    void equalsIsFalseWhenMultiplierDiffers() {
        UUID player = UUID.randomUUID();
        BoostData a = new BoostData(2, 1_000L, player);
        BoostData b = new BoostData(3, 1_000L, player);
        assertNotEquals(a, b);
    }

    @Test
    void equalsIsFalseWhenEndTimeDiffers() {
        UUID player = UUID.randomUUID();
        BoostData a = new BoostData(2, 1_000L, player);
        BoostData b = new BoostData(2, 2_000L, player);
        assertNotEquals(a, b);
    }

    @Test
    void equalsIsFalseWhenPlayerDiffers() {
        BoostData a = new BoostData(2, 1_000L, UUID.randomUUID());
        BoostData b = new BoostData(2, 1_000L, UUID.randomUUID());
        assertNotEquals(a, b);
    }

    @Test
    void equalsAndHashCodeToleratePlayerBeingNull() {
        BoostData a = new BoostData(2, 1_000L, null);
        BoostData b = new BoostData(2, 1_000L, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
