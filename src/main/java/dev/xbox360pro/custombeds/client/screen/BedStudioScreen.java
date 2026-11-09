package dev.xbox360pro.custombeds.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import dev.xbox360pro.custombeds.CustomBeds;
import dev.xbox360pro.custombeds.client.BedImageMaker;
import dev.xbox360pro.custombeds.client.ClientBedTextures;
import dev.xbox360pro.custombeds.client.ClientPacketHandler;
import dev.xbox360pro.custombeds.client.FilePicker;
import dev.xbox360pro.custombeds.network.Network;
import dev.xbox360pro.custombeds.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Press B: upload a picture to make a new bed, and browse every bed made on this world. */
public class BedStudioScreen extends Screen {
    private static final int PANEL_W = 196;
    private static final int GAP = 8;
    private static final int PANEL_H = 214;
    private static final int ROW_H = 30;
    private static final ResourceLocation PREVIEW_ID = new ResourceLocation(CustomBeds.MODID, "dynamic/preview");

    // library state is shared so it survives re-opening the screen
    private static List<Packets.DesignEntry> designs = new ArrayList<>();
    private static boolean canCopyAll;
    private static boolean listLoaded;

    private EditBox nameBox;
    private EditBox pathBox;
    private Button loadButton;
    private Button browseButton;
    private Button createButton;
    private Button copyButton;
    private Button giftButton;

    private byte[] pendingPng;
    private DynamicTexture previewTexture;
    private boolean busy;
    private Component status = Component.translatable("screen.custombeds.status.idle");
    private int statusColor = 0xA0A0A0;

    private int scroll;
    private int selected = -1;
    private int left;
    private int top;

    public BedStudioScreen() {
        super(Component.translatable("screen.custombeds.title"));
    }

    // ------------------------------------------------------------------ layout

    private int rightX() { return left + PANEL_W + GAP; }
    private int listX() { return rightX() + 6; }
    private int listY() { return top + 22; }
    private int listW() { return PANEL_W - 12; }
    private int listH() { return 158; }

