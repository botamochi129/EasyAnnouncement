package com.botamochi.easyannouncement.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class AnnounceTilePositionsSavedData extends PersistentState {
    private Set<BlockPos> announceTilePositions = new HashSet<>();

    public AnnounceTilePositionsSavedData() {
        super();
    }

    public Set<BlockPos> getPositions() {
        return announceTilePositions;
    }

    public void addPosition(BlockPos pos) {
        announceTilePositions.add(pos);
        markDirty();
    }

    public void removePosition(BlockPos pos) {
        announceTilePositions.remove(pos);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (BlockPos pos : announceTilePositions) {
            list.add(NbtLong.of(pos.asLong()));
        }
        nbt.put("announceTilePositions", list);
        return nbt;
    }

    public static AnnounceTilePositionsSavedData createAndLoad(ServerWorld world) {
        PersistentStateManager persistentStateManager = world.getPersistentStateManager();
        return persistentStateManager.getOrCreate(AnnounceTilePositionsSavedData::fromNbt, AnnounceTilePositionsSavedData::new, "easy_announcement_positions");
    }

    public static AnnounceTilePositionsSavedData fromNbt(NbtCompound nbt) {
        AnnounceTilePositionsSavedData savedData = new AnnounceTilePositionsSavedData();
        savedData.readNbt(nbt);
        return savedData;
    }

    public void readNbt(NbtCompound nbt) {
        NbtList list = nbt.getList("announceTilePositions", NbtElement.LONG_TYPE);
        announceTilePositions.clear();
        for (int i = 0; i < list.size(); i++) {
            NbtElement element = list.get(i);
            if (element instanceof NbtLong longElement) {
                announceTilePositions.add(BlockPos.fromLong(longElement.longValue()));
            }
        }
    }
}