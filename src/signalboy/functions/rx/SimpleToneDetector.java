package signalboy.functions.rx;

import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.Goertzel;

public class SimpleToneDetector extends RX {

    Goertzel goertzel;
    int N;

    @Override
    public void initialize() {

        N = CaptureAudioDevice.SAMPLE_RATE/1200;

        goertzel = new Goertzel(1200, CaptureAudioDevice.SAMPLE_RATE);
        goertzel.init();

        //goertzelOld = new GoertzelOld(CaptureAudioDevice.SAMPLE_RATE, 2200, N);
        //goertzelOld.initGoertzel();

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] data) {

        int samples = data.length/N;
        int detections = 0;

        for (int i = 0; i < data.length-N; i+= N) {

            double sum_sqr = 0;

            for (int ii = 0; ii < N; ii++) {

                goertzel.processSample((float)data[i+ii]);
                sum_sqr += (double)data[i+ii] * (double)data[i+ii];

            }

            float power = (float)sum_sqr / (float)N;
            float rms = (float) Math.sqrt(power);

            float magnitude = (float)Math.sqrt(goertzel.getMagnitude());
            float correlation = magnitude/rms;

            //if (correlation > 159) {
            //    detections++;
           // }

            System.out.printf("dbs:%f\n", correlation);
            goertzel.reset();
            //goertzelOld.resetGoertzel();

        }

       // System.out.println("samples: "+samples);
       // System.out.println("detections: "+detections);

    }

}
