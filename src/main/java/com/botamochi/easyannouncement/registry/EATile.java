package com.botamochi.easyannouncement.registry;

import com.botamochi.easyannouncement.Easyannouncement;
import com.botamochi.easyannouncement.tile.AnnounceTile;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

/**
 * EATile クラスは、ブロックエンティティの登録を行います。
 */
public class EATile {

    /**
     * AnnounceTile のブロックエンティティタイプ。
     */
    public static final BlockEntityType<AnnounceTile> EA_BLOCK_TILE = FabricBlockEntityTypeBuilder.create(
            AnnounceTile::new, Easyannouncement.EA_BLOCK
    ).build();

    /**
     * ブロックエンティティを登録します。
     */
    public static void init() {
        Registry.register(Registry.BLOCK_ENTITY_TYPE, Easyannouncement.id("announce_tile"), EA_BLOCK_TILE);
    }
}