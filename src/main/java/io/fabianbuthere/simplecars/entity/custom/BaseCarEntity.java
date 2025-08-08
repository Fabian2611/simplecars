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
import net.minecraft.util.Mth;
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
import org.jetbrains.annotations.Range;

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
    private static final EntityDataAccessor<Float> DRIFT_DURATION =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DRIFTING =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BOOST_REMAINING =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BOOST_MULTIPLIER =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LAST_STEER_INPUT =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DRIFTING_POSSIBLE =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_BRAKING =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> LAST_THROTTLE_INPUT_BOOST =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BOOST_THROTTLE_RELEASED =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOOST_MAINTAIN_ACTIVE =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BOOST_MAINTAIN_SPEED =
            SynchedEntityData.defineId(BaseCarEntity.class, EntityDataSerializers.FLOAT);

    private float pendingSteerInput = 0.0f;
    private float preSteerBlend = 0.0f;
    private float lastPendingSteer = 0.0f;
    private static final float PRESTEER_BLEND_TIME = 0.18f; // seconds

    private double getDriftDuration() {
        return this.entityData.get(DRIFT_DURATION);
    }
    private void setDriftDuration(double value) {
        this.entityData.set(DRIFT_DURATION, (float)value);
    }

    private boolean getIsDrifting() {
        return this.entityData.get(IS_DRIFTING);
    }
    private void setIsDrifting(boolean value) {
        this.entityData.set(IS_DRIFTING, value);
    }

    private double getBoostRemaining() {
        return this.entityData.get(BOOST_REMAINING);
    }
    private void setBoostRemaining(double value) {
        this.entityData.set(BOOST_REMAINING, (float)value);
    }

    private double getBoostMultiplier() {
        return this.entityData.get(BOOST_MULTIPLIER);
    }
    private void setBoostMultiplier(double value) {
        this.entityData.set(BOOST_MULTIPLIER, (float)value);
    }

    private double getLastSteerInput() {
        return this.entityData.get(LAST_STEER_INPUT);
    }
    private void setLastSteerInput(double value) {
        this.entityData.set(LAST_STEER_INPUT, (float)value);
    }

    private boolean getIsDriftingPossible() {
        return this.entityData.get(IS_DRIFTING_POSSIBLE);
    }
    private void setIsDriftingPossible(boolean value) {
        this.entityData.set(IS_DRIFTING_POSSIBLE, value);
    }

    public boolean isBraking() {
        return this.entityData.get(IS_BRAKING);
    }
    public void setBraking(boolean braking) {
        this.entityData.set(IS_BRAKING, braking);
    }

    private float getLastThrottleInputBoostValue() {
        return this.entityData.get(LAST_THROTTLE_INPUT_BOOST);
    }
    private void setLastThrottleInputBoostValue(float value) {
        this.entityData.set(LAST_THROTTLE_INPUT_BOOST, value);
    }

    private boolean getBoostThrottleReleased() {
        return this.entityData.get(BOOST_THROTTLE_RELEASED);
    }
    private void setBoostThrottleReleased(boolean value) {
        this.entityData.set(BOOST_THROTTLE_RELEASED, value);
    }

    private boolean getBoostMaintainActive() {
        return this.entityData.get(BOOST_MAINTAIN_ACTIVE);
    }
    private void setBoostMaintainActive(boolean value) {
        this.entityData.set(BOOST_MAINTAIN_ACTIVE, value);
    }

    private double getBoostMaintainSpeed() {
        return this.entityData.get(BOOST_MAINTAIN_SPEED);
    }
    private void setBoostMaintainSpeed(double value) {
        this.entityData.set(BOOST_MAINTAIN_SPEED, (float) value);
    }

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
    public Vec3 getBackLicensePlateOffset() { return (this.frameItem != null) ? this.frameItem.getBackLicensePlateOffset() : Vec3.ZERO; }

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

        // Drift state
        pCompound.putBoolean("IsBraking", this.isBraking());
        pCompound.putDouble("DriftDuration", this.getDriftDuration());
        pCompound.putBoolean("IsDrifting", this.getIsDrifting());
        pCompound.putDouble("BoostRemaining", this.getBoostRemaining());
        pCompound.putDouble("BoostMultiplier", this.getBoostMultiplier());
        pCompound.putDouble("LastSteerInput", this.getLastSteerInput());
        pCompound.putBoolean("IsDriftingPossible", this.getIsDriftingPossible());

        pCompound.putFloat("LastThrottleInputBoost", this.getLastThrottleInputBoostValue());
        pCompound.putBoolean("BoostThrottleReleased", this.getBoostThrottleReleased());
        pCompound.putBoolean("BoostMaintainActive", this.getBoostMaintainActive());
        pCompound.putFloat("BoostMaintainSpeed", (float) this.getBoostMaintainSpeed());

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

        // Drift state
        if (pCompound.contains("IsBraking")) this.setBraking(pCompound.getBoolean("IsBraking"));
        if (pCompound.contains("DriftDuration")) this.setDriftDuration(pCompound.getDouble("DriftDuration"));
        if (pCompound.contains("IsDrifting")) this.setIsDrifting(pCompound.getBoolean("IsDrifting"));
        if (pCompound.contains("BoostRemaining")) this.setBoostRemaining(pCompound.getDouble("BoostRemaining"));
        if (pCompound.contains("BoostMultiplier")) this.setBoostMultiplier(pCompound.getDouble("BoostMultiplier"));
        if (pCompound.contains("LastSteerInput")) this.setLastSteerInput(pCompound.getDouble("LastSteerInput"));
        if (pCompound.contains("IsDriftingPossible")) this.setIsDriftingPossible(pCompound.getBoolean("IsDriftingPossible"));

        if (pCompound.contains("LastThrottleInputBoost")) this.setLastThrottleInputBoostValue(pCompound.getFloat("LastThrottleInputBoost"));
        if (pCompound.contains("BoostThrottleReleased")) this.setBoostThrottleReleased(pCompound.getBoolean("BoostThrottleReleased"));
        if (pCompound.contains("BoostMaintainActive")) this.setBoostMaintainActive(pCompound.getBoolean("BoostMaintainActive"));
        if (pCompound.contains("BoostMaintainSpeed")) this.setBoostMaintainSpeed(pCompound.getFloat("BoostMaintainSpeed"));

        updateCarWeight();
        updateCarStatsFromParts(); // always recalculate stats from parts after loading
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
        this.entityData.define(SYNCED_CAR_UPWARD_DRIVE, 1.0F);
        this.entityData.define(SYNCED_CAR_WEIGHT, 60.0F);
        // Add drift-related synced data
        this.entityData.define(IS_BRAKING, false);
        this.entityData.define(DRIFT_DURATION, 0.0F);
        this.entityData.define(IS_DRIFTING, false);
        this.entityData.define(BOOST_REMAINING, 0.0F);
        this.entityData.define(BOOST_MULTIPLIER, 1.0F);
        this.entityData.define(LAST_STEER_INPUT, 0.0F);
        this.entityData.define(IS_DRIFTING_POSSIBLE, true);

        this.entityData.define(LAST_THROTTLE_INPUT_BOOST, 0.0F);
        this.entityData.define(BOOST_THROTTLE_RELEASED, false);
        this.entityData.define(BOOST_MAINTAIN_ACTIVE, false);
        this.entityData.define(BOOST_MAINTAIN_SPEED, 0.0F);
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
    protected void actuallyHurt(@NotNull DamageSource pDamageSource, float pDamageAmount) {
    }


    // TODO: Sounds
    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
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

    private boolean isDriftingAllowed() {
        return this.getIsDriftingPossible();
    }

    // --- "CONSTANTS" ---
    // Base physics parameters
    public static double REFERENCE_WEIGHT = 500.0;           // Reference vehicle mass used to normalize handling/forces (higher car weight => lower weightFactor)
    public static double MIN_WEIGHT_FACTOR = 0.4;            // Lower clamp for weightFactor to prevent overly sluggish handling on heavy cars
    public static double MAX_WEIGHT_FACTOR = 2.0;            // Upper clamp for weightFactor to prevent overly twitchy handling on light cars
    public static double TICK_DELTA_TIME = 1.0;              // Physics integration step in ticks (1 = one Minecraft tick)
    public static double TICKS_PER_SECOND = 20.0;            // Conversion factor from seconds to ticks (20 TPS, usually)
    public static double CONTROL_DEADZONE = 0.01;            // Minimum absolute input considered intentional (epsilon)

    // Resistance and stability
    public static double ROLLING_RESISTANCE = 0.008;         // Baseline opposing force from tire contact with ground (per tick)
    public static double AIR_RESISTANCE = 0.002;             // Coefficient for quadratic aerodynamic drag with speed
    public static double DOWNFORCE_MULTIPLIER = 0.98;        // Multiplier on horizontal motion to simulate downforce/traction (slightly damps slide)

    // Steering parameters
    public static double MIN_TURNING_SPEED = 0.045;          // Minimum forward speed required for steering to have effect
    public static double STEERING_SPEED_FALLOFF = 0.6;       // How much steering authority reduces as speed rises (0=no falloff, 1=strong falloff)
    public static double MIN_STEERING_EFFECTIVENESS = 0.25;  // Floor for steering authority at very high speeds
    public static double TURNING_RADIUS_FACTOR = 2.1;        // Scales base steering into turning radius (higher = tighter turns)
    public static double REVERSE_STEERING_FACTOR = 0.7;      // Fraction of steering effectiveness applied when driving in reverse
    public static double WEIGHT_STEERING_POWER = 0.3;        // Exponent controlling how weightFactor influences steering power
    public static double STEERING_VELOCITY_DENOMINATOR = 0.3;// Normalization factor for steering effectiveness vs vehicle speed
    public static double YAW_CHANGE_MULTIPLIER = 0.45;       // Scales computed steering into yaw rotation per tick
    public static double MAX_STEERING_ANGLE_BASE = 45.0;     // Maximum steering angle (degrees) at low speed before speed-based reductions
    public static double MAX_STEERING_SPEED_REDUCTION = 0.7; // Fraction by which max steering angle is reduced at top speed

    // General driving parameters
    public static double ENHANCED_BRAKING_MULTIPLIER = 1.5;  // Extra braking when throttle input opposes current velocity
    public static double COMPLETE_STOP_THRESHOLD = 0.02;     // Speed below which velocity is snapped to zero to avoid micro creeping
    public static double FUEL_CONSUMPTION_IDLE = 0.1;        // Base fuel cost per tick for maintaining/idle motion

    // Handbrake and quick stop parameters
    public static double HANDBRAKE_FORCE_MULTIPLIER = 1.8;   // Strength multiplier vs normal braking when handbrake is engaged
    public static double HANDBRAKE_MIN_EFFECTIVENESS = 0.5;  // Minimum handbrake effectiveness after weight scaling (for heavy cars)
    public static double HANDBRAKE_WEIGHT_POWER = 0.4;       // Exponent for weightFactor’s influence on handbrake force
    public static double QUICK_STOP_EXTRA_FORCE = 1.3;       // Additional speed-based force during quick-stop mode
    public static double HANDBRAKE_FORCE_CLAMP = 0.12;       // Maximum deceleration per tick under handbrake to avoid jerky stops
    public static double HANDBRAKE_GRADUAL_FACTOR = 0.85;    // Gradual application factor for a smoother handbrake feel
    public static double HANDBRAKE_LOW_SPEED_SCALING = 0.5;  // Reduces handbrake strength at very low speeds for smoother final stop
    public static double HANDBRAKE_STOP_VELOCITY = 0.01;     // Below this speed handbrake won’t push the car into reverse (epsilon)

    // Drift mechanics parameters
    public static double DRIFT_SPEED_THRESHOLD = 0.1;        // Minimum speed required to enter/maintain a drift (blocks / tick)
    public static double DRIFT_STEERING_THRESHOLD = 0.05;    // Minimum steering input considered for drift entry
    public static double DRIFT_TRACTION_REDUCTION = 0.5;     // Fraction of longitudinal traction removed while drifting (more slide)
    public static double DRIFT_SPEED_BOOST_FACTOR = 1.8;     // Temporary speed cap multiplier while actively drifting
    public static double DRIFT_TURNING_ENHANCEMENT = 1.8;    // Steering multiplier applied during drift for faster rotation
    public static double DRIFT_SIDE_SLIP_FACTOR = 0.7;       // Base magnitude of lateral slip generated during drift
    public static double DRIFT_YAW_MULTIPLIER = 1.6;         // Additional yaw rate applied while drifting
    public static double DRIFT_TRANSITION_SMOOTHING = 0.15;  // Blend factor controlling smoothness of drift entry/exit
    public static double DRIFT_SIDE_SLIP_SPEED_SCALING = 0.6;// Power by which lateral slip scales with speed (basically exponential)
    public static double DRIFT_PASSIVE_SLIP = 0.2;           // Lateral slip applied even with zero steering during drift
    public static double DRIFT_THROTTLE_BONUS = 0.3;         // Extra longitudinal acceleration permitted when throttling in drift

    // Drift boost parameters
    public static double DRIFT_BOOST_ACCUMULATION_RATE = 0.06; // Rate at which drift “quality” accumulates to determine boost strength
    public static double DRIFT_BOOST_MAX_MULTIPLIER = 2.5;     // Maximum post-drift speed multiplier (1.0 = no boost)
    public static double DRIFT_BOOST_DECAY_RATE = 0.004;       // Per-tick decay of boost multiplier back toward 1.0
    public static double DRIFT_BOOST_MIN_DURATION = 8.0;       // Minimum accumulated drift to grant any boost (ticks)
    public static double DRIFT_BOOST_MAX_DURATION = 80.0;      // Accumulated drift considered “perfect” for max boost (ticks)
    public static double DRIFT_BOOST_MIN_SECONDS = 4.0;        // Minimum post-drift boost duration (seconds)
    public static double DRIFT_BOOST_MAX_SECONDS = 15.0;       // Maximum post-drift boost duration (seconds)
    public static double BOOST_THRUST_FACTOR = 0.7;            // Fraction of engine acceleration added as constant thrust while boosting
    public static double BOOST_MAINTAIN_EPSILON = 0.002;       // Tolerance when clamping velocity to the boost “maintain speed” cap

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (hasAllPartsRequiredForTravel() && getTotalFuelUnits() > 0.0 && this.isVehicle() &&
                !this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof Player player &&
                (!level().isClientSide || this.isControlledByLocalInstance())) {

            // --- INPUTS ---
            float throttleInput = player.zza;           // Forward/backward (-1..1)
            float steerInput = -player.xxa;             // Left/right (-1..1), corrected direction
            boolean handbrakeActive = this.isBraking(); // Handbrake status
            boolean throttlePressed = Math.abs(throttleInput) > CONTROL_DEADZONE;
            boolean hasMovementInput = throttlePressed || Math.abs(steerInput) > CONTROL_DEADZONE;

            // --- CAR PARAMETERS ---
            double maxSpeed = this.carMaxSpeed;
            double acceleration = this.carAcceleration;
            double drag = this.carDrag;
            double brakeStrength = this.carBrakeStrength;
            float steeringFactor = this.carSteeringFactor;
            double maxStepHeight = this.carMaximumUpwardDrive;
            double weight = this.weight;

            // --- STEP HEIGHT ---
            this.setMaxUpStep((float) maxStepHeight);

            // --- WEIGHT FACTOR CALC ---
            double weightFactor = REFERENCE_WEIGHT / weight;
            weightFactor = Mth.clamp(weightFactor, MIN_WEIGHT_FACTOR, MAX_WEIGHT_FACTOR);

            // --- VEHICLE STATE ---
            double currentVel = getCarForwardVelocity();
            double absVel = Math.abs(currentVel);
            boolean isReversing = currentVel < -MIN_TURNING_SPEED;
            boolean isMoving = absVel > DRIFT_SPEED_THRESHOLD;

            // --- HANDBRAKE EFFECTIVENESS ---
            double handbrakeEffectiveness = Math.pow(weightFactor, HANDBRAKE_WEIGHT_POWER);
            handbrakeEffectiveness = Mth.clamp(handbrakeEffectiveness, HANDBRAKE_MIN_EFFECTIVENESS, 1.0);

            // --- DRIFT STATE ---
            boolean wasDrifting = this.getIsDrifting();
            boolean driftingAllowed = this.isDriftingAllowed();

            boolean shouldDrift = handbrakeActive && hasMovementInput && isMoving && driftingAllowed;
            boolean isQuickStop = handbrakeActive && !shouldDrift;

            this.setIsDrifting(shouldDrift);
            boolean isDrifting = shouldDrift;

            // Update drift duration and compute boost at drift end
            if (isDrifting) {
                double inputFactor = Math.abs(throttleInput) + Math.abs(steerInput) * 0.5;
                inputFactor = Math.max(0.5, inputFactor);

                double newDuration = this.getDriftDuration() +
                        DRIFT_BOOST_ACCUMULATION_RATE * (1.0 + absVel * 2.0) * inputFactor;

                this.setDriftDuration(Math.min(newDuration, DRIFT_BOOST_MAX_DURATION));
            } else if (wasDrifting && !isDrifting && this.getDriftDuration() > DRIFT_BOOST_MIN_DURATION && driftingAllowed) {
                double driftQuality = Math.min(this.getDriftDuration() / DRIFT_BOOST_MAX_DURATION, 1.0);

                double newBoostMult = 1.0 + (DRIFT_BOOST_MAX_MULTIPLIER - 1.0) * driftQuality;

                double boostSeconds = DRIFT_BOOST_MIN_SECONDS +
                        (DRIFT_BOOST_MAX_SECONDS - DRIFT_BOOST_MIN_SECONDS) * driftQuality;
                double newBoostRemaining = boostSeconds * TICKS_PER_SECOND;

                this.setBoostMultiplier(newBoostMult);
                this.setBoostRemaining(newBoostRemaining);

                // Reset boost/throttle interaction tracking at boost start
                setBoostThrottleReleased(false);
                setBoostMaintainActive(false);
                setBoostMaintainSpeed(0.0);
            } else if (!isDrifting) {
                this.setDriftDuration(0.0);
            }

            // Apply and decay boost
            boolean boostActive = this.getBoostRemaining() > 0;
            if (boostActive) {
                this.setBoostRemaining(this.getBoostRemaining() - 1.0);
                if (this.getBoostMultiplier() > 1.0) {
                    this.setBoostMultiplier(Math.max(1.0, this.getBoostMultiplier() - DRIFT_BOOST_DECAY_RATE));
                }
                if (this.getBoostRemaining() <= 0) {
                    this.setBoostMultiplier(1.0);
                    // Clear tracking when boost ends
                    setBoostThrottleReleased(false);
                    setBoostMaintainActive(false);
                    setBoostMaintainSpeed(0.0);
                }
            } else {
                // Ensure flags are cleared if not boosting
                setBoostThrottleReleased(false);
                setBoostMaintainActive(false);
                setBoostMaintainSpeed(0.0);
            }

            // Detect throttle release/press transitions while boosting
            boolean lastThrottlePressed = Math.abs(getLastThrottleInputBoostValue()) > CONTROL_DEADZONE;
            if (boostActive) {
                if (lastThrottlePressed && !throttlePressed) {
                    // Player released throttle during boost -> "waste" future potential
                    setBoostThrottleReleased(true);
                }
                if (!lastThrottlePressed && throttlePressed) {
                    // Player pressed throttle again during boost after a release -> maintain current speed
                    if (getBoostThrottleReleased()) {
                        setBoostMaintainActive(true);
                        setBoostMaintainSpeed(currentVel); // signed speed to preserve direction
                        // We keep boostThrottleReleased true so repeated releases can lower the cap again
                    }
                }
            }

            // Store current steer input
            this.setLastSteerInput(steerInput);

            // --- EFFECTIVE MAX SPEED & ACCEL MULT ---
            double effectiveMaxSpeed = maxSpeed;
            double accelerationMultiplier = 1.0;

            if (isDrifting) {
                effectiveMaxSpeed *= DRIFT_SPEED_BOOST_FACTOR;
            }
            if (boostActive) {
                double boostMultiplier = this.getBoostMultiplier();
                effectiveMaxSpeed *= boostMultiplier; // override cap while active
                accelerationMultiplier = 1.0 + (boostMultiplier - 1.0) * 2.0;
            }

            // --- ACCEL/DECEL LOGIC ---
            // During boost, do NOT auto-push to boosted cap when throttle is released (player should slow down)
            double targetSpeed = throttleInput * effectiveMaxSpeed;

            // If in "maintain" mode, cap the target to not exceed the maintain speed
            if (boostActive && getBoostMaintainActive() && throttlePressed) {
                // Keep direction consistent with current velocity
                double capped = getBoostMaintainSpeed();
                // If player's input attempts to exceed the cap, clamp it back
                if (Math.signum(targetSpeed) == Math.signum(capped) && Math.abs(targetSpeed) > Math.abs(capped)) {
                    targetSpeed = capped;
                }
            }

            double speedError = targetSpeed - currentVel;
            double forceApplied = 0.0;

            SimplecarsMod.LOGGER.debug(
                    "Car properties: MaxSpeed={} Acceleration={} BoostRemaining={} BoostMultiplier={} EffectiveMaxSpeed={} EffectiveAcceleration={} CurrentVelocity={} TargetSpeed={}",
                    this.carMaxSpeed, this.carAcceleration, this.getBoostRemaining(), this.getBoostMultiplier(),
                    effectiveMaxSpeed, acceleration * weightFactor * accelerationMultiplier, currentVel, targetSpeed
            );

            // Special "maintain speed" handling: counter drag only, don't accelerate past maintain speed
            if (boostActive && getBoostMaintainActive() && throttlePressed) {
                forceApplied = drag * absVel * absVel * Math.signum(currentVel) * weightFactor; // netForce ~ 0 after drag subtraction -> keep current speed
            }
            // Handle different cases of movement control
            else if (isDrifting) {
                if (throttlePressed) {
                    double driftAccel = acceleration * weightFactor *
                            (1.0 - DRIFT_TRACTION_REDUCTION + DRIFT_THROTTLE_BONUS);
                    driftAccel *= accelerationMultiplier; // boost helps during drift too
                    forceApplied = Math.signum(speedError) * driftAccel;
                } else {
                    double naturalDecel = (ROLLING_RESISTANCE + AIR_RESISTANCE * absVel) * weightFactor * 0.6;
                    forceApplied = -Math.signum(currentVel) * naturalDecel;
                }
            } else if (isQuickStop && absVel > HANDBRAKE_STOP_VELOCITY) {
                double speedFactor = Math.min(absVel / 0.3, 1.0);
                double brakeFactor = handbrakeEffectiveness * (HANDBRAKE_FORCE_MULTIPLIER +
                        (speedFactor * QUICK_STOP_EXTRA_FORCE));
                if (absVel < 0.1) {
                    brakeFactor *= HANDBRAKE_LOW_SPEED_SCALING +
                            ((absVel / 0.1) * (1.0 - HANDBRAKE_LOW_SPEED_SCALING));
                }
                double rawBrakeForce = brakeStrength * brakeFactor;
                double clampedBrakeForce = Math.min(rawBrakeForce, HANDBRAKE_FORCE_CLAMP);
                forceApplied = -Math.signum(currentVel) * clampedBrakeForce * HANDBRAKE_GRADUAL_FACTOR;
            } else if (throttlePressed) {
                if (Math.signum(throttleInput) == Math.signum(currentVel) || Math.abs(currentVel) < 0.001) {
                    forceApplied = Math.signum(speedError) * acceleration * weightFactor * accelerationMultiplier;
                } else {
                    forceApplied = Math.signum(speedError) * brakeStrength * weightFactor * ENHANCED_BRAKING_MULTIPLIER;
                }
            } else {
                if (absVel > 0.001) {
                    double naturalDecel = (ROLLING_RESISTANCE + AIR_RESISTANCE * absVel) * weightFactor;
                    forceApplied = -Math.signum(currentVel) * naturalDecel;
                }
            }

            // Persistent thrust while boosted: ONLY when throttle is pressed and not in maintain mode
            if (boostActive && !isQuickStop && throttlePressed && !getBoostMaintainActive()) {
                double boostAccel = acceleration * weightFactor * (this.getBoostMultiplier() - 1.0) * BOOST_THRUST_FACTOR;
                double desiredDir = Math.signum(throttleInput);
                forceApplied += boostAccel * desiredDir;
            }

            // Apply acceleration with drag consideration
            double dragForce = drag * absVel * absVel * Math.signum(currentVel) * weightFactor;
            double netForce = forceApplied - dragForce;

            double deltaV = netForce * TICK_DELTA_TIME;

            double nextVel = currentVel + deltaV;

            // In maintain mode, do not allow accelerating beyond the maintain speed cap
            if (boostActive && getBoostMaintainActive() && throttlePressed) {
                if (Math.signum(nextVel) == Math.signum(getBoostMaintainSpeed()) &&
                        Math.abs(nextVel) > Math.abs(getBoostMaintainSpeed()) + BOOST_MAINTAIN_EPSILON) {
                    nextVel = getBoostMaintainSpeed();
                }
            }

            if ((!throttlePressed || (isQuickStop && absVel <= HANDBRAKE_STOP_VELOCITY)) &&
                    Math.abs(nextVel) < COMPLETE_STOP_THRESHOLD) {
                nextVel = 0.0;
            }

            // --- LATERAL SLIP (unchanged except for variables used) ---
            double lateralSlippage = 0.0;
            if (isDrifting) {
                double driftDurationFactor = Math.min(this.getDriftDuration() / 5.0, 1.0);
                double steerInfluence = Math.signum(steerInput) * Math.min(Math.abs(steerInput) * 1.5, 1.0);
                double passiveSlip = DRIFT_PASSIVE_SLIP * Math.signum(steerInput == 0 ? getLastSteerInput() : steerInput);
                double baseSlip = (DRIFT_SIDE_SLIP_FACTOR * steerInfluence + passiveSlip) * handbrakeEffectiveness;

                double speedFactor = Math.pow(absVel / effectiveMaxSpeed, DRIFT_SIDE_SLIP_SPEED_SCALING);
                speedFactor = Mth.clamp(speedFactor, 0.2, 1.0);

                lateralSlippage = baseSlip * speedFactor * absVel * driftDurationFactor;

                if (Math.abs(steerInput) > 0.1 && absVel > DRIFT_SPEED_THRESHOLD) {
                    double minSlip = absVel * 0.2 * Math.signum(steerInput);
                    lateralSlippage = Math.signum(lateralSlippage) == Math.signum(minSlip) ?
                            Math.max(Math.abs(lateralSlippage), Math.abs(minSlip)) * Math.signum(lateralSlippage) :
                            lateralSlippage;
                }

                double maxSlip = absVel * 0.8;
                lateralSlippage = Mth.clamp(lateralSlippage, -maxSlip, maxSlip);
            }

            // --- STEERING ---
            double effectiveSteer = 0.0;
            if (Math.abs(steerInput) > 0.001 && absVel > MIN_TURNING_SPEED) {
                double baseSteer = steerInput * steeringFactor * TURNING_RADIUS_FACTOR;

                double speedRatio = absVel / effectiveMaxSpeed;
                double steerReduction = 1.0 - (speedRatio * STEERING_SPEED_FALLOFF);
                steerReduction = Mth.clamp(steerReduction, MIN_STEERING_EFFECTIVENESS, 1.0);

                double weightSteerFactor = Math.pow(weightFactor, WEIGHT_STEERING_POWER);

                double driftSteeringMult = 1.0;
                if (isDrifting) {
                    double driftFactor = Math.min(this.getDriftDuration() / 8.0, 1.0);
                    driftSteeringMult = 1.0 + (DRIFT_TURNING_ENHANCEMENT - 1.0) * driftFactor;
                }

                effectiveSteer = baseSteer * steerReduction * weightSteerFactor * driftSteeringMult;

                if (isReversing) {
                    effectiveSteer *= -REVERSE_STEERING_FACTOR;
                }

                if (isDrifting) {
                    double driftFactor = Math.min(this.getDriftDuration() / 8.0, 1.0);
                    double yawEnhancement = 1.0 + (DRIFT_YAW_MULTIPLIER - 1.0) * driftFactor;
                    effectiveSteer *= yawEnhancement;
                }

                double maxSteerAngle = MAX_STEERING_ANGLE_BASE * (1.0 - speedRatio * MAX_STEERING_SPEED_REDUCTION);
                effectiveSteer = Mth.clamp(effectiveSteer, -maxSteerAngle, maxSteerAngle);
            }

            // --- APPLY YAW ---
            if (Math.abs(effectiveSteer) > 0.001) {
                double steerVelocityFactor = Math.min(absVel / (effectiveMaxSpeed * STEERING_VELOCITY_DENOMINATOR), 1.0);
                float yawChange = (float)(effectiveSteer * steerVelocityFactor * YAW_CHANGE_MULTIPLIER);

                this.setYRot(this.getYRot() + yawChange);
                this.yRotO = this.getYRot();
            }

            // --- MOVEMENT VECTOR ---
            float yaw = this.getYRot();
            double radYaw = Math.toRadians(yaw);
            double forwardX = -Mth.sin((float) radYaw);
            double forwardZ = Mth.cos((float) radYaw);

            double lateralX = 0;
            double lateralZ = 0;
            if (Math.abs(lateralSlippage) > 0.001) {
                lateralX = -forwardZ * lateralSlippage;
                lateralZ = forwardX * lateralSlippage;
            }

            double dy = this.getDeltaMovement().y;

            Vec3 horizontalMovement = new Vec3(
                    (forwardX * nextVel + lateralX) * DOWNFORCE_MULTIPLIER,
                    dy,
                    (forwardZ * nextVel + lateralZ) * DOWNFORCE_MULTIPLIER
            );

            this.setDeltaMovement(horizontalMovement);
            updateCarForwardVelocity(nextVel);

            // Fuel consumption
            double fuelMultiplier = 1.0;
            if (isDrifting) fuelMultiplier *= 1.5;
            if (boostActive) fuelMultiplier *= this.getBoostMultiplier();

            if (throttlePressed || absVel > FUEL_CONSUMPTION_IDLE) {
                useUpFuel((Math.abs(forceApplied) + absVel * FUEL_CONSUMPTION_IDLE) * fuelMultiplier);
            }

            // Update last throttle for next tick transitions
            setLastThrottleInputBoostValue(throttleInput);

            super.travel(horizontalMovement);
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

    public AbstractFrameItem getFrameItem() {
        return this.frameItem;
    }

}
