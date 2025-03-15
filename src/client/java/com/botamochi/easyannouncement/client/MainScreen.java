package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.screen.MainScreenHandler;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MainScreen extends Screen implements ScreenHandlerProvider<MainScreenHandler> {
    private final MainScreenHandler handler;
    private List<Long> selectedPlatforms = new ArrayList<>();
    public TextFieldWidget secondsField;
    private CyclingButtonWidget<String> jsonSelector;
    private List<String> jsonFiles;

    public MainScreen(MainScreenHandler handler, PlayerInventory inventory, Text title) {
        super(title);
        this.handler = handler;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int yStart = this.height / 4;
        int yOffset = 40;

        AnnounceTile announceTile = getAnnounceTile();
        if (announceTile != null) {
            selectedPlatforms = announceTile.getSelectedPlatformIds();
            secondsField = new TextFieldWidget(textRenderer, x, yStart + yOffset, buttonWidth, buttonHeight, Text.of("秒数入力"));
            secondsField.setMaxLength(3);
            secondsField.setText(String.valueOf(announceTile.getSeconds()));
            this.addDrawableChild(secondsField);
        }

        this.addDrawableChild(new ButtonWidget(x, yStart, buttonWidth, buttonHeight, Text.of("プラットフォーム選択"), button -> {
            if (announceTile != null) {
                this.client.setScreen(new PlatformSelectionScreen(announceTile.getPos(), announceTile.getSelectedPlatformIds()));
            }
        }));

        jsonFiles = getAvailableJsonFiles();
        if (announceTile != null) {
            String selectedJson = announceTile.getSelectedJson();
            jsonSelector = addDrawableChild(CyclingButtonWidget.<String>builder(Text::of)
                    .values(jsonFiles)
                    .initially(selectedJson) // 初期値を設定
                    .build(x, yStart + 2 * yOffset, buttonWidth, buttonHeight, Text.of("Select JSON"),
                            (button, json) -> {
                            }));
        }

        this.addDrawableChild(new ButtonWidget(x, yStart + 3 * yOffset, buttonWidth, buttonHeight, Text.of("保存"), button -> {
            if (announceTile != null) {
                try {
                    int seconds = Integer.parseInt(secondsField.getText());
                    announceTile.setSeconds(seconds);
                    announceTile.setSelectedPlatformIds(selectedPlatforms);
                    String selectedJson = jsonSelector.getValue() != null ? jsonSelector.getValue() : "";
                    announceTile.setSelectedJson(selectedJson);
                    sendUpdatePacket(announceTile.getPos(), seconds, selectedPlatforms, selectedJson);
                } catch (NumberFormatException e) {
                    sendUpdatePacket(announceTile.getPos(), 0, selectedPlatforms, announceTile.getSelectedJson());
                }
            }
            this.client.setScreen(null);
        }));
    }

    private AnnounceTile getAnnounceTile() {
        if (handler.getBlockEntity() instanceof AnnounceTile) {
            return (AnnounceTile) handler.getBlockEntity();
        }
        return null;
    }

    private void sendUpdatePacket(BlockPos pos, int seconds, List<Long> selectedPlatforms, String selectedJson) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(seconds);
        buf.writeLongArray(selectedPlatforms.stream().mapToLong(Long::longValue).toArray());
        buf.writeString(selectedJson);
        ClientPlayNetworking.send(AnnounceSendToClient.ID, buf);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        AnnounceTile announceTile = getAnnounceTile();
        if (announceTile != null) {
            drawCenteredText(matrices, this.textRenderer, Text.of("秒数: " + announceTile.getSeconds()), this.width / 2, this.height / 4 - 20, 0xFFFFFF);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public MainScreenHandler getScreenHandler() {
        return handler;
    }

    public void updateData(int seconds, List<Long> selectedPlatforms, String selectedJson) {
        this.secondsField.setText(String.valueOf(seconds));
        this.selectedPlatforms = new ArrayList<>(selectedPlatforms); // コピーを作成
        this.jsonSelector.setValue(selectedJson);

        AnnounceTile announceTile = getAnnounceTile();
        announceTile.markDirty(); // 追加
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static List<String> getAvailableJsonFiles() {
        List<String> jsonFiles = new ArrayList<>();
        for (Identifier id : MinecraftClient.getInstance().getResourceManager().findResources("sounds", path -> path.getPath().endsWith(".json")).keySet()) {
            jsonFiles.add(id.getPath().replace("sounds/", "").replace(".json", ""));
        }
        return jsonFiles;
    }
}