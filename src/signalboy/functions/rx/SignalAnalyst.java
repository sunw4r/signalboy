package signalboy.functions.rx;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import org.jtransforms.fft.DoubleFFT_1D;
import signalboy.audio.AudioDisplay;
import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.WindowFunction;
import signalboy.custom.ResizableCanvas;

public class SignalAnalyst extends RX {

    @FXML ResizableCanvas canvasBitmap;
    @FXML ToggleButton toggleFFT;

    private AudioDisplay mAudioFFT;

    @Override
    public void initialize() {

        mAudioFFT = new AudioDisplay(canvasBitmap);

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] data) {

        int N = data.length;

        //Normalizing Buffer
        double[] normalBuffer = new double[data.length];
        for (int i = 0; i < data.length; i++) {

            normalBuffer[i] = (double)data[i]/(double)Short.MAX_VALUE;
            normalBuffer[i] *= WindowFunction.hamming(i, data.length);

        }

        //Applying FFT
        double[] fftBuffer = new double[normalBuffer.length * 2];
        for (int i = 0; i < normalBuffer.length; i++) {
            fftBuffer[2*i] = normalBuffer[i];
            fftBuffer[2*i+1] = 0;
        }
        //System.arraycopy(normalBuffer, 0, fftBuffer, 0, normalBuffer.length);

        DoubleFFT_1D fft = new DoubleFFT_1D(normalBuffer.length);
        fft.complexForward(fftBuffer);

        double[] magnitudeBuffer = new double[N/2];
        for(int i = 0; i < N/2; i++) {

            //Calculate the magnitude
            double real = fftBuffer[2*i];
            double imag = fftBuffer[2*i+1];
            double mag = Math.sqrt(real*real + imag*imag);
            magnitudeBuffer[i] = mag;

        }

        double[] magUtil = new double[magnitudeBuffer.length];
        System.arraycopy(magnitudeBuffer, 0, magUtil, 0, magUtil.length);

        double maxMagIndex = -1;
        double maxMag = 0.0f;
        for (int i = 0; i < magUtil.length; i++) {
            if (magUtil[i] > maxMag) {
                maxMag = magUtil[i];
                maxMagIndex = i;
            }
        }

        for (int i = 0; i < magUtil.length; i++) {
            if (maxMag > 0) {
                magUtil[i] /= maxMag;
            }
        }

        //Print main frequency
        System.out.println(maxMagIndex * (double)CaptureAudioDevice.SAMPLE_RATE / (double)CaptureAudioDevice.BUFFER_SIZE);

        //Draw FFT
        mAudioFFT.setData(magUtil);

    }


}
