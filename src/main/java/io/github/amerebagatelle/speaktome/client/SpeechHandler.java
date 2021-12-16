package io.github.amerebagatelle.speaktome.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.text2speech.Narrator;
import edu.cmu.sphinx.api.SpeechResult;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeechHandler {
    public static final Path SPEECH_RECOGNITION_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("speaktome.json");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final HashMap<Pattern, List<Runnable>> speechCommands = new HashMap<>();
    private static final ArrayList<SpeechResult> commandsToHandle = new ArrayList<>();

    public static void tick() {
        for (SpeechResult speechResult : commandsToHandle) {
            for(Pattern pattern : speechCommands.keySet()) {
                Matcher matcher = pattern.matcher(speechResult.getHypothesis());
                if(!matcher.find()) continue;

                for (Runnable runnable : speechCommands.get(pattern)) {
                    runnable.run();
                }
            }
        }
        commandsToHandle.clear();
    }

    public static void registerCommand(Pattern pattern, List<Runnable> runnables) {
        speechCommands.put(pattern, runnables);
    }

    public static void loadCommands() {
        speechCommands.clear();

        JsonObject commands;

        try {
            if (!Files.exists(SPEECH_RECOGNITION_CONFIG_PATH)) {
                Files.createFile(SPEECH_RECOGNITION_CONFIG_PATH);
                Files.write(SPEECH_RECOGNITION_CONFIG_PATH, "{}".getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file", e);
        }

        try(FileReader reader = new FileReader(SPEECH_RECOGNITION_CONFIG_PATH.toFile())) {
            commands = GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not read config file.", e);
        }

        for (Map.Entry<String, JsonElement> entry : commands.get("commands").getAsJsonObject().entrySet()) {
            registerCommand(Pattern.compile(entry.getKey()), getCommandsFor(entry.getValue().getAsJsonObject()));
        }
    }

    private static List<Runnable> getCommandsFor(JsonObject commands) {
        ArrayList<Runnable> runnables = new ArrayList<>();
        int repeatNumber = commands.get("repeat") != null ? commands.get("repeat").getAsInt() : 1;
        for (Map.Entry<String, JsonElement> entry : commands.entrySet()) {
            Runnable runnable = switch (entry.getKey()) {
                case "say" -> () -> Narrator.getNarrator().say(entry.getValue().getAsString(), false);
                case "command" -> () -> Objects.requireNonNull(MinecraftClient.getInstance().player).sendChatMessage("/" + entry.getValue().getAsString());
                case "message" -> () -> Objects.requireNonNull(MinecraftClient.getInstance().player).sendChatMessage(entry.getValue().getAsString());
                default -> null;
            };
            if(runnable != null) for (int i = 0; i < repeatNumber; i++) {
                runnables.add(runnable);
            }
        }
        return runnables;
    }

    public static void addCommandToProcess(SpeechResult speech) {
        commandsToHandle.add(speech);
    }
}
