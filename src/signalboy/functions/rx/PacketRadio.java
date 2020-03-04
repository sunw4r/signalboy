package signalboy.functions.rx;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.Complex;

public class PacketRadio extends RX {

    private int FREQ_MARK = 1200;
    private int FREQ_SPACE = 2200;
    private int BAUD = 1200;

    private int correlation_length = CaptureAudioDevice.SAMPLE_RATE / BAUD;

    private Complex[] correlation_mark = new Complex[correlation_length];
    private Complex[] correlation_space = new Complex[correlation_length];

    private int sym_phase_inc = Short.MAX_VALUE * BAUD / CaptureAudioDevice.SAMPLE_RATE;

    private CircularFifoQueue<Complex> circularFifoQueue;

    // Bit recovery
    private int shift_reg = 0;
    private int sym_reg = 0;
    private int sym_phase = 0;

    private double correlate(CircularFifoQueue<Complex> circularBuff, Complex[] correlations) {

        Complex out = new Complex(0.0, 0.0);

        for (int i = 0; i < correlations.length; i++) {
            out = out.plus(circularBuff.get(i).times(correlations[i]));
        }

        return out.abs();

    }


    @Override
    public void initialize() {

        circularFifoQueue = new CircularFifoQueue<Complex>(correlation_length);
        for (int i = 0; i < circularFifoQueue.maxSize(); i++) {
            Complex complex = new Complex(0.0, 0.0);
            circularFifoQueue.add(complex);
        }

        // Setup matched filters
        float phi_m, phi_s;
        int i;
        for(phi_m = phi_s = 0, i = 0; i < correlation_length; i++) {
            // Coeffecients
            correlation_mark[i] = new Complex(Math.cos(phi_m), Math.sin(phi_m));
            correlation_space[i] = new Complex(Math.cos(phi_s), Math.sin(phi_s));
            // Increment phase
            phi_m +=  2. * Math.PI * (double)FREQ_MARK / (double)CaptureAudioDevice.SAMPLE_RATE;
            phi_s +=  2. * Math.PI * (double)FREQ_SPACE / (double)CaptureAudioDevice.SAMPLE_RATE;
        }

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] data) {

        String zeroum = "";

        for (int i = 0; i < data.length; i++) {

            // Push new sample.
            circularFifoQueue.add(new Complex((double)data[i], 0.0));

            // Execute correlation.
            double out = correlate(circularFifoQueue, correlation_mark)
                    - correlate(circularFifoQueue, correlation_space);

            System.out.println(out);


        }

        //System.out.println(zeroum);

    }

}
