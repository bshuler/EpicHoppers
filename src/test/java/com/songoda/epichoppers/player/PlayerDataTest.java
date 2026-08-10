package com.songoda.epichoppers.player;

import com.songoda.epichoppers.api.hopper.Filter;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerDataTest {

    /**
     * Minimal stand-in Hopper - PlayerData only stores and returns whatever
     * is set as lastHopper, so a real EHopper (which needs a live
     * Location/Level/Filter to construct) is unnecessary machinery for this
     * test.
     */
    private static final Hopper STUB_HOPPER = new Hopper() {
        @Override public org.bukkit.block.Hopper getHopper() { return null; }
        @Override public void sync(Block toSync, boolean filtered, Player player) { }
        @Override public Location getLocation() { return null; }
        @Override public int getX() { return 0; }
        @Override public int getY() { return 0; }
        @Override public int getZ() { return 0; }
        @Override public Level getLevel() { return null; }
        @Override public UUID getPlacedBy() { return null; }
        @Override public UUID getLastPlayer() { return null; }
        @Override public void setLastPlayer(UUID uuid) { }
        @Override public Material getAutoCrafting() { return null; }
        @Override public void setAutoCrafting(Material autoCrafting) { }
        @Override public TeleportTrigger getTeleportTrigger() { return null; }
        @Override public void setTeleportTrigger(TeleportTrigger teleportTrigger) { }
        @Override public Block getSyncedBlock() { return null; }
        @Override public void setSyncedBlock(Block syncedBlock) { }
        @Override public Filter getFilter() { return null; }
    };

    @Test
    void defaultsMatchDeclaredInitialState() {
        UUID uuid = UUID.randomUUID();
        PlayerData data = new PlayerDataManager().getPlayerData(uuid);

        assertEquals(uuid, data.getPlayerUUID());
        assertNull(data.getLastHopper());
        assertEquals(MenuType.NOT_IN, data.getInMenu());
        assertNull(data.getSyncType());
        assertNull(data.getLastTeleport());
    }

    @Test
    void lastHopperRoundTrips() {
        PlayerData data = new PlayerDataManager().getPlayerData(UUID.randomUUID());
        data.setLastHopper(STUB_HOPPER);
        assertSame(STUB_HOPPER, data.getLastHopper());
    }

    @Test
    void inMenuRoundTrips() {
        PlayerData data = new PlayerDataManager().getPlayerData(UUID.randomUUID());
        data.setInMenu(MenuType.OVERVIEW);
        assertEquals(MenuType.OVERVIEW, data.getInMenu());
    }

    @Test
    void syncTypeRoundTrips() {
        PlayerData data = new PlayerDataManager().getPlayerData(UUID.randomUUID());
        data.setSyncType(SyncType.FILTERED);
        assertEquals(SyncType.FILTERED, data.getSyncType());
    }

    @Test
    void lastTeleportRoundTrips() {
        PlayerData data = new PlayerDataManager().getPlayerData(UUID.randomUUID());
        Date now = new Date();
        data.setLastTeleport(now);
        assertSame(now, data.getLastTeleport());
    }
}
