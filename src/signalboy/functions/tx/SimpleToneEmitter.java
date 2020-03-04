package signalboy.functions.tx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import signalboy.audio.OutputAudioDevice;

public class SimpleToneEmitter extends TX {

    @FXML Button buttonPlay;
    @FXML TextField textFieldFrequency;
    @FXML TextField textFieldDuration;

    private double volume = 0.3f;

    @Override
    public void initialize() {

        buttonPlay.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                int frequency = Integer.parseInt(textFieldFrequency.getCharacters().toString());
                int durationSeconds = Integer.parseInt(textFieldDuration.getCharacters().toString());
                emitFrequencyTone(durationSeconds, frequency);

            }
        });

    }

    private boolean testShiftKey = false;

    private void emitFrequencyTone(int durationSeconds, int frequency) {

        int duration = OutputAudioDevice.SAMPLE_RATE * durationSeconds; //seconds
        short[] buffer = new short[duration];

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = generate16bitSineWavefreq(frequency, i);

        }

        playBuffer(buffer);

    }

    @Override
    public void onStop() {



    }

    private short generate16bitSineWavefreq(float frequencyOfSignal, float index) {

        float samplingInterval = (float) ((float)OutputAudioDevice.SAMPLE_RATE / (float)frequencyOfSignal);
        float angle = ((2.0f * (float)Math.PI * (float)index / samplingInterval));

        short result = (short)(Math.sin(angle) * Short.MAX_VALUE * volume);

        return result;

    }

}
