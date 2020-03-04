package signalboy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainscreen.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        primaryStage.getIcons().add(new Image("file:resources/icon_small.png"));
        primaryStage.setTitle("Signal Boy");
        primaryStage.setScene(new Scene(root, 1024, 860));
        primaryStage.show();
    }

    @Override
    public void stop(){
        System.out.println("Stage is closing");
        controller.exitApplication(null);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
