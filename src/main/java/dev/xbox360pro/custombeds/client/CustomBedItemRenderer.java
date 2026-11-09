package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.item.CustomBedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws a mini 3D version of the bed (with its picture) for the item in hand / inventory. */
public class CustomBedItemRenderer extends BlockEntityWithoutLevelRenderer {

    public CustomBedItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        DesignRef design = CustomBedItem.getDesign(stack);
        ResourceLocation tex = ClientBedTextures.texture(design == null ? null : design.id());
        pose.pushPose();
        // the bed is 1 x 0.69 x 2 blocks; shrink it to fit inside one block, centered
        pose.translate(0.25, 0.33, 0.0);
        pose.scale(0.5f, 0.5f, 0.5f);
        BedGeometry.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay);
        pose.popPose();
    }
}
