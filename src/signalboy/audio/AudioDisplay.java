package signalboy.audio;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class AudioDisplay {

    private Canvas mCanvas;
    private GraphicsContext mGC;
    private double[] mCurrentData;
    private Thread mDrawThread;
    private boolean mIsWorking;
    private Color lineColor = Color.GREENYELLOW;

    public AudioDisplay(Canvas canvas) {

        mCanvas = canvas;
        mGC = mCanvas.getGraphicsContext2D();
        mIsWorking = true;
        mDrawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(mIsWorking) {
                    try {
                        Thread.sleep(30);
                        redraw();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mDrawThread.start();

    }

    public void setLineColor(Color color) {
        lineColor = color;
    }

    private void redraw() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                mGC.setFill(Color.BLACK);
                mGC.fillRect(0, 0, mCanvas.widthProperty().get(), mCanvas.heightProperty().get());

                if (mCurrentData != null) {

                    mGC.setStroke(lineColor);
                    mGC.setLineWidth(1.0f);

                    double dataLength = mCurrentData.length;
                    double canvasWidth = mCanvas.widthProperty().get();
                    double floor = mCanvas.heightProperty().get()*0.5d;

                    if (dataLength > canvasWidth) {
                        //trunc if length is bigger than canvas width

                        double oldX = 0;
                        double oldY = mCanvas.heightProperty().get();
                        int truncFactor = (int) Math.floor(dataLength / canvasWidth);

                        for (int i = 0; i < canvasWidth; i++) {

                            double x = i;
                            double y = floor-
                                    ((double)mCurrentData[i * truncFactor] * (mCanvas.heightProperty().get()/2));

                            mGC.strokeLine(oldX, oldY, x, y);

                            oldX = x;
                            oldY = y;

                        }


                    } else {
                        //interpolate if smaller

                        double oldX = 0;
                        double oldY = mCanvas.heightProperty().get();
                        double interpFactor = canvasWidth / dataLength;

                        for (int i = 0; i < dataLength; i++) {

                            double x = Math.ceil(i * interpFactor);
                            double y = floor-
                                    ((double)mCurrentData[i] * (mCanvas.heightProperty().get()/2));

                            mGC.strokeLine(oldX, oldY, x, y);

                            oldX = x;
                            oldY = y;

                        }

                    }
                }
            }
        });

    }

    public void stop() {

        mIsWorking = false;

    }

    public void setData(double[] data) {

        mCurrentData = data;

    }

}
