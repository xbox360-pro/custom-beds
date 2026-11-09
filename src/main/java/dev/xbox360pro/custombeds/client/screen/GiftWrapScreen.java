package dev.xbox360pro.custombeds.client.screen;

import com.mojang.authlib.GameProfile;
import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.item.CustomBedItem;
import dev.xbox360pro.custombeds.item.CustomPaintingItem;
import dev.xbox360pro.custombeds.network.Network;
import dev.xbox360pro.custombeds.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Pick a custom bed, pick a friend, write a note, wrap it (and optionally send it straight to them). */
public class GiftWrapScreen extends Screen {
    private static final int W = 300;
    private static final int H = 200;
    private static final int COLS = 8;
    private static final int MAX_BEDS = 32;
    private static final int ROW = 12;
    private static final int VISIBLE_PLAYERS = 6;

    @Nullable
    private final Screen parent;
    private final List<Integer> bedSlots = new ArrayList<>();
    private final List<GameProfile> players = new ArrayList<>();
    private int selBed = -1;
    private int selPlayer = -1;
    private int playerScroll;
    private int left;
    private int top;

    private EditBox message;
    private Button wrapButton;
    private Button sendButton;

    public GiftWrapScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.custombeds.gift_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        gather();

        String oldMsg = message != null ? message.getValue() : "";
        message = new EditBox(font, left + 10, top + 120, W - 20, 16, Component.translatable("screen.custombeds.message"));
        message.setMaxLength(Packets.MAX_MESSAGE);
        message.setHint(Component.translatable("screen.custombeds.message_hint").withStyle(ChatFormatting.DARK_GRAY));
        message.setValue(oldMsg);
        addRenderableWidget(message);

        wrapButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.wrap"),
                b -> submit(false)).bounds(left + 10, top + 170, 90, 20).build());
        sendButton = addRenderableWidget(Button.builder(Component.translatable("screen.custombeds.wrap_send"),
                b -> submit(true)).bounds(left + 105, top + 170, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),
                b -> onClose()).bounds(left + 200, top + 170, 90, 20).build());
        updateButtons();
    }

    private void gather() {
        bedSlots.clear();
        players.clear();
        if (minecraft == null || minecraft.player == null) return;
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && bedSlots.size() < MAX_BEDS; i++) {
            if (inv.getItem(i).getItem() instanceof CustomBedItem
                    || inv.getItem(i).getItem() instanceof CustomPaintingItem) bedSlots.add(i);
        }
        if (selBed >= bedSlots.size()) selBed = -1;
        if (selBed < 0 && !bedSlots.isEmpty()) selBed = 0;

        UUID self = minecraft.player.getUUID();
        if (minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                GameProfile profile = info.getProfile();
                if (!profile.getId().equals(self)) players.add(profile);
            }
        }
        players.sort(Comparator.comparing(GameProfile::getName, String.CASE_INSENSITIVE_ORDER));
        if (selPlayer >= players.size()) selPlayer = -1;
    }

    private boolean hasWrap() {
        if (minecraft == null || minecraft.player == null) return false;
        if (minecraft.player.getAbilities().instabuild) return true;
        return wrapCount() > 0;
    }

    private int wrapCount() {
        Inventory inv = minecraft.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModRegistry.GIFT_WRAP.get())) n += s.getCount();
        }
        return n;
    }

    private void updateButtons() {
        boolean ok = selBed >= 0 && hasWrap();
        wrapButton.active = ok;
        sendButton.active = ok && selPlayer >= 0;
    }

    private void submit(boolean send) {
        if (selBed < 0 || selBed >= bedSlots.size()) return;
        UUID recipient = selPlayer >= 0 ? players.get(selPlayer).getId() : null;
        Network.sendToServer(new Packets.WrapGift(bedSlots.get(selBed), recipient, message.getValue(), send));
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        message.tick();
    }

    // ------------------------------------------------------------------ input

    private int bedX(int i) { return left + 10 + (i % COLS) * 18; }
    private int bedY(int i) { return top + 32 + (i / COLS) * 18; }
    private int listX() { return left + 168; }
    private int listY() { return top + 32; }
    private int listW() { return W - 178; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < bedSlots.size(); i++) {
            if (mx >= bedX(i) && mx < bedX(i) + 18 && my >= bedY(i) && my < bedY(i) + 18) {
                selBed = i;
                updateButtons();
                return true;
            }
        }
        if (mx >= listX() && mx < listX() + listW() && my >= listY() && my < listY() + VISIBLE_PLAYERS * ROW) {
            int i = (int) ((my - listY()) / ROW) + playerScroll;
            if (i >= 0 && i < players.size()) {
                selPlayer = (selPlayer == i) ? -1 : i; // click again to un-select
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= listX() && mx < listX() + listW() && my >= listY() && my < listY() + VISIBLE_PLAYERS * ROW) {
            playerScroll = Mth.clamp(playerScroll - (int) Math.signum(delta), 0, Math.max(0, players.size() - VISIBLE_PLAYERS));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left, top, left + W, top + H, 0xD0141418);
        g.renderOutline(left, top, W, H, 0xFFB0485A);
        g.drawCenteredString(font, title, left + W / 2, top + 6, 0xFF7A8C);

        // 1. beds
        g.drawString(font, Component.translatable("screen.custombeds.step_bed"), left + 10, top + 20, 0xFFD45E);
        Inventory inv = minecraft.player.getInventory();
        ItemStack hoveredStack = ItemStack.EMPTY;
        if (bedSlots.isEmpty()) {
            int y = top + 40;
            for (FormattedCharSequence line : font.split(Component.translatable("screen.custombeds.no_beds"), 145)) {
                g.drawString(font, line, left + 10, y, 0x909090);
                y += 10;
            }
        }
        for (int i = 0; i < bedSlots.size(); i++) {
            int x = bedX(i), y = bedY(i);
            ItemStack stack = inv.getItem(bedSlots.get(i));
            g.fill(x, y, x + 18, y + 18, i == selBed ? 0xFFFFC94A : 0xFF3A3A48);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF22222C);
            g.renderItem(stack, x + 1, y + 1);
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) hoveredStack = stack;
        }

        // 2. who
        g.drawString(font, Component.translatable("screen.custombeds.step_who"), listX(), top + 20, 0xFFD45E);
        g.fill(listX(), listY(), listX() + listW(), listY() + VISIBLE_PLAYERS * ROW, 0x80000000);
        if (players.isEmpty()) {
            int y = listY() + 4;
            for (FormattedCharSequence line : font.split(Component.translatable("screen.custombeds.nobody_online"), listW() - 6)) {
                g.drawString(font, line, listX() + 3, y, 0x808080);
                y += 10;
            }
        }
        for (int row = 0; row < VISIBLE_PLAYERS; row++) {
            int i = row + playerScroll;
            if (i >= players.size()) break;
            int y = listY() + row * ROW;
            if (i == selPlayer) g.fill(listX(), y, listX() + listW(), y + ROW, 0x60FFFFFF);
            g.drawString(font, font.plainSubstrByWidth(players.get(i).getName(), listW() - 6), listX() + 3, y + 2,
                    i == selPlayer ? 0xFFFFFF : 0xC0C0C0);
        }

        // 3. note
        g.drawString(font, Component.translatable("screen.custombeds.step_note"), left + 10, top + 108, 0xFFD45E);

        // gift wrap status
        Component wrapInfo;
        int wrapColor;
        if (minecraft.player.getAbilities().instabuild) {
            wrapInfo = Component.translatable("screen.custombeds.wrap_creative");
            wrapColor = 0x7FD67F;
        } else if (wrapCount() > 0) {
            wrapInfo = Component.translatable("screen.custombeds.wrap_count", wrapCount());
            wrapColor = 0x7FD67F;
        } else {
            wrapInfo = Component.translatable("screen.custombeds.wrap_none");
            wrapColor = 0xFF6060;
        }
        int y = top + 142;
        for (FormattedCharSequence line : font.split(wrapInfo, W - 20)) {
            g.drawString(font, line, left + 10, y, wrapColor);
            y += 10;
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (!hoveredStack.isEmpty()) g.renderTooltip(font, hoveredStack, mouseX, mouseY);
    }
}
