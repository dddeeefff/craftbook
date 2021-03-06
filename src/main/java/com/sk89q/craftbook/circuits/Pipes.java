package com.sk89q.craftbook.circuits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.PistonBaseMaterial;

import com.sk89q.craftbook.AbstractMechanic;
import com.sk89q.craftbook.AbstractMechanicFactory;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.LocalPlayer;
import com.sk89q.craftbook.bukkit.BukkitConfiguration;
import com.sk89q.craftbook.bukkit.CircuitCore;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.circuits.ic.ICMechanic;
import com.sk89q.craftbook.circuits.ic.PipeInputIC;
import com.sk89q.craftbook.util.InventoryUtil;
import com.sk89q.craftbook.util.ItemSyntax;
import com.sk89q.craftbook.util.ItemUtil;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.craftbook.util.RegexUtil;
import com.sk89q.craftbook.util.SignUtil;
import com.sk89q.craftbook.util.VerifyUtil;
import com.sk89q.craftbook.util.events.SourcedBlockRedstoneEvent;
import com.sk89q.craftbook.util.exceptions.InvalidMechanismException;
import com.sk89q.craftbook.util.exceptions.ProcessedMechanismException;
import com.sk89q.worldedit.BlockWorldVector;

public class Pipes extends AbstractMechanic {

    public static class Factory extends AbstractMechanicFactory<Pipes> {

        /**
         * Used to construct a pipe from a mechanic as an output method.
         *
         * @param pipe The pipe start block. Doesn't need to be a pipe.
         * @param source The block that the pipe is facing.
         * @param items The items to send to the pipe.
         * @return The pipe constructed, otherwise null.
         */
        public static Pipes setupPipes(Block pipe, Block source, ItemStack ... items) {

            if (pipe.getType() == Material.PISTON_STICKY_BASE) {

                PistonBaseMaterial p = (PistonBaseMaterial) pipe.getState().getData();
                Block fac = pipe.getRelative(p.getFacing());
                if (fac.getLocation().equals(source.getLocation()))
                    if (CircuitCore.inst().getPipeFactory() != null)
                        return CircuitCore.inst().getPipeFactory().detectWithItems(BukkitUtil.toWorldVector(pipe), Arrays.asList(items));
            }

            return null;
        }

        @Override
        public Pipes detect(BlockWorldVector pt) throws InvalidMechanismException {

            return detectWithItems(pt, null);
        }

        public Pipes detectWithItems(BlockWorldVector pt, List<ItemStack> items) {

            Material type = BukkitUtil.toWorld(pt).getBlockAt(BukkitUtil.toLocation(pt)).getType();

            if (type == Material.PISTON_STICKY_BASE) {

                ChangedSign sign = getSignOnPiston(BukkitUtil.toBlock(pt));

                if (CraftBookPlugin.inst().getConfiguration().pipeRequireSign && sign == null)
                    return null;

                return new Pipes(pt, sign == null ? null : sign, items);
            }

            return null;
        }

        @Override
        public Pipes detect(BlockWorldVector pos, LocalPlayer player, ChangedSign sign) throws InvalidMechanismException, ProcessedMechanismException {

            if(sign.getLine(1).equalsIgnoreCase("[Pipe]")) {
                player.checkPermission("craftbook.circuits.pipes");

                player.print("circuits.pipes.create");
                sign.setLine(1, "[Pipe]");

                throw new ProcessedMechanismException();
            }

            return null;
        }
    }

    public static ChangedSign getSignOnPiston(Block block) {

        PistonBaseMaterial piston = (PistonBaseMaterial) block.getState().getData();
        for(BlockFace face : LocationUtil.getDirectFaces()) {

            if(face == piston.getFacing() || !SignUtil.isSign(block.getRelative(face)))
                continue;
            if(block.getRelative(face).getType() != Material.SIGN_POST && (face == BlockFace.UP || face == BlockFace.DOWN))
                continue;
            else if (block.getRelative(face).getType() == Material.SIGN_POST && face != BlockFace.UP && face != BlockFace.DOWN)
                continue;
            if(block.getRelative(face).getType() != Material.SIGN_POST && !SignUtil.getBackBlock(block.getRelative(face)).getLocation().equals(block.getLocation()))
                continue;
            ChangedSign sign = BukkitUtil.toChangedSign(block.getRelative(face));
            if(sign != null && sign.getLine(1).equalsIgnoreCase("[Pipe]"))
                return sign;
        }

        return null;
    }

    /**
     * Construct the mechanic for a location.
     *
     * @param pt The location
     * @param sign The sign
     * @items The items to start with (Optional)
     */
    private Pipes(BlockWorldVector pt, ChangedSign sign, List<ItemStack> items) {

        if(items != null && !items.isEmpty()) {
            customInitialization = true;
            this.items.addAll(items);
            startPipe(BukkitUtil.toBlock(pt));
        }
    }

