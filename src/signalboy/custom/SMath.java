package signalboy.custom;

public class SMath {

    public static double clamp(double max, double min, double value) {

        if (value > max)
            return  max;

        if (value < min)
            return  min;

        return value;

    }

}
