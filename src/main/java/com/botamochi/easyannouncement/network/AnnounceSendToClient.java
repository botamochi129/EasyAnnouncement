package com.botamochi.easyannouncement.network;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import mtr.data.RailwayData;
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
    public static final Identifier ANNOUNCE_START_ID = new Identifier(Easyannouncement.MOD_ID, "announce_start");

    public static void sendToClient(ServerPlayerEntity player, BlockPos pos, int seconds, List<Long> selectedPlatforms, String selectedJson) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(seconds);
        buf.writeLongArray(selectedPlatforms.stream().mapToLong(Long::longValue).toArray());
        buf.writeString(selectedJson);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendAnnounceStartPacket(ServerPlayerEntity player, List<Long> selectedPlatforms, BlockPos pos, String selectedJson, String destination, String routeType) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeLongArray(selectedPlatforms.stream().mapToLong(Long::longValue).toArray());
        buf.writeString(selectedJson);
        buf.writeString(destination);
        buf.writeString(routeType);
        System.out.println("Server sending announce start packet: selectedJson=" + selectedJson + ", platforms=" + selectedPlatforms + ", destination:"+destination+",routetype:"+routeType); // 確認ログ
        ServerPlayNetworking.send(player, ANNOUNCE_START_ID, buf);
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
            System.out.println("Received announce update (for GUI): selectedJson=" + selectedJson + ", platforms=" + selectedPlatforms); // 確認

            server.execute(() -> {
                if (player.getWorld().getBlockEntity(pos) instanceof AnnounceTile announceTile) {
                    if (selectedPlatforms != null) {
                        announceTile.setSeconds(seconds);
                        announceTile.setSelectedPlatformIds(selectedPlatforms);
                        announceTile.setSelectedJson(selectedJson);
                    }
                    announceTile.markDirty();
                }
            });
        });
    }
}