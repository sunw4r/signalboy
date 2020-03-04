package signalboy.audio;

public class Goertzel {

    private int mFrequency;
    private int mSampleRate;

    private double mNorm;
    private double mCoeff;

    private double Q1;
    private double Q2;

    public Goertzel(int frequency, int sampleRate) {

        mFrequency = frequency;
        mSampleRate = sampleRate;

    }

    public void init() {

        mNorm = (double)mFrequency/(double)mSampleRate;
        mCoeff = 2.0f * (double)Math.cos(2.0f * Math.PI * mNorm);

        reset();

    }

    public void processSample(double sample) {

        double Q0 = sample + mCoeff * Q1 - Q2;
        Q2 = Q1;
        Q1 = Q0;

    }

    public double getMagnitude() {

        return Q2 * Q2 + Q1 * Q1 - mCoeff * Q1 * Q2;

    }

    public double getSqrtMagnitude() {

        return Math.sqrt(getMagnitude());

    }

    public void reset() {

        Q1 = 0;
        Q2 = 0;

    }

}
