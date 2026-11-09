package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xbox360pro.custombeds.block.CustomBedBlock;
import dev.xbox360pro.custombeds.block.CustomBedBlockEntity;
import dev.xbox360pro.custombeds.data.DesignRef;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class CustomBedRenderer implements BlockEntityRenderer<CustomBedBlockEntity> {

    public CustomBedRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CustomBedBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof CustomBedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) return;

        Direction facing = state.getValue(BedBlock.FACING);
        DesignRef design = be.getDesign();
        ResourceLocation tex = ClientBedTextures.texture(design == null ? null : design.id());

        pose.pushPose();
        // rotate so local +z points from the head toward the foot
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        BedGeometry.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay);
        pose.popPose();
    }
}
