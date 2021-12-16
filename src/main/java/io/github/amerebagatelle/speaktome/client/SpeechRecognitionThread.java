package io.github.amerebagatelle.speaktome.client;

import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.Result;
import net.minecraft.client.MinecraftClient;

public class SpeechRecognitionThread extends Thread {
    private LiveSpeechRecognizer recognizer;

    public SpeechRecognitionThread(LiveSpeechRecognizer recognizer) {
        this.recognizer = recognizer;
        this.recognizer.startRecognition(true);
    }

    public void setRecognizer(LiveSpeechRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    @Override
    public void run() {
        while(true) {
            SpeechResult speechResult = recognizer.getResult();
            if (speechResult != null) {
                SpeechHandler.addCommandToProcess(speechResult);
            }
        }
    }
}
