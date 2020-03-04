package signalboy.audio;

public class ConvolutionPattern {

    private double[] mPattern;

    public ConvolutionPattern(double[] pattern) {

        mPattern = pattern;

    }

    public double convolutionSum(double[] signal, int signalIndex, double median) {

        double sum = 0;

        for (int ii = 0; ii < mPattern.length; ii++) {

            int circularIndex = signalIndex+ii;
            if (circularIndex > signal.length-1) {
                circularIndex -= signal.length;
            }
            sum += (signal[circularIndex] - median) * mPattern[ii];

        }

        return sum;

    }

}
