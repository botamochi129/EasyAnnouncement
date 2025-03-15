package com.botamochi.easyannouncement.block;

import com.botamochi.easyannouncement.Easyannouncement;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.structure.rule.AxisAlignedLinearPosRuleTest;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.AxisTransformation;
import net.minecraft.world.World;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;

import com.botamochi.easyannouncement.tile.AnnounceTile;

public class AnnounceBlock extends BlockWithEntity {
    public AnnounceBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        AnnounceTile announceTile = new AnnounceTile(pos, state);

        // AnnounceTile が設置された位置を登録
        Easyannouncement.registerAnnounceTilePosition(pos);

        return announceTile;
    }

    // BlockEntity が削除されるときに呼ばれる
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AnnounceTile) {
            Easyannouncement.unregisterAnnounceTilePosition(pos);  // AnnounceTile の位置を登録解除
        }
    }
}
