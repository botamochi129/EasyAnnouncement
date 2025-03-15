package com.botamochi.easyannouncement.event;

import mtr.data.Platform;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface PlatformSelectionEvent {

    void onPlatformSelected(BlockPos pos, List<Platform> selectedPlatforms, int delaySeconds);

    interface Listener {
        void onPlatformSelected(BlockPos pos, List<Platform> selectedPlatforms, int delaySeconds);
    }

    net.fabricmc.fabric.api.event.Event<Listener> EVENT = net.fabricmc.fabric.api.event.EventFactory.createArrayBacked(Listener.class, listeners -> (pos, selectedPlatforms, delaySeconds) -> {
        for (Listener listener : listeners) {
            listener.onPlatformSelected(pos, selectedPlatforms, delaySeconds); // メソッド名を修正
        }
    });
}