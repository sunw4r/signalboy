package signalboy.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CaptureAudioDevice {

    public interface CaptureListener {
        public void onBuffer(short[] buffer);
    }

    public static final int SAMPLE_RATE = 48000; //samples per second
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final int BUFFER_SIZE = 4800;

    private Mixer mMixer;
    private CaptureListener mListener;
    private boolean mStarted;
    private Thread mCapThread;

    public CaptureAudioDevice(Mixer mixer, CaptureListener listener) {

        mMixer = mixer;
        mListener = listener;

    }

    public void startCap() {

        if (!mStarted) {
            mStarted = true;

            mCapThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {

                        AudioFormat format = getAudioFormat();

                        TargetDataLine line = AudioSystem.getTargetDataLine(getAudioFormat(), mMixer.getMixerInfo());
                        line.open(getAudioFormat());
                        line.start();

                        int bufferSize = BUFFER_SIZE;

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        while (mStarted) {

                            byte buffer[] = new byte[bufferSize*(SAMPLE_SIZE_BITS/8)];
                            int count = line.read(buffer, 0, buffer.length);

                            if (mListener != null) {
                                mListener.onBuffer(from8toShort(buffer));
                            }

                        }

                        out.close();
                        line.stop();
                        line.close();

                    } catch (LineUnavailableException e) {

                        e.printStackTrace();

                    } catch (IOException e) {

                        e.printStackTrace();

                    }

                }
            });
            mCapThread.start();

        }

    }

    public void stop() {
        mStarted = false;
        mCapThread.interrupt();
    }

    private static AudioFormat getAudioFormat() {

        boolean signed = true;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS,
                CHANNELS, signed, bigEndian);
        return format;

    }

    //Utilities
    public short[] from8toShort(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }

    public float[] from16toFloat(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

}
