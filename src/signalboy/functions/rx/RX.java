package signalboy.functions.rx;

import javafx.application.Platform;

public abstract class RX {

    public abstract void initialize();
    public abstract void onStop();
    public abstract void processBuffer(short[] data);
    public void setBuffer(short[] buffer) {

       // processBuffer(buffer);
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        processBuffer(buffer);
                    }
                });
            }
        });
        worker.start();
    }

}
