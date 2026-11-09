package dev.xbox360pro.custombeds.client;

import dev.xbox360pro.custombeds.CustomBeds;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Opens the operating system's "open file" dialog without freezing the game. */
public final class FilePicker {
    private FilePicker() {}

    private static final AtomicBoolean OPEN = new AtomicBoolean();

    public static boolean isOpen() {
        return OPEN.get();
    }

    /** Calls {@code onResult} on the game thread with the chosen path, or null if cancelled. */
    public static void pickImage(Consumer<String> onResult) {
        if (!OPEN.compareAndSet(false, true)) return;
        Thread thread = new Thread(() -> {
            String result = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String[] patterns = {"*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"};
                PointerBuffer filters = stack.mallocPointer(patterns.length);
                for (String p : patterns) filters.put(stack.UTF8(p));
                filters.flip();
                result = TinyFileDialogs.tinyfd_openFileDialog("Pick an image for your bed", null, filters,
                        "Images (png, jpg, bmp, gif)", false);
            } catch (Throwable t) {
                CustomBeds.LOGGER.warn("File picker failed", t);
            } finally {
                OPEN.set(false);
            }
            String chosen = result;
            Minecraft.getInstance().execute(() -> onResult.accept(chosen));
        }, "CustomBeds file picker");
        thread.setDaemon(true);
        thread.start();
    }
}
