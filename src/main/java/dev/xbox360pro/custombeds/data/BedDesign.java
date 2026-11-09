package dev.xbox360pro.custombeds.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A design uploaded by a player: either a bed (64x128 texture) or a painting
 * ({@code width*64} x {@code height*64} texture, size in blocks).
 */
public record BedDesign(UUID id, String name, UUID ownerId, String ownerName, long created, byte[] png,
                        int kind, int width, int height) {

    public static final int KIND_BED = 0;
    public static final int KIND_PAINTING = 1;

    public boolean isPainting() {
        return kind == KIND_PAINTING;
    }

    public DesignRef ref() {
        return new DesignRef(id, name, ownerName);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putUUID("OwnerId", ownerId);
        tag.putString("OwnerName", ownerName);
        tag.putLong("Created", created);
        tag.putByteArray("Png", png);
        tag.putInt("Kind", kind);
        tag.putInt("W", width);
        tag.putInt("H", height);
        return tag;
    }

    public static BedDesign load(CompoundTag tag) {
        return new BedDesign(
                tag.getUUID("Id"),
                tag.getString("Name"),
                tag.getUUID("OwnerId"),
                tag.getString("OwnerName"),
                tag.getLong("Created"),
                tag.getByteArray("Png"),
                tag.getInt("Kind"),   // older saves have no Kind -> 0 = bed
                tag.getInt("W"),
                tag.getInt("H"));
    }
}
