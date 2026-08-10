package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.handlers.EnchantmentHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writing this test surfaced a real, significant bug fixed alongside it:
 * the sync-touch target location used to be smuggled into the item's second
 * lore line via {@code Methods.toHiddenString} (a §-before-every-character
 * trick). On modern Paper/MockBukkit, {@code ItemMeta.setLore}/{@code
 * getLore} round-trip through Adventure legacy-component (de)serialization,
 * which silently consumes any character that is also a valid legacy color
 * code (0-9, a-f, k-o, r) - and a serialized "world;x;y;z" string is
 * guaranteed to contain several. The location came back mangled and
 * un-parseable on every real death (confirmed empirically: a real serialized
 * location like "world;0;0;0" round-tripped through lore as "wd;0;;"). Fixed
 * by storing the location in the item's PersistentDataContainer instead
 * (see {@code EnchantmentHandler.syncLocationKey()}), which is not subject
 * to that round-trip. Same class of bug, same fix, also applied to
 * {@code BlockListeners.handleSyncTouch}.
 */
class EntityListenersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private EntityListeners listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new EntityListeners(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EntityDeathEvent deathEvent(org.bukkit.entity.LivingEntity victim, List<ItemStack> drops) {
        DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        return new EntityDeathEvent(victim, source, drops);
    }

    @Test
    void onDropRoutesTheKillsDropsIntoTheSyncTouchChestInsteadOfDroppingThemAndForgetsTheEntity() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(0, 0, 0);
        chest.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_SWORD), chest);
        player.getInventory().setItemInMainHand(tool);

        Zombie zombie = world.spawn(world.getBlockAt(5, 0, 0).getLocation(), Zombie.class);
        zombie.setHealth(0.5);

        EntityDamageByEntityEvent damage = new EntityDamageByEntityEvent(player, zombie,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        listener.onDed(damage);

        List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Material.ROTTEN_FLESH, 2)));
        EntityDeathEvent death = deathEvent(zombie, drops);
        listener.onDrop(death);

        assertTrue(death.getDrops().isEmpty());
        org.bukkit.inventory.Inventory chestInventory =
                ((org.bukkit.inventory.InventoryHolder) chest.getState()).getInventory();
        assertEquals(Material.ROTTEN_FLESH, chestInventory.getItem(0).getType());
        assertEquals(2, chestInventory.getItem(0).getAmount());

        // Second death of a *different* entity must not still be tracked as
        // the same kill - onDed()/onDrop() key by entity UUID and onDrop()
        // must forget the entry once consumed.
        EntityDeathEvent secondDeath = deathEvent(zombie, new ArrayList<>(List.of(new ItemStack(Material.ROTTEN_FLESH, 1))));
        listener.onDrop(secondDeath);
        assertEquals(1, secondDeath.getDrops().size());
    }

    @Test
    void onDedIgnoresDamageNotDealtByAPlayer() {
        Zombie attacker = world.spawn(world.getBlockAt(0, 0, 0).getLocation(), Zombie.class);
        Zombie victim = world.spawn(world.getBlockAt(1, 0, 0).getLocation(), Zombie.class);
        victim.setHealth(0.5);

        EntityDamageByEntityEvent damage = new EntityDamageByEntityEvent(attacker, victim,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        listener.onDed(damage);

        EntityDeathEvent death = deathEvent(victim,
                new ArrayList<>(List.of(new ItemStack(Material.ROTTEN_FLESH, 1))));
        listener.onDrop(death);

        assertEquals(1, death.getDrops().size());
    }

    @Test
    void onDedIgnoresAPlayerNotHoldingASyncTouchTool() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

        Zombie zombie = world.spawn(world.getBlockAt(0, 0, 0).getLocation(), Zombie.class);
        zombie.setHealth(0.5);

        EntityDamageByEntityEvent damage = new EntityDamageByEntityEvent(player, zombie,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        listener.onDed(damage);

        EntityDeathEvent death = deathEvent(zombie,
                new ArrayList<>(List.of(new ItemStack(Material.ROTTEN_FLESH, 1))));
        listener.onDrop(death);

        assertEquals(1, death.getDrops().size());
    }

    @Test
    void onDedDoesNotTrackAHitThatDoesNotKillTheTarget() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(0, 0, 0);
        chest.setType(Material.CHEST);
        player.getInventory().setItemInMainHand(new EnchantmentHandler().createSyncTouch(
                new ItemStack(Material.DIAMOND_SWORD), chest));

        Zombie zombie = world.spawn(world.getBlockAt(5, 0, 0).getLocation(), Zombie.class);
        zombie.setHealth(20);

        EntityDamageByEntityEvent damage = new EntityDamageByEntityEvent(player, zombie,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1);
        listener.onDed(damage);

        EntityDeathEvent death = deathEvent(zombie,
                new ArrayList<>(List.of(new ItemStack(Material.ROTTEN_FLESH, 1))));
        listener.onDrop(death);

        assertEquals(1, death.getDrops().size());
    }
}
