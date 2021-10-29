package com.lezw.util;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public final class Dialogs {
    public static void showYesInfoDialog(String header, String message, StackPane dialogPane, Node btn, AnchorPane root) {//提示框
        JFXDialogLayout content = new JFXDialogLayout();
        //圆勾
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE);
        //提示框图标样式 #ff5c04橙色代表感叹号警告，#c21918红色代表错误 green绿色代表勾
        icon.setStyle("-fx-font-size: 30px;-fx-fill: green");
        Total(header, message, dialogPane, btn, root, content, icon);
    }

    public static void showNoInfoDialog(String header, String message, StackPane dialogPane, Node btn, AnchorPane root) {//提示框
        JFXDialogLayout content = new JFXDialogLayout();
        //圆叉
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TIMES_CIRCLE);
        //提示框图标样式 #ff5c04橙色代表感叹号警告，#c21918红色代表错误 green绿色代表勾
        icon.setStyle("-fx-font-size: 30px;-fx-fill: #c21918");
        Total(header, message, dialogPane, btn, root, content, icon);
    }

    public static void showWarnInfoDialog(String header, String message, StackPane dialogPane, Node btn, AnchorPane root) {//提示框
        JFXDialogLayout content = new JFXDialogLayout();
        //圆感叹号
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE);
        //提示框图标样式 #ff5c04橙色代表感叹号警告，#c21918红色代表错误 green绿色代表勾
        icon.setStyle("-fx-font-size: 30px;-fx-fill: #ff5c04");
        Total(header, message, dialogPane, btn, root, content, icon);
    }

    private static void Total(String header, String message, StackPane dialogPane, Node btn, AnchorPane root, JFXDialogLayout content, FontAwesomeIconView icon) {
        //显示提示框
        root.getChildren().add(dialogPane);
        content.setHeading(icon, new Text("           "+header));
        content.setBody(new VBox(new Label(message)));
        JFXDialog dialog = new JFXDialog(dialogPane, content, JFXDialog.DialogTransition.CENTER, false);
        dialog.setPrefHeight(dialogPane.getMinHeight());
        dialog.setPrefWidth(dialogPane.getMinWidth());
        JFXButton button = new JFXButton("确定");
        button.setStyle("-fx-background-color:#3986F7;");
        button.setOnAction((ActionEvent event) -> {
            dialog.close();
            btn.setDisable(false);
            //不显示提示框避免遮挡
            root.getChildren().remove(dialogPane);
        });
        btn.setDisable(true);
        content.setActions(button);
        dialog.show();
    }

}