    @Override
    protected void init() {
        left = (width - (PANEL_W * 2 + GAP)) / 2;
        top = Math.max(4, (height - PANEL_H) / 2);
        int x = left + 8;
        int w = PANEL_W - 16;

        String oldName = nameBox != null ? nameBox.getValue() : "";
        String oldPath = pathBox != null ? pathBox.getValue() : "";

        nameBox = new EditBox(font, x, top + 22, w, 16, Component.translatable("screen.custombeds.name"));
        nameBox.setMaxLength(Packets.MAX_NAME);
        nameBox.setHint(Component.translatable("screen.custombeds.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(oldName);
        addRenderableWidget(nameBox);

        pathBox = new EditBox(font, x, top + 44, 96, 16, Component.translatable("screen.custombeds.path"));
        pathBox.setMaxLength(1024);
        pathBox.setHint(Component.translatable("screen.custombeds.path_hint").withStyle(ChatFormatting.DARK_GRAY));
        pathBox.setValue(oldPath);
        addRenderableWidget(pathBox);

        loadButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.load"),
                b -> loadPath(pathBox.getValue())).bounds(x + 98, top + 43, 34, 18).build());
        browseButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.browse"),
                b -> browse()).bounds(x + 134, top + 43, 46, 18).build());

        createButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.create"),
                b -> create()).bounds(x, top + 152, w, 20).build());

        copyButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.get_copy"),
                b -> getCopy()).bounds(rightX() + 6, top + 186, 89, 20).build());
        giftButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.gift"),
                b -> minecraft.setScreen(new GiftWrapScreen(this))).bounds(rightX() + 101, top + 186, 89, 20).build());

        if (pendingPng != null && previewTexture == null) makePreview(pendingPng);

        Network.sendToServer(new Packets.RequestList());
        updateButtons();
    }

    private void updateButtons() {
        createButton.active = pendingPng != null && !busy;
        loadButton.active = !busy;
        browseButton.active = !busy && !FilePicker.isOpen();
        Packets.DesignEntry e = selectedEntry();
        copyButton.active = e != null && (canCopyAll || isMine(e));
    }

    private Packets.DesignEntry selectedEntry() {
        return selected >= 0 && selected < designs.size() ? designs.get(selected) : null;
    }

    private boolean isMine(Packets.DesignEntry e) {
        return minecraft != null && minecraft.player != null && e.ownerId().equals(minecraft.player.getUUID());
    }

    // ------------------------------------------------------------------ actions

    private void browse() {
        setStatus(Component.translatable("screen.custombeds.status.picking"), 0xA0A0A0);
        FilePicker.pickImage(path -> {
            if (path != null && !path.isBlank()) {
                if (pathBox != null) pathBox.setValue(path);
                loadPath(path);
            } else {
                setStatus(Component.translatable("screen.custombeds.status.idle"), 0xA0A0A0);
            }
            if (createButton != null) updateButtons();
        });
        updateButtons();
    }

    private void loadPath(String raw) {
        String cleaned = raw == null ? "" : raw.trim();
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1); // Windows "Copy as path" adds quotes
        }
        if (cleaned.isEmpty()) {
            setStatus(Component.translatable("screen.custombeds.status.no_path"), 0xFF5555);
            return;
        }
        Path path;
        try {
            path = Path.of(cleaned);
        } catch (Exception e) {
            setStatus(Component.translatable("screen.custombeds.status.bad_path"), 0xFF5555);
            return;
        }
        if (!Files.isRegularFile(path)) {
            setStatus(Component.translatable("screen.custombeds.status.not_found"), 0xFF5555);
            return;
        }

        busy = true;
        setStatus(Component.translatable("screen.custombeds.status.loading"), 0xFFFF55);
        updateButtons();

        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage src = ImageIO.read(path.toFile());
                if (src == null) throw new IOException("unsupported format");
                return BedImageMaker.toPng(BedImageMaker.compose(src));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor()).whenComplete((png, err) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            if (err != null || png == null) {
                CustomBeds.LOGGER.warn("Couldn't read bed image {}", path, err);
                setStatus(Component.translatable("screen.custombeds.status.bad_image"), 0xFF5555);
            } else {
                pendingPng = png;
                makePreview(png);
                if (nameBox != null && nameBox.getValue().isBlank()) {
                    String file = path.getFileName().toString();
                    int dot = file.lastIndexOf('.');
                    if (dot > 0) file = file.substring(0, dot);
                    nameBox.setValue(file.length() > Packets.MAX_NAME ? file.substring(0, Packets.MAX_NAME) : file);
                }
                setStatus(Component.translatable("screen.custombeds.status.ready"), 0x55FF55);
            }
            if (createButton != null) updateButtons();
        }));
    }

    private void makePreview(byte[] png) {
        releasePreview();
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
            previewTexture = new DynamicTexture(img);
            Minecraft.getInstance().getTextureManager().register(PREVIEW_ID, previewTexture);
        } catch (IOException e) {
            previewTexture = null;
        }
    }

    private void releasePreview() {
        if (previewTexture != null) {
            Minecraft.getInstance().getTextureManager().release(PREVIEW_ID);
            previewTexture = null;
        }
    }

    private void create() {
        if (pendingPng == null || busy) return;
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) name = "Custom";
        busy = true;
        setStatus(Component.translatable("screen.custombeds.status.uploading"), 0xFFFF55);
        ClientPacketHandler.upload(name, pendingPng);
        updateButtons();
    }

    private void getCopy() {
        Packets.DesignEntry e = selectedEntry();
        if (e != null) Network.sendToServer(new Packets.GetCopy(e.id()));
    }

    public void onUploadResult(boolean ok, Component message) {
        busy = false;
        setStatus(message, ok ? 0x55FF55 : 0xFF5555);
        if (ok) {
            pendingPng = null;
            releasePreview();
            nameBox.setValue("");
            pathBox.setValue("");
            Network.sendToServer(new Packets.RequestList());
        }
        updateButtons();
    }

    public static void updateList(List<Packets.DesignEntry> entries, boolean copyAll) {
        designs = new ArrayList<>(entries);
        canCopyAll = copyAll;
        listLoaded = true;
        if (Minecraft.getInstance().screen instanceof BedStudioScreen s) {
            s.selected = -1;
            s.scroll = Mth.clamp(s.scroll, 0, s.maxScroll());
            s.updateButtons();
        }
    }

    public static void forgetList() {
        designs = new ArrayList<>();
        listLoaded = false;
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    // ------------------------------------------------------------------ input

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty() || busy) return;
        String p = paths.get(0).toString();
        pathBox.setValue(p);
        loadPath(p);
    }

    private boolean inList(double mx, double my) {
        return mx >= listX() && mx < listX() + listW() && my >= listY() && my < listY() + listH();
    }

    private int maxScroll() {
        return Math.max(0, designs.size() * ROW_H - listH());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inList(mx, my)) {
            int i = (int) ((my - listY() + scroll) / ROW_H);
            if (i >= 0 && i < designs.size()) {
                selected = i;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (inList(mx, my)) {
            scroll = Mth.clamp(scroll - (int) (delta * 16), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void tick() {
        nameBox.tick();
        pathBox.tick();
        browseButton.active = !busy && !FilePicker.isOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        releasePreview();
        super.removed();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int rx = rightX();

        // panels
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xD0141418);
        g.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF5A5A70);
        g.fill(rx, top, rx + PANEL_W, top + PANEL_H, 0xD0141418);
        g.renderOutline(rx, top, PANEL_W, PANEL_H, 0xFF5A5A70);

        g.drawString(font, Component.translatable("screen.custombeds.new_bed"), left + 8, top + 8, 0xFFD45E);
        g.drawString(font, Component.translatable("screen.custombeds.library"), rx + 8, top + 8, 0xFFD45E);

        // preview of the blanket
        int px = left + 8, py = top + 66;
        g.fill(px - 1, py - 1, px + 49, py + 79, 0xFF3A3A48);
        if (previewTexture != null) {
            g.blit(PREVIEW_ID, px, py, 48, 78, 0, 0, 64, BedImageMaker.IMG_H, 64, BedImageMaker.H);
        } else {
            g.fill(px, py, px + 48, py + 78, 0xFF22222C);
            g.drawCenteredString(font, "?", px + 24, py + 35, 0x777777);
        }

        // tips next to the preview
        int tx = px + 56, ty = py, tw = PANEL_W - 16 - 56;
        for (FormattedCharSequence line : font.split(Component.translatable("screen.custombeds.tips"), tw)) {
            g.drawString(font, line, tx, ty, 0x9A9AAA);
            ty += 10;
        }

        // status
        int sy = top + 178;
        for (FormattedCharSequence line : font.split(status, PANEL_W - 16)) {
            g.drawString(font, line, left + 8, sy, statusColor);
            sy += 10;
            if (sy > top + PANEL_H - 10) break;
        }

        renderLibrary(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderLibrary(GuiGraphics g, int mouseX, int mouseY) {
        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        g.fill(lx, ly, lx + lw, ly + lh, 0x80000000);

        if (designs.isEmpty()) {
            Component msg = Component.translatable(listLoaded ? "screen.custombeds.empty" : "screen.custombeds.loading");
            int y = ly + lh / 2 - 4;
            for (FormattedCharSequence line : font.split(msg, lw - 10)) {
                g.drawCenteredString(font, line, lx + lw / 2, y, 0x808080);
                y += 10;
            }
            return;
        }

        g.enableScissor(lx, ly, lx + lw, ly + lh);
        for (int i = 0; i < designs.size(); i++) {
            int y = ly + i * ROW_H - scroll;
            if (y + ROW_H < ly || y > ly + lh) continue;
            Packets.DesignEntry e = designs.get(i);
            boolean hovered = inList(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_H;
            if (i == selected) g.fill(lx, y, lx + lw, y + ROW_H, 0x60FFFFFF);
            else if (hovered) g.fill(lx, y, lx + lw, y + ROW_H, 0x25FFFFFF);

            ResourceLocation tex = ClientBedTextures.texture(e.id());
            g.blit(tex, lx + 4, y + 3, 15, 24, 0, 0, 64, BedImageMaker.IMG_H, 64, BedImageMaker.H);

            g.drawString(font, font.plainSubstrByWidth(e.name(), lw - 30), lx + 24, y + 6, 0xFFFFFF);
            Component by = isMine(e)
                    ? Component.translatable("screen.custombeds.by_you")
                    : Component.translatable("screen.custombeds.by", e.ownerName());
            g.drawString(font, by, lx + 24, y + 17, isMine(e) ? 0x7FD67F : 0x909090);
        }
        g.disableScissor();
    }
}
