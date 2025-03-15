// Easyannouncement.java
package com.botamochi.easyannouncement;

import com.botamochi.easyannouncement.block.AnnounceBlock;
import com.botamochi.easyannouncement.event.PlatformSelectionEvent;
import com.botamochi.easyannouncement.item.EATab;
import com.botamochi.easyannouncement.network.AnnounceSendToClient;
import com.botamochi.easyannouncement.registry.EASounds;
import com.botamochi.easyannouncement.registry.EATile;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Easyannouncement Mod のメインクラス。
 */
public class Easyannouncement implements ModInitializer {

    public static String MOD_ID = "easyannouncement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    // AnnounceTileが設置されている場所を追跡するセット
    private static final Set<BlockPos> announceTilePositions = new HashSet<>();

    public static Block EA_BLOCK = new AnnounceBlock(FabricBlockSettings.of(Material.GLASS).strength(6.0f, 6.0f).mapColor(MapColor.WHITE_GRAY).nonOpaque());
    public static BlockItem EA_BLOCKITEM = new BlockItem(EA_BLOCK, new Item.Settings().group(EATab.EA));

    @Override
    public void onInitialize() {
        // ブロックとブロックアイテムの登録
        Registry.register(Registry.BLOCK, id("announce_block"), EA_BLOCK);
        Registry.register(Registry.ITEM, id("announce_block"), EA_BLOCKITEM);

        // ブロックエンティティの登録
        EATile.init();

        // サウンドの登録
        EASounds.register();

        // イベントリスナーの登録
        PlatformSelectionEvent.EVENT.register(new AnnouncePlatformSelectionListener());

        // サーバー側でパケットを受信する
        AnnounceSendToClient.register();

        // AnnounceTileが設置されている位置を管理
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (World world : server.getWorlds()) {
                // すべてのAnnounceTileの位置に対してtickを呼び出す
                for (BlockPos pos : announceTilePositions) {
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity instanceof AnnounceTile) {
                        AnnounceTile announceTile = (AnnounceTile) blockEntity;
                        AnnounceTile.tick(world, pos, world.getBlockState(pos), announceTile);
                    }
                }
            }
        });
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    private static class AnnouncePlatformSelectionListener implements PlatformSelectionEvent.Listener { // PlatformSelectionEvent.Listener を実装
        @Override
        public void onPlatformSelected(net.minecraft.util.math.BlockPos pos, java.util.List<mtr.data.Platform> selectedPlatforms, int delaySeconds) { // メソッド名を修正
            LOGGER.info("Selected platforms: " + selectedPlatforms + ", Delay: " + delaySeconds + " seconds, BlockPos: " + pos);
        }
    }

    // AnnounceTileが設置される際に呼び出されるメソッド
    public static void registerAnnounceTilePosition(BlockPos pos) {
        announceTilePositions.add(pos);
    }

    // AnnounceTileが削除される際に呼び出されるメソッド
    public static void unregisterAnnounceTilePosition(BlockPos pos) {
        announceTilePositions.remove(pos);
    }

}