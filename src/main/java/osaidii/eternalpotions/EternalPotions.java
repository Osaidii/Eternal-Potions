package osaidii.eternalpotions;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osaidii.eternalpotions.effect.EternalEffects;
import osaidii.eternalpotions.item.ModItems;
import osaidii.eternalpotions.network.EternalNetworking;
import osaidii.eternalpotions.network.KingsDataPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EternalPotions implements ModInitializer {
	public static final String MOD_ID = "eternal-potions";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType CROWN_PARTICLE = FabricParticleTypes.simple();
	public static final SimpleParticleType ETERNAL_LEVEL1_PARTICLE = FabricParticleTypes.simple();
	public static final SimpleParticleType ETERNAL_LEVEL2_PARTICLE = FabricParticleTypes.simple();

	private static final List<Holder<MobEffect>> BIG_FOUR = List.of(
			EternalEffects.ETERNAL_SPEED,
			EternalEffects.ETERNAL_REGENERATION,
			EternalEffects.ETERNAL_RESISTANCE,
			EternalEffects.ETERNAL_STRENGTH
	);

	private static boolean allThronesAnnounced = false;

	public static MobEffectInstance eternalInstance(Holder<MobEffect> effect, int amplifier) {
		return new MobEffectInstance(
				effect,
				MobEffectInstance.INFINITE_DURATION,
				amplifier,
				false,
				false,
				true
		);
	}

	public static Component effectName(Holder<MobEffect> effect) {
		return Component.translatable(effect.value().getDescriptionId());
	}

	@Override
	public void onInitialize() {
		EternalEffects.initialize();
		ModItems.initialize();
		EternalNetworking.register();

		Registry.register(BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(MOD_ID, "crown_particle"),
				CROWN_PARTICLE);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(MOD_ID, "eternal_level1_particle"),
				ETERNAL_LEVEL1_PARTICLE);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(MOD_ID, "eternal_level2_particle"),
				ETERNAL_LEVEL2_PARTICLE);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(ModItems.ETERNAL_SHARD);
		});

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(ModItems.ETERNAL_POTION);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> BrewWaypoints.clear(server));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			EternalState state = getState(server);
			String currentName = player.getName().getString();

			for (Holder<MobEffect> effect : BIG_FOUR) {
				String effectId = effect.value().getDescriptionId();
				UUID kingUuid = state.getKing(effectId);
				if (kingUuid != null && kingUuid.equals(player.getUUID())) {
					state.updateKingName(effectId, currentName);
				}
			}

			broadcastKings(server);
		});

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

					server.getPlayerList().broadcastSystemMessage(
							Component.translatable("eternal-potions.king.fallen",
											effectName(effect),
											player.getName())
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

			checkThrones(server, state);
			broadcastKings(server);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tickCounter = server.getTickCount();
			EternalState state = getState(server);

			if (tickCounter % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					SimpleParticleType particle = selectParticle(player, state);
					if (particle != null) {
						ServerLevel level = (ServerLevel) player.level();
						level.sendParticles(
								particle,
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

				boolean removed = cleanupGhostKings(server, state);
				if (removed) {
					checkThrones(server, state);
					broadcastKings(server);
				}
				BrewWaypoints.tick(server);

				broadcastKings(server);
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

	public static void broadcastKings(MinecraftServer server) {
		EternalState state = getState(server);
		List<KingsDataPayload.KingEntry> entries = new ArrayList<>(4);

		for (Holder<MobEffect> effect : BIG_FOUR) {
			String effectId = effect.value().getDescriptionId();
			String name = state.getKingName(effectId);
			entries.add(new KingsDataPayload.KingEntry(effectId, name == null ? "" : name));
		}

		KingsDataPayload payload = new KingsDataPayload(entries);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void closeMenusViewing(MinecraftServer server, BlockPos pos) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			AbstractContainerMenu menu = player.containerMenu;
			if (menu == null) continue;

			boolean viewing = false;
			for (var slot : menu.slots) {
				if (slot.container instanceof BrewingStandBlockEntity stand) {
					if (stand.getBlockPos().equals(pos)) {
						viewing = true;
						break;
					}
				}
			}

			if (viewing) {
				player.closeContainer();
			}
		}
	}

	private static SimpleParticleType selectParticle(ServerPlayer player, EternalState state) {
		UUID playerId = player.getUUID();
		boolean isKing = false;
		boolean hasLevel2 = false;
		boolean hasLevel1 = false;

		for (Holder<MobEffect> effect : BIG_FOUR) {
			UUID kingUuid = state.getKing(effect.value().getDescriptionId());
			if (kingUuid != null && kingUuid.equals(playerId)) {
				isKing = true;
			}

			MobEffectInstance inst = player.getEffect(effect);
			if (inst != null) {
				if (inst.getAmplifier() >= 1) {
					hasLevel2 = true;
				} else if (inst.getAmplifier() == 0) {
					hasLevel1 = true;
				}
			}
		}

		if (isKing) return CROWN_PARTICLE;
		if (hasLevel2) return ETERNAL_LEVEL2_PARTICLE;
		if (hasLevel1) return ETERNAL_LEVEL1_PARTICLE;
		return null;
	}

	private static boolean cleanupGhostKings(MinecraftServer server, EternalState state) {
		boolean any = false;
		for (Holder<MobEffect> effect : BIG_FOUR) {
			String effectId = effect.value().getDescriptionId();
			UUID kingUuid = state.getKing(effectId);
			if (kingUuid == null) continue;

			ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
			if (king == null) continue;

			MobEffectInstance inst = king.getEffect(effect);
			if (inst == null || inst.getAmplifier() < 2) {
				state.removeKing(effectId);
				server.getPlayerList().broadcastSystemMessage(
						Component.translatable("eternal-potions.king.throne_empty", effectName(effect))
								.withStyle(ChatFormatting.GRAY),
						false
				);
				any = true;
			}
		}
		return any;
	}

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
			if (name == null) name = "?";
			server.getPlayerList().broadcastSystemMessage(
					Component.translatable("eternal-potions.king.all_four_single", name)
							.withStyle(ChatFormatting.GOLD),
					false
			);
		} else {
			server.getPlayerList().broadcastSystemMessage(
					Component.translatable("eternal-potions.king.all_four_multi")
							.withStyle(ChatFormatting.GOLD),
					false
			);
		}
	}

	private static boolean hasPermission(CommandSourceStack source) {
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
							.then(Commands.literal("stop")
									.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.executes(ctx -> {
												CommandSourceStack source = ctx.getSource();
												ServerLevel level = source.getLevel();
												BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

												if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) {
													source.sendFailure(Component.translatable(
															"eternal-potions.command.stop.no_stand",
															pos.getX(), pos.getY(), pos.getZ()));
													return 0;
												}

												ItemStack reagent = stand.getItem(3);
												boolean hasShard = !reagent.isEmpty() && reagent.is(ModItems.ETERNAL_SHARD);
												boolean hasPotion = !stand.getItem(0).isEmpty()
														|| !stand.getItem(1).isEmpty()
														|| !stand.getItem(2).isEmpty();

												if (!hasShard || !hasPotion) {
													source.sendFailure(Component.translatable(
															"eternal-potions.command.stop.not_brewing",
															pos.getX(), pos.getY(), pos.getZ()));
													return 0;
												}

												closeMenusViewing(level.getServer(), pos);
												BrewWaypoints.stopBrew(level, pos);
												level.destroyBlock(pos, true);

												level.getServer().getPlayerList().broadcastSystemMessage(
														Component.translatable("eternal-potions.brew.stopped",
																		pos.getX(), pos.getY(), pos.getZ())
																.withStyle(ChatFormatting.LIGHT_PURPLE),
														false
												);

												source.sendSuccess(() -> Component.translatable(
														"eternal-potions.command.stop.destroyed",
														pos.getX(), pos.getY(), pos.getZ()), true);
												return 1;
											})
									)
							)
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
														String effectNameArg = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectNameArg);

														if (effect == null) {
															ctx.getSource().sendFailure(Component.translatable(
																	"eternal-potions.command.unknown_effect", effectNameArg));
															return 0;
														}

														MinecraftServer server = ctx.getSource().getServer();
														EternalState state = getState(server);

														MobEffectInstance existing = target.getEffect(effect);
														int nextAmplifier = existing == null ? 0 : existing.getAmplifier() + 1;

														if (nextAmplifier >= 2) {
															String effectId = effect.value().getDescriptionId();

															UUID currentKing = state.getKing(effectId);
															if (currentKing != null && currentKing.equals(target.getUUID())) {
																ctx.getSource().sendFailure(Component.translatable(
																		"eternal-potions.command.already_king", effectName(effect)));
																return 0;
															}

															if (!state.isSlotEmpty(effectId)) {
																ctx.getSource().sendFailure(Component.translatable(
																		"eternal-potions.command.king_slot_taken",
																		effectName(effect), target.getName()));
																target.sendSystemMessage(Component.translatable(
																				"eternal-potions.command.cannot_reach_iii",
																				effectName(effect))
																		.withStyle(ChatFormatting.RED));
																return 0;
															}

															state.crownKing(effectId, target.getUUID(), target.getName().getString());
															target.addEffect(eternalInstance(effect, 2));
															server.getPlayerList().broadcastSystemMessage(
																	Component.translatable("eternal-potions.king.crowned",
																					target.getName(), effectName(effect))
																			.withStyle(ChatFormatting.GOLD),
																	false
															);
															target.sendSystemMessage(Component.translatable(
																			"eternal-potions.king.crowned_self",
																			effectName(effect))
																	.withStyle(ChatFormatting.GOLD));
															ctx.getSource().sendSuccess(() -> Component.translatable(
																	"eternal-potions.command.crowned",
																	target.getName(), effectName(effect)), true);
															checkThrones(server, state);
															broadcastKings(server);
															return 1;
														}

														target.addEffect(eternalInstance(effect, nextAmplifier));
														String level = nextAmplifier == 0 ? "I" : "II";
														target.sendSystemMessage(Component.translatable(
																		"eternal-potions.command.gave_self",
																		effectName(effect), level)
																.withStyle(ChatFormatting.GOLD));
														ctx.getSource().sendSuccess(() -> Component.translatable(
																"eternal-potions.command.gave",
																effectName(effect), level, target.getName()), true);
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
														String effectNameArg = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectNameArg);

														if (effect == null) {
															ctx.getSource().sendFailure(Component.translatable(
																	"eternal-potions.command.unknown_effect", effectNameArg));
															return 0;
														}

														if (target.getEffect(effect) == null) {
															ctx.getSource().sendFailure(Component.translatable(
																	"eternal-potions.command.no_effect",
																	target.getName(), effectName(effect)));
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
															target.addEffect(eternalInstance(effect, 1));
															server.getPlayerList().broadcastSystemMessage(
																	Component.translatable("eternal-potions.king.fallen",
																					effectName(effect), target.getName())
																			.withStyle(ChatFormatting.RED),
																	false
															);
															target.sendSystemMessage(Component.translatable(
																			"eternal-potions.command.dethroned_self",
																			effectName(effect))
																	.withStyle(ChatFormatting.RED));
														} else {
															target.removeEffect(effect);
															target.sendSystemMessage(Component.translatable(
																			"eternal-potions.command.removed_self",
																			effectName(effect))
																	.withStyle(ChatFormatting.RED));
														}

														ctx.getSource().sendSuccess(() -> Component.translatable(
																"eternal-potions.command.removed",
																effectName(effect), target.getName()), true);
														checkThrones(server, state);
														broadcastKings(server);
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
																String effectNameArg = StringArgumentType.getString(ctx, "effect");
																Holder<MobEffect> effect = parseEffect(effectNameArg);
																if (effect == null) {
																	ctx.getSource().sendFailure(Component.translatable(
																			"eternal-potions.command.unknown_effect", effectNameArg));
																	return 0;
																}
																MinecraftServer server = ctx.getSource().getServer();
																EternalState state = getState(server);
																String effectId = effect.value().getDescriptionId();

																UUID existingKing = state.getKing(effectId);
																if (existingKing != null && !existingKing.equals(target.getUUID())) {
																	ServerPlayer oldKing = server.getPlayerList().getPlayer(existingKing);
																	if (oldKing != null) {
																		oldKing.removeEffect(effect);
																		oldKing.addEffect(eternalInstance(effect, 1));
																		oldKing.sendSystemMessage(Component.translatable(
																						"eternal-potions.command.dethroned_self",
																						effectName(effect))
																				.withStyle(ChatFormatting.RED));
																	}
																}

																state.crownKing(effectId, target.getUUID(), target.getName().getString());
																target.addEffect(eternalInstance(effect, 2));
																server.getPlayerList().broadcastSystemMessage(
																		Component.translatable("eternal-potions.king.crowned",
																						target.getName(), effectName(effect))
																				.withStyle(ChatFormatting.GOLD),
																		false
																);
																checkThrones(server, state);
																broadcastKings(server);
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
														String effectNameArg = StringArgumentType.getString(ctx, "effect");
														Holder<MobEffect> effect = parseEffect(effectNameArg);
														if (effect == null) {
															ctx.getSource().sendFailure(Component.translatable(
																	"eternal-potions.command.unknown_effect", effectNameArg));
															return 0;
														}
														MinecraftServer server = ctx.getSource().getServer();
														EternalState state = getState(server);
														UUID kingUuid = state.getKing(effect.value().getDescriptionId());
														if (kingUuid == null) {
															ctx.getSource().sendFailure(Component.translatable(
																	"eternal-potions.command.no_king", effectName(effect)));
															return 0;
														}
														ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
														if (king != null) {
															king.removeEffect(effect);
															king.addEffect(eternalInstance(effect, 1));
															king.sendSystemMessage(Component.translatable(
																			"eternal-potions.command.dethroned_self",
																			effectName(effect))
																	.withStyle(ChatFormatting.RED));
														}
														state.removeKing(effect.value().getDescriptionId());
														ctx.getSource().sendSuccess(() -> Component.translatable(
																"eternal-potions.command.dethroned", effectName(effect)), true);
														checkThrones(server, state);
														broadcastKings(server);
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
														ctx.getSource().sendSuccess(() -> Component.translatable(
																"eternal-potions.command.list_empty", effectName(effect)), false);
													} else {
														String name = state.getKingName(effect.value().getDescriptionId());
														if (name == null) name = "?";
														final String finalName = name;
														ctx.getSource().sendSuccess(() -> Component.translatable(
																"eternal-potions.command.list_line", effectName(effect), finalName), false);
													}
												}
												return 1;
											})
									)
							)
			);
		});
	}

	private static Holder<MobEffect> parseEffect(String name) {
		return switch (name.toLowerCase()) {
			case "speed" -> EternalEffects.ETERNAL_SPEED;
			case "regeneration", "regen" -> EternalEffects.ETERNAL_REGENERATION;
			case "resistance", "resis" -> EternalEffects.ETERNAL_RESISTANCE;
			case "strength", "strenght", "strengh" -> EternalEffects.ETERNAL_STRENGTH;
			default -> null;
		};
	}

	public static Holder<MobEffect> effectFromId(String effectId) {
		for (Holder<MobEffect> effect : BIG_FOUR) {
			if (effect.value().getDescriptionId().equals(effectId)) return effect;
		}
		return null;
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