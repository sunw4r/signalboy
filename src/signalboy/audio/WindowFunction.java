package signalboy.audio;

public class WindowFunction {

    public static double flatTop (int i, int N) {

        double  a0 = 1,
                a1 = 1.93,
                a2 = 1.29,
                a3 = 0.388,
                a4 = 0.028,
                f = 6.283185307179586*(double)i/((double)N-1);

        return a0 - a1*Math.cos(f) +a2*Math.cos(2*f) - a3*Math.cos(3*f) + a4 * Math.cos(4*f);

    }

    public static double hann (int i, int N) {

        return 0.5*(1 - Math.cos(6.283185307179586*(double)i/((double)N-1)));

    }

    public static double hamming (int i, int N) {
        return 0.54 - 0.46 * Math.cos(6.283185307179586*(double)i/((double)N-1));
    }

}
