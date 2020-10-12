package io.github.seggan.slimypowersuits.machines;

import io.github.seggan.slimypowersuits.SlimyPowerSuits;
import io.github.seggan.slimypowersuits.util.Utils;
import io.github.seggan.slimypowersuits.lists.MiscItems;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import jdk.nashorn.internal.objects.annotations.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.cscorelib2.item.CustomItem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.cscorelib2.inventory.ItemUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * This is a multiblock machine for crafting
 * Slimy Power Suits materials.
 * Heavily influenced by the Enhanced Crafting Table
 * and Magical Workbench
 *
 * @author NCBPFluffyBear
 */
public class ElementForge extends MultiBlockMachine {

    @Getter
    public static RecipeType getElementForgeRecipe() {
        return new RecipeType(
            new NamespacedKey(SlimyPowerSuits.getInstance(), "element_forge_recipe"),
            MiscItems.ELEMENT_FORGE,
            "&cElement Forge",
            "&7Used to create new elements"
        );
    }

    public ElementForge(Category category, SlimefunItemStack item) {
        super(category, item, new ItemStack[] {
            new ItemStack(Material.IRON_BLOCK), new CustomItem(Material.PISTON, "&fPiston &7(Facing Down)"),
            new ItemStack(Material.IRON_BLOCK),
            new ItemStack(Material.NETHER_BRICK_WALL), null, new ItemStack(Material.NETHER_BRICK_WALL),
            new CustomItem(Material.HOPPER, "&fHopper &7(Facing Inwards)"), new ItemStack(Material.SMITHING_TABLE),
            new ItemStack(Material.DISPENSER)
        }, BlockFace.UP);
    }

    @Override
    public void onInteract(Player p, Block b) {
        Block dispenser = locateDispenser(b);
        Block piston = b.getRelative(0, 2, 0);

        BlockState dispenserState = PaperLib.getBlockState(dispenser, false).getState();
        BlockData pistonData = piston.getBlockData();

        if (dispenserState instanceof Dispenser && pistonData instanceof Piston
            && ((Piston) pistonData).getFacing() == BlockFace.DOWN
            && piston.getRelative(0, -1, 0).getType() == Material.AIR) {
            Dispenser disp = (Dispenser) dispenserState;
            Piston piss = (Piston) pistonData;
            Inventory inv = disp.getInventory();
            List<ItemStack[]> inputs = RecipeType.getRecipeInputList(this);

            for (ItemStack[] input : inputs) {
                if (isCraftable(inv, input)) {
                    ItemStack output = RecipeType.getRecipeOutputList(this, input).clone();

                    if (Slimefun.hasUnlocked(p, output, true)) {
                        Inventory outputInv = findOutputInventory(output, dispenser, inv);

                        if (outputInv != null) {
                            craft(output, inv, outputInv);

                            movePiston(piston, piss, true);

                            Utils.runSync(() -> movePiston(piston, piss, false), 10L);

                        } else {
                            SlimefunPlugin.getLocalization().sendMessage(p, "machines.full-inventory", true);
                        }
                    }

                    return;
                }
            }

            SlimefunPlugin.getLocalization().sendMessage(p, "machines.pattern-not-found", true);
        }
    }

    private boolean isCraftable(Inventory inv, ItemStack[] recipe) {
        for (int j = 0; j < inv.getContents().length; j++) {
            if (!SlimefunUtils.isItemSimilar(inv.getContents()[j], recipe[j], true)) {
                return false;
            }
        }

        return true;
    }

    private void craft(ItemStack output, Inventory inv, Inventory outputInv) {
        for (int j = 0; j < 9; j++) {
            ItemStack item = inv.getContents()[j];

            if (item != null && item.getType() != Material.AIR) {
                ItemUtils.consumeItem(item, true);
            }
        }
        outputInv.addItem(output);
    }

    private Block locateDispenser(Block b) {

        if (b.getRelative(1, 0, 0).getType() == Material.DISPENSER) {
            return  b.getRelative(1, 0, 0);
        } else if (b.getRelative(0, 0, 1).getType() == Material.DISPENSER) {
            return  b.getRelative(0, 0, 1);
        } else if (b.getRelative(-1, 0, 0).getType() == Material.DISPENSER) {
            return  b.getRelative(-1, 0, 0);
        } else if (b.getRelative(0, 0, -1).getType() == Material.DISPENSER) {
            return  b.getRelative(0, 0, -1);
        }

        return null;
    }

    private void movePiston(Block piston, Piston piss, boolean extended) {
        piss.setExtended(extended);
        piston.setBlockData(piss, false);

        // Updating the Piston Head
        if (extended) {
            PistonHead head = (PistonHead) Material.PISTON_HEAD.createBlockData();
            head.setFacing(BlockFace.DOWN);

            piston.getRelative(BlockFace.DOWN).setBlockData(head, false);
            piston.getWorld().playSound(piston.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 0.5f, 0.5f);
        } else {
            piston.getRelative(BlockFace.DOWN).setType(Material.AIR);
        }
    }
}
