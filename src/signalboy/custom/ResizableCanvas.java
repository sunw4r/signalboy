package signalboy.custom;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ResizableCanvas extends Canvas {

    GraphicsContext gc                  = getGraphicsContext2D();
    int             canvasWidth         = 0;
    int             canvasHeight        = 0;

    /**
     * Constructor
     */
    public ResizableCanvas() {

        // if i didn't add the draw to the @Override resize(double width, double
        // height) then it must be into the below listeners

        // Redraw canvas when size changes.
        widthProperty().addListener((observable, oldValue, newValue) -> {
            canvasWidth = (int) widthProperty().get();
        });
        heightProperty().addListener((observable, oldValue, newValue) -> {
            canvasHeight = (int) heightProperty().get();
        });

    }

    /**
     * Redraw the Canvas
     */
    private void draw() {

       // gc.clearRect(0, 0, canvasWidth, canvasHeight);

    }

    @Override
    public double minHeight(double width) {
        return 1;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double minWidth(double height) {
        return 1;
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        super.setWidth(width);
        super.setHeight(height);
        draw();
    }
}
