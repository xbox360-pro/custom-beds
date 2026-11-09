package dev.xbox360pro.custombeds.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Turns any picture into a 64x128 bed texture.
 * <pre>
 *  rows   0..103  the blanket (your picture, center-cropped to the bed's shape)
 *  rows 104..111  pillow / sheet (white)
 *  rows 112..127  wooden frame
 * </pre>
 */
public final class BedImageMaker {
    private BedImageMaker() {}

    public static final int W = 64;
    public static final int H = 128;
    public static final int IMG_H = 104;
    public static final int WHITE_V = 104;
    public static final int WOOD_V = 112;

    public static final int PAINTING_PX_PER_BLOCK = 64;

    /** Bed texture: picture on the blanket plus pillow and wood strips. */
    public static BufferedImage compose(BufferedImage src) {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        BufferedImage blanket = resampleCrop(src, W, IMG_H);
        out.setRGB(0, 0, W, IMG_H, blanket.getRGB(0, 0, W, IMG_H, null, 0, W), 0, W);
        paintPillowAndWood(out);
        return out;
    }

    /** Painting texture: picture center-cropped to w x h blocks, 64 pixels per block. */
    public static BufferedImage composePainting(BufferedImage src, int w, int h) {
        return resampleCrop(src, w * PAINTING_PX_PER_BLOCK, h * PAINTING_PX_PER_BLOCK);
    }

    /** Shrinks very large pictures so re-processing (e.g. changing painting size) stays instant. */
    public static BufferedImage shrink(BufferedImage src, int maxSide) {
        int sw = src.getWidth(), sh = src.getHeight();
        if (Math.max(sw, sh) <= maxSide) return src;
        double s = maxSide / (double) Math.max(sw, sh);
        return resampleCrop(src, Math.max(1, (int) Math.round(sw * s)), Math.max(1, (int) Math.round(sh * s)), false);
    }

    /** Picks a painting size (1-4 blocks each way) that matches the picture's shape. */
    public static int[] suggestPaintingSize(BufferedImage src) {
        double aspect = Math.log(src.getWidth() / (double) src.getHeight());
        int bw = 1, bh = 1;
        double best = Double.MAX_VALUE;
        for (int w = 1; w <= 4; w++) {
            for (int h = 1; h <= 4; h++) {
                double score = Math.abs(Math.log(w / (double) h) - aspect) * 10 + Math.abs(w * h - 4) * 0.1;
                if (score < best) {
                    best = score;
                    bw = w;
                    bh = h;
                }
            }
        }
        return new int[]{bw, bh};
    }

    public static BufferedImage resampleCrop(BufferedImage src, int outW, int outH) {
        return resampleCrop(src, outW, outH, true);
    }

    /** Center-crops to the output's shape and box-filter resamples (smooth for photos, crisp for pixel art). */
    private static BufferedImage resampleCrop(BufferedImage src, int outW, int outH, boolean onWhite) {
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        int sw = src.getWidth(), sh = src.getHeight();
        int[] px = src.getRGB(0, 0, sw, sh, null, 0, sw);

        double target = outW / (double) outH;
        double cx = 0, cy = 0, cw = sw, ch = sh;
        if (sw / (double) sh > target) {
            cw = sh * target;
            cx = (sw - cw) / 2.0;
        } else {
            ch = sw / target;
            cy = (sh - ch) / 2.0;
        }

        for (int ty = 0; ty < outH; ty++) {
            int y0 = clamp((int) Math.floor(cy + ty * ch / outH), 0, sh - 1);
            int y1 = clamp((int) Math.ceil(cy + (ty + 1) * ch / outH), y0 + 1, sh);
            for (int tx = 0; tx < outW; tx++) {
                int x0 = clamp((int) Math.floor(cx + tx * cw / outW), 0, sw - 1);
                int x1 = clamp((int) Math.ceil(cx + (tx + 1) * cw / outW), x0 + 1, sw);
                long r = 0, g = 0, b = 0, a = 0, n = 0;
                for (int y = y0; y < y1; y++) {
                    int row = y * sw;
                    for (int x = x0; x < x1; x++) {
                        int c = px[row + x];
                        int ca = (c >>> 24) & 0xFF;
                        r += (long) ((c >> 16) & 0xFF) * ca;
                        g += (long) ((c >> 8) & 0xFF) * ca;
                        b += (long) (c & 0xFF) * ca;
                        a += ca;
                        n++;
                    }
                }
                int argb;
                if (onWhite) {
                    // composite over white so transparent images don't leave holes
                    double alpha = a / (255.0 * n);
                    int rr = (int) Math.round(a == 0 ? 255 : (r / (double) a) * alpha + 255 * (1 - alpha));
                    int gg = (int) Math.round(a == 0 ? 255 : (g / (double) a) * alpha + 255 * (1 - alpha));
                    int bb = (int) Math.round(a == 0 ? 255 : (b / (double) a) * alpha + 255 * (1 - alpha));
                    argb = 0xFF000000 | clamp(rr, 0, 255) << 16 | clamp(gg, 0, 255) << 8 | clamp(bb, 0, 255);
                } else {
                    int aa = (int) Math.round(a / (double) n);
                    int rr = a == 0 ? 0 : (int) Math.round(r / (double) a);
                    int gg = a == 0 ? 0 : (int) Math.round(g / (double) a);
                    int bb = a == 0 ? 0 : (int) Math.round(b / (double) a);
                    argb = clamp(aa, 0, 255) << 24 | clamp(rr, 0, 255) << 16 | clamp(gg, 0, 255) << 8 | clamp(bb, 0, 255);
                }
                out.setRGB(tx, ty, argb);
            }
        }
        return out;
    }

    /** Placeholder shown while a painting's image is still downloading. */
    public static BufferedImage paintingPlaceholder() {
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                out.setRGB(x, y, ((x + y) / 3) % 2 == 0 ? 0xFFB9C6E0 : 0xFFA6B4D3);
            }
        }
        return out;
    }

    /** Placeholder shown while a bed's image is still downloading. */
    public static BufferedImage placeholder() {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < IMG_H; y++) {
            for (int x = 0; x < W; x++) {
                boolean stripe = ((x + y) / 6) % 2 == 0;
                out.setRGB(x, y, stripe ? 0xFFB9C6E0 : 0xFFA6B4D3);
            }
        }
        paintPillowAndWood(out);
        return out;
    }

    private static void paintPillowAndWood(BufferedImage out) {
        Random rand = new Random(1234);
        for (int y = WHITE_V; y < WOOD_V; y++) {
            for (int x = 0; x < W; x++) {
                int v = 238 + rand.nextInt(12);
                out.setRGB(x, y, 0xFF000000 | v << 16 | v << 8 | v);
            }
        }
        for (int y = WOOD_V; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double grain = Math.sin(x * 0.35 + Math.sin(y * 0.9) * 1.5) * 0.08 + rand.nextDouble() * 0.06;
                int r = clamp((int) (150 * (0.9 + grain)), 0, 255);
                int g = clamp((int) (104 * (0.9 + grain)), 0, 255);
                int b = clamp((int) (62 * (0.9 + grain)), 0, 255);
                out.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b);
            }
        }
    }

    public static byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(img, "png", out)) throw new IOException("No PNG writer");
        return out.toByteArray();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
