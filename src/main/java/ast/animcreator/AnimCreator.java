package ast.animcreator;

import ast.animcreator.commands.Commands;
import ast.animcreator.core.*;
import ast.animcreator.item.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AnimCreator implements ModInitializer {
	public static final String MOD_ID = "animcreator";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static BlockPos curRegionFirst = null;
	private static BlockPos curRegionSecond = null;

	@Override
	public void onInitialize() {

		FileStorage.initModStorageDir();

		CommandRegistrationCallback.EVENT.register(Commands::registerCommands);
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopped);

		UseItemCallback.EVENT.register(this::onRightClickItem);
		UseBlockCallback.EVENT.register(this::UseBlockCallback);
		AttackBlockCallback.EVENT.register(this::AttackBlockCallback);

		ServerTickEvents.END_SERVER_TICK.register(AnimPlayer::onEndTick);

		ModItems.registerModItems();
	}

	private void onServerStarting(MinecraftServer server) {

	}

	private void onServerStarted(MinecraftServer server) {
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		for (ServerPlayerEntity player : players) {
			player.sendMessage(Text.literal("MOD STORAGE PATH IS " + FileStorage.modPath));
		}
		GlobalManager.server = server;

		List<String> errors = new ArrayList<>();
		FileStorage.loadAllAnimFiles(errors);
		for (String error : errors) {
			System.err.println(error);
		}
	}

	private void onServerStopped(MinecraftServer server) {
		//NOTE maybe consider not stopping all animations when server stops in the future
		// and instead continue playing where they stopped. But right now I don't see the point
		// and it is simpler this way.
		AnimPlayer.stopAllAnimations();
		// Clear all animations, they will be reloaded when the world is loaded again.
		GlobalManager.animations.clear();

		if (GlobalManager.curAnimation != null && GlobalManager.curAnimation.editedUnsaved) {
			// Save current animation in a tmp file
			List<String> errors = new ArrayList<>();
			FileStorage.saveTmpAnimFile(errors);
			for (String error : errors) {
				System.err.println(error);
			}
		}
		GlobalManager.curAnimation = null;
	}

	private ActionResult onRightClickItem(PlayerEntity player, World world, Hand hand) {
		if (!world.isClient) {
			if (player.getInventory().getSelectedStack().isOf(Items.LIME_DYE)) {
				return ItemEvents.secondRegionSelection(player);
			}
			else if (player.getInventory().getSelectedStack().isOf(Items.BLAZE_ROD)) {
				return ItemEvents.showNextFrameEvent(player);
			}
			else if (player.getInventory().getSelectedStack().isOf(Items.BREEZE_ROD)) {
				return ItemEvents.showPrevFrameEvent(player);
			}
		}
		return ActionResult.PASS;
	}

	private ActionResult UseBlockCallback(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		return ActionResult.PASS;
	}

	private ActionResult AttackBlockCallback(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		if (!world.isClient) {
			if (player.getInventory().getSelectedStack().isOf(Items.LIME_DYE)) {
				return ItemEvents.firstRegionSelection(player, world, pos);
			}
			return ActionResult.PASS;
		}
		else {
			return ActionResult.PASS;
		}
	}
}