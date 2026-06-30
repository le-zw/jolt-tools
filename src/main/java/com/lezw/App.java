package com.lezw;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    private int windowCount = 0;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 加载主窗口的 FXML 文件
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        AnchorPane mainPane = loader.load();
        Scene mainScene = new Scene(mainPane);

        // 设置主窗口的标题和图标
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("DevTools - Main");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/icon.png"))));

        // 注册快捷键
        registerShortcut(primaryStage);

        // 显示主窗口
        primaryStage.show();
    }

    private void registerShortcut(Stage primaryStage) {
        KeyCombination keyCombination = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        primaryStage.getScene().setOnKeyPressed(event -> {
            if (keyCombination.match(event)) {
                // 按下 Ctrl + N 时创建新的主界面窗口
                try {
                    Stage newStage = new Stage();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                    AnchorPane pane = loader.load();
                    Scene scene = new Scene(pane);
                    newStage.setScene(scene);
                    newStage.setTitle("DevTools - Window " + (++windowCount));
                    newStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/icon.png"))));
                    newStage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
