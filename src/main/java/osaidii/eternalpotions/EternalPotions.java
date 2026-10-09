package osaidii.eternalpotions;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osaidii.eternalpotions.item.ModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EternalPotions implements ModInitializer {
	public static final String MOD_ID = "eternal-potions";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType CROWN_PARTICLE = FabricParticleTypes.simple();

	private static final List<Holder<MobEffect>> BIG_FOUR = List.of(
			MobEffects.SPEED,
			MobEffects.REGENERATION,
			MobEffects.RESISTANCE,
			MobEffects.STRENGTH
	);

	/** UUID -> description IDs of thrones that were held at moment of death. */
	private static final Map<UUID, List<String>> PENDING_KING_RESPAWN = new HashMap<>();

	/** Guards the "all four thrones claimed" broadcast so it only fires once per fill. */
	private static boolean allThronesAnnounced = false;

	@Override
	public void onInitialize() {
		ModItems.initialize();

		Registry.register(BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(MOD_ID, "crown_particle"),
				CROWN_PARTICLE);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(ModItems.ETERNAL_SHARD);
		});

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(ModItems.ETERNAL_POTION);
		});

		// -------- Death handling (also stashes pending respawn thrones) --------
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof ServerPlayer player)) return;

			ServerLevel level = (ServerLevel) player.level();
			MinecraftServer server = level.getServer();
			EternalState state = getState(server);

			boolean hadEternalEffect = false;
			for (Holder<MobEffect> effect : BIG_FOUR) {
				if (player.getEffect(effect) != null) {
					hadEternalEffect = true;
					break;
				}
			}

			for (Holder<MobEffect> effect : BIG_FOUR) {
				String effectId = effect.value().getDescriptionId();
				UUID kingUuid = state.getKing(effectId);

				if (kingUuid != null && kingUuid.equals(player.getUUID())) {
					state.removeKing(effectId);

					// Remember this throne so we can restore it at Level II on respawn.
					PENDING_KING_RESPAWN
							.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
							.add(effectId);

					server.getPlayerList().broadcastSystemMessage(
							Component.literal("The King of " + getEffectDisplayName(effect) + " (")
									.append(player.getName())
									.append(Component.literal(") has fallen. The throne is empty."))
									.withStyle(ChatFormatting.RED),
							false
					);
				}
			}

			for (Holder<MobEffect> effect : BIG_FOUR) {
				player.removeEffect(effect);
			}

			if (hadEternalEffect && player.getRandom().nextFloat() < 0.5f) {
				ItemStack shardStack = new ItemStack(ModItems.ETERNAL_SHARD);
				ItemEntity drop = new ItemEntity(
						level,
						player.getX(),
						player.getY(),
						player.getZ(),
						shardStack
				);
				level.addFreshEntity(drop);
			}
		});

		// -------- King death -> respawn at Level II --------
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			List<String> effectIds = PENDING_KING_RESPAWN.remove(oldPlayer.getUUID());
			if (effectIds == null || effectIds.isEmpty()) return;

			for (String effectId : effectIds) {
				Holder<MobEffect> effect = effectFromId(effectId);
				if (effect == null) continue;
				newPlayer.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 1));
				newPlayer.sendSystemMessage(Component.literal(
								"Your royal blood endures — you keep " +
										getEffectDisplayName(effect) + " II.")
						.withStyle(ChatFormatting.GOLD));
			}
		});

		// -------- Tick: crown aura + ghost king cleanup --------
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tickCounter = server.getTickCount();
			EternalState state = getState(server);

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UUID playerId = player.getUUID();

				boolean isKing = false;
				for (Holder<MobEffect> effect : BIG_FOUR) {
					UUID kingUuid = state.getKing(effect.value().getDescriptionId());
					if (kingUuid != null && kingUuid.equals(playerId)) {
						isKing = true;
						break;
					}
				}

				ServerLevel level = (ServerLevel) player.level();

				if (isKing && tickCounter % 20 == 0) {
					level.sendParticles(
							CROWN_PARTICLE,
							player.getX(),
							player.getY() + 1.9,
							player.getZ(),
							1,
							0.25,
							0.05,
							0.25,
							0.0
					);
				}
			}

			// -------- Ghost king cleanup --------
			if (tickCounter % 20 == 0) {
				cleanupGhostKings(server, state);
			}
		});

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(state.getBlock() instanceof BrewingStandBlock)) return true;

			if (blockEntity instanceof BrewingStandBlockEntity stand) {
				ItemStack reagent = stand.getItem(3);
				if (reagent.is(ModItems.ETERNAL_SHARD)) {
					boolean hasPotion = !stand.getItem(0).isEmpty()
							|| !stand.getItem(1).isEmpty()
							|| !stand.getItem(2).isEmpty();
					if (hasPotion) {
						return false;
					}
				}
			}

			return true;
		});

		registerCommands();

		LOGGER.info("Eternal Potions loaded");
	}

	// ---------------------------------------------------------------
	//  Ghost king cleanup
	// ---------------------------------------------------------------
	private static void cleanupGhostKings(MinecraftServer server, EternalState state) {
		for (Holder<MobEffect> effect : BIG_FOUR) {
			String effectId = effect.value().getDescriptionId();
			UUID kingUuid = state.getKing(effectId);
			if (kingUuid == null) continue;

			ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
			if (king == null) continue; // offline — leave the throne intact

			MobEffectInstance inst = king.getEffect(effect);
			if (inst == null || inst.getAmplifier() < 2) {
				state.removeKing(effectId);
				server.getPlayerList().broadcastSystemMessage(
						Component.literal("The throne of " + getEffectDisplayName(effect) +
										" sits empty once more.")
								.withStyle(ChatFormatting.GRAY),
						false
				);
			}
		}
	}

	// ---------------------------------------------------------------
	//  All-four-thrones broadcast
	// ---------------------------------------------------------------
	public static void checkThrones(MinecraftServer server, EternalState state) {
		boolean allFilled = true;
		for (Holder<MobEffect> effect : BIG_FOUR) {
			if (state.isSlotEmpty(effect.value().getDescriptionId())) {
				allFilled = false;
				break;
			}
		}

		if (!allFilled) {
			allThronesAnnounced = false;
			return;
		}
		if (allThronesAnnounced) return;
		allThronesAnnounced = true;

		UUID soleHolder = null;
		boolean oneHolder = true;
		for (Holder<MobEffect> effect : BIG_FOUR) {
			UUID u = state.getKing(effect.value().getDescriptionId());
			if (soleHolder == null) soleHolder = u;
			else if (!soleHolder.equals(u)) { oneHolder = false; break; }
		}

		if (oneHolder && soleHolder != null) {
			String name = state.getKingName(BIG_FOUR.get(0).value().getDescriptionId());
			if (name == null) name = "A player";
			server.getPlayerList().broadcastSystemMessage(
					Component.literal(name + " has claimed ALL FOUR THRONES. The Eternal belongs to them.")
							.withStyle(ChatFormatting.GOLD),
					false
			);
		} else {
			server.getPlayerList().broadcastSystemMessage(
					Component.literal("All four thrones have been claimed.").withStyle(ChatFormatting.GOLD),
					false
			);
		}
	}

	// ---------------------------------------------------------------
	//  Command permission gate
	// ---------------------------------------------------------------
	/**
	 * Console / RCON always passes. Players must be opped on the server.
	 * Uses NameAndId because PlayerList.isOp stopped accepting GameProfile in 26.3.
	 */
	private static boolean hasPermission(net.minecraft.commands.CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) return true;
		NameAndId id = new NameAndId(player.getUUID(), player.getName().getString());
		return source.getServer().getPlayerList().isOp(id);
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					Commands.literal("eternal")
							.requires(EternalPotions::hasPermission)
							.then(Commands.literal("give")
									.then(Commands.argument("player", EntityArgument.player())
											.then(Commands.argument("effect", StringArgumentType.word())
													.suggests((ctx, builder) -> {
														builder.suggest("speed");
														builder.suggest("regeneration");
														builder.suggest("resistance");
														builder.suggest("strength");
														return builder.buildFuture();
													})
													.executes(ctx -> {
														ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
														String effectName = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectName);

														if (effect == null) {
															ctx.getSource().sendFailure(Component.literal("Unknown effect: " + effectName));
															return 0;
														}

														MinecraftServer server = ctx.getSource().getServer();
														EternalState state = getState(server);

														MobEffectInstance existing = target.getEffect(effect);
														int nextAmplifier = existing == null ? 0 : existing.getAmplifier() + 1;

														if (nextAmplifier >= 2) {
															String effectId = effect.value().getDescriptionId();
															if (!state.isSlotEmpty(effectId)) {
																ctx.getSource().sendFailure(Component.literal(
																		getEffectDisplayName(effect) + " King slot is taken. " +
																				target.getName().getString() + " stays at II."));
																target.sendSystemMessage(Component.literal(
																		"You cannot reach " + getEffectDisplayName(effect) +
																				" III — someone else is King.").withStyle(ChatFormatting.RED));
																return 0;
															}

															state.crownKing(effectId, target.getUUID(), target.getName().getString());
															target.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 2));
															server.getPlayerList().broadcastSystemMessage(
																	Component.literal("")
																			.append(target.getName())
																			.append(Component.literal(" has been crowned the King of " + getEffectDisplayName(effect) + "!"))
																			.withStyle(ChatFormatting.GOLD),
																	false
															);
															target.sendSystemMessage(Component.literal(
																	"You are now King of " + getEffectDisplayName(effect) + "!").withStyle(ChatFormatting.GOLD));
															ctx.getSource().sendSuccess(() -> Component.literal(
																	"Crowned " + target.getName().getString() +
																			" as King of " + getEffectDisplayName(effect)), true);
															checkThrones(server, state);
															return 1;
														}

														target.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, nextAmplifier));
														String level = nextAmplifier == 0 ? "I" : "II";
														target.sendSystemMessage(Component.literal(
																"You were given " + getEffectDisplayName(effect) + " " + level).withStyle(ChatFormatting.GOLD));
														ctx.getSource().sendSuccess(() -> Component.literal(
																"Gave " + getEffectDisplayName(effect) + " " + level +
																		" to " + target.getName().getString()), true);
														return 1;
													})
											)
									)
							)
							.then(Commands.literal("take")
									.then(Commands.argument("player", EntityArgument.player())
											.then(Commands.argument("effect", StringArgumentType.word())
													.suggests((ctx, builder) -> {
														builder.suggest("speed");
														builder.suggest("regeneration");
														builder.suggest("resistance");
														builder.suggest("strength");
														return builder.buildFuture();
													})
													.executes(ctx -> {
														ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
														String effectName = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectName);

														if (effect == null) {
															ctx.getSource().sendFailure(Component.literal("Unknown effect: " + effectName));
															return 0;
														}

														if (target.getEffect(effect) == null) {
															ctx.getSource().sendFailure(Component.literal(
																	target.getName().getString() + " doesn't have " + getEffectDisplayName(effect)));
															return 0;
														}

														MinecraftServer server = ctx.getSource().getServer();
														EternalState state = getState(server);
														String effectId = effect.value().getDescriptionId();

														boolean wasKing = false;
														UUID kingUuid = state.getKing(effectId);
														if (kingUuid != null && kingUuid.equals(target.getUUID())) {
															wasKing = true;
															state.removeKing(effectId);
														}

														if (wasKing) {
															target.removeEffect(effect);
															target.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 1));
															server.getPlayerList().broadcastSystemMessage(
																	Component.literal("The King of " + getEffectDisplayName(effect) + " (")
																			.append(target.getName())
																			.append(Component.literal(") has fallen. The throne is empty."))
																			.withStyle(ChatFormatting.RED),
																	false
															);
															target.sendSystemMessage(Component.literal(
																	"You have been dethroned from " + getEffectDisplayName(effect) +
																			" and dropped to Level II.").withStyle(ChatFormatting.RED));
														} else {
															target.removeEffect(effect);
															target.sendSystemMessage(Component.literal(
																			"Your " + getEffectDisplayName(effect) + " was removed.")
																	.withStyle(ChatFormatting.RED));
														}

														ctx.getSource().sendSuccess(() -> Component.literal(
																"Removed " + getEffectDisplayName(effect) +
																		" from " + target.getName().getString()), true);
														return 1;
													})
											)
									)
							)
							.then(Commands.literal("king")
									.then(Commands.literal("crown")
											.then(Commands.argument("player", EntityArgument.player())
													.then(Commands.argument("effect", StringArgumentType.word())
															.suggests((ctx, builder) -> {
																builder.suggest("speed");
																builder.suggest("regeneration");
																builder.suggest("resistance");
																builder.suggest("strength");
																return builder.buildFuture();
															})
															.executes(ctx -> {
																ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
																String effectName = StringArgumentType.getString(ctx, "effect");
																Holder<MobEffect> effect = parseEffect(effectName);
																if (effect == null) {
																	ctx.getSource().sendFailure(Component.literal("Unknown effect: " + effectName));
																	return 0;
																}
																MinecraftServer server = ctx.getSource().getServer();
																EternalState state = getState(server);
																state.crownKing(effect.value().getDescriptionId(), target.getUUID(), target.getName().getString());
																target.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 2));
																server.getPlayerList().broadcastSystemMessage(
																		Component.literal("")
																				.append(target.getName())
																				.append(Component.literal(" has been crowned the King of " + getEffectDisplayName(effect) + "!"))
																				.withStyle(ChatFormatting.GOLD),
																		false
																);
																checkThrones(server, state);
																return 1;
															})
													)
											)
									)
									.then(Commands.literal("dethrone")
											.then(Commands.argument("effect", StringArgumentType.word())
													.suggests((ctx, builder) -> {
														builder.suggest("speed");
														builder.suggest("regeneration");
														builder.suggest("resistance");
														builder.suggest("strength");
														return builder.buildFuture();
													})
													.executes(ctx -> {
														String effectName = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectName);
														if (effect == null) {
															ctx.getSource().sendFailure(Component.literal("Unknown effect: " + effectName));
															return 0;
														}
														MinecraftServer server = ctx.getSource().getServer();
														EternalState state = getState(server);
														UUID kingUuid = state.getKing(effect.value().getDescriptionId());
														if (kingUuid == null) {
															ctx.getSource().sendFailure(Component.literal("No King of " + getEffectDisplayName(effect)));
															return 0;
														}
														ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
														if (king != null) {
															king.removeEffect(effect);
															king.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 1));
															king.sendSystemMessage(Component.literal(
																	"You have been dethroned from " + getEffectDisplayName(effect) +
																			" and dropped to Level II.").withStyle(ChatFormatting.RED));
														}
														state.removeKing(effect.value().getDescriptionId());
														ctx.getSource().sendSuccess(() -> Component.literal(
																"Dethroned the King of " + getEffectDisplayName(effect)), true);
														return 1;
													})
											)
									)
									.then(Commands.literal("list")
											.executes(ctx -> {
												MinecraftServer server = ctx.getSource().getServer();
												EternalState state = getState(server);
												for (Holder<MobEffect> effect : BIG_FOUR) {
													UUID kingUuid = state.getKing(effect.value().getDescriptionId());
													if (kingUuid == null) {
														ctx.getSource().sendSuccess(() -> Component.literal(getEffectDisplayName(effect) + ": empty"), false);
													} else {
														String name = state.getKingName(effect.value().getDescriptionId());
														if (name == null) name = "unknown";
														final String finalName = name;
														ctx.getSource().sendSuccess(() -> Component.literal(getEffectDisplayName(effect) + ": " + finalName), false);
													}
												}
												return 1;
											})
									)
							)
			);
		});
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	private static Holder<MobEffect> parseEffect(String name) {
		return switch (name.toLowerCase()) {
			case "speed" -> MobEffects.SPEED;
			case "regeneration", "regen" -> MobEffects.REGENERATION;
			case "resistance", "resis" -> MobEffects.RESISTANCE;
			case "strength", "strenght", "strengh" -> MobEffects.STRENGTH;
			default -> null;
		};
	}

	/** Reverse lookup from a description ID back to a Holder. */
	public static Holder<MobEffect> effectFromId(String effectId) {
		for (Holder<MobEffect> effect : BIG_FOUR) {
			if (effect.value().getDescriptionId().equals(effectId)) return effect;
		}
		return null;
	}

	public static String getEffectDisplayName(Holder<MobEffect> effect) {
		if (effect == MobEffects.SPEED) return "Speed";
		if (effect == MobEffects.REGENERATION) return "Regeneration";
		if (effect == MobEffects.RESISTANCE) return "Resistance";
		if (effect == MobEffects.STRENGTH) return "Strength";
		return "Unknown";
	}

	public static EternalState getState(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(
				new SavedDataType<>(
						Identifier.fromNamespaceAndPath(MOD_ID, "kings"),
						EternalState::new,
						EternalState.CODEC,
						null
				)
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}