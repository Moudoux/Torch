package org.bukkit.craftbukkit.entity;

import net.minecraft.server.EntityFallingBlock;

import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;

public class CraftFallingBlock extends CraftEntity implements FallingBlock {

    public CraftFallingBlock(CraftServer server, EntityFallingBlock entity) {
        super(server, entity);
    }

    @Override
    public EntityFallingBlock getHandle() {
        return (EntityFallingBlock) entity;
    }

    @Override
    public String toString() {
        return "CraftFallingBlock";
    }

    public EntityType getType() {
        return EntityType.FALLING_BLOCK;
    }

    public Material getMaterial() {
        return Material.getMaterial(getBlockId());
    }

    public int getBlockId() {
        return CraftMagicNumbers.getId(getHandle().getBlock().getBlock());
    }

    public byte getBlockData() {
        return (byte) getHandle().getBlock().getBlock().toLegacyData(getHandle().getBlock());
    }

    public boolean getDropItem() {
        return getHandle().dropItem;
    }

    public void setDropItem(boolean drop) {
        getHandle().dropItem = drop;
    }

    @Override
    public boolean canHurtEntities() {
        return getHandle().hurtEntities;
    }

    @Override
    public void setHurtEntities(boolean hurtEntities) {
        getHandle().hurtEntities = hurtEntities;
    }

    @Override
    public void setTicksLived(int value) {
        super.setTicksLived(value);

        // Second field for EntityFallingBlock
        getHandle().ticksLived = value;
    }

    // Paper start
    @Override
    public org.bukkit.Location getSourceLoc() {
        return getOrigin();
    }
    // Paper end
}
