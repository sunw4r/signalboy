package signalboy.functions.tx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import signalboy.audio.Interpolate;
import signalboy.audio.OutputAudioDevice;
import signalboy.custom.ResizableCanvas;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

public class APTMod extends TX {

    @FXML Button buttonPickImage;
    @FXML Button buttonModulate;
    @FXML ResizableCanvas canvasBitmap;
    @FXML CheckBox checkBoxSyncTrain;

    static final int SAMPLE_RATE = 20800; //samples per second
    static final int SAMPLE_SIZE_BITS = 16;
    static final int CHANNELS = 1;
    static final boolean BIT_SIGNED = true;
    static final boolean BIT_BIG_ENDIAN = false;

    AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS,
            CHANNELS, BIT_SIGNED, BIT_BIG_ENDIAN);

    private static final int CARRIER_FREQUENCY = 2400;
    private static final int APT_BAUD = 4160;
    private final int SYNC_WORDS = 39;

    private double[] syncTrain;

    private byte[] dataToModulate;

    @Override
    public void initialize() {

        int pulseDuration = (int)((SYNC_WORDS/14)+0.5);
        syncTrain = new double[pulseDuration*14];

        int syncPhase = -1;
        for (int i = 0; i < syncTrain.length; i+= pulseDuration) {

            int durationCorrection = pulseDuration;
            /*if (syncPhase < 0) {
                durationCorrection +=1;
            } else {
                durationCorrection -= 1;
            }*/

            for (int ii = 0; ii < durationCorrection; ii++) {

                if (i + ii < syncTrain.length) {
                    syncTrain[i + ii] = 1 * syncPhase;
                }

            }

            syncPhase *= -1;

        }

        buttonPickImage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Image");
                File file = fileChooser.showOpenDialog(buttonPickImage.getScene().getWindow());
                if (file != null) {
                    openImageFile(file);
                }

            }
        });

        buttonModulate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                startModulation();

            }
        });

    }

    private void startModulation() {

        if (dataToModulate != null) {

            int pixelsRate = (int)(((double)SAMPLE_RATE/(double)APT_BAUD));
            //create buffer data of proper size
            short[] bufferData = new short[dataToModulate.length*pixelsRate];

            //Create interpolated signal
            double[] signal = new double[bufferData.length];
            for (int i = 0; i < dataToModulate.length; i++) {

                double amplitude = (double)((((dataToModulate[i])&0xFF)/255.0f)*2.0f)-1.0f;
                if (i+1 < dataToModulate.length) {
                    double nextAmplitude = (double)((((dataToModulate[i+1])&0xFF)/255.0f)*2.0f)-1.0f;
                    for (int ii = 0; ii < pixelsRate; ii++) {
                        signal[i * pixelsRate + ii] = Interpolate.linear(amplitude, nextAmplitude, (double)ii/(double)pixelsRate);
                    }

                } else {

                    for (int ii = 0; ii < pixelsRate; ii++) {
                        signal[i * pixelsRate + ii] = amplitude;
                    }

                }

            }

            for (int i = 0; i < bufferData.length; i++) {
                double carrier = (generateCosineWaveFreq(CARRIER_FREQUENCY, i));
                bufferData[i] = (short)(carrier*(0.5d+0.5d*signal[i])*(double) Short.MAX_VALUE);
            }

            playBuffer(bufferData, audioFormat);

        }

    }

    private void openImageFile(File file) {

        try {

            Image picture = ImageIO.read(file);
            byte[] imgdata = get2080BufferedImage(picture);
            GraphicsContext gc = canvasBitmap.getGraphicsContext2D();
            PixelWriter pixelWriter = canvasBitmap.getGraphicsContext2D().getPixelWriter();
            canvasBitmap.setWidth(2080);
            if (checkBoxSyncTrain.isSelected()) {
                canvasBitmap.setHeight(imgdata.length / (2080 - syncTrain.length));
            } else {
                canvasBitmap.setHeight(imgdata.length / 2080);
            }

            //combine data with sync
            int x = 0;
            int y = 0;
            if (checkBoxSyncTrain.isSelected()) {

                dataToModulate = new byte[imgdata.length + (int) (syncTrain.length * canvasBitmap.getHeight())];
                for (int i = 0; i < dataToModulate.length; i++) {

                    if (x < syncTrain.length) {

                        dataToModulate[2080 * y + x] = (byte) syncTrain[x];

                    } else {

                        dataToModulate[2080 * y + x] = imgdata[i - syncTrain.length * (y + 1)];

                    }

                    x++;
                    if (x >= 2080) {
                        x = 0;
                        y++;
                    }

                }

            } else {

                dataToModulate = new byte[imgdata.length];
                for (int i = 0; i < dataToModulate.length; i++) {

                    dataToModulate[2080 * y + x] = imgdata[2080 * y + x];

                    x++;
                    if (x >= 2080) {
                        x = 0;
                        y++;
                    }

                }
            }

            //draw image
            x = 0;
            y = 0;
            for (int i = 0; i < dataToModulate.length; i++) {

                int modulationByte = ((dataToModulate[i]) & 0xFF);
                if (modulationByte < 0) {
                    modulationByte = 0;
                }
                float r = (float) modulationByte / 255.0f;
                pixelWriter.setColor(x, y, new Color(r, r, r, 1.0));

                x++;
                if (x >= 2080) {
                    x =0;
                    y++;
                }
            }

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    @Override
    public void onStop() {

    }

    private double generateCosineWaveFreq(double frequencyOfSignal, int index) {

        double samplingInterval = SAMPLE_RATE / frequencyOfSignal;
        double angle = 2.0f * Math.PI * index / samplingInterval;

        double result = Math.cos(angle);

        return result;

    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public byte[] get2080BufferedImage(Image img)
    {
        int aptWidth = 2080;
        if (checkBoxSyncTrain.isSelected()) {
            aptWidth = 2080-syncTrain.length;
        }
        int aptHeight = (int)((double)img.getHeight(null)*(double)2080/(double)img.getWidth(null));

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(aptWidth, aptHeight, BufferedImage.TYPE_BYTE_GRAY);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        bGr.drawImage(img, 0, 0, aptWidth, aptHeight, 0, 0, img.getWidth(null), img.getHeight(null), null);
        bGr.dispose();

        // Return the buffered image
        return ((DataBufferByte) bimage.getRaster().getDataBuffer()).getData();
    }

}
