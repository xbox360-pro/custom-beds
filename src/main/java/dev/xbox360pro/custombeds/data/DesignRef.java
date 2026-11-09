package dev.xbox360pro.custombeds.data;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Small pointer to an uploaded bed design. Stored on bed items and bed block entities. */
public record DesignRef(UUID id, String name, String designer) {

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putString("By", designer);
        return tag;
    }

    @Nullable
    public static DesignRef fromTag(@Nullable CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        return new DesignRef(tag.getUUID("Id"), tag.getString("Name"), tag.getString("By"));
    }
}
