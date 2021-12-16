package io.github.amerebagatelle.speaktome.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.text2speech.Narrator;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class SpeakToMeClient implements ClientModInitializer {
    public static SpeechRecognitionThread speechRecognitionThread;

    @Override
    public void onInitializeClient() {
        // Disable sphinx4 logger
        Logger cmRootLogger = Logger.getLogger("default.config");
        cmRootLogger.setLevel(java.util.logging.Level.OFF);
        String conFile = System.getProperty("java.util.logging.config.file");
        if (conFile == null) {
            System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
        }

        SpeechHandler.loadCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> SpeechHandler.tick());

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("speakToMeReload").executes(context -> {
            SpeechHandler.loadCommands();
            speechRecognitionThread.setRecognizer(rebuildRecognizer());
            return 0;
        }));

        LiveSpeechRecognizer recognizer = rebuildRecognizer();

        speechRecognitionThread = new SpeechRecognitionThread(recognizer);
        speechRecognitionThread.start();

        ClientPlayConnectionEvents.JOIN.register((networkHandler, packetSender, client) -> speechRecognitionThread.active = true);

        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, client) -> speechRecognitionThread.active = false);
    }

    public LiveSpeechRecognizer rebuildRecognizer() {
        try {
            Configuration configuration = new Configuration();

            configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
            configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
            configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

            return new LiveSpeechRecognizer(configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
