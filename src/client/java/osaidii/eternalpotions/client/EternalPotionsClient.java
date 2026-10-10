package osaidii.eternalpotions.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EndRodParticle;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.network.KingsDataPayload;

import java.util.List;

public class EternalPotionsClient implements ClientModInitializer {

	private static volatile List<KingsDataPayload.KingEntry> LAST_KINGS = List.of();

	/** Set by /kings, consumed on the next client tick. */
	private static boolean pendingOpen = false;

	public static List<KingsDataPayload.KingEntry> getLastKings() {
		return LAST_KINGS;
	}

	@Override
	public void onInitializeClient() {
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.CROWN_PARTICLE,
				EndRodParticle.Provider::new
		);
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.ETERNAL_LEVEL1_PARTICLE,
				EndRodParticle.Provider::new
		);
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.ETERNAL_LEVEL2_PARTICLE,
				EndRodParticle.Provider::new
		);

		ClientPlayNetworking.registerGlobalReceiver(
				KingsDataPayload.TYPE,
				(payload, context) -> LAST_KINGS = payload.kings()
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				LAST_KINGS = List.of()
		);

		// Deferred open — avoids the chat-close race that would replace our screen.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!pendingOpen) return;
			pendingOpen = false;

			if (client.player == null || client.level == null) return;
			client.setScreenAndShow(new KingsScreen());
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommands.literal("kings")
							.executes(ctx -> {
								pendingOpen = true;
								return 1;
							})
			);
		});
	}
}