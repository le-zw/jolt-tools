package com.lezw;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * @author longzhongwei
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        AnchorPane pane = loader.load();
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("DevTools");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/image/icon.png")));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}