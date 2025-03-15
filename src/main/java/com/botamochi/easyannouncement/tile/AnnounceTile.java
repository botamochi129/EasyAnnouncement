package com.botamochi.easyannouncement.tile;

import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.registry.EATile;
import com.botamochi.easyannouncement.screen.MainScreenHandler;
import com.botamochi.easyannouncement.util.AnnouncementJsonLoader;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnounceTile extends BlockEntity implements ExtendedScreenHandlerFactory {
    private int seconds = 0;  // 放送前の時間
    private List<Long> selectedPlatformIds = new ArrayList<>();
    private String selectedJson = "default";
    private long lastMarkDirtyTime = 0;
    private static final long MARK_DIRTY_INTERVAL = 1000; // 1 秒ごとに markDirty() を呼び出す
    private int announcementIndex = 0;
    private List<String> announcementQueue = new ArrayList<>();
    private static final Map<String, SoundEvent> soundEventMap = new ConcurrentHashMap<>(); // ConcurrentHashMap を使用& 追加: サウンドイベントを格納するマップ
    private double duration = 1.0; // 追加: サウンドの長さを格納する変数

    public AnnounceTile(BlockPos pos, BlockState state) {
        super(EATile.EA_BLOCK_TILE, pos, state);
    }

    private void loadAnnouncementData() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("assets/easyannouncement/sounds/" + selectedJson + ".json")) {
            if (inputStream != null) {
                AnnouncementJsonLoader.loadJson(inputStream, selectedJson);
            } else {
                System.out.println("JSON file not found in mod assets: " + selectedJson + ".json");
            }
        } catch (Exception e) {
            System.out.println("Failed to load announcement JSON: " + e.getMessage());
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            AnnounceSendToClient.sendToClient(serverPlayer, pos, this.seconds, this.selectedPlatformIds, this.selectedJson); //修正
        }
        return new MainScreenHandler(syncId, inv, this);
    }

    public void tick(World world, BlockPos pos, BlockState state) { // インスタンスメソッドに変更
        if (world.isClient || world.getServer().isStopping()) return; // ポーズ中は早期リターン

        for (Long platformId : selectedPlatformIds) {
            if (checkTrainApproaching(world, platformId, getSeconds())) {
                System.out.println("Train Approaching!!!");
                loadAnnouncementQueue();
                announcementIndex = 0;
                playAnnouncement(world, pos);
                break;
            }
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, AnnounceTile announceTile) { // 追加: AnnounceTileインスタンスからtickメソッドを呼ぶためのstaticメソッド
        announceTile.tick(world, pos, state);
    }

    private static boolean checkTrainApproaching(World world, long platformId, int secondsBefore) {
        RailwayData railwayData = RailwayData.getInstance(world);
        if (railwayData == null) return false;

        List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
        if (schedules == null || schedules.isEmpty()) return false;

        long currentTime = System.currentTimeMillis();
        for (ScheduleEntry entry : schedules) {
            if ((entry.arrivalMillis - currentTime) / 50 == secondsBefore * 20) {
                return true;
            }
        }
        return false;
    }

    private void loadAnnouncementQueue() {
        System.out.println("loadAnnouncementQueue called: selectedJson=" + selectedJson); // 修正
        loadAnnouncementData();
        announcementQueue.clear();

        List<String> jsonSounds = AnnouncementJsonLoader.getSoundsForJson(selectedJson);
        if (jsonSounds == null || jsonSounds.isEmpty()) {
            System.out.println("Failed to load JSON, using default announcement");
            System.out.println(selectedJson);
            jsonSounds = List.of("easyannouncement:mamonaku", "easyannouncement:mairimasu"); // デフォルト音声
        }

        System.out.println(jsonSounds);
        announcementQueue.addAll(getFormattedAnnouncement(jsonSounds)); // 修正: プレースホルダーを置換したリストを追加
        registerSoundEvents(announcementQueue); // 修正: 置換後のリストを登録
        System.out.println("Announcement queue: " + announcementQueue); // 修正
    }

    private void registerSoundEvents(List<String> soundIds) {
        soundEventMap.clear();
        for (String soundId : soundIds) {
            Identifier identifier = new Identifier(soundId);
            SoundEvent soundEvent = Registry.SOUND_EVENT.get(identifier);
            if (soundEvent != null) {
                soundEventMap.put(soundId, soundEvent);
            } else {
                System.out.println("Sound event not found in registry: " + soundId);
            }
        }
    }

    private void playAnnouncement(World world, BlockPos pos) {
        if (world.getServer().isStopping()) return; // ポーズ中は早期リターン
        long startTime = System.currentTimeMillis();
        System.out.println("playAnnouncement called: announcementIndex=" + announcementIndex + ", announcementQueueSize=" + announcementQueue.size()); // 修正
        System.out.println(AnnouncementJsonLoader.getSoundsForJson(selectedJson));
        if (announcementIndex >= announcementQueue.size()) {
            System.out.println("playAnnouncement return");
            return;
        }

        String soundId = announcementQueue.get(announcementIndex);
        SoundEvent soundEvent = soundEventMap.get(soundId); // マップからサウンドイベントを取得

        if (soundEvent != null) {
            System.out.println("Sound event sucsess");
            world.playSound(null, pos, soundEvent, SoundCategory.MASTER, 2.0F, 1.0F);
        } else {
            System.out.println("Sound event not found ");
        }

        System.out.println("Playing announcement: " + soundId);

        announcementIndex++;
        if (announcementIndex < announcementQueue.size()) {
            world.getServer().execute(() -> {
                try {
                    // JSONからサウンドの長さを取得
                    double duration = AnnouncementJsonLoader.getDurationForSound(selectedJson, soundId);
                    System.out.println("Duration for " + soundId + ": " + duration); // ログ追加
                    long sleepTime = (long) (duration * 1000); // ミリ秒に変換
                    System.out.println("Sleep time for " + soundId + ": " + sleepTime); // ログ追加


                    long sleepStartTime = System.currentTimeMillis();
                    if (world.getServer().isStopping()) { // ポーズ中はスレッドを中断
                        Thread.currentThread().interrupt();
                        return;
                    }
                    Thread.sleep(sleepTime); // 指定された時間だけ待機
                    long sleepEndTime = System.currentTimeMillis();
                    System.out.println("Sleep time: " + (sleepEndTime - sleepStartTime) + "ms");
                    playAnnouncement(world, pos);
                } catch (InterruptedException ignored) {
                }
            });
        }
        long endTime = System.currentTimeMillis();
        System.out.println("playAnnouncement time: " + (endTime - startTime) + "ms");
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    public List<Long> getSelectedPlatformIds() {
        return selectedPlatformIds;
    }

    public void setSelectedPlatformIds(List<Long> selectedPlatformIds) {
        if (!this.selectedPlatformIds.equals(selectedPlatformIds)) {
            this.selectedPlatformIds = selectedPlatformIds;
            markDirty();
        }
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        if (this.seconds != seconds) {
            this.seconds = seconds;
            markDirty();
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLongArray("PlatformId", selectedPlatformIds.stream().mapToLong(Long::longValue).toArray());
        nbt.putInt("TimeBeforeAnnounce", seconds);
        nbt.putString("SelectedJson", selectedJson);
        System.out.println("Saving selectedJson: " + selectedJson);  // ログ追加
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        selectedPlatformIds.clear();
        for (long id : nbt.getLongArray("PlatformId")) {
            selectedPlatformIds.add(id);
        }
        seconds = nbt.getInt("TimeBeforeAnnounce");
        selectedJson = nbt.getString("SelectedJson");
        System.out.println("Read selectedJson: " + selectedJson);  // ログ追加
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        packetByteBuf.writeBlockPos(this.pos);
    }

    @Override
    public void markDirty() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMarkDirtyTime >= MARK_DIRTY_INTERVAL) {
            super.markDirty();
            if (world != null && !world.isClient) {
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_ALL);
                for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                    AnnounceSendToClient.sendToClient(player, pos, seconds, selectedPlatformIds, selectedJson);
                    System.out.println("Sent packet to " + player.getName().getString() + ": pos=" + pos + ", seconds=" + seconds + ", selectedJson=" + selectedJson); // 確認
                }
            }
            lastMarkDirtyTime = currentTime;
        }
        System.out.println("Selected JSON in markDirty: pos=" + pos + ", seconds=" + seconds + ", selectedPlatformIds=" + selectedPlatformIds + ", selectedJson=" + selectedJson); // 修正
    }

    public String getSelectedJson() {
        return selectedJson;
    }

    public void setSelectedJson(String json) {
        if (this.selectedJson != json) {
            this.selectedJson = json;
            markDirty();
        }
    }

    public void sync() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    private List<String> getFormattedAnnouncement(List<String> soundIds) {
        List<String> formattedSounds = new ArrayList<>();
        String platformName = getPlatformName();
        String routeName = getRouteName();
        String destination = getDestination();
        String routeType = getRouteType();

        for (String soundId : soundIds) {
            String formattedSoundId = soundId
                    .replace("($track)", platformName)
                    .replace("($boundfor)", destination)
                    .replace("($routetype)", routeType);
            formattedSounds.add(formattedSoundId);
            System.out.println("Formatted soundId: soundId=" + soundId + ", selectedJson=" + selectedJson + ", formattedSoundId=" + formattedSoundId); // 修正

            // プレースホルダー置換後のサウンドIDの duration を soundDataMap に格納
            double duration = AnnouncementJsonLoader.getDurationForSound(selectedJson, soundId);
            AnnouncementJsonLoader.addDurationForSound(selectedJson, formattedSoundId, duration);
        }
        return formattedSounds;
    }

    private String getPlatformName() {
        if (selectedPlatformIds.isEmpty()) {
            return "platform_name_not_found"; // より明確なエラーメッセージ
        }

        long platformId = selectedPlatformIds.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        String name = platform != null ? platform.name : "platform_name_unknown";
        System.out.println("Platform Name: " + name);
        return name;
    }

    private String getRouteName() {
        if (selectedPlatformIds.isEmpty()) {
            return "route_name_not_found"; // より明確なエラーメッセージ
        }

        long platformId = selectedPlatformIds.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);

        if (platform != null) {
            List<ClientCache.PlatformRouteDetails> routeDetails = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
            if (routeDetails != null && !routeDetails.isEmpty()) {
                String routeName = routeDetails.get(0).routeName;
                System.out.println("Route Name: " + routeName);
                return routeName;
            }
        }

        return "route_name_unknown";
    }

    private String getDestination() {
        if (selectedPlatformIds.isEmpty()) {
            return "destination_not_found";
        }

        long platformId = selectedPlatformIds.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        System.out.println("Selected Platform ID: " + platformId);
        System.out.println("Platform Object: " + platform);

        if (platform != null) {
            RailwayData railwayData = RailwayData.getInstance(world);
            if (railwayData == null) {
                return "railwaydata_unknown";
            }

            List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
            if (schedules == null || schedules.isEmpty()) {
                return "schedules_unknown";
            }

            long routeId = schedules.get(0).routeId;
            Route route = ClientData.DATA_CACHE.routeIdMap.get(routeId);
            System.out.println("Route ID: " + routeId);
            System.out.println("Route Object: " + route);

            if (route == null) {
                return "route_unknown";
            }

            // カスタム行き先を確認
            List<Route.RoutePlatform> routePlatforms = route.platformIds;
            for (Route.RoutePlatform routePlatform : routePlatforms) {
                String customDestination = routePlatform.customDestination;
                if (Route.destinationIsReset(customDestination)) {
                    return null;
                }
                if (customDestination != null && !customDestination.isEmpty()) {
                    if (customDestination.contains("|")) {
                        String[] parts = customDestination.split("\\|");
                        if (parts.length > 1) {
                            String destination = parts[1].toLowerCase();
                            System.out.println("Custom Destination: " + destination);
                            return destination;
                        }
                    }
                    String destination = customDestination.toLowerCase();
                    System.out.println("Custom Destination: " + destination);
                    return destination;
                }
            }

            // カスタム行き先がない場合は、従来の処理を実行
            List<ClientCache.PlatformRouteDetails> routeDetailsList = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);

            if (routeDetailsList == null || routeDetailsList.isEmpty()) {
                return "route_details_unknown";
            }

            ClientCache.PlatformRouteDetails routeDetails = routeDetailsList.stream()
                    .filter(details -> details.routeColor == route.color && details.routeName.equals(route.name.split("\\|\\|")[0]))
                    .findFirst()
                    .orElse(null);

            if (routeDetails == null || routeDetails.stationDetails.isEmpty()) {
                return "route_details_not_found";
            }

            String finalDestination = routeDetails.stationDetails.get(routeDetails.stationDetails.size() - 1).stationName;

            if (finalDestination != null && !finalDestination.isEmpty()) {
                if (finalDestination.contains("|")) {
                    String[] parts = finalDestination.split("\\|");
                    if (parts.length > 1) {
                        String destination = parts[1].toLowerCase();
                        System.out.println("Custom Destination: " + destination);
                        return destination;
                    }
                }
                String destination = finalDestination.toLowerCase();
                System.out.println("Custom Destination: " + destination);
                return destination;
            }

            System.out.println("Final Destination (from PlatformRouteDetails): " + finalDestination);
        }

        return "destination_unknown";
    }

    private String getRouteType() {
        if (selectedPlatformIds.isEmpty()) {
            return "route_type_not_found"; // より明確なエラーメッセージ
        }

        long platformId = selectedPlatformIds.get(0);
        Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);

        if (platform != null) {
            List<ClientCache.PlatformRouteDetails> routeDetails = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
            if (routeDetails != null && !routeDetails.isEmpty()) {
                RailwayData railwayData = RailwayData.getInstance(world);
                if (railwayData == null) {
                    return "railwaydata_unknown";
                }

                List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
                if (schedules == null || schedules.isEmpty()) {
                    return "schedules_unknown";
                }

                long routeId = schedules.get(0).routeId;
                Route route = ClientData.DATA_CACHE.routeIdMap.get(routeId);

                if (route != null) {
                    String routeType = route.lightRailRouteNumber; // lightRailRouteNumberを取得
                    if (routeType != null && !routeType.isEmpty()) {
                        if (routeType.contains("|")) {
                            String[] parts = routeType.split("\\|");
                            if (parts.length > 1) {
                                String type = parts[1].toLowerCase();
                                System.out.println("Route Type: " + type);
                                return type;
                            }
                        } else {
                            String type = routeType.toLowerCase();
                            System.out.println("Route Type: " + type);
                            return type;
                        }
                    }
                }
            }
        }

        return "route_type_unknown";
    }
}