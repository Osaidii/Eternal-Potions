package osaidii.eternalpotions.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.network.chat.Component;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.network.KingsDataPayload;
import osaidii.eternalpotions.network.ModPresencePayload;

import java.util.List;

public class EternalPotionsClient implements ClientModInitializer {

	private static volatile List<KingsDataPayload.KingEntry> LAST_KINGS = List.of();

	private static int pendingOpenTicks = -1;

	public static List<KingsDataPayload.KingEntry> getLastKings() {
		return LAST_KINGS;
	}

	@Override
	public void onInitializeClient() {
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.CROWN_PARTICLE, EndRodParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.ETERNAL_LEVEL1_PARTICLE, EndRodParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.ETERNAL_LEVEL2_PARTICLE, EndRodParticle.Provider::new);

		ClientPlayNetworking.registerGlobalReceiver(
				KingsDataPayload.TYPE,
				(payload, context) -> LAST_KINGS = payload.kings()
		);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			boolean hasMod = ClientPlayNetworking.canSend(ModPresencePayload.TYPE);
			ServerModCheck.set(hasMod);
			EternalPotions.LOGGER.info("[EternalPotion] server has mod: {}", hasMod);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LAST_KINGS = List.of();
			pendingOpenTicks = -1;
			ServerModCheck.set(false);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (pendingOpenTicks < 0) return;

			if (pendingOpenTicks > 0) {
				pendingOpenTicks--;
				return;
			}

			pendingOpenTicks = -1;

			if (client.player == null || client.level == null) return;

			client.setScreenAndShow(new KingsScreen());
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommands.literal("kings")
							.executes(ctx -> {
								if (!ServerModCheck.serverHasMod()) {
									if (ctx.getSource().getPlayer() != null) {
										ctx.getSource().getPlayer().sendSystemMessage(
												Component.literal("Eternal Potions is not installed on this server.")
										);
									}
									return 0;
								}
								pendingOpenTicks = 2;
								return 1;
							})
			);
		});
	}
}