package com.sk89q.craftbook.vehicles.cart;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.util.ItemInfo;
import com.sk89q.craftbook.util.SignUtil;
import com.sk89q.craftbook.util.exceptions.InvalidMechanismException;
import com.sk89q.worldedit.blocks.BlockType;

/**
 * <p>
 * Stores the tuple of three blocks over which any typical CartMechanism is implemented,
 * and also performs detection of two of the blocks if only one
 * is given.
 * </p>
 * <p>
 * Sign text and base block type are not validated by any constructors; this must be performed explicitly by calling
 * the appropriate methods after
 * construction. Selection of signs thus does not concern itself with sign text at all; but this is fine,
 * since if you have two signs in an area where
 * they could concievably contend for control of the mechanism, you would be doing something that ought be physically
 * impossible anyway (though yes,
 * it is possible if editing the world directly without physics).
 * </p>
 *
 * @author hash
 */
public class CartMechanismBlocks {

    /**
     * Declarative constructor. No arguments are validated in any way.
     *
     * @param rail the block containing the rails.
     * @param base the block on which the rails sit; the type of this block is what determines the mechanism type.
     * @param sign the block containing the sign that gives additional configuration to the mechanism,
     *             or null if not interested.
     */
    public CartMechanismBlocks(Block rail, Block base, Block sign) {

        this.rail = rail;
        this.base = base;
        this.sign = sign;
    }

    /**
     * Detecting factory; defers to one of the other three specific detecting factories based on whether the given
     * unknown block appears to be a sign,
     * rail, or base.
     *
     * @param unknown the block to examine.
     */
    public static CartMechanismBlocks find(Block unknown) throws InvalidMechanismException {

        final int ti = unknown.getTypeId();
        if (SignUtil.isSign(unknown)) return findBySign(unknown);
        else if (BlockType.isRailBlock(ti)) return findByRail(unknown);
        else return findByBase(unknown);
    }

    /**
     * <p>
     * Detecting factory, based on the position of the rails. The base must be one block below and the sign if it
     * exists must be two or three blocks
     * below. Signs are guaranteed to be signs (unless they're null) and rails are guaranteed to be rails.
     * </p>
     * <p>
     * This is the most important constructor, since it is the one invoked when processing cart move events.
     * </p>
     *
     * @param rail the block containing the rails.
     */
    public static CartMechanismBlocks findByRail(Block rail) throws InvalidMechanismException {

        if (!BlockType.isRailBlock(rail.getTypeId()))
            throw new InvalidMechanismException("rail argument must be a rail!");
        if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 2))) return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 2));
        else if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 3)))
            return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 3));
        else if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.EAST, 1)))
            return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.EAST, 1));
        else if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.WEST, 1)))
            return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.WEST, 1));
        else if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.NORTH, 1)))
            return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.NORTH, 1));
        else if (SignUtil.isSign(rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.SOUTH, 1)))
            return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), rail.getRelative(BlockFace.DOWN, 1).getRelative(BlockFace.SOUTH, 1));
        return new CartMechanismBlocks(rail, rail.getRelative(BlockFace.DOWN, 1), null);
    }

    /**
     * Detecting factory, based on the position of the base. The rails must be one block above and the sign if it
     * exists must be one or two blocks
     * below. Signs are guaranteed to be signs (unless they're null) and rails are guaranteed to be rails.
     *
     * @param base the block on which the rails sit; the type of this block is what determines the mechanism type.
     */
    public static CartMechanismBlocks findByBase(Block base) throws InvalidMechanismException {

        if (!BlockType.isRailBlock(base.getRelative(BlockFace.UP, 1).getTypeId()))
            throw new InvalidMechanismException("could not find rails.");
        if (SignUtil.isSign(base.getRelative(BlockFace.DOWN, 1)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.DOWN, 1));
        else if (SignUtil.isSign(base.getRelative(BlockFace.DOWN, 2)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.DOWN, 2));
        else if (SignUtil.isSign(base.getRelative(BlockFace.EAST, 1)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.EAST, 1));
        else if (SignUtil.isSign(base.getRelative(BlockFace.WEST, 1)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.WEST, 1));
        else if (SignUtil.isSign(base.getRelative(BlockFace.NORTH, 1)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.NORTH, 1));
        else if (SignUtil.isSign(base.getRelative(BlockFace.SOUTH, 1)))
            return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, base.getRelative(BlockFace.SOUTH, 1));
        return new CartMechanismBlocks(base.getRelative(BlockFace.UP, 1), base, null);
    }

    /**
     * Detecting factory, based on the position of the sign. The base must be one or two blocks above and the rails
     * an additional block above the
     * base. Signs are guaranteed to be signs and rails are guaranteed to be rails.
     *
     * @param sign the block containing the sign that gives additional configuration to the mechanism.
     */
    public static CartMechanismBlocks findBySign(Block sign) throws InvalidMechanismException {

        if (!SignUtil.isSign(sign)) throw new InvalidMechanismException("sign argument must be a sign!");
        if (BlockType.isRailBlock(sign.getRelative(BlockFace.UP, 2).getTypeId()))
            return new CartMechanismBlocks(sign.getRelative(BlockFace.UP, 2), sign.getRelative(BlockFace.UP, 1), sign);
        else if (BlockType.isRailBlock(sign.getRelative(BlockFace.UP, 3).getTypeId()))
            return new CartMechanismBlocks(sign.getRelative(BlockFace.UP, 3), sign.getRelative(BlockFace.UP, 2), sign);
        else if (BlockType.isRailBlock(sign.getRelative(SignUtil.getBack(sign), 1).getRelative(BlockFace.UP, 1).getTypeId()))
            return new CartMechanismBlocks(sign.getRelative(SignUtil.getBack(sign), 1).getRelative(BlockFace.UP, 1), sign.getRelative( SignUtil.getBack(sign), 1), sign);
        throw new InvalidMechanismException("could not find rails.");
    }

    public final Block rail;
    public final Block base;
    public final Block sign;
    public Block from;

    /**
     * This is a stupid but necessary thing since hash completely broke the ability to get the from location of the
     * move event from a mechanism
     * itself.
     */
    public void setFromBlock(Block block) {

        from = block;
    }

    /**
     * @param mechname
     *
     * @return true if the bracketed keyword on the sign matches the given mechname; false otherwise or if no sign.
     *
     * @throws ClassCastException if the declarative constructor was used in such a way that a non-sign block was
     *                            specified for a sign.
     */
    public boolean matches(String mechname) {

        return hasSign() && getSign().getLine(1).equalsIgnoreCase("[" + mechname + "]");
        // the astute will notice there's a problem coming up here with the one dang thing that had to go and break
        // the mold with second line definer.
    }

    /**
     * @param mat
     *
     * @return true if the base block is the same type as the given block.
     */
    public boolean matches(ItemInfo mat) {

        return base.getTypeId() == mat.getId() && base.getData() == mat.getData();
    }

    /**
     * @return a Sign BlockState, or null if there is no sign block.
     *
     * @throws ClassCastException if there a sign block is set, but it's not *actually* a sign block.
     */
    public ChangedSign getSign() {

        return !hasSign() ? null : BukkitUtil.toChangedSign(sign);
    }

    public boolean hasSign() {

        return sign != null && SignUtil.isSign(sign);
    }

    public boolean hasRail() {

        return rail != null;
    }

    public boolean hasBase() {

        return base != null;
    }
}