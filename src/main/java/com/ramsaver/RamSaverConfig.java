package com.ramsaver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración persistente del mod, guardada en config/ramsaver.json
 */
public class RamSaverConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir().resolve("ramsaver.json");

	// --- Opciones configurables ---

	/** Activa o desactiva todo el comportamiento automático del mod. */
	public boolean enabled = true;

	/**
	 * Porcentaje de heap usado (0-100) a partir del cual se considera "presión de memoria".
	 * Bajado a 70% (en vez de 80%) porque con poco heap disponible (equipos de 4GB de RAM)
	 * conviene reaccionar antes, ya que el margen hasta un OutOfMemoryError es más pequeño.
	 */
	public int memoryPressureThresholdPercent = 70;

	/** Porcentaje de heap usado a partir del cual se restauran los valores normales. */
	public int memoryRecoveryThresholdPercent = 50;

	/**
	 * Cada cuántos ticks de cliente se revisa la memoria (20 ticks = 1 segundo).
	 * Más frecuente (60 ticks = 3s) que el valor por defecto general, para reaccionar rápido
	 * en equipos con poco margen de memoria.
	 */
	public int checkIntervalTicks = 60;

	/** Si está activo, permite forzar una sugerencia de recolección de basura cuando hay presión sostenida. */
	public boolean allowManualGcHint = true;

	/** Cuántas revisiones seguidas con presión de memoria deben pasar antes de pedir GC (evita abusar de System.gc()). */
	public int sustainedPressureChecksBeforeGc = 3;

	/**
	 * Distancia de renderizado mínima a la que el mod puede bajar (chunks).
	 * 4 es el mínimo "jugable" razonable; menos que eso casi no se ve nada.
	 */
	public int minViewDistance = 4;

	/** Distancia de simulación mínima a la que el mod puede bajar (chunks). */
	public int minSimulationDistance = 4;

	/** Escalado mínimo de distancia de entidades (0.0 - 1.0). */
	public double minEntityDistanceScaling = 0.35;

	public static RamSaverConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				RamSaverConfig loaded = GSON.fromJson(reader, RamSaverConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException e) {
				RamSaverMod.LOGGER.warn("No se pudo leer ramsaver.json, se usarán valores por defecto", e);
			}
		}
		RamSaverConfig defaultConfig = new RamSaverConfig();
		defaultConfig.save();
		return defaultConfig;
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			RamSaverMod.LOGGER.warn("No se pudo guardar ramsaver.json", e);
		}
	}
}
