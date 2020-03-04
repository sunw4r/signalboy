package signalboy.audio;

public class ConvolutionFreq {

    private double[] mFreqSample;

    public ConvolutionFreq(float freq, int baud) {

        int duration = (int)((float)OutputAudioDevice.SAMPLE_RATE/(float)baud);
        mFreqSample = new double[duration];

        for (int i = 0; i < mFreqSample.length; i++) {

            mFreqSample[i] = generateSineWavefreq(freq, i);

        }

    }

    private double generateSineWavefreq(float frequencyOfSignal, float index) {

        float samplingInterval = (float) ((float)CaptureAudioDevice.SAMPLE_RATE / (float)frequencyOfSignal);
        float angle = ((2.0f * (float)Math.PI * (float)index / samplingInterval));

        double result = (Math.sin(angle) * 1d);

        return result;

    }

    public double convolutionSum(double[] signal, int signalIndex, double median) {

        double sum = 0;

        for (int ii = 0; ii < mFreqSample.length; ii++) {

            int circularIndex = signalIndex+ii;
            if (circularIndex > signal.length-1) {
                circularIndex -= signal.length;
            }
            sum += (signal[circularIndex] - median) * mFreqSample[ii];

        }

        return sum;

    }

}
