package signalboy;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import signalboy.audio.AudioDisplay;
import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.OutputAudioDevice;
import signalboy.audio.VolumeBar;
import signalboy.functions.rx.RX;
import signalboy.functions.tx.TX;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainController implements CaptureAudioDevice.CaptureListener, TX.TXDelegate {

    @FXML private MenuItem menuItemClose;
    @FXML private Menu menuNew;
    @FXML private ComboBox<String> comboAudioSources;
    @FXML private ComboBox<String> comboAudioOutput;
    @FXML private Canvas canvasVolume;
    @FXML private Canvas canvasFFT;
    @FXML private TabPane tabPaneDecoders;
    @FXML private Button buttonStop;

    private HashMap<String, CaptureAudioDevice> mCaptureDevices;
    private HashMap<String, OutputAudioDevice> mOutputDevices;
    private CaptureAudioDevice mCurrentDevice;
    private OutputAudioDevice mOutputDevice;
    private ArrayList<RX> mRunningRXs;
    private ArrayList<TX> mRunningTXs;
    private HashMap<String, String> mDecodersReference;
    private HashMap<String, String> mOutputsReference;
    private VolumeBar volumeBar;
    private AudioDisplay audioDisplay;

    public void initialize() {

        //Basic stuff
        mRunningRXs = new ArrayList<RX>();
        mRunningTXs = new ArrayList<TX>();
        volumeBar = new VolumeBar(canvasVolume);
        audioDisplay = new AudioDisplay(canvasFFT);

        menuItemClose.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                exitApplication(event);
            }
        });

        //Decoders
        mDecodersReference = new HashMap<String, String>();
        //mDecodersReference.put("RX Simple Tone Detector", "functions/rx/tonedetector.fxml");
        //mDecodersReference.put("RX APRS", "functions/rx/aprs.fxml");
       // mDecodersReference.put("RX Packet Radio 1200", "functions/rx/packetradio.fxml");
        mDecodersReference.put("RX APT (NOAA Sats)", "functions/rx/apt.fxml");
        mDecodersReference.put("RX ASCII FSK", "functions/rx/asciiFSKDemod.fxml");
        //mDecodersReference.put("RX APRS Packet Radio", "functions/rx/aprsAX25Demod.fxml");
        //mDecodersReference.put("RX Signal Analysis", "functions/rx/signalanalyst.fxml");

        for (Map.Entry<String, String> entry : mDecodersReference.entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            MenuItem decoderItem = new MenuItem();
            decoderItem.setText(key);
            decoderItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {

                    openNewDecoderTab(key, value);

                }
            });
            menuNew.getItems().add(decoderItem);

        }

        SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
        menuNew.getItems().add(separatorMenuItem);

        //Outputs
        mOutputsReference = new HashMap<String, String>();
        mOutputsReference.put("TX Simple Tone", "functions/tx/simpletone.fxml");
        mOutputsReference.put("TX ASCII FSK", "functions/tx/asciiFSK.fxml");
        mOutputsReference.put("TX APT Modulator (NOAA Sats)", "functions/tx/aptmod.fxml");

        for (Map.Entry<String, String> entry : mOutputsReference.entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            MenuItem outputItem = new MenuItem();
            outputItem.setText(key);
            outputItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {

                    openNewOutputTab(key, value);

                }
            });
            menuNew.getItems().add(outputItem);

        }

        //Audio sources and combobox
        mCaptureDevices = new HashMap<String, CaptureAudioDevice>();
        mOutputDevices = new HashMap<String, OutputAudioDevice>();
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; i++) {

            Mixer mixer = AudioSystem.getMixer(mixerInfo[i]);
            Line.Info[] targetLineInfo = mixer.getTargetLineInfo();
            if (targetLineInfo.length > 0) {

                Mixer.Info mInfo = mixerInfo[i];
                if (mInfo.getName().indexOf("Port") == -1) {
                    CaptureAudioDevice cDevice = new CaptureAudioDevice(mixer, this);
                    mCaptureDevices.put(mInfo.getName(), cDevice);
                    comboAudioSources.getItems().add(mInfo.getName());
                }

            }
            Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
            if (sourceLineInfo.length > 0) {

                Mixer.Info mInfo = mixerInfo[i];
                if (OutputAudioDevice.testIfWorks(mixer)) {

                    OutputAudioDevice oDevice = new OutputAudioDevice(mixer);
                    mOutputDevices.put(mInfo.getName(), oDevice);
                    comboAudioOutput.getItems().add(mInfo.getName());

                }

            }

        }

        comboAudioSources.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                String selected = comboAudioSources.getValue();
                if (selected != null && !selected.isEmpty()) {

                    System.out.println(selected);

                    if (mCurrentDevice != null) {
                        mCurrentDevice.stop();
                    }

                    CaptureAudioDevice device = mCaptureDevices.get(selected);
                    mCurrentDevice = device;
                    mCurrentDevice.startCap();

                }

            }
        });

        comboAudioOutput.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                String selected = comboAudioOutput.getValue();
                if (selected != null && !selected.isEmpty()) {

                    System.out.println(selected);

                    OutputAudioDevice device = mOutputDevices.get(selected);
                    device.start();
                    mOutputDevice = device;

                    if (mOutputDevice != null) {
                        mOutputDevice.stop();
                    }

                }

            }
        });

        buttonStop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mOutputDevice != null) {
                    mOutputDevice.stopCurrentBuffer();
                }
            }
        });

    }

    private void openNewOutputTab(String outputName, String outputReference) {

        try {

            FXMLLoader loader = new FXMLLoader(MainController.this.getClass().getResource(outputReference));
            Pane pane = loader.load();
            TX TX = (TX) loader.getController();

            Tab outputTab = new Tab();
            outputTab.setText(outputName);
            outputTab.setContent(pane);
            outputTab.setClosable(true);
            outputTab.setOnClosed(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    TX.onStop();
                    mRunningTXs.remove(TX);
                }
            });

            tabPaneDecoders.getTabs().add(outputTab);
            mRunningTXs.add(TX);
            TX.registerDelegate(this);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    private void openNewDecoderTab(String decoderName, String decoderReference) {

        try {

            FXMLLoader loader = new FXMLLoader(MainController.this.getClass().getResource(decoderReference));
            Pane pane = loader.load();
            RX RX = (RX) loader.getController();

            Tab decoderTab = new Tab();
            decoderTab.setText(decoderName);
            decoderTab.setContent(pane);
            decoderTab.setClosable(true);
            decoderTab.setOnClosed(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    RX.onStop();
                    mRunningRXs.remove(RX);
                }
            });

            tabPaneDecoders.getTabs().add(decoderTab);
            mRunningRXs.add(RX);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    private void processAudioDisplay(short[] buffer) {
        double[] displayHalf = new double[buffer.length];
        if ( buffer.length > 1024) {
            displayHalf = new double[1024];
        }
        for (int i = 0; i < displayHalf.length; i++) {
            displayHalf[i] = (double)buffer[i]/(double)Short.MAX_VALUE;
        }
        audioDisplay.setData(displayHalf);

    }

    private void processVolume(short[] buffer) {

        int max = 0;
        for (int i = 0; i < buffer.length; i++) {
            if (Math.abs(buffer[i]) > max) {
                max = Math.abs(buffer[i]);
            }
        }
        float currentVolume = (float)max/(float)Short.MAX_VALUE;
        volumeBar.setVolume(currentVolume);

    }

    //Capture Audio Listener
    @Override
    public void onBuffer(short[] buffer) {

        processVolume(buffer);
        processAudioDisplay(buffer);

        for(RX RX : mRunningRXs) {

            RX.setBuffer(buffer);

        }

    }

    //TX delegate
    @Override
    public void playBuffer(short[] buffer) {

        if (mOutputDevice != null) {
            mOutputDevice.playBuffer(buffer);
        }

    }

    @Override
    public void playBuffer(short[] buffer, AudioFormat format) {

        if (mOutputDevice != null) {
            mOutputDevice.playBuffer(buffer, format);
        }

    }

    @Override
    public void stopCurrentBuffer() {

        if (mOutputDevice != null) {
            mOutputDevice.stopCurrentBuffer();
        }

    }

    //Override exit listener
    @FXML
    public void exitApplication(ActionEvent event) {
        if (mCurrentDevice != null) {
            mCurrentDevice.stop();
        }
        Platform.exit();
        System.exit(0);
    }
}
