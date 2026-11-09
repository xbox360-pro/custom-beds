package dev.xbox360pro.custombeds.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** A bed design uploaded by a player. {@code png} is a 64x128 bed texture. */
public record BedDesign(UUID id, String name, UUID ownerId, String ownerName, long created, byte[] png) {

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
        return tag;
    }

    public static BedDesign load(CompoundTag tag) {
        return new BedDesign(
                tag.getUUID("Id"),
                tag.getString("Name"),
                tag.getUUID("OwnerId"),
                tag.getString("OwnerName"),
                tag.getLong("Created"),
                tag.getByteArray("Png"));
    }
}
