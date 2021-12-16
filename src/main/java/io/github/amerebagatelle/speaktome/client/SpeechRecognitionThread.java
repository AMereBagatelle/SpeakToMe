package io.github.amerebagatelle.speaktome.client;

import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.Result;
import net.minecraft.client.MinecraftClient;

public class SpeechRecognitionThread extends Thread {
    private LiveSpeechRecognizer recognizer;

    public boolean active = false;

    public SpeechRecognitionThread(LiveSpeechRecognizer recognizer) {
        this.recognizer = recognizer;
        this.recognizer.startRecognition(true);
    }

    public void setRecognizer(LiveSpeechRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while(true) {
            if(active) {
                SpeechResult speechResult = recognizer.getResult();
                if (speechResult != null) {
                    SpeechHandler.addCommandToProcess(speechResult);
                }
            }
        }
    }
}
