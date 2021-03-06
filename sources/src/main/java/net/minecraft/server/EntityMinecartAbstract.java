package net.minecraft.server;

import com.google.common.collect.Maps;
import com.koloboke.collect.map.hash.HashIntObjMaps;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;
// CraftBukkit end

public abstract class EntityMinecartAbstract extends Entity implements INamableTileEntity {

    private static final DataWatcherObject<Integer> a = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> b = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Float> c = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.c);
    private static final DataWatcherObject<Integer> d = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> e = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Boolean> f = DataWatcher.a(EntityMinecartAbstract.class, DataWatcherRegistry.h);
    private boolean g;
    private static final int[][][] h = new int[][][] { { { 0, 0, -1}, { 0, 0, 1}}, { { -1, 0, 0}, { 1, 0, 0}}, { { -1, -1, 0}, { 1, 0, 0}}, { { -1, 0, 0}, { 1, -1, 0}}, { { 0, 0, -1}, { 0, -1, 1}}, { { 0, -1, -1}, { 0, 0, 1}}, { { 0, 0, 1}, { 1, 0, 0}}, { { 0, 0, 1}, { -1, 0, 0}}, { { 0, 0, -1}, { -1, 0, 0}}, { { 0, 0, -1}, { 1, 0, 0}}};
    private int at;
    private double au;
    private double av;
    private double aw;
    private double ax;
    private double ay;

    // CraftBukkit start
    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public double maxSpeed = 0.4D;
    // CraftBukkit end

    public EntityMinecartAbstract(World world) {
        super(world);
        this.i = true;
        this.setSize(0.98F, 0.7F);
    }

    public static EntityMinecartAbstract a(World world, double d0, double d1, double d2, EntityMinecartAbstract.EnumMinecartType entityminecartabstract_enumminecarttype) {
        switch (entityminecartabstract_enumminecarttype) {
        case CHEST:
            return new EntityMinecartChest(world, d0, d1, d2);

        case FURNACE:
            return new EntityMinecartFurnace(world, d0, d1, d2);

        case TNT:
            return new EntityMinecartTNT(world, d0, d1, d2);

        case SPAWNER:
            return new EntityMinecartMobSpawner(world, d0, d1, d2);

        case HOPPER:
            return new EntityMinecartHopper(world, d0, d1, d2);

        case COMMAND_BLOCK:
            return new EntityMinecartCommandBlock(world, d0, d1, d2);

        default:
            return new EntityMinecartRideable(world, d0, d1, d2);
        }
    }

    @Override
	protected boolean playStepSound() {
        return false;
    }

    @Override
	protected void i() {
        this.datawatcher.register(EntityMinecartAbstract.a, Integer.valueOf(0));
        this.datawatcher.register(EntityMinecartAbstract.b, Integer.valueOf(1));
        this.datawatcher.register(EntityMinecartAbstract.c, Float.valueOf(0.0F));
        this.datawatcher.register(EntityMinecartAbstract.d, Integer.valueOf(0));
        this.datawatcher.register(EntityMinecartAbstract.e, Integer.valueOf(6));
        this.datawatcher.register(EntityMinecartAbstract.f, Boolean.valueOf(false));
    }

    @Override
	@Nullable
    public AxisAlignedBB j(Entity entity) {
        return entity.isCollidable() ? entity.getBoundingBox() : null;
    }

    @Override
	@Nullable
    public AxisAlignedBB ag() {
        return null;
    }

    @Override
	public boolean isCollidable() {
        return true;
    }

    public EntityMinecartAbstract(World world, double d0, double d1, double d2) {
        this(world);
        this.setPosition(d0, d1, d2);
        this.motX = 0.0D;
        this.motY = 0.0D;
        this.motZ = 0.0D;
        this.lastX = d0;
        this.lastY = d1;
        this.lastZ = d2;
    }

    @Override
	public double ay() {
        return 0.0D;
    }

    @Override
	public boolean damageEntity(DamageSource damagesource, float f) {
        if (!this.world.isClientSide && !this.dead) {
            if (this.isInvulnerable(damagesource)) {
                return false;
            } else {
                // CraftBukkit start - fire VehicleDamageEvent
                Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                org.bukkit.entity.Entity passenger = (damagesource.getEntity() == null) ? null : damagesource.getEntity().getBukkitEntity();

                VehicleDamageEvent event = new VehicleDamageEvent(vehicle, passenger, f);
                this.world.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return true;
                }

                f = (float) event.getDamage();
                // CraftBukkit end
                this.e(-this.u());
                this.d(10);
                this.ap();
                this.setDamage(this.getDamage() + f * 10.0F);
                boolean flag = damagesource.getEntity() instanceof EntityHuman && ((EntityHuman) damagesource.getEntity()).abilities.canInstantlyBuild;

                if (flag || this.getDamage() > 40.0F) {
                    // CraftBukkit start
                    VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
                    this.world.getServer().getPluginManager().callEvent(destroyEvent);

                    if (destroyEvent.isCancelled()) {
                        this.setDamage(40); // Maximize damage so this doesn't get triggered again right away
                        return true;
                    }
                    // CraftBukkit end
                    this.az();
                    if (flag && !this.hasCustomName()) {
                        this.die();
                    } else {
                        this.a(damagesource);
                    }
                }

                return true;
            }
        } else {
            return true;
        }
    }

    public void a(DamageSource damagesource) {
        this.die();
        if (this.world.getGameRules().getBoolean("doEntityDrops")) {
            ItemStack itemstack = new ItemStack(Items.MINECART, 1);

            if (this.hasCustomName()) {
                itemstack.g(this.getCustomName());
            }

            this.a(itemstack, 0.0F);
        }

    }

    @Override
	public boolean isInteractable() {
        return !this.dead;
    }

    @Override
	public EnumDirection bm() {
        return this.g ? this.getDirection().opposite().e() : this.getDirection().e();
    }

    @Override
	public void A_() {
        // CraftBukkit start
        double prevX = this.locX;
        double prevY = this.locY;
        double prevZ = this.locZ;
        float prevYaw = this.yaw;
        float prevPitch = this.pitch;
        // CraftBukkit end

        if (this.getType() > 0) {
            this.d(this.getType() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        // Paper start - Configurable nether ceiling damage
        // Extracted to own function
        /*
        if (this.locY < -64.0D) {
            this.Y();
        }
        */
        this.checkAndDoHeightDamage();
        // Paper end

        int i;

        if (!this.world.isClientSide && this.world instanceof WorldServer) {
            this.world.methodProfiler.a("portal");
            MinecraftServer minecraftserver = this.world.getMinecraftServer();

            i = this.V();
            if (this.ak) {
                if (true || minecraftserver.getAllowNether()) { // CraftBukkit - multi-world should still allow teleport even if default vanilla nether disabled
                    if (!this.isPassenger() && this.al++ >= i) {
                        this.al = i;
                        this.portalCooldown = this.aE();
                        byte b0;

                        if (this.world.worldProvider.getDimensionManager().getDimensionID() == -1) {
                            b0 = 0;
                        } else {
                            b0 = -1;
                        }

                        this.c(b0);
                    }

                    this.ak = false;
                }
            } else {
                if (this.al > 0) {
                    this.al -= 4;
                }

                if (this.al < 0) {
                    this.al = 0;
                }
            }

            if (this.portalCooldown > 0) {
                --this.portalCooldown;
            }

            
        }

        if (this.world.isClientSide) {
            if (this.at > 0) {
                double d0 = this.locX + (this.au - this.locX) / this.at;
                double d1 = this.locY + (this.av - this.locY) / this.at;
                double d2 = this.locZ + (this.aw - this.locZ) / this.at;
                double d3 = MathHelper.g(this.ax - this.yaw);

                this.yaw = (float) (this.yaw + d3 / this.at);
                this.pitch = (float) (this.pitch + (this.ay - this.pitch) / this.at);
                --this.at;
                this.setPosition(d0, d1, d2);
                this.setYawPitch(this.yaw, this.pitch);
            } else {
                this.setPosition(this.locX, this.locY, this.locZ);
                this.setYawPitch(this.yaw, this.pitch);
            }

        } else {
            this.lastX = this.locX;
            this.lastY = this.locY;
            this.lastZ = this.locZ;
            if (!this.isNoGravity()) {
                this.motY -= 0.03999999910593033D;
            }

            int j = MathHelper.floor(this.locX);

            i = MathHelper.floor(this.locY);
            int k = MathHelper.floor(this.locZ);

            if (BlockMinecartTrackAbstract.b(this.world, new BlockPosition(j, i - 1, k))) {
                --i;
            }

            BlockPosition blockposition = new BlockPosition(j, i, k);
            IBlockData iblockdata = this.world.getType(blockposition);

            if (BlockMinecartTrackAbstract.i(iblockdata)) {
                this.a(blockposition, iblockdata);
                if (iblockdata.getBlock() == Blocks.ACTIVATOR_RAIL) {
                    this.a(j, i, k, iblockdata.get(BlockPoweredRail.POWERED).booleanValue());
                }
            } else {
                this.q();
            }

            this.checkBlockCollisions();
            this.pitch = 0.0F;
            double d4 = this.lastX - this.locX;
            double d5 = this.lastZ - this.locZ;

            if (d4 * d4 + d5 * d5 > 0.001D) {
                this.yaw = (float) (MathHelper.c(d5, d4) * 180.0D / 3.141592653589793D);
                if (this.g) {
                    this.yaw += 180.0F;
                }
            }

            double d6 = MathHelper.g(this.yaw - this.lastYaw);

            if (d6 < -170.0D || d6 >= 170.0D) {
                this.yaw += 180.0F;
                this.g = !this.g;
            }

            this.setYawPitch(this.yaw, this.pitch);
            // CraftBukkit start
            org.bukkit.World bworld = this.world.getWorld();
            Location from = new Location(bworld, prevX, prevY, prevZ, prevYaw, prevPitch);
            Location to = new Location(bworld, this.locX, this.locY, this.locZ, this.yaw, this.pitch);
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();

            this.world.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleUpdateEvent(vehicle));

            if (!from.equals(to)) {
                this.world.getServer().getPluginManager().callEvent(new org.bukkit.event.vehicle.VehicleMoveEvent(vehicle, from, to));
            }
            // CraftBukkit end
            if (this.v() == EntityMinecartAbstract.EnumMinecartType.RIDEABLE && this.motX * this.motX + this.motZ * this.motZ > 0.01D) {
                List list = this.world.getEntities(this, this.getBoundingBox().grow(0.20000000298023224D, 0.0D, 0.20000000298023224D), IEntitySelector.a(this));

                if (!list.isEmpty()) {
                    for (int l = 0; l < list.size(); ++l) {
                        Entity entity = (Entity) list.get(l);

                        if (!(entity instanceof EntityHuman) && !(entity instanceof EntityIronGolem) && !(entity instanceof EntityMinecartAbstract) && !this.isVehicle() && !entity.isPassenger()) {
                            // CraftBukkit start
                            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, entity.getBukkitEntity());
                            this.world.getServer().getPluginManager().callEvent(collisionEvent);

                            if (collisionEvent.isCancelled()) {
                                continue;
                            }
                            // CraftBukkit end
                            entity.startRiding(this);
                        } else {
                            // CraftBukkit start
                            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, entity.getBukkitEntity());
                            this.world.getServer().getPluginManager().callEvent(collisionEvent);

                            if (collisionEvent.isCancelled()) {
                                continue;
                            }
                            // CraftBukkit end
                            entity.collide(this);
                        }
                    }
                }
            } else {
                Iterator iterator = this.world.getEntities(this, this.getBoundingBox().grow(0.20000000298023224D, 0.0D, 0.20000000298023224D)).iterator();

                while (iterator.hasNext()) {
                    Entity entity1 = (Entity) iterator.next();

                    if (!this.w(entity1) && entity1.isCollidable() && entity1 instanceof EntityMinecartAbstract) {
                        // CraftBukkit start
                        VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, entity1.getBukkitEntity());
                        this.world.getServer().getPluginManager().callEvent(collisionEvent);

                        if (collisionEvent.isCancelled()) {
                            continue;
                        }
                        // CraftBukkit end
                        entity1.collide(this);
                    }
                }
            }

            this.ak();
        }
    }

    protected double o() {
        return this.maxSpeed; // CraftBukkit
    }

    public void a(int i, int j, int k, boolean flag) {}

    protected void q() {
        double d0 = this.o();

        this.motX = MathHelper.a(this.motX, -d0, d0);
        this.motZ = MathHelper.a(this.motZ, -d0, d0);
        if (this.onGround) {
            // CraftBukkit start - replace magic numbers with our variables
            this.motX *= this.derailedX;
            this.motY *= this.derailedY;
            this.motZ *= this.derailedZ;
            // CraftBukkit end
        }

        this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
        if (!this.onGround) {
            // CraftBukkit start - replace magic numbers with our variables
            this.motX *= this.flyingX;
            this.motY *= this.flyingY;
            this.motZ *= this.flyingZ;
            // CraftBukkit end
        }

    }

    protected void a(BlockPosition blockposition, IBlockData iblockdata) {
        this.fallDistance = 0.0F;
        Vec3D vec3d = this.j(this.locX, this.locY, this.locZ);

        this.locY = blockposition.getY();
        boolean flag = false;
        boolean flag1 = false;
        BlockMinecartTrackAbstract blockminecarttrackabstract = (BlockMinecartTrackAbstract) iblockdata.getBlock();

        if (blockminecarttrackabstract == Blocks.GOLDEN_RAIL) {
            flag = iblockdata.get(BlockPoweredRail.POWERED).booleanValue();
            flag1 = !flag;
        }

        double d0 = 0.0078125D;
        BlockMinecartTrackAbstract.EnumTrackPosition blockminecarttrackabstract_enumtrackposition = iblockdata.get(blockminecarttrackabstract.g());

        switch (blockminecarttrackabstract_enumtrackposition) {
        case ASCENDING_EAST:
            this.motX -= 0.0078125D;
            ++this.locY;
            break;

        case ASCENDING_WEST:
            this.motX += 0.0078125D;
            ++this.locY;
            break;

        case ASCENDING_NORTH:
            this.motZ += 0.0078125D;
            ++this.locY;
            break;

        case ASCENDING_SOUTH:
            this.motZ -= 0.0078125D;
            ++this.locY;
        }

        int[][] aint = EntityMinecartAbstract.h[blockminecarttrackabstract_enumtrackposition.a()];
        double d1 = aint[1][0] - aint[0][0];
        double d2 = aint[1][2] - aint[0][2];
        double d3 = Math.sqrt(d1 * d1 + d2 * d2);
        double d4 = this.motX * d1 + this.motZ * d2;

        if (d4 < 0.0D) {
            d1 = -d1;
            d2 = -d2;
        }

        double d5 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);

        if (d5 > 2.0D) {
            d5 = 2.0D;
        }

        this.motX = d5 * d1 / d3;
        this.motZ = d5 * d2 / d3;
        Entity entity = this.bx().isEmpty() ? null : (Entity) this.bx().get(0);
        double d6;
        double d7;
        double d8;
        double d9;

        if (entity instanceof EntityLiving) {
            d6 = ((EntityLiving) entity).bf;
            if (d6 > 0.0D) {
                d7 = -Math.sin(entity.yaw * 0.017453292F);
                d8 = Math.cos(entity.yaw * 0.017453292F);
                d9 = this.motX * this.motX + this.motZ * this.motZ;
                if (d9 < 0.01D) {
                    this.motX += d7 * 0.1D;
                    this.motZ += d8 * 0.1D;
                    flag1 = false;
                }
            }
        }

        if (flag1) {
            d6 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);
            if (d6 < 0.03D) {
                this.motX *= 0.0D;
                this.motY *= 0.0D;
                this.motZ *= 0.0D;
            } else {
                this.motX *= 0.5D;
                this.motY *= 0.0D;
                this.motZ *= 0.5D;
            }
        }

        d6 = blockposition.getX() + 0.5D + aint[0][0] * 0.5D;
        d7 = blockposition.getZ() + 0.5D + aint[0][2] * 0.5D;
        d8 = blockposition.getX() + 0.5D + aint[1][0] * 0.5D;
        d9 = blockposition.getZ() + 0.5D + aint[1][2] * 0.5D;
        d1 = d8 - d6;
        d2 = d9 - d7;
        double d10;
        double d11;
        double d12;

        if (d1 == 0.0D) {
            this.locX = blockposition.getX() + 0.5D;
            d10 = this.locZ - blockposition.getZ();
        } else if (d2 == 0.0D) {
            this.locZ = blockposition.getZ() + 0.5D;
            d10 = this.locX - blockposition.getX();
        } else {
            d11 = this.locX - d6;
            d12 = this.locZ - d7;
            d10 = (d11 * d1 + d12 * d2) * 2.0D;
        }

        this.locX = d6 + d1 * d10;
        this.locZ = d7 + d2 * d10;
        this.setPosition(this.locX, this.locY, this.locZ);
        d11 = this.motX;
        d12 = this.motZ;
        if (this.isVehicle()) {
            d11 *= 0.75D;
            d12 *= 0.75D;
        }

        double d13 = this.o();

        d11 = MathHelper.a(d11, -d13, d13);
        d12 = MathHelper.a(d12, -d13, d13);
        this.move(EnumMoveType.SELF, d11, 0.0D, d12);
        if (aint[0][1] != 0 && MathHelper.floor(this.locX) - blockposition.getX() == aint[0][0] && MathHelper.floor(this.locZ) - blockposition.getZ() == aint[0][2]) {
            this.setPosition(this.locX, this.locY + aint[0][1], this.locZ);
        } else if (aint[1][1] != 0 && MathHelper.floor(this.locX) - blockposition.getX() == aint[1][0] && MathHelper.floor(this.locZ) - blockposition.getZ() == aint[1][2]) {
            this.setPosition(this.locX, this.locY + aint[1][1], this.locZ);
        }

        this.r();
        Vec3D vec3d1 = this.j(this.locX, this.locY, this.locZ);

        if (vec3d1 != null && vec3d != null) {
            double d14 = (vec3d.y - vec3d1.y) * 0.05D;

            d5 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);
            if (d5 > 0.0D) {
                this.motX = this.motX / d5 * (d5 + d14);
                this.motZ = this.motZ / d5 * (d5 + d14);
            }

            this.setPosition(this.locX, vec3d1.y, this.locZ);
        }

        int i = MathHelper.floor(this.locX);
        int j = MathHelper.floor(this.locZ);

        if (i != blockposition.getX() || j != blockposition.getZ()) {
            d5 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);
            this.motX = d5 * (i - blockposition.getX());
            this.motZ = d5 * (j - blockposition.getZ());
        }

        if (flag) {
            double d15 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);

            if (d15 > 0.01D) {
                double d16 = 0.06D;

                this.motX += this.motX / d15 * 0.06D;
                this.motZ += this.motZ / d15 * 0.06D;
            } else if (blockminecarttrackabstract_enumtrackposition == BlockMinecartTrackAbstract.EnumTrackPosition.EAST_WEST) {
                if (this.world.getType(blockposition.west()).m()) {
                    this.motX = 0.02D;
                } else if (this.world.getType(blockposition.east()).m()) {
                    this.motX = -0.02D;
                }
            } else if (blockminecarttrackabstract_enumtrackposition == BlockMinecartTrackAbstract.EnumTrackPosition.NORTH_SOUTH) {
                if (this.world.getType(blockposition.north()).m()) {
                    this.motZ = 0.02D;
                } else if (this.world.getType(blockposition.south()).m()) {
                    this.motZ = -0.02D;
                }
            }
        }

    }

    protected void r() {
        if (this.isVehicle() || !this.slowWhenEmpty) { // CraftBukkit - add !this.slowWhenEmpty
            this.motX *= 0.996999979019165D;
            this.motY *= 0.0D;
            this.motZ *= 0.996999979019165D;
        } else {
            this.motX *= 0.9599999785423279D;
            this.motY *= 0.0D;
            this.motZ *= 0.9599999785423279D;
        }

    }

    @Override
	public void setPosition(double d0, double d1, double d2) {
        this.locX = d0;
        this.locY = d1;
        this.locZ = d2;
        float f = this.width / 2.0F;
        float f1 = this.length;

        this.a(new AxisAlignedBB(d0 - f, d1, d2 - f, d0 + f, d1 + f1, d2 + f));
    }

    @Nullable
    public Vec3D j(double d0, double d1, double d2) {
        int i = MathHelper.floor(d0);
        int j = MathHelper.floor(d1);
        int k = MathHelper.floor(d2);

        if (BlockMinecartTrackAbstract.b(this.world, new BlockPosition(i, j - 1, k))) {
            --j;
        }

        IBlockData iblockdata = this.world.getType(new BlockPosition(i, j, k));

        if (BlockMinecartTrackAbstract.i(iblockdata)) {
            BlockMinecartTrackAbstract.EnumTrackPosition blockminecarttrackabstract_enumtrackposition = iblockdata.get(((BlockMinecartTrackAbstract) iblockdata.getBlock()).g());
            int[][] aint = EntityMinecartAbstract.h[blockminecarttrackabstract_enumtrackposition.a()];
            double d3 = i + 0.5D + aint[0][0] * 0.5D;
            double d4 = j + 0.0625D + aint[0][1] * 0.5D;
            double d5 = k + 0.5D + aint[0][2] * 0.5D;
            double d6 = i + 0.5D + aint[1][0] * 0.5D;
            double d7 = j + 0.0625D + aint[1][1] * 0.5D;
            double d8 = k + 0.5D + aint[1][2] * 0.5D;
            double d9 = d6 - d3;
            double d10 = (d7 - d4) * 2.0D;
            double d11 = d8 - d5;
            double d12;

            if (d9 == 0.0D) {
                d12 = d2 - k;
            } else if (d11 == 0.0D) {
                d12 = d0 - i;
            } else {
                double d13 = d0 - d3;
                double d14 = d2 - d5;

                d12 = (d13 * d9 + d14 * d11) * 2.0D;
            }

            d0 = d3 + d9 * d12;
            d1 = d4 + d10 * d12;
            d2 = d5 + d11 * d12;
            if (d10 < 0.0D) {
                ++d1;
            }

            if (d10 > 0.0D) {
                d1 += 0.5D;
            }

            return new Vec3D(d0, d1, d2);
        } else {
            return null;
        }
    }

    public static void a(DataConverterManager dataconvertermanager, Class<?> oclass) {}

    @Override
	protected void a(NBTTagCompound nbttagcompound) {
        if (nbttagcompound.getBoolean("CustomDisplayTile")) {
            Block block;

            if (nbttagcompound.hasKeyOfType("DisplayTile", 8)) {
                block = Block.getByName(nbttagcompound.getString("DisplayTile"));
            } else {
                block = Block.getById(nbttagcompound.getInt("DisplayTile"));
            }

            int i = nbttagcompound.getInt("DisplayData");

            this.setDisplayBlock(block == null ? Blocks.AIR.getBlockData() : block.fromLegacyData(i));
            this.setDisplayBlockOffset(nbttagcompound.getInt("DisplayOffset"));
        }

    }

    @Override
	protected void b(NBTTagCompound nbttagcompound) {
        if (this.A()) {
            nbttagcompound.setBoolean("CustomDisplayTile", true);
            IBlockData iblockdata = this.getDisplayBlock();
            MinecraftKey minecraftkey = Block.REGISTRY.b(iblockdata.getBlock());

            nbttagcompound.setString("DisplayTile", minecraftkey == null ? "" : minecraftkey.toString());
            nbttagcompound.setInt("DisplayData", iblockdata.getBlock().toLegacyData(iblockdata));
            nbttagcompound.setInt("DisplayOffset", this.getDisplayBlockOffset());
        }

    }

    @Override
	public void collide(Entity entity) {
        if (!this.world.isClientSide) {
            if (!entity.noclip && !this.noclip) {
                if (!this.w(entity)) {
                    double d0 = entity.locX - this.locX;
                    double d1 = entity.locZ - this.locZ;
                    double d2 = d0 * d0 + d1 * d1;

                    if (d2 >= 9.999999747378752E-5D) {
                        d2 = MathHelper.sqrt(d2);
                        d0 /= d2;
                        d1 /= d2;
                        double d3 = 1.0D / d2;

                        if (d3 > 1.0D) {
                            d3 = 1.0D;
                        }

                        d0 *= d3;
                        d1 *= d3;
                        d0 *= 0.10000000149011612D;
                        d1 *= 0.10000000149011612D;
                        d0 *= 1.0F - this.R;
                        d1 *= 1.0F - this.R;
                        d0 *= 0.5D;
                        d1 *= 0.5D;
                        if (entity instanceof EntityMinecartAbstract) {
                            double d4 = entity.locX - this.locX;
                            double d5 = entity.locZ - this.locZ;
                            Vec3D vec3d = (new Vec3D(d4, 0.0D, d5)).a();
                            Vec3D vec3d1 = (new Vec3D(MathHelper.cos(this.yaw * 0.017453292F), 0.0D, MathHelper.sin(this.yaw * 0.017453292F))).a();
                            double d6 = Math.abs(vec3d.b(vec3d1));

                            if (d6 < 0.800000011920929D) {
                                return;
                            }

                            double d7 = entity.motX + this.motX;
                            double d8 = entity.motZ + this.motZ;

                            if (((EntityMinecartAbstract) entity).v() == EntityMinecartAbstract.EnumMinecartType.FURNACE && this.v() != EntityMinecartAbstract.EnumMinecartType.FURNACE) {
                                this.motX *= 0.20000000298023224D;
                                this.motZ *= 0.20000000298023224D;
                                this.f(entity.motX - d0, 0.0D, entity.motZ - d1);
                                entity.motX *= 0.949999988079071D;
                                entity.motZ *= 0.949999988079071D;
                            } else if (((EntityMinecartAbstract) entity).v() != EntityMinecartAbstract.EnumMinecartType.FURNACE && this.v() == EntityMinecartAbstract.EnumMinecartType.FURNACE) {
                                entity.motX *= 0.20000000298023224D;
                                entity.motZ *= 0.20000000298023224D;
                                entity.f(this.motX + d0, 0.0D, this.motZ + d1);
                                this.motX *= 0.949999988079071D;
                                this.motZ *= 0.949999988079071D;
                            } else {
                                d7 /= 2.0D;
                                d8 /= 2.0D;
                                this.motX *= 0.20000000298023224D;
                                this.motZ *= 0.20000000298023224D;
                                this.f(d7 - d0, 0.0D, d8 - d1);
                                entity.motX *= 0.20000000298023224D;
                                entity.motZ *= 0.20000000298023224D;
                                entity.f(d7 + d0, 0.0D, d8 + d1);
                            }
                        } else {
                            this.f(-d0, 0.0D, -d1);
                            entity.f(d0 / 4.0D, 0.0D, d1 / 4.0D);
                        }
                    }

                }
            }
        }
    }

    public void setDamage(float f) {
        this.datawatcher.set(EntityMinecartAbstract.c, Float.valueOf(f));
    }

    public float getDamage() {
        return this.datawatcher.get(EntityMinecartAbstract.c).floatValue();
    }

    public void d(int i) {
        this.datawatcher.set(EntityMinecartAbstract.a, Integer.valueOf(i));
    }

    public int getType() {
        return this.datawatcher.get(EntityMinecartAbstract.a).intValue();
    }

    public void e(int i) {
        this.datawatcher.set(EntityMinecartAbstract.b, Integer.valueOf(i));
    }

    public int u() {
        return this.datawatcher.get(EntityMinecartAbstract.b).intValue();
    }

    public abstract EntityMinecartAbstract.EnumMinecartType v();

    public IBlockData getDisplayBlock() {
        return !this.A() ? this.x() : Block.getByCombinedId(this.getDataWatcher().get(EntityMinecartAbstract.d).intValue());
    }

    public IBlockData x() {
        return Blocks.AIR.getBlockData();
    }

    public int getDisplayBlockOffset() {
        return !this.A() ? this.z() : this.getDataWatcher().get(EntityMinecartAbstract.e).intValue();
    }

    public int z() {
        return 6;
    }

    public void setDisplayBlock(IBlockData iblockdata) {
        this.getDataWatcher().set(EntityMinecartAbstract.d, Integer.valueOf(Block.getCombinedId(iblockdata)));
        this.a(true);
    }

    public void setDisplayBlockOffset(int i) {
        this.getDataWatcher().set(EntityMinecartAbstract.e, Integer.valueOf(i));
        this.a(true);
    }

    public boolean A() {
        return this.getDataWatcher().get(EntityMinecartAbstract.f).booleanValue();
    }

    public void a(boolean flag) {
        this.getDataWatcher().set(EntityMinecartAbstract.f, Boolean.valueOf(flag));
    }

    public static enum EnumMinecartType {

        RIDEABLE(0, "MinecartRideable"), CHEST(1, "MinecartChest"), FURNACE(2, "MinecartFurnace"), TNT(3, "MinecartTNT"), SPAWNER(4, "MinecartSpawner"), HOPPER(5, "MinecartHopper"), COMMAND_BLOCK(6, "MinecartCommandBlock");

        private static final Map<Integer, EntityMinecartAbstract.EnumMinecartType> h = HashIntObjMaps.newMutableMap();
        private final int i;
        private final String j;

        private EnumMinecartType(int i, String s) {
            this.i = i;
            this.j = s;
        }

        public int a() {
            return this.i;
        }

        public String b() {
            return this.j;
        }

        static {
            EntityMinecartAbstract.EnumMinecartType[] aentityminecartabstract_enumminecarttype = values();
            int i = aentityminecartabstract_enumminecarttype.length;

            for (int j = 0; j < i; ++j) {
                EntityMinecartAbstract.EnumMinecartType entityminecartabstract_enumminecarttype = aentityminecartabstract_enumminecarttype[j];

                EntityMinecartAbstract.EnumMinecartType.h.put(Integer.valueOf(entityminecartabstract_enumminecarttype.a()), entityminecartabstract_enumminecarttype);
            }

        }
    }

    // CraftBukkit start - Methods for getting and setting flying and derailed velocity modifiers
    public Vector getFlyingVelocityMod() {
        return new Vector(flyingX, flyingY, flyingZ);
    }

    public void setFlyingVelocityMod(Vector flying) {
        flyingX = flying.getX();
        flyingY = flying.getY();
        flyingZ = flying.getZ();
    }

    public Vector getDerailedVelocityMod() {
        return new Vector(derailedX, derailedY, derailedZ);
    }

    public void setDerailedVelocityMod(Vector derailed) {
        derailedX = derailed.getX();
        derailedY = derailed.getY();
        derailedZ = derailed.getZ();
    }
    // CraftBukkit end
}
