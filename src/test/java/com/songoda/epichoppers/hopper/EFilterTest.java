package com.songoda.epichoppers.hopper;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain-JUnit coverage of {@link EFilter} - a pure POJO, no Bukkit runtime
 * required beyond the {@code Material}/{@code Block} types on the
 * classpath. This is one of the whitelist/blacklist/void filter-logic prime
 * targets called out in the modernization brief.
 */
class EFilterTest {

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
    void whiteListDefaultsToEmptyList() {
        EFilter filter = new EFilter();
        assertTrue(filter.getWhiteList().isEmpty());
    }

    @Test
    void blackListDefaultsToEmptyList() {
        EFilter filter = new EFilter();
        assertTrue(filter.getBlackList().isEmpty());
    }

    @Test
    void voidListDefaultsToEmptyList() {
        EFilter filter = new EFilter();
        assertTrue(filter.getVoidList().isEmpty());
    }

    @Test
    void whiteListNullIsCoercedToEmptyListOnGet() {
        EFilter filter = new EFilter();
        filter.setWhiteList(null);
        assertEquals(Collections.emptyList(), filter.getWhiteList());
    }

    @Test
    void blackListNullIsCoercedToEmptyListOnGet() {
        EFilter filter = new EFilter();
        filter.setBlackList(null);
        assertEquals(Collections.emptyList(), filter.getBlackList());
    }

    @Test
    void voidListNullIsCoercedToEmptyListOnGet() {
        EFilter filter = new EFilter();
        filter.setVoidList(null);
        assertEquals(Collections.emptyList(), filter.getVoidList());
    }

    @Test
    void whiteListRoundTrips() {
        EFilter filter = new EFilter();
        List<Material> list = Arrays.asList(Material.DIRT, Material.STONE);
        filter.setWhiteList(list);
        assertSame(list, filter.getWhiteList());
    }

    @Test
    void blackListRoundTrips() {
        EFilter filter = new EFilter();
        List<Material> list = Collections.singletonList(Material.LAVA_BUCKET);
        filter.setBlackList(list);
        assertSame(list, filter.getBlackList());
    }

    @Test
    void voidListRoundTrips() {
        EFilter filter = new EFilter();
        List<Material> list = Collections.singletonList(Material.BEDROCK);
        filter.setVoidList(list);
        assertSame(list, filter.getVoidList());
    }

    @Test
    void endPointDefaultsToNull() {
        EFilter filter = new EFilter();
        assertNull(filter.getEndPoint());
    }

    @Test
    void endPointRoundTrips() {
        EFilter filter = new EFilter();
        World world = server.addSimpleWorld("endpoint-world");
        Block block = world.getBlockAt(5, 6, 7);
        filter.setEndPoint(block);
        assertSame(block, filter.getEndPoint());
    }
}
