package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PlatformSelectionScreen extends Screen {
    private final BlockPos blockPos;
    private List<Platform> platforms;
    private List<Long> selectedPlatforms;
    private int scrollOffset = 0;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;
    private static final int VISIBLE_BUTTONS = 5;
    private static final int BUTTON_WIDTH = 200;

    public PlatformSelectionScreen(BlockPos blockPos, List<Long> selectedPlatforms) {
        super(Text.of("プラットフォーム選択"));
        this.blockPos = blockPos;
        this.selectedPlatforms = new ArrayList<>(selectedPlatforms); // 初期化
        this.platforms = getPlatforms();
    }

    private List<Platform> getPlatforms() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            return new ArrayList<>();  // 空のリストを返す
        }

        // 駅情報を取得する
        Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, blockPos);
        if (station == null) {
            return new ArrayList<>();  // 駅がnullの場合、空のリストを返す
        }

        // 駅のプラットフォームをリクエストして返す
        return new ArrayList<>(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
    }

    @Override
    protected void init() {
        super.init();

        // プラットフォームの選択ボタンを更新
        updateButtons();
    }

    private void updateButtons() {
        this.clearChildren();  // 既存のボタンをクリア

        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int yStart = this.height / 4;
        int yOffset = BUTTON_HEIGHT + BUTTON_SPACING;

        for (int i = 0; i < VISIBLE_BUTTONS && i + scrollOffset < platforms.size(); i++) {
            Platform platform = platforms.get(i + scrollOffset);
            boolean isSelected = selectedPlatforms.contains(platform.id);
            this.addDrawableChild(new ButtonWidget(x, yStart + i * yOffset, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.of("プラットフォーム " + platform.name + (isSelected ? " ✓" : "")), button -> {
                if (isSelected) {
                    selectedPlatforms.remove(platform.id);  // すでに選択されている場合は解除
                } else {
                    selectedPlatforms.add(platform.id);  // 未選択の場合は選択
                }
                updateButtons();  // ボタンの状態を更新
            }));
        }

        // スクロールボタンの追加
        addScrollButtons(x, yStart, yOffset);

        // 保存ボタン
        this.addDrawableChild(new ButtonWidget(x, yStart + (VISIBLE_BUTTONS + 1) * yOffset, BUTTON_WIDTH, BUTTON_HEIGHT, Text.of("保存"), button -> {
            saveSelectionAndClose();
        }));
    }

    private void addScrollButtons(int x, int yStart, int yOffset) {
        // スクロール上ボタン
        if (scrollOffset > 0) {
            this.addDrawableChild(new ButtonWidget(x, yStart - yOffset, BUTTON_WIDTH, BUTTON_HEIGHT, Text.of("↑ スクロール"), button -> {
                scrollOffset = Math.max(scrollOffset - 1, 0);
                updateButtons();
            }));
        }

        // スクロール下ボタン
        if (scrollOffset + VISIBLE_BUTTONS < platforms.size()) {
            this.addDrawableChild(new ButtonWidget(x, yStart + VISIBLE_BUTTONS * yOffset, BUTTON_WIDTH, BUTTON_HEIGHT, Text.of("↓ スクロール"), button -> {
                scrollOffset = Math.min(scrollOffset + 1, platforms.size() - VISIBLE_BUTTONS);
                updateButtons();
            }));
        }
    }

    private void saveSelectionAndClose() {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof AnnounceTile announceTile) {
                try {
                    int seconds = announceTile.getSeconds(); // AnnounceTileから秒数を取得
                    announceTile.setSeconds(seconds); // 秒数をAnnounceTileに設定
                    announceTile.setSelectedPlatformIds(selectedPlatforms); // プラットフォームIDを設定
                    String json = announceTile.getSelectedJson(); // 選択したJSONを取得
                    sendUpdatePacket(blockPos, seconds, selectedPlatforms, json); // サーバーに送信
                } catch (NumberFormatException e) {
                    sendUpdatePacket(announceTile.getPos(), 0, selectedPlatforms, announceTile.getSelectedJson());
                }
            }
        }
        this.client.setScreen(null);  // Saveが完了したら画面を閉じる
    }

    private void sendUpdatePacket(BlockPos pos, int seconds, List<Long> selectedPlatforms, String json) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        // サーバーにパケットを送信
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(seconds);  // 秒数を送信
        buf.writeLongArray(selectedPlatforms.stream().mapToLong(Long::longValue).toArray());  // 選択したプラットフォームを送信
        buf.writeString(json);

        // サーバーに送信
        ClientPlayNetworking.send(AnnounceSendToClient.ID, buf); // IDの変更
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);  // 背景の描画
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 4 - 20, 0xFFFFFF);  // タイトルの描画
        super.render(matrices, mouseX, mouseY, delta);  // 他の要素を描画
    }

    @Override
    public boolean shouldPause() {
        return false;  // この画面を開いている間、ゲームを一時停止させない
    }
}