package com.ramsaver;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RamSaverMod implements ModInitializer {

	public static final String MOD_ID = "ramsaver";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RamSaverConfig CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = RamSaverConfig.load();
		LOGGER.info("RAM Saver inicializado. Umbral de presión de memoria: {}%", CONFIG.memoryPressureThresholdPercent);
	}
}
