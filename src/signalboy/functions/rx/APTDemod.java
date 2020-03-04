package signalboy.functions.rx;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.DoubleFHT_1D;
import signalboy.custom.ImgUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class APTDemod extends RX {

    @FXML TabPane tabPane;
    @FXML Canvas canvasBitmap;
    @FXML Canvas canvasProcess;
    @FXML Button processButton;
    @FXML Button saveRawImageButton;
    @FXML Button saveColoredImageButton;

    private final int APT_PIXELS_PER_LINE = 2080;
    private final int APT_BAUDS = 4160; //2 lines of 2080 pixels per second.
    private final int SYNC_WORDS = 39;
    private final int INIT_CHANNEL_A = 85;
    private final int END_CHANNEL_A = 992;
    private final int CHANNEL_SIZE = END_CHANNEL_A - INIT_CHANNEL_A;
    private final int INIT_CHANNEL_B = 1124;
    private final int END_CHANNEL_B = INIT_CHANNEL_B + (CHANNEL_SIZE);
    private final int CHANNEL_INTERVAL = INIT_CHANNEL_B-INIT_CHANNEL_A;

    private double aptPixelRate = ((double) CaptureAudioDevice.SAMPLE_RATE / (double)APT_BAUDS);
    private PixelWriter mPixelWriter;
    private PixelReader mPixelReader;
    private int currentX = 0;
    private int currentY = 0;
    private WritableImage currentImage;
    private boolean captureStarted;

    private double[] syncTrain;
    private double[] syncTrainInterp;
    private int lastSyncIndex = 0;

    private double[] fullSecondBuffer;
    private int fullSecondBufferIndex = 0;
    private int fullSecondBufferIndexMax;

    double contrastRatio = 0.01d;
    private ArrayList<Double> magnitudeList = new ArrayList<Double>();
    private BufferedImage lutFalseColor;

    public static float[] NEW_VIS_FALSE_CURVE = {
            0.000000f, 0.008996f, 0.017990f, 0.026980f, 0.035964f, 0.044941f, 0.053908f, 0.062864f, 0.071806f, 0.080733f, 0.089642f, 0.098533f, 0.107402f, 0.116248f, 0.125069f, 0.133864f,
            0.142629f, 0.151364f, 0.160067f, 0.168734f, 0.177366f, 0.185958f, 0.194511f, 0.203021f, 0.211488f, 0.219908f, 0.228280f, 0.236602f, 0.244873f, 0.253090f, 0.261251f, 0.269354f,
            0.277398f, 0.285381f, 0.293300f, 0.301154f, 0.308941f, 0.316659f, 0.324306f, 0.331880f, 0.339379f, 0.346801f, 0.354145f, 0.361408f, 0.368588f, 0.375684f, 0.382694f, 0.389615f,
            0.396446f, 0.403185f, 0.409830f, 0.416380f, 0.422831f, 0.429182f, 0.435432f, 0.441578f, 0.448669f, 0.454624f, 0.460553f, 0.466455f, 0.472329f, 0.478176f, 0.483995f, 0.489786f,
            0.495549f, 0.501282f, 0.506986f, 0.512661f, 0.518306f, 0.523920f, 0.529504f, 0.535057f, 0.540579f, 0.546069f, 0.551527f, 0.556952f, 0.562345f, 0.567705f, 0.573032f, 0.578325f,
            0.583584f, 0.588809f, 0.593999f, 0.599153f, 0.604273f, 0.609357f, 0.614404f, 0.619416f, 0.624390f, 0.629328f, 0.634228f, 0.639090f, 0.643914f, 0.648699f, 0.653446f, 0.658154f,
            0.662822f, 0.667450f, 0.672038f, 0.676586f, 0.681093f, 0.685558f, 0.689982f, 0.694364f, 0.698704f, 0.703002f, 0.707256f, 0.711467f, 0.715635f, 0.719758f, 0.723838f, 0.727873f,
            0.731862f, 0.735807f, 0.739706f, 0.743559f, 0.747365f, 0.751125f, 0.754838f, 0.758503f, 0.762121f, 0.765691f, 0.769212f, 0.772685f, 0.776109f, 0.779483f, 0.782808f, 0.786082f,
            0.789306f, 0.792479f, 0.795601f, 0.798672f, 0.801691f, 0.804657f, 0.807572f, 0.810433f, 0.813688f, 0.816446f, 0.819174f, 0.821871f, 0.824539f, 0.827178f, 0.829788f, 0.832368f,
            0.834921f, 0.837444f, 0.839940f, 0.842408f, 0.844848f, 0.847261f, 0.849647f, 0.852006f, 0.854338f, 0.856644f, 0.858924f, 0.861178f, 0.863407f, 0.865610f, 0.867789f, 0.869942f,
            0.872071f, 0.874176f, 0.876257f, 0.878314f, 0.880348f, 0.882358f, 0.884345f, 0.886310f, 0.888252f, 0.890172f, 0.892070f, 0.893947f, 0.895801f, 0.897635f, 0.899448f, 0.901240f,
            0.903012f, 0.904763f, 0.906495f, 0.908207f, 0.909899f, 0.911573f, 0.913227f, 0.914863f, 0.916481f, 0.918080f, 0.919662f, 0.921226f, 0.922773f, 0.924302f, 0.925815f, 0.927311f,
            0.928791f, 0.930255f, 0.931703f, 0.933135f, 0.934553f, 0.935955f, 0.937342f, 0.938715f, 0.940074f, 0.941418f, 0.942749f, 0.944066f, 0.945370f, 0.946662f, 0.947940f, 0.949206f,
            0.950459f, 0.951701f, 0.952931f, 0.954150f, 0.955357f, 0.956554f, 0.957739f, 0.958915f, 0.960080f, 0.961235f, 0.962381f, 0.963517f, 0.964644f, 0.965762f, 0.966872f, 0.967973f,
            0.969066f, 0.970152f, 0.971229f, 0.972300f, 0.973363f, 0.974419f, 0.975469f, 0.976512f, 0.977550f, 0.978581f, 0.979608f, 0.980628f, 0.981644f, 0.982655f, 0.983661f, 0.984664f,
            0.985662f, 0.986656f, 0.987647f, 0.988634f, 0.989619f, 0.990601f, 0.991580f, 0.992558f, 0.993533f, 0.994506f, 0.995478f, 0.996449f, 0.997419f, 0.998389f, 0.999357f, 1.000000f,
    };

    //FXML call
    public void initialize() {


        //lut reference for false color
        File imageFile = new File("resources/lut_falsecolor.png");
        try {
            lutFalseColor = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentImage = new WritableImage(2080, 2000);

        //mPixelWriter = canvasBitmap.getGraphicsContext2D().getPixelWriter();
        mPixelWriter = currentImage.getPixelWriter();
        mPixelReader = currentImage.getPixelReader();

        canvasBitmap.setWidth(2080);
        canvasBitmap.setHeight(2000);

        canvasProcess.setWidth(2080);
        canvasProcess.setHeight(2000);

        //canvasBitmap.setScaleX(0.5);
        //canvasBitmap.setScaleY(0.5);

        fullSecondBufferIndexMax = (CaptureAudioDevice.SAMPLE_RATE / CaptureAudioDevice.BUFFER_SIZE) / 2;
        fullSecondBuffer = new double[CaptureAudioDevice.SAMPLE_RATE / 2];

        int syncBlackBorderSize = 1;

        int originalPulseDuration = (int)((SYNC_WORDS/14)+0.5);
        syncTrain = new double[(originalPulseDuration*14)+syncBlackBorderSize];

        //Sync square waves
        int syncPhase = 1;
        for (int i = 0; i < syncTrain.length; i++) {

            if (i < syncBlackBorderSize) {

                syncTrain[i] = -1;

            } else {

                syncTrain[i] = 1 * syncPhase;

                if (i % originalPulseDuration == 0) {
                    syncPhase *= -1;
                }
            }
        }

        syncPhase = -1;
        syncTrainInterp = new double[(int)(syncTrain.length*aptPixelRate)];
        for (int i = 6; i < syncTrainInterp.length; i++) {

            if (i < syncBlackBorderSize*aptPixelRate) {

                syncTrain[i] = -1;

            } else {

                int durationCorrection = (int) (originalPulseDuration * aptPixelRate);


                syncTrainInterp[i] = 1 * syncPhase;

                if (i % durationCorrection == 0) {
                    syncPhase *= -1;
                }

            }
        }

        System.out.println(Arrays.toString(syncTrain));

        processButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                captureStarted = true;

                processButton.setText("Stop and Process");
                processButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {

                        captureStarted = false;
                        stopAndProcess();
                        saveRawImageButton.setDisable(false);

                    }
                });

            }
        });

        saveColoredImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter =
                        new FileChooser.ExtensionFilter("png files (*.png)", "*.png");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show save file dialog
                File file = fileChooser.showSaveDialog(saveRawImageButton.getScene().getWindow());
                if(file != null){
                    try {
                        WritableImage writableImage = new WritableImage((int) canvasProcess.getWidth(), (int) canvasProcess.getHeight());
                        canvasProcess.snapshot(null, writableImage);
                        RenderedImage renderedImage = getTrimmedImage(writableImage);
                        ImageIO.write(renderedImage, "png", file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });

        saveRawImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter =
                        new FileChooser.ExtensionFilter("png files (*.png)", "*.png");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show save file dialog
                File file = fileChooser.showSaveDialog(saveRawImageButton.getScene().getWindow());
                if(file != null){
                    try {
                        WritableImage writableImage = new WritableImage((int) canvasBitmap.getWidth(), (int) canvasBitmap.getHeight());
                        canvasBitmap.snapshot(null, writableImage);
                        RenderedImage renderedImage = getTrimmedImage(writableImage);
                        ImageIO.write(renderedImage, "png", file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] buffer) {

        if (!captureStarted) {
            return;
        }

        double[] doubleConvertedBuffer = convertFromShortArrayToDoubleArray(buffer);

        System.arraycopy(doubleConvertedBuffer, 0, fullSecondBuffer,
                fullSecondBufferIndex * CaptureAudioDevice.BUFFER_SIZE, doubleConvertedBuffer.length);

        if (fullSecondBufferIndex < fullSecondBufferIndexMax-1) {
            fullSecondBufferIndex++;
            return;
        } else {
            fullSecondBufferIndex = 0;
        }

        //fullSecondBuffer = realignWithSync(fullSecondBuffer);

        double[] magnitudeBuffer = new double[fullSecondBuffer.length];

        //Hilbert Transform (AM demodulation)
        DoubleFHT_1D fht1D = new DoubleFHT_1D();
        double[] afht = new double[fullSecondBuffer.length * 2];
        System.arraycopy(fullSecondBuffer, 0, afht, 0, fullSecondBuffer.length);
        fht1D.realForwardFull(afht);

        for(int i = 0 ; i < afht.length; i+=2) {

            //Calculate the magnitude
            double real = afht[i];
            double imag = afht[i+1];
            double mag = Math.sqrt(real*real + imag*imag);
            magnitudeBuffer[i/2] = mag;

        }

        //truncate the buffer to get 1 frame per pixel
        magnitudeToGrayscale(truncBuffer(magnitudeBuffer));

    }

    double syncThreshold = 30000;
    boolean syncOnThisLine = false;

    //Convolution to align with sync signal
    private double[] realignWithSync(double[] buffer) {

        double maxValue = 0;
        int highProbabilityIndex = 0;

        double median = 0;
        for (int ii = 0; ii < buffer.length; ii++) {
            median += buffer[ii];
        }
        median = median/buffer.length;

        for (int i = 0; i < buffer.length; i++) {

            double sum = 0;

            for (int ii = 0; ii < syncTrainInterp.length; ii++) {

                int circularIndex = i+ii;
                if (circularIndex > buffer.length-1) {
                    circularIndex -= buffer.length;
                }
                sum += (buffer[circularIndex] - median) * syncTrainInterp[ii];

            }

            if(sum > maxValue){

                maxValue = sum;
                highProbabilityIndex = i;

            }

        }

        if (maxValue > syncThreshold) {

            double[] rearranged = new double[buffer.length];
            for (int i = 0; i < rearranged.length; i++) {
                int rearrangedIndex = highProbabilityIndex+i;
                if (rearrangedIndex > buffer.length-1) {
                    rearrangedIndex -= buffer.length;
                }
                rearranged[i] = buffer[rearrangedIndex];
            }

            // lastSyncIndex = highProbabilityIndex;
            //System.out.printf("maxvalue: %f\n",maxValue);
            //System.out.println("highProbabilityIndex: "+highProbabilityIndex);
            syncOnThisLine = true;
            return  rearranged;

        } else {

            syncOnThisLine = false;
            return buffer;

        }

        //System.out.println("maxvalue:"+maxValue);

        /*if (maxValue < 1) {
            highProbabilityIndex = lastSyncIndex;
        }*/

    }

    double maxMag = 0.0d;
    double minMag = 1.0d;

    private void magnitudeToGrayscale(double[] truncMagData) {

        double[] digitized = new double[truncMagData.length];
        for(int i = 0; i < truncMagData.length; i++) {

            digitized[i] = Math.abs(truncMagData[i]);
            if (syncOnThisLine) {
                if (digitized[i] > maxMag) maxMag = digitized[i];
                if (digitized[i] < minMag) minMag = digitized[i];
            }

        }
        double delta = maxMag - minMag;
        for(int i = 0; i < digitized.length; i++) {
           //digitized[i] = digitized[i]/maxMag;
            digitized[i] = (digitized[i]-minMag)/(maxMag-minMag);
        }

       renderOnBitmap(digitized);

    }

    //Trunc to the correct sample rate and align with sync
    private double[] truncBuffer(double[] buffer) {

        buffer = realignWithSync(buffer);

        int truncSize = (int)((double)(buffer.length / aptPixelRate));
        double[] truncBuffer = new double[truncSize];
        for (int i = 0; i < truncBuffer.length; i++) {

                truncBuffer[i] = buffer[(int) ((double) i * aptPixelRate)];

                //Storing the magnitude data for later processing;
                if (syncOnThisLine) {
                    magnitudeList.add(truncBuffer[i]);
                }

        }

        return truncBuffer;

    }

    private void stopAndProcess() {

        maxMag = 0.0d;
        minMag = 1.0d;

        captureStarted = false;
        canvasProcess.getGraphicsContext2D().clearRect(0, 0, canvasBitmap.getWidth(), canvasBitmap.getHeight());
        processButton.setText("Start Capture");
        processButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                saveRawImageButton.setDisable(true);
                captureStarted = true;
                currentImage = new WritableImage(2080, 2000);
                mPixelWriter = currentImage.getPixelWriter();
                mPixelReader = currentImage.getPixelReader();
                magnitudeList.clear();
                canvasBitmap.getGraphicsContext2D().clearRect(0, 0, canvasBitmap.getWidth(), canvasBitmap.getHeight());
                currentX = 0;
                currentY = 0;

                processButton.setText("Stop and Process");
                processButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {

                        stopAndProcess();
                        saveRawImageButton.setDisable(false);

                    }
                });

            }
        });

        //Normalizing the magnitude values
        Double[] magnitudeData = new Double[magnitudeList.size()];
        magnitudeData = magnitudeList.toArray(magnitudeData);

        double maxMag = 0;
        double minMag = 1;

        for (int i = 0; i < magnitudeData.length; i++) {

            if (maxMag < magnitudeData[i]) {
                maxMag = magnitudeData[i];
            }

            if (minMag > magnitudeData[i]) {
                minMag = magnitudeData[i];
            }

        }

        for (int i = 0; i < magnitudeData.length; i++) {

            //normalize with max mag
            double delta = maxMag - minMag;
            magnitudeData[i] = (magnitudeData[i]-minMag)/delta;

            //Contrast test
            if (magnitudeData[i] > 0.5) {
                magnitudeData[i] += contrastRatio;
            } else {
                magnitudeData[i] -= contrastRatio;
            }

            if (magnitudeData[i] > 1d) { magnitudeData[i] = 1d; }
            if (magnitudeData[i] < 0d) { magnitudeData[i] = 0d; }

            //magnitudeData[i] = (double)NEW_VIS_FALSE_CURVE[(int)(magnitudeData[i]*255.0)];

            /*magnitudeData[i] = (((magnitudeData[i] - 0.5d) * contrast) + 0.5d);
            if (magnitudeData[i] > 1d) { magnitudeData[i] = 1d; }
            if (magnitudeData[i] < 0d) { magnitudeData[i] = 0d; }*/

            //TODO: apply contrast curve
            //double contrastCurve = NEW_VIS_FALSE_CURVE[(int)(magnitudeData[i]*255)];
            //magnitudeData[i] = contrastCurve;

        }

        /*WritableImage test = new WritableImage(2080, (magnitudeData.length/2080));
        PixelWriter testPW = test.getPixelWriter();
        for (int x = 0; x < test.getWidth(); x++) {

            for (int y = 0; y < test.getHeight(); y++) {

                double gray = SMath.clamp(1, 0, magnitudeData[(int)(x + y * test.getWidth())]);
                double[] rgb = {gray, gray, gray};

                Color pixel = new Color(rgb[0], rgb[1], rgb[2], 1.0d);
                testPW.setColor(x, y, pixel);

            }

        }

        canvasBitmap.getGraphicsContext2D().drawImage(test, 0 ,0);*/

        //Construct the false color image, using the two channels as coordinates for the lut
        int falseColorW = END_CHANNEL_B - INIT_CHANNEL_B;
        int falseColorH = (int)(magnitudeData.length/2080);
        if (falseColorH <= 0) {
            return;
        }
        System.out.println("w: "+falseColorW+" h:"+falseColorH);
        WritableImage falseColorImg = new WritableImage(falseColorW, falseColorH);

        for (int x = 0; x < falseColorImg.getWidth(); x++) {

            for (int y = 0; y < falseColorImg.getHeight(); y++) {

                double infraredGray = magnitudeData[y * 2080 + (x + INIT_CHANNEL_B)];
                double visibleGray = magnitudeData[y * 2080 + (x + INIT_CHANNEL_A)];

                double[] lutPixel = getLut((int)(infraredGray*255), (int)(visibleGray*255));
                Color pixel = new Color(lutPixel[0], lutPixel[1], lutPixel[2], 1.0d);
                falseColorImg.getPixelWriter().setColor(x, y, pixel);

            }

        }

        canvasProcess.getGraphicsContext2D().drawImage(falseColorImg, 0, 0);
        tabPane.getSelectionModel().select(1);

        saveColoredImageButton.setDisable(false);

    }

    private double[] getLut(int x, int y) {

        int clr = lutFalseColor.getRGB(x, y);
        int red = (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue = clr & 0x000000ff;

        double[] lutpixel = new double[3];
        lutpixel[0] = (double)red/255.0;
        lutpixel[1] = (double)green/255.0;
        lutpixel[2] = (double)blue/255.0;

        return lutpixel;

    }

    private void renderOnBitmap(double[] grayScale) {

        for (int i = 0; i < grayScale.length; i++) {

            if (currentX >= 0 && currentY >= 0) {

                double gray = grayScale[i];

                //Contrast test
                if (gray > 0.5) {
                    gray += contrastRatio;
                } else {
                    gray -= contrastRatio;
                }

                if (gray > 1d) { gray = 1d; }
                if (gray < 0d) { gray = 0d; }

               // double gray = NEW_VIS_FALSE_CURVE[(int)(grayScale[i]*255d)];

                /*gray = (((gray - 0.5d) * realTimeContrast) + 0.5d);
                if (gray > 1d) { gray = 1d; }
                if (gray < 0d) { gray = 0d; }*/

                double[] rgb = {gray, gray, gray};

               /* if (currentX > INIT_CHANNEL_A && currentX < END_CHANNEL_A) {

                    int contrastCurveIndex = (int)(gray * 255.0);
                    float contrast = NEW_VIS_FALSE_CURVE[contrastCurveIndex];

                    rgb[0] = rgb[0] * contrast;
                    rgb[1] = rgb[1] * contrast;
                    rgb[2] = rgb[2] * contrast;


                }

                if (currentX > INIT_CHANNEL_B && currentX < END_CHANNEL_B) {

                    Color chanColor = mPixelReader.getColor(currentX - CHANNEL_INTERVAL, currentY);
                    int lutX = (int)(gray * 255.0);
                    int lutY = (int)(chanColor.getRed()*255.0);

                    double[] lutRGB = getLut(lutX, lutY);
                    rgb[0] = lutRGB[0];
                    rgb[1] = lutRGB[1];
                    rgb[2] = lutRGB[2];

                }*/

                Color pixel = new Color(rgb[0], rgb[1], rgb[2], 1.0d);
                mPixelWriter.setColor(currentX, currentY, pixel);

                currentX++;
                if (currentX == 2080) {
                    currentY++;
                    currentX = 0;
                }

            }
        }

        canvasBitmap.getGraphicsContext2D().drawImage(currentImage, 0, 0);

    }

    private RenderedImage getTrimmedImage(WritableImage img) {

        BufferedImage image = SwingFXUtils.fromFXImage(img, null);
        Rectangle bounds = ImgUtils.getBounds(image, java.awt.Color.WHITE);
        BufferedImage trimmed = image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        return trimmed;

    }

    private double[] convertFromShortArrayToDoubleArray(short[] shortData) {
        int size = shortData.length;
        double[] doubleData = new double[size];
        for (int i = 0; i < size; i++) {
            doubleData[i] = (double)shortData[i] / (double)Short.MAX_VALUE;
        }
        return doubleData;
    }

}
