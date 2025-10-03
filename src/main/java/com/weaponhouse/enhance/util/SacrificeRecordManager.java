package com.weaponhouse.enhance.util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.weaponhouse.enhance.Enhance;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class SacrificeRecordManager {
    private static final Path RECORD_DIR = FMLPaths.CONFIGDIR.get().resolve("enhance");
    private static final Path RECORD_FILE = RECORD_DIR.resolve("sacrificed_records.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RECORD_TYPE = new TypeToken<ConcurrentHashMap<String, Set<String>>>(){}.getType();
    private static final ConcurrentHashMap<String, Set<String>> RECORD_CACHE = new ConcurrentHashMap<>();
    static {
        loadRecords();
    }
    private static void loadRecords() {
        try {
            if (!Files.exists(RECORD_FILE)) {
                Files.createDirectories(RECORD_DIR);
                Files.write(RECORD_FILE, "{}".getBytes());
            }
            String json = new String(Files.readAllBytes(RECORD_FILE));
            ConcurrentHashMap<String, Set<String>> records = GSON.fromJson(json, RECORD_TYPE);
            if (records != null) {
                RECORD_CACHE.putAll(records);
            }
        } catch (IOException e) {}
    }

    private static void saveRecords() {
        try {
            Files.write(RECORD_FILE, GSON.toJson(RECORD_CACHE).getBytes());
        } catch (IOException e) {}
    }
    public static boolean isSacrificed(UUID playerId, String buffId) {
        String key = playerId.toString();
        return RECORD_CACHE.containsKey(key) && RECORD_CACHE.get(key).contains(buffId);
    }
    public static void addSacrificed(UUID playerId, String buffId) {
        String key = playerId.toString();
        RECORD_CACHE.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(buffId);
        saveRecords();
    }
}