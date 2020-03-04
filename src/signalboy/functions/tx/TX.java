package signalboy.functions.tx;

import javax.sound.sampled.AudioFormat;

public abstract class TX {

    public interface TXDelegate {
        public void playBuffer(short[] buffer);
        public void playBuffer(short[] buffer, AudioFormat format);
        public void stopCurrentBuffer();
    }

    private TXDelegate mDelegate;

    public void registerDelegate(TXDelegate txDelegate) {
        mDelegate = txDelegate;
    }

    public abstract void initialize();
    public abstract void onStop();

    public void playBuffer(short[] buffer) {
        if (mDelegate != null) {
            mDelegate.playBuffer(buffer);
        }
    }

    public void playBuffer(short[] buffer, AudioFormat format) {
        if (mDelegate != null) {
            mDelegate.playBuffer(buffer, format);
        }
    }

    public void stopCurrentBuffer() {
        if (mDelegate != null) {
            mDelegate.stopCurrentBuffer();
        }
    }


}
