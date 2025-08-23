package ast.animcreator.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemEvents {

    private static BlockPos curRegionFirst;
    private static BlockPos curRegionSecond;

    public static ActionResult regionSelection(PlayerEntity player) {
        HitResult blockHit = player.raycast(20.0, 0.0f, false);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) blockHit).getBlockPos();

            player.sendMessage(Text.literal("Second region corner set at [" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ() + "]"), false);
            curRegionSecond = pos;
            if (curRegionFirst != null) {
                GlobalManager.curRegion = new Region(curRegionFirst, curRegionSecond);
                player.sendMessage(Text.literal("Total region size : " + GlobalManager.curRegion.computeRegionSize()), false);
            }
        }
        return ActionResult.SUCCESS;
    }

    public static ActionResult showNextFrameEvent(PlayerEntity player) {
        if (GlobalManager.curAnimation == null) {
            player.sendMessage(Text.literal("Not currently creating or modifying an animation."), false);
            return ActionResult.PASS;
        }
        if (AnimPlayer.isAnimationPlaying(GlobalManager.curAnimation.name)) {
            player.sendMessage(Text.literal("Current animation is playing. Pause it or stop it to show individual frames."), false);
            return ActionResult.PASS;
        }
        GlobalManager.curAnimation.advanceOneFrame();

        return ActionResult.SUCCESS;
    }

    public static ActionResult showPrevFrameEvent(PlayerEntity player) {
        if (GlobalManager.curAnimation == null) {
            player.sendMessage(Text.literal("Not currently creating or modifying an animation."), false);
            return ActionResult.PASS;
        }
        if (AnimPlayer.isAnimationPlaying(GlobalManager.curAnimation.name)) {
            player.sendMessage(Text.literal("Current animation is playing. Pause it or stop it to show individual frames."), false);
            return ActionResult.PASS;
        }
        GlobalManager.curAnimation.goBackOneFrame();

        return ActionResult.SUCCESS;
    }
}