    private List<ItemStack> items = new ArrayList<ItemStack>();

    private boolean customInitialization = false;

    public List<ItemStack> getItems() {

        items.removeAll(Collections.singleton(null));
        return items;
    }

    public void searchNearbyPipes(Block block, Set<Location> visitedPipes, Set<ItemStack> filters, Set<ItemStack> exceptions) {

        BukkitConfiguration config = CraftBookPlugin.inst().getConfiguration();

        LinkedList<Block> searchQueue = new LinkedList<Block>();

        //Enumerate the search queue.
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {

                    if(items.isEmpty())
                        return;

                    if (!config.pipesDiagonal) {
                        if (x != 0 && y != 0) continue;
                        if (x != 0 && z != 0) continue;
                        if (y != 0 && z != 0) continue;
                    } else {

                        if (Math.abs(x) == Math.abs(y) && Math.abs(x) == Math.abs(z) && Math.abs(y) == Math.abs(z)) {
                            if (config.pipeInsulator.isSame(block.getRelative(x, 0, 0))
                                    && config.pipeInsulator.isSame(block.getRelative(0, y, 0))
                                    && config.pipeInsulator.isSame(block.getRelative(0, 0, z))) {
                                continue;
                            }
                        } else if (Math.abs(x) == Math.abs(y)) {
                            if (config.pipeInsulator.isSame(block.getRelative(x, 0, 0))
                                    && config.pipeInsulator.isSame(block.getRelative(0, y, 0))) {
                                continue;
                            }
                        } else if (Math.abs(x) == Math.abs(z)) {
                            if (config.pipeInsulator.isSame(block.getRelative(x, 0, 0))
                                    && config.pipeInsulator.isSame(block.getRelative(0, 0, z))) {
                                continue;
                            }
                        } else if (Math.abs(y) == Math.abs(z)) {
                            if (config.pipeInsulator.isSame(block.getRelative(0, y, 0))
                                    && config.pipeInsulator.isSame(block.getRelative(0, 0, z))) {
                                continue;
                            }
                        }
                    }

                    Block off = block.getRelative(x, y, z);

                    if (!isValidPipeBlock(off.getType())) continue;

                    if (visitedPipes.contains(off.getLocation())) continue;

                    visitedPipes.add(off.getLocation());

                    if(block.getType() == Material.STAINED_GLASS && off.getType() == Material.STAINED_GLASS && block.getData() != off.getData()) continue;

                    if(off.getType() == Material.GLASS || off.getType() == Material.STAINED_GLASS)
                        searchQueue.add(off);
                    else if(off.getType() == Material.PISTON_BASE)
                        searchQueue.add(0, off); //Pistons are treated with higher priority.
                }
            }
        }

        //Use the queue to search blocks.
        for(Block bl : searchQueue) {
            if (bl.getType() == Material.GLASS || bl.getType() == Material.STAINED_GLASS)
                searchNearbyPipes(bl, visitedPipes, filters, exceptions);
            else if (bl.getType() == Material.PISTON_BASE) {

                PistonBaseMaterial p = (PistonBaseMaterial) bl.getState().getData();

                ChangedSign sign = getSignOnPiston(bl);

                HashSet<ItemStack> pFilters = new HashSet<ItemStack>();
                HashSet<ItemStack> pExceptions = new HashSet<ItemStack>();

                if(sign != null) {

                    for(String line3 : RegexUtil.COMMA_PATTERN.split(sign.getLine(2))) {
                        pFilters.add(ItemSyntax.getItem(line3.trim()));
                    }
                    for(String line4 : RegexUtil.COMMA_PATTERN.split(sign.getLine(3))) {
                        pExceptions.add(ItemSyntax.getItem(line4.trim()));
                    }

                    pFilters.removeAll(Collections.singleton(null));
                    pExceptions.removeAll(Collections.singleton(null));
                }

                List<ItemStack> filteredItems = new ArrayList<ItemStack>(VerifyUtil.<ItemStack>withoutNulls(ItemUtil.filterItems(items, pFilters, pExceptions)));

                if(filteredItems.isEmpty())
                    continue;

                List<ItemStack> newItems = new ArrayList<ItemStack>();

                Block fac = bl.getRelative(p.getFacing());
                if (fac.getState() instanceof InventoryHolder) {

                    newItems.addAll(InventoryUtil.addItemsToInventory((InventoryHolder) fac.getState(), filteredItems.toArray(new ItemStack[filteredItems.size()])));

                } else if (fac.getType() == Material.WALL_SIGN) {

                    CircuitCore circuitCore = CircuitCore.inst();
                    if (circuitCore.getICFactory() == null) continue;

                    try {
                        ICMechanic icmech = circuitCore.getICFactory().detect(BukkitUtil.toWorldVector(fac));
                        if (icmech == null || !(icmech.getIC() instanceof PipeInputIC)) continue;
                        newItems.addAll(((PipeInputIC) icmech.getIC()).onPipeTransfer(BukkitUtil.toWorldVector(bl), filteredItems));
                    } catch (Exception e) {
                        BukkitUtil.printStacktrace(e);
                    }
                } else {

                    newItems.addAll(filteredItems);
                }

                items.removeAll(filteredItems);
                items.addAll(newItems);

                if (!items.isEmpty()) searchNearbyPipes(block, visitedPipes, filters, exceptions);
            }
        }
    }

    private boolean isValidPipeBlock(Material typeId) {

        return typeId == Material.GLASS || typeId == Material.STAINED_GLASS || typeId == Material.PISTON_BASE || typeId == Material.PISTON_STICKY_BASE || typeId == Material.WALL_SIGN;
    }

    public void startPipe(Block block) {

        Set<ItemStack> filters = new HashSet<ItemStack>();
        Set<ItemStack> exceptions = new HashSet<ItemStack>();

        ChangedSign sign = getSignOnPiston(block);

        if(sign != null) {

            for(String line3 : RegexUtil.COMMA_PATTERN.split(sign.getLine(2))) {

                filters.add(ItemSyntax.getItem(line3.trim()));
            }
            for(String line4 : RegexUtil.COMMA_PATTERN.split(sign.getLine(3))) {

                exceptions.add(ItemSyntax.getItem(line4.trim()));
            }
        }

        filters.removeAll(Collections.singleton(null));
        exceptions.removeAll(Collections.singleton(null));

        Set<Location> visitedPipes = new HashSet<Location>();

        if (block.getType() == Material.PISTON_STICKY_BASE) {

            List<ItemStack> leftovers = new ArrayList<ItemStack>();

            PistonBaseMaterial p = (PistonBaseMaterial) block.getState().getData();
            Block fac = block.getRelative(p.getFacing());
            if (fac.getType() == Material.CHEST || fac.getType() == Material.TRAPPED_CHEST || fac.getType() == Material.DROPPER || fac.getType() == Material.DISPENSER) {

                for (ItemStack stack : ((InventoryHolder) fac.getState()).getInventory().getContents()) {

                    if (!ItemUtil.isStackValid(stack))
                        continue;

                    if(!ItemUtil.doesItemPassFilters(stack, filters, exceptions))
                        continue;

                    items.add(stack);
                    ((InventoryHolder) fac.getState()).getInventory().removeItem(stack);
                    if (CraftBookPlugin.inst().getConfiguration().pipeStackPerPull)
                        break;
                }
                visitedPipes.add(fac.getLocation());
                searchNearbyPipes(block, visitedPipes, filters, exceptions);

                if (!items.isEmpty()) {
                    for (ItemStack item : items) {
                        if (item == null) continue;
                        leftovers.addAll(((InventoryHolder) fac.getState()).getInventory().addItem(item).values());
                    }
                }
            } else if (fac.getType() == Material.FURNACE || fac.getType() == Material.BURNING_FURNACE) {

                Furnace f = (Furnace) fac.getState();
                if(!ItemUtil.doesItemPassFilters(f.getInventory().getResult(), filters, exceptions))
                    return;
                items.add(f.getInventory().getResult());
                if (f.getInventory().getResult() != null) f.getInventory().setResult(null);
                visitedPipes.add(fac.getLocation());
                searchNearbyPipes(block, visitedPipes, filters, exceptions);

                if (!items.isEmpty()) {
                    for (ItemStack item : items) {
                        if (item == null) continue;
                        if(f.getInventory().getResult() == null)
                            f.getInventory().setResult(item);
                        else
                            leftovers.add(ItemUtil.addToStack(f.getInventory().getResult(), item));
                    }
                } else f.getInventory().setResult(null);
            } else if (!items.isEmpty()) {
                searchNearbyPipes(block, visitedPipes, filters, exceptions);
                if (!items.isEmpty() && !customInitialization) //IC's should handle their own leftovers.
                    for (ItemStack item : items) {
                        if (!ItemUtil.isStackValid(item)) continue;
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item);
                    }
            }

            if (!leftovers.isEmpty()) {
                for (ItemStack item : leftovers) {
                    if (!ItemUtil.isStackValid(item)) continue;
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item);
                }
            }
        }
    }

    @Override
    public void onBlockRedstoneChange(SourcedBlockRedstoneEvent event){
        startPipe(event.getBlock());
    }
}