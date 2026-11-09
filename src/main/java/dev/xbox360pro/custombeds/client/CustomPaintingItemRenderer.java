package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.item.CustomPaintingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Shows the painting's actual picture as the item icon, framed, keeping its shape. */
public class CustomPaintingItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation BACK = new ResourceLocation("minecraft", "textures/painting/back.png");

    public CustomPaintingItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        DesignRef design = CustomPaintingItem.getDesign(stack);
        ResourceLocation tex = ClientBedTextures.texture(design == null ? null : design.id(), true);
        int w = CustomPaintingItem.getW(stack), h = CustomPaintingItem.getH(stack);

        // fit inside a 14/16 square, centered, keeping aspect
        float max = 14f / 16f;
        float fw = w >= h ? max : max * w / h;
        float fh = h >= w ? max : max * h / w;
        float x0 = 0.5f - fw / 2, x1 = 0.5f + fw / 2, y0 = 0.5f - fh / 2, y1 = 0.5f + fh / 2;
        float frame = 1f / 16f;

        pose.pushPose();
        PoseStack.Pose p = pose.last();
        Matrix4f m = p.pose();
        Matrix3f n = p.normal();

        VertexConsumer pic = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        CustomPaintingRenderer.v(pic, m, n, x0, y1, 0.53f, 0, 0, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(pic, m, n, x0, y0, 0.53f, 0, 1, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(pic, m, n, x1, y0, 0.53f, 1, 1, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(pic, m, n, x1, y1, 0.53f, 1, 0, 0, 0, 1, light, overlay);

        VertexConsumer wood = buffers.getBuffer(RenderType.entityCutoutNoCull(BACK));
        float fx0 = x0 - frame, fx1 = x1 + frame, fy0 = y0 - frame, fy1 = y1 + frame;
        CustomPaintingRenderer.v(wood, m, n, fx0, fy1, 0.5f, 0, 0, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(wood, m, n, fx0, fy0, 0.5f, 0, 1, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(wood, m, n, fx1, fy0, 0.5f, 1, 1, 0, 0, 1, light, overlay);
        CustomPaintingRenderer.v(wood, m, n, fx1, fy1, 0.5f, 1, 0, 0, 0, 1, light, overlay);
        pose.popPose();
    }
}
