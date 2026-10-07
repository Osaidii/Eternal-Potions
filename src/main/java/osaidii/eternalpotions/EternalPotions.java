package osaidii.eternalpotions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osaidii.eternalpotions.item.ModItems;

import java.util.List;

public class EternalPotions implements ModInitializer {
	public static final String MOD_ID = "eternal-potions";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final List<Holder<MobEffect>> BIG_FOUR = List.of(
			MobEffects.STRENGTH,
			MobEffects.SPEED,
			MobEffects.RESISTANCE,
			MobEffects.REGENERATION
	);

	@Override
	public void onInitialize() {
		ModItems.initialize();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(ModItems.ETERNAL_SHARD);
		});

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(ModItems.ETERNAL_POTION);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof ServerPlayer player)) return;

			// Check if the player had any of the four big effects
			boolean hadEternalEffect = false;
			for (Holder<MobEffect> effect : BIG_FOUR) {
				if (player.getEffect(effect) != null) {
					hadEternalEffect = true;
					break;
				}
			}

			if (!hadEternalEffect) return;

			// 50% chance to drop a shard
			if (player.getRandom().nextFloat() < 0.5f) {
				ItemStack shardStack = new ItemStack(ModItems.ETERNAL_SHARD);
				ItemEntity drop = new ItemEntity(
						player.level(),
						player.getX(),
						player.getY(),
						player.getZ(),
						shardStack
				);
				player.level().addFreshEntity(drop);
			}
		});

		LOGGER.info("Eternal Potions loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}