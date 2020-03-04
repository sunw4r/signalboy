package signalboy.audio;

import java.util.ArrayList;

public class Interpolate {

    public static double linear(double a, double b, double f) {

        return (a * (1.0 - f)) + (b * f);

    }

    public static double cosine(double a, double b, double f) {

        double mu2;

        mu2 = (1-Math.cos(f*Math.PI))/2;
        return(a*(1-mu2)+b*mu2);

    }

}