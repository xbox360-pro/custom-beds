package dev.xbox360pro.custombeds.entity;

import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.item.CustomPaintingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** A painting that shows an uploaded picture. Size is 1-4 blocks each way. */
public class CustomPaintingEntity extends HangingEntity implements IEntityAdditionalSpawnData {
    public static final int MAX_SIZE = 4;

    @Nullable
    private DesignRef design;
    private int widthBlocks = 1;
    private int heightBlocks = 1;

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, Level level) {
        super(type, level);
    }

    public CustomPaintingEntity(Level level, BlockPos pos, Direction facing, @Nullable DesignRef design, int w, int h) {
        super(ModRegistry.CUSTOM_PAINTING.get(), level, pos);
        this.design = design;
        this.widthBlocks = clampSize(w);
        this.heightBlocks = clampSize(h);
        setDirection(facing);
    }

    private static int clampSize(int v) {
        return Mth.clamp(v, 1, MAX_SIZE);
    }

    @Nullable
    public DesignRef getDesign() {
        return design;
    }

    public int getWidthBlocks() {
        return widthBlocks;
    }

    public int getHeightBlocks() {
        return heightBlocks;
    }

    public Direction getFacing() {
        return direction;
    }

    @Override
    public int getWidth() {
        return widthBlocks * 16;
    }

    @Override
    public int getHeight() {
        return heightBlocks * 16;
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
        playSound(SoundEvents.PAINTING_BREAK, 1.0f, 1.0f);
        if (breaker instanceof Player player && player.getAbilities().instabuild) return;
        spawnAtLocation(makeItem());
    }

    private ItemStack makeItem() {
        return CustomPaintingItem.create(design, widthBlocks, heightBlocks);
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
    }

    @Override
    public ItemStack getPickResult() {
        return makeItem();
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        setPos(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        setPos(x, y, z);
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Facing", (byte) direction.get2DDataValue());
        tag.putInt("W", widthBlocks);
        tag.putInt("H", heightBlocks);
        if (design != null) tag.put("Design", design.toTag());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        widthBlocks = clampSize(tag.getInt("W"));
        heightBlocks = clampSize(tag.getInt("H"));
        design = tag.contains("Design") ? DesignRef.fromTag(tag.getCompound("Design")) : null;
        direction = Direction.from2DDataValue(tag.getByte("Facing"));
        super.readAdditionalSaveData(tag);
        setDirection(direction);
    }

    // ------------------------------------------------------------------ syncing to clients

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(direction.get2DDataValue());
        buf.writeByte(widthBlocks);
        buf.writeByte(heightBlocks);
        buf.writeBoolean(design != null);
        if (design != null) {
            buf.writeUUID(design.id());
            buf.writeUtf(design.name(), 256);
            buf.writeUtf(design.designer(), 256);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        Direction facing = Direction.from2DDataValue(buf.readByte());
        widthBlocks = clampSize(buf.readByte());
        heightBlocks = clampSize(buf.readByte());
        design = buf.readBoolean() ? new DesignRef(buf.readUUID(), buf.readUtf(256), buf.readUtf(256)) : null;
        setDirection(facing);
    }
}
