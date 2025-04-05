package com.botamochi.easyannouncement.screen;

import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EAScreenHandlers {
    public static ScreenHandlerType<MainScreenHandler> MAIN_SCREEN_HANDLER;

    public static void register() {
        MAIN_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(
                new Identifier("easyannouncement", "main_screen_handler"),
                (syncId, inventory, buf) -> {
                    BlockEntity be = inventory.player.getWorld().getBlockEntity(buf.readBlockPos());
                    if (be instanceof AnnounceTile) {
                        return new MainScreenHandler(syncId, inventory, (AnnounceTile) be);
                    }
                    return null;
                }
        );
    }
}
