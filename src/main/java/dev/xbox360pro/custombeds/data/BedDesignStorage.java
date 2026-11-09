package dev.xbox360pro.custombeds.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-saved list of every bed design ever uploaded on this world/server. */
public class BedDesignStorage extends SavedData {
    private static final String NAME = "custombeds_designs";

    private final Map<UUID, BedDesign> designs = new LinkedHashMap<>();

    public static BedDesignStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(BedDesignStorage::load, BedDesignStorage::new, NAME);
    }

    public static BedDesignStorage load(CompoundTag tag) {
        BedDesignStorage storage = new BedDesignStorage();
        ListTag list = tag.getList("Designs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                BedDesign design = BedDesign.load(list.getCompound(i));
                storage.designs.put(design.id(), design);
            } catch (Exception ignored) {
                // skip a corrupt entry rather than losing the whole list
            }
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BedDesign design : designs.values()) list.add(design.save());
        tag.put("Designs", list);
        return tag;
    }

    public void add(BedDesign design) {
        designs.put(design.id(), design);
        setDirty();
    }

    @Nullable
    public BedDesign get(UUID id) {
        return designs.get(id);
    }

    public Collection<BedDesign> all() {
        return designs.values();
    }

    public int countOwnedBy(UUID owner) {
        int n = 0;
        for (BedDesign d : designs.values()) if (d.ownerId().equals(owner)) n++;
        return n;
    }

    public List<BedDesign> newestFirst() {
        List<BedDesign> list = new ArrayList<>(designs.values());
        list.sort((a, b) -> Long.compare(b.created(), a.created()));
        return list;
    }
}
