package com.botamochi.easyannouncement.client;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.registry.EASounds;
import com.botamochi.easyannouncement.screen.EAScreenHandlers;
import com.botamochi.easyannouncement.screen.MainScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class EasyannouncementClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Easyannouncement.EA_BLOCK, RenderLayer.getTranslucent());
        ScreenRegistry.register(EAScreenHandlers.MAIN_SCREEN_HANDLER, MainScreen::new);
        ClientNetworkHandler.register();
        AnnounceReceiveFromServer.register();
    }
}