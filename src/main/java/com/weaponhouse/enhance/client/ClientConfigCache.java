package com.weaponhouse.enhance.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLLoader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
public class ClientConfigCache {
    private static boolean bossbarEnabled = true;
    private static boolean blindMode = false;
    private static final Path CONFIG_PATH = FMLLoader.getGamePath()
            .resolve("config")
            .resolve("enhance")
            .resolve("settings.json");
    static {
        loadConfigFromFile();
    }
    private static void loadConfigFromFile() {
        File configFile = CONFIG_PATH.toFile();
        if (!configFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new GsonBuilder().create();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> config = gson.fromJson(reader, type);
            if (config.containsKey("enhance_bossbar")) {
                bossbarEnabled = (boolean) config.get("enhance_bossbar");
            }
            if (config.containsKey("blind_mode")) {
                blindMode = (boolean) config.get("blind_mode");
            }
        } catch (IOException e) {
            bossbarEnabled = true;
            blindMode = false;
        }
    }
    public static void setBossbarEnabled(boolean enabled) {
        bossbarEnabled = enabled;
    }
    public static boolean isBossbarEnabled() {
        return bossbarEnabled;
    }
    public static boolean isBlindModeEnabled() {
        return blindMode;
    }
    public static void setBlindMode(boolean enabled) {
        blindMode = enabled;
    }
}
