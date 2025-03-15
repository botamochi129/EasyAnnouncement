package com.botamochi.easyannouncement.registry;

import com.botamochi.easyannouncement.Easyannouncement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EASounds {
    private static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();

    public static void register() {
        try {
            // リソースパック内の sounds.json を読み込む
            InputStream stream = EASounds.class.getClassLoader().getResourceAsStream("assets/" + Easyannouncement.MOD_ID + "/sounds.json");
            if (stream == null) {
                System.err.println("Failed to load sounds.json");
                return;
            }
            String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
            JsonObject soundJson = JsonParser.parseString(json).getAsJsonObject();

            for (String key : soundJson.keySet()) {
                Identifier id = new Identifier(Easyannouncement.MOD_ID, key);
                SoundEvent soundEvent = new SoundEvent(id);
                SOUND_EVENTS.put(key, soundEvent);

                // SoundEvent を登録
                Registry.register(Registry.SOUND_EVENT, id, soundEvent);
                System.out.println("Registered sound: " + id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SoundEvent getSound(String name) {
        return SOUND_EVENTS.getOrDefault(name, null);
    }
}