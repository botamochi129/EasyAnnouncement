package com.botamochi.easyannouncement.screen;

import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class MainScreenHandler extends ScreenHandler {
    private final AnnounceTile blockEntity;

    public MainScreenHandler(int syncId, PlayerInventory playerInventory, AnnounceTile blockEntity) {
        super(EAScreenHandlers.MAIN_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
    }

    public AnnounceTile getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        // Implement the method to handle slot transfer
        return ItemStack.EMPTY;
    }
}