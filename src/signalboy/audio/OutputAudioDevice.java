package signalboy.audio;

import javax.sound.sampled.*;

public class OutputAudioDevice {

    public static boolean testIfWorks(Mixer mixer) {

        try {

            AudioFormat format = getAudioFormat();

            byte[] test = {0, 0, 0, 0, 0, 0, 0, 0};

            SourceDataLine line = AudioSystem.getSourceDataLine(getAudioFormat(), mixer.getMixerInfo());
            line.open(getAudioFormat());
            line.start();
            line.write(test, 0,  test.length);
            line.drain();
            line.close();

            return true;

        } catch (Exception e) {

           // e.printStackTrace();
            return  false;

        }

    }

    private static AudioFormat getAudioFormat() {

        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS,
                CHANNELS, signed, bigEndian);
        return format;

    }

    private static byte [] shortToByte(short [] input)
    {
        int short_index, byte_index;
        int iterations = input.length;

        byte [] buffer = new byte[input.length * 2];

        short_index = byte_index = 0;

        for(/*NOP*/; short_index != iterations; /*NOP*/)
        {
            buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }

        return buffer;
    }

    public static final int SAMPLE_RATE = 48000; //samples per second
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;

    private Mixer mMixer;
    private SourceDataLine mLine;

    public OutputAudioDevice(Mixer mixer) {
        mMixer = mixer;
    }

    public void playBuffer(short[] buffer) {

        playBuffer(buffer, getAudioFormat());

    }

    public void playBuffer(short[] buffer, AudioFormat format) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] bBuffer = shortToByte(buffer);
                        if (mLine.isOpen()) {
                            mLine.drain();
                            mLine.stop();
                            mLine.close();
                        }
                        mLine.open(format);
                        mLine.start();
                        mLine.write(bBuffer, 0, bBuffer.length);
                        mLine.drain();
                        mLine.stop();
                        mLine.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

    }

    public void stopCurrentBuffer() {
        if (mLine.isOpen()) {
            mLine.stop();
            mLine.close();
        }
    }

    public void start() {
        try {
            mLine = AudioSystem.getSourceDataLine(getAudioFormat(), mMixer.getMixerInfo());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stop() {

    }

}
