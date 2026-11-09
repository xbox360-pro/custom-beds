package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws a whole 2-block bed. Units are "pixels" (16 = one block).
 * Local space: x 0..16 across, y up, z 0..32 from the HEAD (z=0) to the FOOT (z=32).
 * The texture layout comes from {@link BedImageMaker}.
 */
public final class BedGeometry {
    private BedGeometry() {}

    private static final float TW = BedImageMaker.W;
    private static final float TH = BedImageMaker.H;

    private static final float[] WOOD = {0, BedImageMaker.WOOD_V, 64, 128};
    private static final float[] WHITE = {0, BedImageMaker.WHITE_V, 64, BedImageMaker.WOOD_V};

    // face bits for skipping hidden faces
    private static final int UP = 1, DOWN = 2, NORTH = 4, SOUTH = 8, WEST = 16, EAST = 32, ALL = 63;

    public static void render(PoseStack poseStack, VertexConsumer vc, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        // legs
        box(m, n, vc, 0, 0, 0, 3, 3, 3, WOOD, ALL & ~UP, light, overlay);
        box(m, n, vc, 13, 0, 0, 16, 3, 3, WOOD, ALL & ~UP, light, overlay);
        box(m, n, vc, 0, 0, 29, 3, 3, 32, WOOD, ALL & ~UP, light, overlay);
        box(m, n, vc, 13, 0, 29, 16, 3, 32, WOOD, ALL & ~UP, light, overlay);
        // frame
        box(m, n, vc, 0, 3, 0, 16, 5, 32, WOOD, ALL & ~UP, light, overlay);
        // sheet under the pillow (head end)
        box(m, n, vc, 0, 5, 0, 16, 9, 6, WHITE, ALL & ~DOWN & ~SOUTH, light, overlay);
        // pillow
        box(m, n, vc, 2, 9, 1, 14, 11, 5, WHITE, ALL & ~DOWN, light, overlay);
        // the blanket with the uploaded picture
        blanket(m, n, vc, light, overlay);
    }

    private static void blanket(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, int overlay) {
        float x0 = 0, x1 = 16, y0 = 5, y1 = 9, z0 = 6, z1 = 32;
        float ih = BedImageMaker.IMG_H;
        // top: picture upright when you stand at the foot of the bed
        quad(m, n, vc, light, overlay, 0, 1, 0,
                x0, y1, z0, 0, 0,
                x1, y1, z0, 64, 0,
                x1, y1, z1, 64, ih,
                x0, y1, z1, 0, ih);
        // left side: stretch the picture's left column down the side
        quad(m, n, vc, light, overlay, -1, 0, 0,
                x0, y1, z0, 0.1f, 0,
                x0, y1, z1, 0.1f, ih,
                x0, y0, z1, 0.9f, ih,
                x0, y0, z0, 0.9f, 0);
        // right side
        quad(m, n, vc, light, overlay, 1, 0, 0,
                x1, y1, z1, 63.9f, ih,
                x1, y1, z0, 63.9f, 0,
                x1, y0, z0, 63.1f, 0,
                x1, y0, z1, 63.1f, ih);
        // foot end: the picture's bottom row
        quad(m, n, vc, light, overlay, 0, 0, 1,
                x0, y1, z1, 0, ih - 0.9f,
                x1, y1, z1, 64, ih - 0.9f,
                x1, y0, z1, 64, ih - 0.1f,
                x0, y0, z1, 0, ih - 0.1f);
    }

    private static void box(Matrix4f m, Matrix3f n, VertexConsumer vc,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float[] uv, int faces, int light, int overlay) {
        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];
        if ((faces & UP) != 0) quad(m, n, vc, light, overlay, 0, 1, 0,
                x0, y1, z0, u0, v0, x1, y1, z0, u1, v0, x1, y1, z1, u1, v1, x0, y1, z1, u0, v1);
        if ((faces & DOWN) != 0) quad(m, n, vc, light, overlay, 0, -1, 0,
                x0, y0, z1, u0, v0, x1, y0, z1, u1, v0, x1, y0, z0, u1, v1, x0, y0, z0, u0, v1);
        if ((faces & NORTH) != 0) quad(m, n, vc, light, overlay, 0, 0, -1,
                x1, y1, z0, u0, v0, x0, y1, z0, u1, v0, x0, y0, z0, u1, v1, x1, y0, z0, u0, v1);
        if ((faces & SOUTH) != 0) quad(m, n, vc, light, overlay, 0, 0, 1,
                x0, y1, z1, u0, v0, x1, y1, z1, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1);
        if ((faces & WEST) != 0) quad(m, n, vc, light, overlay, -1, 0, 0,
                x0, y1, z0, u0, v0, x0, y1, z1, u1, v0, x0, y0, z1, u1, v1, x0, y0, z0, u0, v1);
        if ((faces & EAST) != 0) quad(m, n, vc, light, overlay, 1, 0, 0,
                x1, y1, z1, u0, v0, x1, y1, z0, u1, v0, x1, y0, z0, u1, v1, x1, y0, z1, u0, v1);
    }

    private static void quad(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, int overlay,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        vertex(m, n, vc, light, overlay, nx, ny, nz, ax, ay, az, au, av);
        vertex(m, n, vc, light, overlay, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(m, n, vc, light, overlay, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(m, n, vc, light, overlay, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, int overlay,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        vc.vertex(m, x / 16f, y / 16f, z / 16f)
                .color(1f, 1f, 1f, 1f)
                .uv(u / TW, v / TH)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }
}
