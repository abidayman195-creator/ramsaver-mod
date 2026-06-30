package com.ramsaver;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.text.Text;

public class RamSaverClient implements ClientModInitializer {

	private int tickCounter = 0;
	private int sustainedPressureCount = 0;
	private boolean optionsLowered = false;

	// Valores originales del usuario, para poder restaurarlos cuando baje la presión de memoria
	private int originalViewDistance = -1;
	private int originalSimulationDistance = -1;
	private double originalEntityDistanceScaling = -1;
	private ParticlesMode originalParticlesMode = null;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("ramsaver")
						.executes(this::showStatus)
						.then(ClientCommandManager.literal("gc")
								.executes(this::forceGc))
						.then(ClientCommandManager.literal("toggle")
								.executes(this::toggleEnabled))
				)
		);
		RamSaverMod.LOGGER.info("RAM Saver (cliente) listo. Usa /ramsaver para ver el estado.");
	}

	private void onClientTick(MinecraftClient client) {
		RamSaverConfig config = RamSaverMod.CONFIG;
		if (config == null || !config.enabled) {
			return;
		}

		tickCounter++;
		if (tickCounter < config.checkIntervalTicks) {
			return;
		}
		tickCounter = 0;

		MemoryStats stats = readMemoryStats();
		boolean underPressure = stats.usedPercent >= config.memoryPressureThresholdPercent;
		boolean recovered = stats.usedPercent <= config.memoryRecoveryThresholdPercent;

		if (underPressure) {
			sustainedPressureCount++;
			lowerSettingsIfNeeded(client);

			if (config.allowManualGcHint && sustainedPressureCount >= config.sustainedPressureChecksBeforeGc) {
				sustainedPressureCount = 0;
				suggestGarbageCollection(stats);
			}
		} else {
			sustainedPressureCount = 0;
			if (recovered) {
				restoreSettingsIfNeeded(client);
			}
		}
	}

	private void lowerSettingsIfNeeded(MinecraftClient client) {
		if (optionsLowered) {
			return;
		}
		GameOptions options = client.options;
		RamSaverConfig config = RamSaverMod.CONFIG;

		originalViewDistance = options.getViewDistance().getValue();
		originalSimulationDistance = options.getSimulationDistance().getValue();
		originalEntityDistanceScaling = options.getEntityDistanceScaling().getValue();
		originalParticlesMode = options.getParticles().getValue();

		options.getViewDistance().setValue(Math.min(originalViewDistance, config.minViewDistance));
		options.getSimulationDistance().setValue(Math.min(originalSimulationDistance, config.minSimulationDistance));
		options.getEntityDistanceScaling().setValue(Math.min(originalEntityDistanceScaling, config.minEntityDistanceScaling));
		options.getParticles().setValue(ParticlesMode.MINIMAL);
		options.write();

		optionsLowered = true;
		RamSaverMod.LOGGER.info("RAM Saver: memoria alta detectada, bajando ajustes de cliente temporalmente.");
		if (client.player != null) {
			client.player.sendMessage(Text.literal("[RAM Saver] Memoria alta: bajando distancia de render/simulación y partículas."), false);
		}
	}

	private void restoreSettingsIfNeeded(MinecraftClient client) {
		if (!optionsLowered) {
			return;
		}
		GameOptions options = client.options;

		if (originalViewDistance > 0) options.getViewDistance().setValue(originalViewDistance);
		if (originalSimulationDistance > 0) options.getSimulationDistance().setValue(originalSimulationDistance);
		if (originalEntityDistanceScaling > 0) options.getEntityDistanceScaling().setValue(originalEntityDistanceScaling);
		if (originalParticlesMode != null) options.getParticles().setValue(originalParticlesMode);
		options.write();

		optionsLowered = false;
		RamSaverMod.LOGGER.info("RAM Saver: memoria recuperada, restaurando ajustes originales.");
		if (client.player != null) {
			client.player.sendMessage(Text.literal("[RAM Saver] Memoria estable: ajustes originales restaurados."), false);
		}
	}

	/**
	 * Sugiere al JVM liberar memoria no usada. System.gc() es solo una sugerencia, no una orden,
	 * y se usa con moderación (cooldown controlado por sustainedPressureChecksBeforeGc) porque
	 * llamarlo con frecuencia puede generar pausas y no mejora el uso de RAM a largo plazo.
	 */
	private void suggestGarbageCollection(MemoryStats stats) {
		RamSaverMod.LOGGER.info("RAM Saver: presión de memoria sostenida ({}% usado), sugiriendo GC.", stats.usedPercent);
		System.gc();
	}

	private int showStatus(CommandContext<FabricClientCommandSource> context) {
		MemoryStats stats = readMemoryStats();
		context.getSource().sendFeedback(Text.literal(String.format(
				"[RAM Saver] Uso: %d%% (%d MB / %d MB máx). Ajustes bajados: %s. Mod activo: %s",
				stats.usedPercent, stats.usedMb, stats.maxMb, optionsLowered, RamSaverMod.CONFIG.enabled
		)));
		return 1;
	}

	private int forceGc(CommandContext<FabricClientCommandSource> context) {
		MemoryStats before = readMemoryStats();
		System.gc();
		MemoryStats after = readMemoryStats();
		context.getSource().sendFeedback(Text.literal(String.format(
				"[RAM Saver] GC sugerido. Antes: %d MB usados, después: %d MB usados.",
				before.usedMb, after.usedMb
		)));
		return 1;
	}

	private int toggleEnabled(CommandContext<FabricClientCommandSource> context) {
		RamSaverConfig config = RamSaverMod.CONFIG;
		config.enabled = !config.enabled;
		config.save();
		context.getSource().sendFeedback(Text.literal("[RAM Saver] Mod " + (config.enabled ? "activado" : "desactivado") + "."));
		return 1;
	}

	private MemoryStats readMemoryStats() {
		Runtime runtime = Runtime.getRuntime();
		long maxBytes = runtime.maxMemory();
		long totalBytes = runtime.totalMemory();
		long freeBytes = runtime.freeMemory();
		long usedBytes = totalBytes - freeBytes;

		int usedPercent = (int) Math.round((usedBytes * 100.0) / maxBytes);
		long usedMb = usedBytes / (1024 * 1024);
		long maxMb = maxBytes / (1024 * 1024);

		return new MemoryStats(usedPercent, usedMb, maxMb);
	}

	private record MemoryStats(int usedPercent, long usedMb, long maxMb) {
	}
}
