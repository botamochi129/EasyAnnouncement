package com.botamochi.easyannouncement.tile;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.registry.EATile;
import com.botamochi.easyannouncement.screen.MainScreenHandler;
import mtr.data.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class AnnounceTile extends BlockEntity implements ExtendedScreenHandlerFactory {
    private int seconds = 0;  // 放送前の時間
    private List<Long> selectedPlatformIds = new ArrayList<>();
    private String selectedJson = "default";
    private long lastMarkDirtyTime = 0;
    private static final long MARK_DIRTY_INTERVAL = 1000; // 1 秒ごとに markDirty() を呼び出す
    public static final Identifier ANNOUNCE_START_ID = new Identifier(Easyannouncement.MOD_ID, "announce_start");
    private long lastAnnounceTriggerTime = 0;
    private static final long MIN_TRIGGER_INTERVAL = 1000; // 例: 1 秒間隔

    public AnnounceTile(BlockPos pos, BlockState state) {
        super(EATile.EA_BLOCK_TILE, pos, state);
        Easyannouncement.registerAnnounceTilePosition(pos);
    }

    public static RailwayData getRailwayData(World world) {
        return RailwayData.getInstance(world);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            AnnounceSendToClient.sendToClient(serverPlayer, pos, seconds, selectedPlatformIds, selectedJson); // 修正: selectedJson のみを送信
        }
        return new MainScreenHandler(syncId, inv, this);
    }

    public void startAnnouncement(ServerPlayerEntity player) {
        if (!world.isClient) {
            AnnounceSendToClient.sendAnnounceStartPacket(player, selectedPlatformIds, pos, selectedJson, getDestination(selectedPlatformIds), getRouteType(selectedPlatformIds));
        }
    }

    public void tick(World world, BlockPos pos, BlockState state) { // インスタンスメソッドに変更
        if (world.isClient || world.getServer().isStopping()) return; // ポーズ中は早期リターン

        long currentTime = System.currentTimeMillis();
        for (Long platformId : selectedPlatformIds) {
            if (checkTrainApproaching(world, platformId, getSeconds())) {
                if (currentTime - lastAnnounceTriggerTime >= MIN_TRIGGER_INTERVAL) {
                    System.out.println("Train Approaching!!! Sending announce trigger.");
                    if (world.getServer() != null) {
                        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                            startAnnouncement(player);
                        }
                        lastAnnounceTriggerTime = currentTime; // トリガー時間を更新
                    }
                } else {
                    System.out.println("重複トリガーをスキップ: " + pos);
                }
                break; // 最初の接近でトリガーしたらループを抜ける (必要に応じて調整)
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
                if (world.getServer() != null) {
                    for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                        AnnounceSendToClient.sendToClient(player, pos, seconds, selectedPlatformIds, selectedJson); // 修正: selectedJson のみを送信
                        System.out.println("Sent announce trigger to " + player.getName().getString() + ": pos=" + pos + ", selectedJson=" + selectedJson); // 確認
                    }
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

    public World getWorld() {
        return world;
    }

    private String getDestination(List<Long> selectedPlatforms) {
        if (selectedPlatforms.isEmpty()) {
            return "destination_not_found";
        }
        long platformId = selectedPlatforms.get(0);
        RailwayData railwayData = AnnounceTile.getRailwayData(world);
        if (railwayData == null) return "railwaydata_unknown";
        Platform platform = railwayData.dataCache.platformIdMap.get(platformId);
        if (platform != null) {
            World world = getWorld();
            if (world == null) return "world_unknown";
            List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
            if (schedules == null || schedules.isEmpty()) return "schedules_unknown";
            long routeId = schedules.get(0).routeId;
            Route route = railwayData.dataCache.routeIdMap.get(routeId);
            if (route == null) return "route_unknown";

            for (Route.RoutePlatform routePlatform : route.platformIds) {
                String customDestination = routePlatform.customDestination;
                if (Route.destinationIsReset(customDestination)) return null;
                if (customDestination != null && !customDestination.isEmpty()) {
                    int lastPipeIndex = customDestination.lastIndexOf('|');
                    if (lastPipeIndex != -1 && lastPipeIndex < customDestination.length() - 1) {
                        return customDestination.substring(lastPipeIndex + 1).toLowerCase().trim();
                    } else {
                        return customDestination.toLowerCase().trim(); // | がない場合はそのまま小文字化
                    }
                }
            }

            String finalDestination = route.getDestination(route.platformIds.size() - 1);
            if (finalDestination == null) {
                long lastPlatformId = route.getLastPlatformId();
                Platform lastPlatform = railwayData.dataCache.platformIdMap.get(lastPlatformId);
                if (lastPlatform != null) {
                    finalDestination = railwayData.dataCache.platformIdToStation.get(lastPlatformId).name;
                }
            }
            if (finalDestination != null) {
                int lastPipeIndex = finalDestination.lastIndexOf('|');
                if (lastPipeIndex != -1 && lastPipeIndex < finalDestination.length() - 1) {
                    return finalDestination.substring(lastPipeIndex + 1).toLowerCase().trim();
                } else {
                    return finalDestination.toLowerCase().trim(); // | がない場合はそのまま小文字化
                }
            } else {
                return "destination_unknown";
            }
        }
        return "destination_unknown";
    }

    private String getRouteType(List<Long> selectedPlatforms) {
        if (selectedPlatforms.isEmpty()) {
            return "route_type_not_found";
        }
        long platformId = selectedPlatforms.get(0);
        RailwayData railwayData = RailwayData.getInstance(world);
        if (railwayData == null) return "railwaydata_unknown";
        Platform platform = railwayData.dataCache.platformIdMap.get(platformId);
        if (platform != null) {
            World world = getWorld();
            if (world == null) return "world_unknown";
            List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
            if (schedules == null || schedules.isEmpty()) return "schedules_unknown";
            long routeId = schedules.get(0).routeId;
            Route route = railwayData.dataCache.routeIdMap.get(routeId);
            if (route != null) {
                String routeType = route.lightRailRouteNumber;
                if (routeType != null && !routeType.isEmpty()) {
                    int lastPipeIndex = routeType.lastIndexOf('|');
                    if (lastPipeIndex != -1 && lastPipeIndex < routeType.length() - 1) {
                        return routeType.substring(lastPipeIndex + 1).toLowerCase().trim();
                    } else {
                        return routeType.toLowerCase().trim(); // | がない場合はそのまま小文字化
                    }
                }
            }
        }
        return "route_type_unknown";
    }
}