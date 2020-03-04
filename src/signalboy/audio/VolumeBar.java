package signalboy.audio;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VolumeBar {

    private Canvas mCanvas;
    private GraphicsContext mGC;
    private float mCurrentVolume;
    private Thread mDrawThread;
    private boolean mIsWorking;

    public VolumeBar(Canvas canvas) {

        mCanvas = canvas;
        mGC = mCanvas.getGraphicsContext2D();
        mIsWorking = true;
        mDrawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(mIsWorking) {
                    try {
                        Thread.sleep(30);
                        drawVolume();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mDrawThread.start();

    }

    public void setVolume(float volume) {
        mCurrentVolume = volume;
    }

    public void drawVolume() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                //Draw the background
                mGC.setFill(Color.BLACK);
                mGC.fillRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight());

                //Draw the volume
                mGC.setFill(Color.GREENYELLOW);
                mGC.fillRect(0, 0, (float)mCanvas.getWidth()*mCurrentVolume, mCanvas.getHeight());

            }
        });

    }

}
