package dev.xbox360pro.custombeds.block;

import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.data.DesignRef;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class CustomBedBlockEntity extends BlockEntity {
    @Nullable
    private DesignRef design;

    public CustomBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CUSTOM_BED_BE.get(), pos, state);
    }

    @Nullable
    public DesignRef getDesign() {
        return design;
    }

    public void setDesign(@Nullable DesignRef design) {
        this.design = design;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        design = tag.contains("Design") ? DesignRef.fromTag(tag.getCompound("Design")) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (design != null) tag.put("Design", design.toTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // the head half draws the whole 2-block bed
        return new AABB(worldPosition).inflate(1.0);
    }
}
