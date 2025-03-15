package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ClientNetworkHandler {
    public static final Identifier ID = AnnounceSendToClient.ID;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int seconds = buf.readInt();
            long[] platformIds = buf.readLongArray();
            List<Long> selectedPlatforms = new ArrayList<>();
            for (long platformId : platformIds) {
                selectedPlatforms.add(platformId);
            }
            String selectedJson = buf.readString();

            System.out.println("Received packet: pos=" + pos + ", seconds=" + seconds + ", selectedJson=" + selectedJson); // 確認

            client.execute(() -> {
                ClientPlayerEntity player = client.player;
                if (player != null && player.world.getBlockEntity(pos) instanceof AnnounceTile announceTile) {
                    announceTile.setSeconds(seconds);
                    announceTile.setSelectedPlatformIds(selectedPlatforms);
                    announceTile.setSelectedJson(selectedJson);

                    if (client.currentScreen instanceof MainScreen mainScreen) {
                        mainScreen.updateData(seconds, selectedPlatforms, selectedJson);
                    }
                }
            });
        });
    }
}