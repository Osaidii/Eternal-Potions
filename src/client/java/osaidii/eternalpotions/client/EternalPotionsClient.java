package osaidii.eternalpotions.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import osaidii.eternalpotions.EternalPotions;

public class EternalPotionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleProviderRegistry.getInstance().register(
				EternalPotions.CROWN_PARTICLE,
				EndRodParticle.Provider::new
		);
	}
}