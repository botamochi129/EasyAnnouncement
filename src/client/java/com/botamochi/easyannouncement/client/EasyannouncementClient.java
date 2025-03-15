// EasyannouncementClient.java
package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.screen.EAScreenHandlers;
import com.botamochi.easyannouncement.screen.MainScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.gui.screen.Screen;

public class EasyannouncementClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Easyannouncement.EA_BLOCK, RenderLayer.getTranslucent());
        ScreenRegistry.register(EAScreenHandlers.MAIN_SCREEN_HANDLER, (ScreenRegistry.Factory<MainScreenHandler, MainScreen>) (screenHandler, playerInventory, title) -> new MainScreen(screenHandler, playerInventory, title));
        ClientNetworkHandler.register();
    }
}