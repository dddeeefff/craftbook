// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.craftbook.circuits.gates.world.entity;

import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.ICVerificationException;
import com.sk89q.craftbook.circuits.ic.RestrictedIC;
import com.sk89q.craftbook.util.EntityUtil;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.craftbook.util.RegexUtil;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;

public class CreatureSpawner extends AbstractIC {

    EntityType type;
    String data;
    int amount;

    public CreatureSpawner(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public void load() {

        type = EntityType.fromName(getSign().getLine(2).trim().toLowerCase(Locale.ENGLISH));
        if (type == null) {
            type = EntityType.PIG;
        }
        String line = getSign().getLine(3).trim();
        // parse the amount or rider type
        try {
            String[] entityInf = RegexUtil.ASTERISK_PATTERN.split(line, 2);
            data = entityInf[0];
            amount = Integer.parseInt(entityInf[1]);
        } catch (Exception e) {
            amount = 1;
            data = line;
        }
    }

    @Override
    public String getTitle() {

        return "Creature Spawner";
    }

    @Override
    public String getSignTitle() {

        return "CREATURE SPAWNER";
    }

    @Override
    public void trigger(ChipState chip) {

        Block center = getBackBlock();

        if(!center.getChunk().isLoaded())
            return;

        if (chip.getInput(0)) if (center.getRelative(0, 1, 0).getTypeId() == BlockID.MOB_SPAWNER) {

            org.bukkit.block.CreatureSpawner sp = (org.bukkit.block.CreatureSpawner) center.getRelative(0, 1,
                    0).getState();
            sp.setCreatureTypeByName(type.getName());
            sp.update();
        } else {
            Location loc = LocationUtil.getCenterOfBlock(LocationUtil.getNextFreeSpace(center, BlockFace.UP));
            // spawn amount of mobs
            for (int i = 0; i < amount; i++) {
                Entity entity = loc.getWorld().spawn(loc, type.getEntityClass());
                if(entity instanceof Skeleton)
                    ((Skeleton) entity).getEquipment().setItemInHand(new ItemStack(ItemID.BOW, 1));
                EntityUtil.setEntityData(entity, data);
            }
        }
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new CreatureSpawner(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Spawns a mob with specified data.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {"entitytype", "+odata*amount"};
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {

            if (EntityType.fromName(sign.getLine(2).trim().toLowerCase(Locale.ENGLISH)) == null) {
                throw new ICVerificationException("Invalid Entity! See bukkit EntityType list!");
            } else if (!EntityType.fromName(sign.getLine(2).trim().toLowerCase(Locale.ENGLISH)).isSpawnable()) {
                throw new ICVerificationException("Entity is not spawnable!");
            }
        }
    }
}
