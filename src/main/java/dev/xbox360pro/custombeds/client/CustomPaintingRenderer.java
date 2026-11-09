package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.entity.CustomPaintingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity> {
    private static final ResourceLocation BACK = new ResourceLocation("minecraft", "textures/painting/back.png");

    public CustomPaintingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CustomPaintingEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        DesignRef design = entity.getDesign();
        ResourceLocation tex = ClientBedTextures.texture(design == null ? null : design.id(), true);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        pose.scale(0.0625f, 0.0625f, 0.0625f);
        float hw = entity.getWidthBlocks() * 8f;
        float hh = entity.getHeightBlocks() * 8f;
        int overlay = OverlayTexture.NO_OVERLAY;
        PoseStack.Pose p = pose.last();
        Matrix4f m = p.pose();
        Matrix3f n = p.normal();

        // the picture (front at z = -0.5, same as vanilla paintings)
        VertexConsumer front = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        v(front, m, n, hw, hh, -0.5f, 0, 0, 0, 0, -1, light, overlay);
        v(front, m, n, -hw, hh, -0.5f, 1, 0, 0, 0, -1, light, overlay);
        v(front, m, n, -hw, -hh, -0.5f, 1, 1, 0, 0, -1, light, overlay);
        v(front, m, n, hw, -hh, -0.5f, 0, 1, 0, 0, -1, light, overlay);

        // wooden back and edges
        VertexConsumer back = buffers.getBuffer(RenderType.entitySolid(BACK));
        float bu = entity.getWidthBlocks(), bv = entity.getHeightBlocks();
        v(back, m, n, -hw, hh, 0.5f, 0, 0, 0, 0, 1, light, overlay);
        v(back, m, n, hw, hh, 0.5f, bu, 0, 0, 0, 1, light, overlay);
        v(back, m, n, hw, -hh, 0.5f, bu, bv, 0, 0, 1, light, overlay);
        v(back, m, n, -hw, -hh, 0.5f, 0, bv, 0, 0, 1, light, overlay);
        // top
        v(back, m, n, hw, hh, -0.5f, 0, 0, 0, 1, 0, light, overlay);
        v(back, m, n, hw, hh, 0.5f, 0, 0.0625f, 0, 1, 0, light, overlay);
        v(back, m, n, -hw, hh, 0.5f, bu, 0.0625f, 0, 1, 0, light, overlay);
        v(back, m, n, -hw, hh, -0.5f, bu, 0, 0, 1, 0, light, overlay);
        // bottom
        v(back, m, n, -hw, -hh, -0.5f, 0, 0, 0, -1, 0, light, overlay);
        v(back, m, n, -hw, -hh, 0.5f, 0, 0.0625f, 0, -1, 0, light, overlay);
        v(back, m, n, hw, -hh, 0.5f, bu, 0.0625f, 0, -1, 0, light, overlay);
        v(back, m, n, hw, -hh, -0.5f, bu, 0, 0, -1, 0, light, overlay);
        // sides
        v(back, m, n, hw, -hh, -0.5f, 0, 0, 1, 0, 0, light, overlay);
        v(back, m, n, hw, -hh, 0.5f, 0.0625f, 0, 1, 0, 0, light, overlay);
        v(back, m, n, hw, hh, 0.5f, 0.0625f, bv, 1, 0, 0, light, overlay);
        v(back, m, n, hw, hh, -0.5f, 0, bv, 1, 0, 0, light, overlay);
        v(back, m, n, -hw, hh, -0.5f, 0, 0, -1, 0, 0, light, overlay);
        v(back, m, n, -hw, hh, 0.5f, 0.0625f, 0, -1, 0, 0, light, overlay);
        v(back, m, n, -hw, -hh, 0.5f, 0.0625f, bv, -1, 0, 0, light, overlay);
        v(back, m, n, -hw, -hh, -0.5f, 0, bv, -1, 0, 0, light, overlay);

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                  float nx, float ny, float nz, int light, int overlay) {
        vc.vertex(m, x, y, z).color(1f, 1f, 1f, 1f).uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(n, nx, ny, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(CustomPaintingEntity entity) {
        DesignRef design = entity.getDesign();
        return ClientBedTextures.texture(design == null ? null : design.id(), true);
    }
}
