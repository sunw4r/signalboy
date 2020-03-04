package signalboy.audio;

import org.jtransforms.fft.DoubleFFT_1D;

//Fast Hilbert Transform, mainly for APT
public class DoubleFHT_1D {

    public void realForwardFull(double[] a) {

        int N = a.length;
        double[] H = new double[N];

        DoubleFFT_1D fft = new DoubleFFT_1D(N/2);
        fft.realForwardFull(a);

        int NOver2 = (int) Math.floor(N / 2 + 0.5);
        int w;

        H[0] = 1.0;
        H[NOver2] = 1.0;

        for (w = 1; w <= NOver2 - 1; w++)
            H[w] = 2.0;

        for (w = NOver2 + 1; w <= N - 1; w++)
            H[w] = 0.0;

        for (w = 0; w < N; w++) {
            a[w] *= H[w];
        }

        fft.complexInverse(a, false);

    }

}
