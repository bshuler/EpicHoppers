package com.songoda.epichoppers.player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataManagerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getPlayerDataByUuidCreatesOnFirstAccess() {
        PlayerDataManager manager = new PlayerDataManager();
        UUID uuid = UUID.randomUUID();

        PlayerData data = manager.getPlayerData(uuid);
        assertEquals(uuid, data.getPlayerUUID());
    }

    @Test
    void getPlayerDataByUuidReturnsTheSameInstanceOnRepeatedCalls() {
        PlayerDataManager manager = new PlayerDataManager();
        UUID uuid = UUID.randomUUID();

        PlayerData first = manager.getPlayerData(uuid);
        PlayerData second = manager.getPlayerData(uuid);
        assertSame(first, second);
    }

    @Test
    void getPlayerDataByUuidReturnsNullForNullUuid() {
        PlayerDataManager manager = new PlayerDataManager();
        assertNull(manager.getPlayerData((UUID) null));
    }

    @Test
    void getPlayerDataByPlayerDelegatesToUuidOverload() {
        PlayerDataManager manager = new PlayerDataManager();
        PlayerMock player = server.addPlayer();

        PlayerData data = manager.getPlayerData(player);
        assertEquals(player.getUniqueId(), data.getPlayerUUID());
        assertSame(data, manager.getPlayerData(player.getUniqueId()));
    }

    @Test
    void getRegisteredPlayersReflectsEveryCreatedPlayerData() {
        PlayerDataManager manager = new PlayerDataManager();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        manager.getPlayerData(a);
        manager.getPlayerData(b);

        assertEquals(2, manager.getRegisteredPlayers().size());
    }

    @Test
    void getRegisteredPlayersIsUnmodifiable() {
        PlayerDataManager manager = new PlayerDataManager();
        assertTrue(manager.getRegisteredPlayers().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> manager.getRegisteredPlayers().add(null));
    }
}
