package io.fabianbuthere.simplecars.entity.custom;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.item.custom.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BaseCarEntity extends Mob {
    private int capacity = 1;
    private Vec3[] seatOffsets = {
            new Vec3(0.0, 0.0, 0.0)
    };
    private Map<String, Double> fluidTank = new HashMap<>();
    private double fuelConsumptionRate = 0.05;

    private double carDrag = 0.07;
    private double carAcceleration = 0.07;
    private double carMaxSpeed = 0.75;
    private double carBrakeStrength = 0.15;
    private float carSteeringFactor = 8.0F;
    private double carMaximumUpwardDrive = 0.75;
    private float weight = 60.0F;
    private String licensePlateText = "ABC-0123";
    private Vec3 licensePlateOffset = new Vec3(0, 0.5, -2.1);

    private AbstractEngineItem engineItem;
    private AbstractFrameItem frameItem;
    private AbstractFuelTankItem fuelTankItem;
    private AbstractWheelItem[] wheelItems;

    private static final Map<String, Double> FLUID_TO_FUEL_PER_MB = new HashMap<>();

    static {
        FLUID_TO_FUEL_PER_MB.put(Fluids.LAVA.getFluidType().getDescriptionId(), 1.0);
        FLUID_TO_FUEL_PER_MB.put("fluid.tfmg.gasoline", 0.8);
        FLUID_TO_FUEL_PER_MB.put("fluid.tfmg.diesel", 0.6);
    }

    private final Map<Integer, Integer> recentCollisions = new HashMap<>();

    private static final EntityDataAccessor<Float> FORWARD_VELOCITY =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<String> ENGINE_ID =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FRAME_ID =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FUEL_TANK_ID =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> WHEEL_IDS =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SYNCED_CAR_DRAG =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_ACCEL =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_MAX_SPEED =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_BRAKE =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_STEER =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_UPWARD_DRIVE =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNCED_CAR_WEIGHT =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);

    private float pendingSteerInput = 0.0f;
    private float preSteerBlend = 0.0f;
    private float lastPendingSteer = 0.0f;
    private static final float PRESTEER_BLEND_TIME = 0.18f; // seconds

    public BaseCarEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setHealth(this.getMaxHealth());
        // Ensure wheelItems is initialized
        this.wheelItems = new AbstractWheelItem[4];
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0);
    }

    public double getCarDrag() { return carDrag; }
    public double getCarAcceleration() { return carAcceleration; }
    public double getCarMaxSpeed() { return carMaxSpeed; }
    public double getCarBrakeStrength() { return carBrakeStrength; }
    public float getCarSteeringFactor() { return carSteeringFactor; }
    public double getCarMaximumUpwardDrive() { return carMaximumUpwardDrive; }
    public String getLicensePlateText() { return licensePlateText; }
    public Vec3 getBackLicensePlateOffset() { return licensePlateOffset; }

    public int getCapacity() {
        return capacity;
    }

    public Vec3[] getSeatOffsets() {
        return seatOffsets;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Capacity", this.capacity);
        pCompound.put("SeatOffsets", this.serializeSeatOffsets());

        CompoundTag carPartsTag = new CompoundTag();
        if (this.engineItem != null) carPartsTag.put("engine", engineItem.serializeToTag());
        if (this.frameItem != null) carPartsTag.put("frame", frameItem.serializeToTag());
        if (this.fuelTankItem != null) carPartsTag.put("fuel_tank", fuelTankItem.serializeToTag());
        if (this.wheelItems != null) {
            ListTag wheelsList = new ListTag();
            for (AbstractWheelItem wheel : wheelItems) {
                if (wheel != null) {
                    wheelsList.add(wheel.serializeToTag());
                }
            }
            carPartsTag.put("wheels", wheelsList);
        }
        pCompound.put("CarParts", carPartsTag);

        pCompound.putDouble("CarDrag", this.carDrag);
        pCompound.putDouble("CarAcceleration", this.carAcceleration);
        pCompound.putDouble("CarMaxSpeed", this.carMaxSpeed);
        pCompound.putDouble("CarBrakeStrength", this.carBrakeStrength);
        pCompound.putFloat("CarSteeringFactor", this.carSteeringFactor);
        pCompound.putDouble("CarMaximumUpwardDrive", this.carMaximumUpwardDrive);
        pCompound.putDouble("CarFuelConsumptionRate", this.fuelConsumptionRate);

        pCompound.put("FluidTank", this.serializeFluidTank());

        if (!this.level().isClientSide) {
            pCompound.putFloat("ForwardVelocity", (float) this.getSyncedForwardVelocity());
        }
    }

    private CompoundTag serializeFluidTank() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Double> entry : fluidTank.entrySet()) {
            tag.putDouble(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    private Map<String, Double> deserializeFluidTank(CompoundTag tag) {
        Map<String, Double> tank = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            tank.put(key, tag.getDouble(key));
        }
        return tank;
    }

    private ListTag serializeSeatOffsets() {
        ListTag seatOffsetsList = new ListTag();
        for (Vec3 offset : seatOffsets) {
            CompoundTag offsetTag = new CompoundTag();
            offsetTag.putDouble("x", offset.x);
            offsetTag.putDouble("y", offset.y);
            offsetTag.putDouble("z", offset.z);
            seatOffsetsList.add(offsetTag);
        }
        return seatOffsetsList;
    }

    private Vec3[] deserializeSeatOffsets(ListTag seatOffsetsList) {
        Vec3[] offsets = new Vec3[seatOffsetsList.size()];
        for (int i = 0; i < seatOffsetsList.size(); i++) {
            CompoundTag offsetTag = seatOffsetsList.getCompound(i);
            offsets[i] = new Vec3(
                    offsetTag.getDouble("x"),
                    offsetTag.getDouble("y"),
                    offsetTag.getDouble("z")
            );
        }
        return offsets;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("Capacity")) {
            this.capacity = pCompound.getInt("Capacity");
        } else {
            this.capacity = 1;
        }
        if (pCompound.contains("SeatOffsets", Tag.TAG_LIST)) {
            ListTag seatOffsetsList = pCompound.getList("SeatOffsets", Tag.TAG_COMPOUND);
            this.seatOffsets = deserializeSeatOffsets(seatOffsetsList);
        } else {
            this.seatOffsets = new Vec3[]{new Vec3(0.0, 0.0, 0.0)};
        }

        // Ensure seatOffsets matches capacity
        if (this.seatOffsets.length != this.capacity) {
            Vec3[] adjusted = new Vec3[this.capacity];
            for (int i = 0; i < this.capacity; i++) {
                adjusted[i] = (i < this.seatOffsets.length) ? this.seatOffsets[i] : new Vec3(0.0, 0.0, 0.0);
            }
            this.seatOffsets = adjusted;
        }

        if (pCompound.contains("FluidTank", Tag.TAG_COMPOUND)) {
            this.fluidTank = deserializeFluidTank(pCompound.getCompound("FluidTank"));
        } else {
            this.fluidTank.clear();
        }

        if (pCompound.contains("CarDrag")) this.carDrag = pCompound.getDouble("CarDrag");
        if (pCompound.contains("CarAcceleration")) this.carAcceleration = pCompound.getDouble("CarAcceleration");
        if (pCompound.contains("CarMaxSpeed")) this.carMaxSpeed = pCompound.getDouble("CarMaxSpeed");
        if (pCompound.contains("CarBrakeStrength")) this.carBrakeStrength = pCompound.getDouble("CarBrakeStrength");
        if (pCompound.contains("CarSteeringFactor")) this.carSteeringFactor = pCompound.getFloat("CarSteeringFactor");
        if (pCompound.contains("CarMaximumUpwardDrive")) this.carMaximumUpwardDrive = pCompound.getDouble("CarMaximumUpwardDrive");
        if (pCompound.contains("CarFuelConsumptionRate")) this.fuelConsumptionRate = pCompound.getDouble("CarFuelConsumptionRate");

        if (pCompound.contains("CarParts", Tag.TAG_COMPOUND)) {
            CompoundTag carPartsTag = pCompound.getCompound("CarParts");
            if (carPartsTag.contains("engine", Tag.TAG_COMPOUND)) {
                Item item = AbstractCarPartItem.getItemFromTag(carPartsTag.getCompound("engine"));
                this.engineItem = (item instanceof AbstractEngineItem) ? (AbstractEngineItem) item : null;
            }
            if (carPartsTag.contains("frame", Tag.TAG_COMPOUND)) {
                Item item = AbstractCarPartItem.getItemFromTag(carPartsTag.getCompound("frame"));
                this.frameItem = (item instanceof AbstractFrameItem) ? (AbstractFrameItem) item : null;
            }
            if (carPartsTag.contains("fuel_tank", Tag.TAG_COMPOUND)) {
                Item item = AbstractCarPartItem.getItemFromTag(carPartsTag.getCompound("fuel_tank"));
                this.fuelTankItem = (item instanceof AbstractFuelTankItem) ? (AbstractFuelTankItem) item : null;
                SimplecarsMod.LOGGER.info("BaseCarEntity loaded fuel tank: {}", this.fuelTankItem != null ? this.fuelTankItem.getDescriptionId() : "null");
            }
            if (carPartsTag.contains("wheels", Tag.TAG_LIST)) {
                ListTag wheelsList = carPartsTag.getList("wheels", Tag.TAG_COMPOUND);
                this.wheelItems = new AbstractWheelItem[wheelsList.size()];
                for (int i = 0; i < wheelsList.size(); i++) {
                    Item item = AbstractCarPartItem.getItemFromTag(wheelsList.getCompound(i));
                    this.wheelItems[i] = (item instanceof AbstractWheelItem) ? (AbstractWheelItem) item : null;
                }
            }
        }

        // If frameItem is set, ensure wheelItems is of correct length
        if (this.frameItem != null) {
            int req = this.frameItem.getRequiredWheels();
            if (wheelItems == null || wheelItems.length != req) {
                AbstractWheelItem[] newWheels = new AbstractWheelItem[req];
                if (this.wheelItems != null)
                    System.arraycopy(this.wheelItems, 0, newWheels, 0, Math.min(this.wheelItems.length, req));
                this.wheelItems = newWheels;
            }
        }

        if (pCompound.contains("ForwardVelocity")) {
            this.setSyncedForwardVelocity(pCompound.getFloat("ForwardVelocity"));
        }

        updateCarWeight();
        updateCarStatsFromParts(); // <-- always recalculate stats from parts after loading
        syncCarData();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FORWARD_VELOCITY, 0.0F);
        this.entityData.define(ENGINE_ID, "");
        this.entityData.define(FRAME_ID, "");
        this.entityData.define(FUEL_TANK_ID, "");
        this.entityData.define(WHEEL_IDS, "");
        this.entityData.define(SYNCED_CAR_DRAG, 0.07F);
        this.entityData.define(SYNCED_CAR_ACCEL, 0.07F);
        this.entityData.define(SYNCED_CAR_MAX_SPEED, 0.75F);
        this.entityData.define(SYNCED_CAR_BRAKE, 0.15F);
        this.entityData.define(SYNCED_CAR_STEER, 8.0F);
        this.entityData.define(SYNCED_CAR_UPWARD_DRIVE, 0.75F);
        this.entityData.define(SYNCED_CAR_WEIGHT, 60.0F);
    }

    private double getSyncedForwardVelocity() {
        return (double) this.entityData.get(FORWARD_VELOCITY);
    }
    private void setSyncedForwardVelocity(double value) {
        this.entityData.set(FORWARD_VELOCITY, (float) value);
    }

    public double getCarForwardVelocity() {
        return getSyncedForwardVelocity();
    }
    private void updateCarForwardVelocity(double value) {
        setSyncedForwardVelocity(value);
    }

    private void syncCarData() {
        this.entityData.set(ENGINE_ID, this.engineItem != null ? this.engineItem.getDescriptionId() : "");
        this.entityData.set(FRAME_ID, this.frameItem != null ? this.frameItem.getDescriptionId() : "");
        this.entityData.set(FUEL_TANK_ID, this.fuelTankItem != null ? this.fuelTankItem.getDescriptionId() : "");
        if (this.wheelItems != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < wheelItems.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(wheelItems[i] != null ? wheelItems[i].getDescriptionId() : "");
            }
            this.entityData.set(WHEEL_IDS, sb.toString());
        } else {
            this.entityData.set(WHEEL_IDS, "");
        }
        this.entityData.set(SYNCED_CAR_DRAG, (float)this.carDrag);
        this.entityData.set(SYNCED_CAR_ACCEL, (float)this.carAcceleration);
        this.entityData.set(SYNCED_CAR_MAX_SPEED, (float)this.carMaxSpeed);
        this.entityData.set(SYNCED_CAR_BRAKE, (float)this.carBrakeStrength);
        this.entityData.set(SYNCED_CAR_STEER, this.carSteeringFactor);
        this.entityData.set(SYNCED_CAR_UPWARD_DRIVE, (float)this.carMaximumUpwardDrive);
        this.entityData.set(SYNCED_CAR_WEIGHT, this.weight);
    }

    public String getEngineId() {
        return this.entityData.get(ENGINE_ID);
    }
    public String getFrameId() {
        return this.entityData.get(FRAME_ID);
    }
    public String getFuelTankId() {
        return this.entityData.get(FUEL_TANK_ID);
    }
    public String[] getWheelIds() {
        String s = this.entityData.get(WHEEL_IDS);
        return s.isEmpty() ? new String[0] : s.split(",");
    }
    public float getSyncedCarDrag() {
        return this.entityData.get(SYNCED_CAR_DRAG);
    }
    public float getSyncedCarAcceleration() {
        return this.entityData.get(SYNCED_CAR_ACCEL);
    }
    public float getSyncedCarMaxSpeed() {
        return this.entityData.get(SYNCED_CAR_MAX_SPEED);
    }
    public float getSyncedCarBrakeStrength() {
        return this.entityData.get(SYNCED_CAR_BRAKE);
    }
    public float getSyncedCarSteeringFactor() {
        return this.entityData.get(SYNCED_CAR_STEER);
    }
    public float getSyncedCarMaximumUpwardDrive() {
        return this.entityData.get(SYNCED_CAR_UPWARD_DRIVE);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource) || isDeadOrDying() || this.level().isClientSide || pSource.is(DamageTypes.CACTUS)) return false;

        Vec3 sourcePos = pSource.getSourcePosition();

        if (sourcePos != null) {
            Vec3 knockbackVec = this.position().subtract(sourcePos).normalize().scale(0.1);
            this.setDeltaMovement(this.getDeltaMovement().add(knockbackVec.x, 0, knockbackVec.z));
        }

        if (pSource.getEntity() instanceof LivingEntity attacker) {
            SimplecarsMod.LOGGER.info("BaseCarEntity hurt by: {}, isBaseCarEntity={}", attacker.getName().getString(), attacker instanceof BaseCarEntity);
            if (!attacker.getMainHandItem().is(Items.NETHERITE_PICKAXE) && !(attacker instanceof BaseCarEntity)) return false;
        }

        float newHealth = this.getHealth() - pAmount;
        this.setHealth(newHealth);
        this.markHurt();

        if (newHealth <= 0) {
            this.die(pSource);
        }

        return true;
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        double length = 1.5;
        double width = 1.5;
        double height = 1.0;

        double halfWidth = width / 2.0;
        double halfLength = length / 2.0;

        return new AABB(
                x - halfWidth,
                y,
                z - halfLength,
                x + halfWidth,
                y + height,
                z + halfLength
        );
    }

    @Override
    protected int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();

        for (Entity passenger : getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                living.fallDistance = 0;
            }
            // Show fuel status to the rider as action bar text
            if (passenger instanceof ServerPlayer serverPlayer && serverPlayer.getVehicle() == this) {
                double fuel = getTotalFuelMillibuckets();
                double maxFuel = this.fuelTankItem != null ? this.fuelTankItem.getBaseFuelCapacity() : 0.0;
                String fuelText = maxFuel > 0 ? String.format("Fuel: %.0f / %.0f mB", fuel, maxFuel) : "No fuel tank";
                serverPlayer.displayClientMessage(Component.literal(fuelText), true);
            }
        }

        if (this.getDeltaMovement().length() < 0.01) return;

        int currentTick = this.tickCount;
        recentCollisions.entrySet().removeIf(e -> currentTick - e.getValue() > 20);

        for (Entity other : level().getEntities(this, this.getBoundingBox().inflate(0.15))) {
            if (other instanceof BaseCarEntity car && car != this) {
                int otherId = car.getId();
                if (recentCollisions.containsKey(otherId)) continue;

                Vec3 relVel = this.getDeltaMovement().subtract(car.getDeltaMovement());
                Vec3 toOther = car.position().subtract(this.position()).normalize();
                double approachSpeed = relVel.dot(toOther);

                double minCrashSpeed = 5.0 / 20.0;

                if (approachSpeed > minCrashSpeed) {
                    float damage = (float) (approachSpeed / minCrashSpeed * 6.0F);
                    damage = Math.min(damage, 12.0F); // Cap damage per collision
                    this.hurt(this.level().damageSources().mobAttack(car), damage);
                    car.hurt(car.level().damageSources().mobAttack(this), damage);

                    this.knockback(approachSpeed * 0.4, toOther.x * 1.2, toOther.z * 1.2);
                    car.knockback(approachSpeed * 0.4, -toOther.x * 1.2, -toOther.z * 1.2);

                    recentCollisions.put(otherId, this.tickCount);
                    car.recentCollisions.put(this.getId(), this.tickCount);
                }
            } else if (other instanceof LivingEntity living && !living.isSpectator() && living.isAlive()) {
                int otherId = other.getId();
                if (recentCollisions.containsKey(otherId)) continue;

                Vec3 relVel = this.getDeltaMovement().subtract(other.getDeltaMovement());
                Vec3 toOther = other.position().subtract(this.position()).normalize();
                double approachSpeed = relVel.dot(toOther);

                double minEntityRunoverSpeed = 3.5 / 20.0;
                if (approachSpeed > minEntityRunoverSpeed) {
                    float damage = (float) (approachSpeed / minEntityRunoverSpeed * 3.0F);
                    damage = Math.min(damage, 8.0F); // Cap entity runover damage

                    Entity passenger = this.getFirstPassenger();
                    if (passenger instanceof LivingEntity attacker) {
                        other.hurt(this.level().damageSources().mobAttack(attacker), damage);
                    }

                    other.push(toOther.x * approachSpeed * 1.4, 0.3 + 0.7 * Math.min(approachSpeed, 1.0), toOther.z * approachSpeed * 1.4);

                    recentCollisions.put(otherId, this.tickCount);
                }
            }
        }
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public boolean isDamageSourceBlocked(DamageSource pDamageSource) {
        return false;
    }

    @Override
    protected void actuallyHurt(DamageSource pDamageSource, float pDamageAmount) {
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource pDamageSource) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        if (!this.level().isClientSide) {
            // Drop the car parts as items
            if (this.engineItem != null) {
                this.spawnAtLocation(new ItemStack(this.engineItem));
            }
            if (this.frameItem != null) {
                this.spawnAtLocation(new ItemStack(this.frameItem));
            }
            if (this.fuelTankItem != null) {
                this.spawnAtLocation(new ItemStack(this.fuelTankItem));
            }
            if (this.wheelItems != null) {
                for (AbstractWheelItem wheel : this.wheelItems) {
                    if (wheel != null) {
                        this.spawnAtLocation(new ItemStack(wheel));
                    }
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        // No goals
    }

    private void updateCarWeight() {
        this.weight = (float) ((frameItem != null ? frameItem.getWeight() : 0.0)
                + (engineItem != null ? engineItem.getWeight() : 0.0)
                + (fuelTankItem != null ? fuelTankItem.getWeight() : 0.0));
        if (this.weight < 50.0f) this.weight = 50.0f;
        this.entityData.set(SYNCED_CAR_WEIGHT, this.weight);
    }

    /**
     * Updates car stats (max speed, acceleration, drag, etc.) from the currently installed parts.
     * Call this after loading, after installing a part, or after any part changes.
     */
    private void updateCarStatsFromParts() {
        if (this.engineItem != null) {
            this.carMaxSpeed = this.engineItem.getBaseMaxSpeed();
            this.carAcceleration = this.engineItem.getBaseAcceleration();
            this.fuelConsumptionRate = this.engineItem.getBaseFuelConsumption();
        }
        if (this.frameItem != null) {
            this.carDrag = this.frameItem.getBaseDrag();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        double handItemFuelValue = getFuelUnitsPerMillibucketForItem(pPlayer.getItemInHand(pHand));
        Fluid fluid = FluidUtil.getFluidContained(pPlayer.getItemInHand(pHand)).map(FluidStack::getFluid).orElse(null);

        if (pPlayer.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        else if (handItemFuelValue > 0.0 && fluid != null) {
            double totalFuelAfterAddingHand = this.fluidTank.getOrDefault(fluid.getFluidType().getDescriptionId(), 0.0) + handItemFuelValue * getItemFluidAmount(pPlayer.getItemInHand(pHand));
            if (this.fuelTankItem == null || totalFuelAfterAddingHand > this.fuelTankItem.getBaseFuelCapacity()) {
                return InteractionResult.CONSUME;
            }
            this.fluidTank.put(fluid.getFluidType().getDescriptionId(), totalFuelAfterAddingHand);
            if (!pPlayer.isCreative()) {
                pPlayer.getItemInHand(pHand).shrink(1);
            }
            return InteractionResult.SUCCESS;
        } else if (pPlayer.getItemInHand(pHand).getItem() instanceof AbstractCarPartItem carPartItem) {
            boolean changed = false;
            if (carPartItem.getPartType() == CarPartType.ENGINE) {
                if (this.engineItem != null) return InteractionResult.CONSUME;
                this.engineItem = (AbstractEngineItem) carPartItem;
                changed = true;
            } else if (carPartItem.getPartType() == CarPartType.FRAME) {
                if (this.frameItem != null) return InteractionResult.CONSUME;
                this.frameItem = (AbstractFrameItem) carPartItem;
                // Re-initialize wheels array to match required wheels for this frame
                int reqWheels = this.frameItem.getRequiredWheels();
                AbstractWheelItem[] oldWheels = this.wheelItems;
                this.wheelItems = new AbstractWheelItem[reqWheels];
                if (oldWheels != null) {
                    System.arraycopy(oldWheels, 0, this.wheelItems, 0, Math.min(oldWheels.length, reqWheels));
                }
                changed = true;
            } else if (carPartItem.getPartType() == CarPartType.FUEL_TANK) {
                if (this.fuelTankItem != null) return InteractionResult.CONSUME;
                this.fuelTankItem = (AbstractFuelTankItem) carPartItem;
                changed = true;
            } else if (carPartItem.getPartType() == CarPartType.WHEEL) {
                boolean hasSpace = false;
                if (this.wheelItems == null) {
                    this.wheelItems = new AbstractWheelItem[4];
                }
                for (int i = 0; i < this.wheelItems.length; i++) {
                    if (this.wheelItems[i] == null) {
                        this.wheelItems[i] = (AbstractWheelItem) carPartItem;
                        hasSpace = true;
                        changed = true;
                        break;
                    }
                }
                if (!hasSpace) {
                    return InteractionResult.CONSUME;
                }
            }
            if (changed) {
                updateCarWeight();
                updateCarStatsFromParts(); // always recalculate stats from parts after installing
                syncCarData();
                if (!pPlayer.isCreative()) {
                    pPlayer.getItemInHand(pHand).shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        } else if (!this.level().isClientSide && !(this.getPassengers().size() >= this.getCapacity())) {
            if (pPlayer.getItemInHand(pHand).getItem() instanceof AbstractCarPartItem) {
                return InteractionResult.FAIL;
            }
            return pPlayer.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static int getItemFluidAmount(ItemStack fuelItem) {
        Optional<FluidStack> fluid = FluidUtil.getFluidContained(fuelItem);
        if (!fluid.isPresent()) return 0;
        FluidStack fluidStack = fluid.get();
        return fluidStack.getAmount();
    }

    private double getFuelUnitsPerMillibucketForItem(ItemStack item) {
        Optional<FluidStack> fluidStack = FluidUtil.getFluidContained(item);
        if (fluidStack.isEmpty()) return 0.0;
        Fluid fluid = fluidStack.get().getFluid();
        return FLUID_TO_FUEL_PER_MB.getOrDefault(fluid.getFluidType().getDescriptionId(), 0.0);
    }

    @Override
    protected void positionRider(Entity pPassenger, MoveFunction pCallback) {
        if (this.hasPassenger(pPassenger)) {
            int index = this.getPassengers().indexOf(pPassenger);
            if (index >= 0 && index < this.seatOffsets.length) {
                Vec3 offset = this.seatOffsets[index];
                double yawRad = Math.toRadians(-this.getYRot());
                double yCos = Math.cos(yawRad);
                double ySin = Math.sin(yawRad);

                double seatX = offset.x * yCos - offset.z * ySin;
                double seatZ = offset.x * ySin + offset.z * yCos;

                double x = this.getX() + seatX;
                double y = this.getY() + offset.y + this.getPassengersRidingOffset() + pPassenger.getMyRidingOffset();
                double z = this.getZ() + seatZ;
                pPassenger.setPos(x, y, z);
                pCallback.accept(pPassenger, x, y, z);
            } else {
                SimplecarsMod.LOGGER.warn("BaseCarEntity has more passengers than seat offsets defined!");
            }
        }
    }

    /// Get an Item from a description ID, e.g. "item.simplecars.engine_v8"
    @SuppressWarnings("removal")
    public static @Nullable Item getItemFromDescriptionId(String descriptionId) {
        String[] parts = descriptionId.split("\\.");
        if (parts.length < 3 || !("item".equals(parts[0]))) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[1], parts[2]));
    }

    private boolean hasAllPartsRequiredForAnimation() {
        return this.frameItem != null && this.engineItem != null;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    private boolean hasAllPartsRequiredForTravel() {
        if (this.engineItem == null || this.frameItem == null || this.fuelTankItem == null || this.wheelItems == null) return false;
        int required = this.frameItem.getRequiredWheels();
        if (this.wheelItems.length != required) return false;
        for (AbstractWheelItem wheel : this.wheelItems) {
            if (wheel == null) return false;
        }
        return true;
    }

    private double getTotalFuelUnits() {
        if (this.fuelTankItem == null) return 0.0;
        double totalFuel = 0.0;
        for (Map.Entry<String, Double> entry : this.fluidTank.entrySet()) {
            if (entry.getValue() > 0) {
                totalFuel += entry.getValue() * FLUID_TO_FUEL_PER_MB.getOrDefault(entry.getKey(), 0.0);
            }
        }
        return totalFuel;
    }

    private double getTotalFuelMillibuckets() {
        return this.fluidTank.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private void useUpFuel(double velocity) {
        if (this.fluidTank == null || this.fuelTankItem == null) return;
        String mostEfficientFuel = null;
        for (String fluidId : FLUID_TO_FUEL_PER_MB.keySet()) {
            if (this.fluidTank.containsKey(fluidId) && this.fluidTank.get(fluidId) > 0.0) {
                if (mostEfficientFuel == null || FLUID_TO_FUEL_PER_MB.get(fluidId) > FLUID_TO_FUEL_PER_MB.get(mostEfficientFuel)) {
                    mostEfficientFuel = fluidId;
                }
            }
        }
        if (mostEfficientFuel != null) {
            // The below line makes absolutely no sense but it feels good so I'm keeping it
            double fuelUnitsNeeded = Math.abs(velocity) * this.fuelConsumptionRate * 10; // Get average fuel units needed per tick
            double availableFuelInMillibuckets = this.fluidTank.getOrDefault(mostEfficientFuel, 0.0);
            double fuelUnitsPerMillibucket = FLUID_TO_FUEL_PER_MB.getOrDefault(mostEfficientFuel, 0.0);
            double millibucketsThatWillBeUsed = fuelUnitsNeeded / fuelUnitsPerMillibucket;

            if (availableFuelInMillibuckets >= millibucketsThatWillBeUsed) {
                this.fluidTank.put(mostEfficientFuel, Math.max(0, availableFuelInMillibuckets - millibucketsThatWillBeUsed));
            } else {
                this.fluidTank.remove(mostEfficientFuel);
            }
        }
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (hasAllPartsRequiredForTravel() && getTotalFuelUnits() > 0.0 && this.isVehicle() && !this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof Player player && (!level().isClientSide || this.isControlledByLocalInstance())) {
            float forward = player.zza;
            float strafe = player.xxa;

            double maxSpeed = this.carMaxSpeed;
            double acceleration = this.carAcceleration;
            double brakeRate = this.carBrakeStrength;
            double friction = this.carDrag;
            float turnSpeed = this.carSteeringFactor;
            double maxUpwardDrive = this.carMaximumUpwardDrive;

            float weightFactor = 500.0f / this.weight;
            weightFactor = Math.max(0.6f, Math.min(weightFactor, 1.4f));

            double weightedAcceleration = acceleration * weightFactor;
            double weightedBrakeRate = brakeRate * weightFactor;
            double weightedMaxSpeed = maxSpeed * weightFactor;

            double velocity = getCarForwardVelocity();

            boolean isNearlyStopped = Math.abs(velocity) < 0.01;
            if (isNearlyStopped) {
                if (Math.abs(strafe) > 0.01) {
                    // Only accumulate pre-steer when nearly stopped
                    pendingSteerInput += strafe * 0.09f;
                    pendingSteerInput = Math.max(-0.25f, Math.min(0.25f, pendingSteerInput));
                } else {
                    pendingSteerInput *= 0.93f;
                    if (Math.abs(pendingSteerInput) < 0.01f) pendingSteerInput = 0.0f;
                }
                preSteerBlend = 0.0f;
            } else {
                // Start blending out pre-steer as soon as we start moving
                if (preSteerBlend == 0.0f && Math.abs(pendingSteerInput) > 0.01f) {
                    preSteerBlend = PRESTEER_BLEND_TIME * 20f; // ticks
                    lastPendingSteer = pendingSteerInput;
                }
                pendingSteerInput = 0.0f;
            }

            float effectiveSteer;
            if (preSteerBlend > 0.0f) {
                float blendAlpha = preSteerBlend / (PRESTEER_BLEND_TIME * 20f);
                effectiveSteer = -lastPendingSteer * blendAlpha + strafe * (1.0f - blendAlpha);
                preSteerBlend -= 1.0f;
                if (preSteerBlend < 0.0f) preSteerBlend = 0.0f;
            } else {
                effectiveSteer = isNearlyStopped ? -pendingSteerInput : strafe;
            }
            double steerAtten = Math.max(0.4, 1.0 - (Math.abs(velocity) / weightedMaxSpeed) * 0.7);
            if (isNearlyStopped && Math.abs(forward) > 0.01 && Math.abs(effectiveSteer) > 0.01) {
                steerAtten *= 1.2;
            }
            float steerDir = velocity > 0 ? 1f : -1f;
            if (Math.abs(effectiveSteer) > 0.01) {
                this.setYRot((float) (this.getYRot() - effectiveSteer * turnSpeed * steerAtten * steerDir));
            }

            if (forward > 0) {
                velocity += weightedAcceleration;
            } else if (forward < 0) {
                velocity -= weightedBrakeRate;
            } else {
                if (velocity > 0) {
                    velocity -= friction;
                    if (velocity < 0) velocity = 0;
                } else if (velocity < 0) {
                    velocity += friction;
                    if (velocity > 0) velocity = 0;
                }
            }

            velocity = Math.max(Math.min(velocity, weightedMaxSpeed), -weightedMaxSpeed * 0.4);

            double yawRad = Math.toRadians(this.getYRot());
            double dx = -Math.sin(yawRad) * velocity;
            double dz =  Math.cos(yawRad) * velocity;
            double dy = this.getDeltaMovement().y;

            // Save intended movement for wall sliding logic
            double intendedDx = dx;
            double intendedDz = dz;
            boolean wallBlocked = false;

            boolean tryStepUp = this.onGround() && (Math.abs(dx) > 0.0001 || Math.abs(dz) > 0.0001);
            if (tryStepUp && maxUpwardDrive > 0) {
                AABB box = this.getBoundingBox();
                AABB nextBox = box.move(dx, 0, dz);
                boolean collides = !level().noCollision(this, nextBox);

                if (collides) {
                    double step = Math.max(0.05, Math.min(0.2, maxUpwardDrive / 5.0));
                    double climbed = 0;
                    boolean canStep = false;
                    while (climbed < maxUpwardDrive) {
                        climbed += step;
                        nextBox = box.move(dx, climbed, dz);
                        if (level().noCollision(this, nextBox)) {
                            dy = climbed;
                            canStep = true;
                            break;
                        }
                    }
                    if (!canStep) {
                        wallBlocked = true;
                    }
                }
            }

            if (wallBlocked) {
                // Project intended movement onto wall tangent
                // Find the wall normal by checking which direction is blocked
                Vec3 pos = this.position();
                double probeDist = 0.2;
                Vec3[] probes = new Vec3[] {
                    new Vec3(probeDist, 0, 0), new Vec3(-probeDist, 0, 0),
                    new Vec3(0, 0, probeDist), new Vec3(0, 0, -probeDist)
                };
                Vec3 wallNormal = null;
                for (Vec3 probe : probes) {
                    AABB probeBox = this.getBoundingBox().move(probe.x, 0, probe.z);
                    if (!level().noCollision(this, probeBox)) {
                        wallNormal = probe.normalize();
                        break;
                    }
                }
                if (wallNormal != null) {
                    // Project intended movement onto wall tangent
                    Vec3 moveVec = new Vec3(intendedDx, 0, intendedDz);
                    Vec3 tangent = moveVec.subtract(wallNormal.scale(moveVec.dot(wallNormal)));
                    // Limit forward speed to 40% of max
                    double tangentLen = tangent.length();
                    double maxSlide = weightedMaxSpeed * 0.4;
                    if (tangentLen > maxSlide) {
                        tangent = tangent.scale(maxSlide / tangentLen);
                    }
                    dx = tangent.x;
                    dz = tangent.z;
                    velocity = Math.max(Math.min(velocity, weightedMaxSpeed * 0.4), -weightedMaxSpeed * 0.4);
                } else {
                    dx = 0;
                    dz = 0;
                }
            }

            if (!this.onGround()) dy -= 0.08;
            else if (dy < 0) dy = 0;

            this.setDeltaMovement(dx, dy, dz);
            this.move(MoverType.SELF, this.getDeltaMovement());

            useUpFuel(velocity);

            if (!this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 1, 0.98));
            }

            if (!this.level().isClientSide || this.isControlledByLocalInstance()) {
                updateCarForwardVelocity(velocity);
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (level().isClientSide) {
            // Engine
            if (ENGINE_ID.equals(key)) {
                String engineId = this.entityData.get(ENGINE_ID);
                this.engineItem = AbstractCarPartItem.getItemByDescriptionId(engineId, AbstractEngineItem.class);
            }
            // Frame
            if (FRAME_ID.equals(key)) {
                String frameId = this.entityData.get(FRAME_ID);
                this.frameItem = AbstractCarPartItem.getItemByDescriptionId(frameId, AbstractFrameItem.class);
                if (this.frameItem != null) {
                    int req = this.frameItem.getRequiredWheels();
                    if (this.wheelItems == null || this.wheelItems.length != req) {
                        AbstractWheelItem[] newWheels = new AbstractWheelItem[req];
                        if (this.wheelItems != null)
                            System.arraycopy(this.wheelItems, 0, newWheels, 0, Math.min(this.wheelItems.length, req));
                        this.wheelItems = newWheels;
                    }
                }
            }
            // Fuel Tank
            if (FUEL_TANK_ID.equals(key)) {
                String fuelTankId = this.entityData.get(FUEL_TANK_ID);
                this.fuelTankItem = AbstractCarPartItem.getItemByDescriptionId(fuelTankId, AbstractFuelTankItem.class);
            }
            // Wheels
            if (WHEEL_IDS.equals(key)) {
                String[] ids = getWheelIds();
                if (ids.length > 0 && (this.wheelItems == null || this.wheelItems.length != ids.length)) {
                    this.wheelItems = new AbstractWheelItem[ids.length];
                }
                for (int i = 0; i < ids.length; i++) {
                    this.wheelItems[i] = AbstractCarPartItem.getItemByDescriptionId(ids[i], AbstractWheelItem.class);
                }
            }
            // Physics
            if (SYNCED_CAR_DRAG.equals(key)) this.carDrag = getSyncedCarDrag();
            if (SYNCED_CAR_ACCEL.equals(key)) this.carAcceleration = getSyncedCarAcceleration();
            if (SYNCED_CAR_MAX_SPEED.equals(key)) this.carMaxSpeed = getSyncedCarMaxSpeed();
            if (SYNCED_CAR_BRAKE.equals(key)) this.carBrakeStrength = getSyncedCarBrakeStrength();
            if (SYNCED_CAR_STEER.equals(key)) this.carSteeringFactor = getSyncedCarSteeringFactor();
            if (SYNCED_CAR_UPWARD_DRIVE.equals(key)) this.carMaximumUpwardDrive = getSyncedCarMaximumUpwardDrive();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canBeLeashed(Player pPlayer) {
        return false;
    }

    @Override
    public boolean alwaysAccepts() {
        return super.alwaysAccepts();
    }
}
