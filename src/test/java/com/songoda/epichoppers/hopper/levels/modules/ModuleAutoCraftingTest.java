package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ModuleAutoCrafting#run(Hopper) is pure state-machine logic driven directly
 * by a repeating scheduler task in production, exactly like ModuleBlockBreak
 * and ModuleSuction; these tests drive it the same way, registering real
 * ShapedRecipes with ServerMock and stocking a real HOPPER block's inventory
 * (MockBukkit's HopperStateMock) with the ingredients.
 */
class ModuleAutoCraftingTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private ELevelManager levelManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        levelManager = (ELevelManager) plugin.getLevelManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EHopper hopperAt(Location loc) {
        world.getBlockAt(loc).setType(Material.HOPPER);
        return new EHopper(loc, levelManager.getLevel(1), null, null, null, new EFilter(),
                TeleportTrigger.DISABLED, null);
    }

    private void registerTorchRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "test_torch"), new ItemStack(Material.TORCH, 4))
                .shape("C", "S")
                .setIngredient('C', Material.COAL)
                .setIngredient('S', Material.STICK);
        server.addRecipe(recipe);
    }

    @Test
    void withNoAutoCraftingMaterialSetNothingHappens() {
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAt(loc);
        registerTorchRecipe();
        hopper.getHopper().getInventory().addItem(new ItemStack(Material.COAL), new ItemStack(Material.STICK));

        new ModuleAutoCrafting().run(hopper);

        assertFalse(hopper.getHopper().getInventory().contains(Material.TORCH));
    }

    @Test
    void craftsAndConsumesIngredientsWhenTheyAreAllPresent() {
        Location loc = new Location(world, 1, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        registerTorchRecipe();
        hopper.getHopper().getInventory().addItem(new ItemStack(Material.COAL), new ItemStack(Material.STICK));

        new ModuleAutoCrafting().run(hopper);

        assertTrue(hopper.getHopper().getInventory().contains(Material.TORCH));
        assertFalse(hopper.getHopper().getInventory().contains(Material.COAL));
        assertFalse(hopper.getHopper().getInventory().contains(Material.STICK));
    }

    @Test
    void missingAnIngredientMeansNoCraftAndNothingIsConsumed() {
        Location loc = new Location(world, 2, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        registerTorchRecipe();
        hopper.getHopper().getInventory().addItem(new ItemStack(Material.COAL));

        new ModuleAutoCrafting().run(hopper);

        assertFalse(hopper.getHopper().getInventory().contains(Material.TORCH));
        assertTrue(hopper.getHopper().getInventory().contains(Material.COAL));
    }

    @Test
    void shapelessRecipesForTheSameResultAreIgnored() {
        Location loc = new Location(world, 3, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        server.addRecipe(new org.bukkit.inventory.ShapelessRecipe(new NamespacedKey(plugin, "test_torch_shapeless"), new ItemStack(Material.TORCH, 4))
                .addIngredient(Material.COAL)
                .addIngredient(Material.STICK));
        hopper.getHopper().getInventory().addItem(new ItemStack(Material.COAL), new ItemStack(Material.STICK));

        new ModuleAutoCrafting().run(hopper);

        assertFalse(hopper.getHopper().getInventory().contains(Material.TORCH));
    }

    @Test
    void duplicateIngredientSlotsOfTheSameMaterialAreCombinedIntoOneRequiredAmount() {
        Location loc = new Location(world, 7, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        // two DIFFERENT ingredient slots that happen to both require coal -
        // stackItems() has to combine them into a single "need 2 coal"
        // entry rather than checking for 1 coal twice.
        server.addRecipe(new ShapedRecipe(new NamespacedKey(plugin, "test_torch_double_coal"), new ItemStack(Material.TORCH, 4))
                .shape("AB")
                .setIngredient('A', Material.COAL)
                .setIngredient('B', Material.COAL));
        hopper.getHopper().getInventory().addItem(new ItemStack(Material.COAL, 2));

        new ModuleAutoCrafting().run(hopper);

        assertTrue(hopper.getHopper().getInventory().contains(Material.TORCH));
        assertFalse(hopper.getHopper().getInventory().contains(Material.COAL));
    }

    @Test
    void aFullButRoomyMatchingHopperInventoryStillCrafts() {
        Location loc = new Location(world, 8, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        registerTorchRecipe();
        // Every slot is occupied (firstEmpty() == -1), but slot 4 already
        // holds a non-full TORCH stack that canMove()'s fallback loop can
        // still find room in, so crafting proceeds anyway.
        hopper.getHopper().getInventory().setItem(0, new ItemStack(Material.COAL));
        hopper.getHopper().getInventory().setItem(1, new ItemStack(Material.STICK));
        hopper.getHopper().getInventory().setItem(2, new ItemStack(Material.COBBLESTONE, 64));
        hopper.getHopper().getInventory().setItem(3, new ItemStack(Material.COBBLESTONE, 64));
        hopper.getHopper().getInventory().setItem(4, new ItemStack(Material.TORCH, 10));

        new ModuleAutoCrafting().run(hopper);

        assertFalse(hopper.getHopper().getInventory().contains(Material.COAL));
        assertFalse(hopper.getHopper().getInventory().contains(Material.STICK));
    }

    @Test
    void aFullNonMatchingHopperInventoryBlocksCraftingEvenWithIngredientsPresent() {
        Location loc = new Location(world, 4, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        registerTorchRecipe();
        ItemStack filler = new ItemStack(Material.COBBLESTONE, 64);
        for (int i = 0; i < hopper.getHopper().getInventory().getSize(); i++) {
            hopper.getHopper().getInventory().setItem(i, filler.clone());
        }

        new ModuleAutoCrafting().run(hopper);

        assertFalse(hopper.getHopper().getInventory().contains(Material.TORCH));
    }

    @Test
    void getBlockedItemsIsEmptyWithNoAutoCraftingMaterialSet() {
        Location loc = new Location(world, 5, 64, 0);
        EHopper hopper = hopperAt(loc);

        List<Material> blocked = new ModuleAutoCrafting().getBlockedItems(hopper);

        assertTrue(blocked.isEmpty());
    }

    @Test
    void getBlockedItemsListsTheCachedRecipesIngredients() {
        Location loc = new Location(world, 6, 64, 0);
        EHopper hopper = hopperAt(loc);
        hopper.setAutoCrafting(Material.TORCH);
        registerTorchRecipe();
        ModuleAutoCrafting module = new ModuleAutoCrafting();

        // first call caches the recipe but (per the production code's own
        // branching) returns an empty list; the second call reads from the
        // now-populated cache and returns the real ingredient materials.
        module.getBlockedItems(hopper);
        List<Material> blocked = module.getBlockedItems(hopper);

        assertTrue(blocked.contains(Material.COAL));
        assertTrue(blocked.contains(Material.STICK));
    }

    @Test
    void getNameReturnsAutoCrafting() {
        assertEquals("AutoCrafting", new ModuleAutoCrafting().getName());
    }

    @Test
    void getDescriptionReflectsEnabledState() {
        String description = new ModuleAutoCrafting().getDescription();
        assertEquals("§7AutoCrafting: §6true", description);
    }
}
