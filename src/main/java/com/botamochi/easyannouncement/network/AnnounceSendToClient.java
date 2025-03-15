package com.botamochi.easyannouncement.network;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AnnounceSendToClient {
    public static final Identifier ID = new Identifier(Easyannouncement.MOD_ID, "announce_update");

    public static void sendToClient(ServerPlayerEntity player, BlockPos pos, int seconds, List<Long> selectedPlatforms, String selectedJson) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(seconds);
        buf.writeLongArray(selectedPlatforms.stream().mapToLong(Long::longValue).toArray());
        buf.writeString(selectedJson);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos();
            int seconds = buf.readInt();
            long[] platformIds = buf.readLongArray();
            List<Long> selectedPlatforms = new ArrayList<>();
            for (long platformId : platformIds) {
                selectedPlatforms.add(platformId);
            }
            String selectedJson = buf.readString();
            System.out.println("Received selectedJson: " + selectedJson); // 追加

            server.execute(() -> {
                if (player.getWorld().getBlockEntity(pos) instanceof AnnounceTile announceTile) {
                    if (selectedPlatforms != null) {
                        announceTile.setSeconds(seconds);
                        announceTile.setSelectedPlatformIds(selectedPlatforms);
                        announceTile.setSelectedJson(selectedJson);
                    }
                    announceTile.markDirty(); // ここでmarkDirtyを呼び出す
                }
            });
        });
    }

    private static void updateAnnounceTile(ServerPlayerEntity player, BlockPos pos, int seconds, List<Long> selectedPlatforms, String selectedJson) {
        if (player.getWorld().getBlockEntity(pos) instanceof AnnounceTile announceTile) {
            if (selectedPlatforms != null) {
                announceTile.setSeconds(seconds);
                announceTile.setSelectedPlatformIds(selectedPlatforms);
                announceTile.setSelectedJson(selectedJson);
            }
            announceTile.markDirty();
        }
    }
}