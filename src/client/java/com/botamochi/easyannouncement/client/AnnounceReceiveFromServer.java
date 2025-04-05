package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.Easyannouncement;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Platform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.Resource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AnnounceReceiveFromServer {
    public static final Identifier ID = new Identifier(Easyannouncement.MOD_ID, "announce_update");
    public static final Identifier ANNOUNCE_START_ID = new Identifier(Easyannouncement.MOD_ID, "announce_start");
    public static String destination = ""; // 初期値を設定
    public static String routeType = "";   // 初期値を設定
    private static final long MIN_ANNOUNCE_INTERVAL = 500; // 例: 500 ミリ秒間隔
    private static final Map<BlockPos, Long> lastAnnounceTime = new HashMap<>();

    private static List<Long> lastReceivedPlatformIds = new ArrayList<>(); // クライアント側で最後に受信したプラットフォームIDを保持

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
            System.out.println("Client received announce update (for GUI): pos=" + pos + ", seconds=" + seconds + ", selectedJson=" + selectedJson + ", platforms=" + selectedPlatforms); // 確認
            client.execute(() -> {
                if (client.world.getBlockEntity(pos) instanceof com.botamochi.easyannouncement.tile.AnnounceTile announceTile) {
                    announceTile.setSeconds(seconds);
                    announceTile.setSelectedPlatformIds(selectedPlatforms);
                    announceTile.setSelectedJson(selectedJson);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ANNOUNCE_START_ID, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            long[] platformIds = buf.readLongArray();
            List<Long> selectedPlatforms = new ArrayList<>();
            for (long platformId : platformIds) {
                selectedPlatforms.add(platformId);
            }
            String selectedJson = buf.readString();
            destination = buf.readString();
            routeType = buf.readString();
            System.out.println("Client received announce start trigger: pos=" + pos + ", selectedJson=" + selectedJson + ", platforms=" + selectedPlatforms + ", destination=" + destination + ", routeType=" + routeType); // 確認
            client.execute(() -> {
                List<SoundData> soundDataList = loadAnnouncementSequence(selectedJson, selectedPlatforms);
                playAnnouncementSounds(client, pos, soundDataList);

                long currentTime = System.currentTimeMillis();
                if (!lastAnnounceTime.containsKey(pos) || currentTime - lastAnnounceTime.get(pos) > MIN_ANNOUNCE_INTERVAL) {
                    lastAnnounceTime.put(pos, currentTime);
                    loadAnnouncementSequence(selectedJson, selectedPlatforms);
                } else {
                    System.out.println("重複アナウンスをスキップ: " + pos);
                }
            });
        });
    }

    private static List<SoundData> loadAnnouncementSequence(String selectedJson, List<Long> selectedPlatforms) {
        List<SoundData> soundDataList = new ArrayList<>();
        Identifier jsonId = new Identifier("easyannouncement", "sounds/" + selectedJson + ".json");
        MinecraftClient client = MinecraftClient.getInstance();
        Optional<Resource> resourceOptional = client.getResourceManager().getResource(jsonId);

        if (resourceOptional.isPresent()) {
            try (InputStream inputStream = resourceOptional.get().getInputStream()) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);
                JsonArray announcementArray = jsonObject.getAsJsonArray("sounds");
                for (int i = 0; i < announcementArray.size(); i++) {
                    JsonObject announcementObject = announcementArray.get(i).getAsJsonObject();
                    String rawSoundPath = announcementObject.get("soundPath").getAsString();
                    double duration = announcementObject.get("duration").getAsDouble();
                    List<String> formattedSoundPaths = getFormattedAnnouncement(rawSoundPath, selectedPlatforms);
                    for (String formattedSoundPath : formattedSoundPaths) {
                        soundDataList.add(new SoundData(formattedSoundPath, duration));
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load announcement sequence JSON: " + e.getMessage());
            }
        } else {
            System.err.println("Announcement sequence JSON not found in resource pack: " + selectedJson + ".json");
        }
        return soundDataList;
    }

    private static List<String> getFormattedAnnouncement(String soundPath, List<Long> selectedPlatforms) {
        List<String> formattedSoundPaths = new ArrayList<>();
        String platformName = getPlatformName(selectedPlatforms);
        String routeName = getRouteName(selectedPlatforms);

        String formattedSoundPath = soundPath
                .replace("($track)", platformName == null ? "" : platformName)
                .replace("($boundfor)", destination == null ? "" : destination)
                .replace("($routetype)", routeType == null ? "" : routeType)
                .replace("($route)", routeName == null ? "" : routeName);
        formattedSoundPaths.add(formattedSoundPath);
        System.out.println("Client formatted soundPath: " + soundPath + " -> " + formattedSoundPath);
        return formattedSoundPaths;
    }

    private static void playAnnouncementSounds(MinecraftClient client, BlockPos pos, List<SoundData> soundDataList) {
        if (soundDataList != null && !soundDataList.isEmpty()) {
            SoundManager soundManager = client.getSoundManager();
            Random random = Random.create();
            long currentDelayMillis = 0;

            for (SoundData soundData : soundDataList) {
                String finalSoundPath = soundData.soundPath;
                long delayMillis = currentDelayMillis;

                new Thread(() -> {
                    try {
                        Thread.sleep(delayMillis);
                        client.submit(() -> {
                            Identifier soundId = new Identifier(Easyannouncement.MOD_ID, finalSoundPath);
                            soundManager.play(new PositionedSoundInstance(soundId, net.minecraft.sound.SoundCategory.MASTER, 2.0F, 1.0F, random, false, 0, SoundInstance.AttenuationType.LINEAR, pos.getX(), pos.getY(), pos.getZ(), false));
                            System.out.println("Client playing sound: " + finalSoundPath + " at " + pos + " with delay " + delayMillis + " ms (played on render thread).");
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                currentDelayMillis += (long) (soundData.duration * 1000);
            }
        }
    }

    private static String getPlatformName(List<Long> selectedPlatforms) {
        if (selectedPlatforms.isEmpty()) {
            return "platform_name_not_found";
        }
        long platformId = selectedPlatforms.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        return platform != null ? platform.name : "platform_name_unknown";
    }

    private static String getRouteName(List<Long> selectedPlatforms) {
        if (selectedPlatforms.isEmpty()) {
            return "route_name_not_found";
        }
        long platformId = selectedPlatforms.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        if (platform != null) {
            List<ClientCache.PlatformRouteDetails> routeDetails = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
            if (routeDetails != null && !routeDetails.isEmpty()) {
                String routeName = routeDetails.get(0).routeName;
                int lastPipeIndex = routeName.lastIndexOf('|');
                if (lastPipeIndex != -1 && lastPipeIndex < routeName.length() - 1) {
                    return routeName.substring(lastPipeIndex + 1).toLowerCase().trim();
                } else {
                    return routeName.toLowerCase().trim(); // | がない場合はそのまま小文字化
                }
            }
        }
        return "route_name_unknown";
    }

    private static class SoundData {
        String soundPath;
        double duration;

        public SoundData(String soundPath, double duration) {
            this.soundPath = soundPath;
            this.duration = duration;
        }
    }
}