package signalboy.functions.tx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import signalboy.audio.OutputAudioDevice;

import javax.sound.sampled.AudioFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AsciiFSKMod extends TX {

    @FXML Button buttonSend;
    @FXML TextField textFieldMark;
    @FXML TextField textFieldSpace;
    @FXML TextField textFieldBaud;
    @FXML TextArea textAreaMessage;
    @FXML TextArea textAreaBinary;

    static int SAMPLE_RATE = 24000; //samples per second
    static final int SAMPLE_SIZE_BITS = 16;
    static final int CHANNELS = 1;
    static final boolean BIT_SIGNED = true;
    static final boolean BIT_BIG_ENDIAN = false;

    AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS,
            CHANNELS, BIT_SIGNED, BIT_BIG_ENDIAN);

    private double volume = 0.5f;
    private String preamble = "01010101";
    private short msgSize = 0;
    private String startSequence = ":";
    private ArrayList<Short> shortSequence = new ArrayList<Short>();
    private int timeIndex = 0;
    private float oldFrequency;
    private float lerpSteps = 1f / 4f;
    private float lerpCurrentStep;

    @Override
    public void initialize() {

        buttonSend.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                int frequencyMark = Integer.parseInt(textFieldMark.getCharacters().toString());
                int frequencySpace = Integer.parseInt(textFieldSpace.getCharacters().toString());
                int baud = Integer.parseInt(textFieldBaud.getCharacters().toString());
                String message = textAreaMessage.getText();
                parseMessage(baud, frequencyMark, frequencySpace, message);

                SAMPLE_RATE = baud * 40;

                audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS,
                        CHANNELS, BIT_SIGNED, BIT_BIG_ENDIAN);

            }
        });

    }

    private void parseMessage(int baud, int frequencyMark, int frequencySpace, String message) {
        if (message.length() <= 0) {
            return;
        }

        shortSequence.clear();
        int size = message.trim().length();
        if (size >= 1000) {
            msgSize = 1000;
            message = message.trim().substring(0, 1000);
        } else {
            msgSize = (short)size;
        }

        byte[] stringBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = startSequence.getBytes();
        StringBuilder binary = new StringBuilder();
        binary.append(preamble);
        for (byte b : headerBytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
            binary.append(' ');
        }
        System.out.println("Msg size:"+msgSize);
        binary.append(String.format("%016d", Integer.parseInt(Integer.toBinaryString(msgSize))));
        for (byte b : stringBytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
            binary.append(' ');
        }
        String binaryComplete = binary.toString();
        binaryComplete = binaryComplete.replace(" ", "");

        textAreaBinary.setText(binaryComplete);

        for (int i = 0; i < binaryComplete.length(); i++) {

            short[] toneBuffer = new short[1];

            if (binaryComplete.charAt(i) == '0') {

                toneBuffer = emitFrequencyTone(baud, frequencySpace);

            } else if (binaryComplete.charAt(i) == '1') {

                toneBuffer = emitFrequencyTone(baud, frequencyMark);

            }

            for (int j = 0; j < toneBuffer.length; j++) {
                shortSequence.add(toneBuffer[j]);
            }

        }

        short[] bufferToPlay = new short[shortSequence.size()];
        for (int i = 0; i < shortSequence.size(); i++) {
            bufferToPlay[i] = shortSequence.get(i);
        }

        playBuffer(bufferToPlay, audioFormat);

    }

    private short[] emitFrequencyTone(int baud, int frequency) {

        int duration = (int)((float)SAMPLE_RATE/(float)baud);
        short[] buffer = new short[duration];

        for (int i = 0; i < buffer.length; i++) {

            buffer[i] = generate16bitSineWavefreq(frequency, i);

        }

        return buffer;

    }

    @Override
    public void onStop() {



    }

    private short generate16bitSineWavefreq(float frequencyOfSignal, float index) {

        oldFrequency += 0.15f*(frequencyOfSignal-oldFrequency);

        float samplingInterval = (float) ((float)SAMPLE_RATE / (float)oldFrequency);
        float angle = ((2.0f * (float)Math.PI * (float)index / samplingInterval));

        short result = (short)(Math.sin(angle) * Short.MAX_VALUE * volume);

        //System.out.println(result);

        return result;

    }

}
