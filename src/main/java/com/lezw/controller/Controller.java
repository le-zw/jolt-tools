package com.lezw.controller;

import com.bazaarvoice.jolt.JoltTransform;
import com.bazaarvoice.jolt.JsonUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import com.lezw.transformjson.TransformJSONResource;
import com.lezw.transformjson.dto.HighlighterFactory;
import com.lezw.transformjson.dto.JoltSpecificationDTO;
import com.lezw.transformjson.dto.ValidationDTO;
import com.lezw.util.Dialogs;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processors.standard.util.jolt.TransformUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private AnchorPane context;

    @FXML
    private StackPane dialogPane;

    @FXML
    private JFXButton validSpecButton;

    @FXML
    private JFXButton beautySpecButton;

    @FXML
    private JFXButton transButton;

    @FXML
    private JFXButton attributeButton;

    @FXML
    private JFXButton beautyInputButton;

    @FXML
    private ChoiceBox<String> dslChoice;

    @FXML
    private CodeArea specText;

    @FXML
    private CodeArea inputText;

    @FXML
    private CodeArea outputText;

    /**
     * Map transform "operation" names
     */
    public static final Map<String, String> STOCK_TRANSFORMS;

    static {
        HashMap<String, String> temp = new HashMap<>();
        temp.put("Shift", "jolt-transform-shift");
        temp.put("Default", "jolt-transform-default");
        temp.put("Modify - Overwrite", "jolt-transform-modify-overwrite");
        temp.put("Modify - Default", "jolt-transform-modify-default");
        temp.put("Modify - Define", "jolt-transform-modify-define");
        temp.put("Remove", "jolt-transform-remove");
        temp.put("Sort", "jolt-transform-sort");
        temp.put("Chain", "jolt-transform-chain");
        temp.put("Custom", "jolt-transform-custom");
        temp.put("Cardinality", "jolt-transform-card");
        STOCK_TRANSFORMS = Collections.unmodifiableMap( temp );
    }
    private final String[] joltDsl = { "Cardinality","Chain","Custom","Default","Modify - Default","Modify - Define","Modify - Overwrite","Remove","Shift","Sort" };
    String transform = "";
    TransformJSONResource transformJSONResource = new TransformJSONResource();
    HashMap<String, String> attributes = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //不显示提示框避免遮挡
        context.getChildren().remove(dialogPane);

        dslChoice.setItems(FXCollections.observableArrayList(joltDsl));
        // 默认选中第2个选项
        dslChoice.getSelectionModel().select(1);
        // 监听选中事件
        dslChoice.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    transform = STOCK_TRANSFORMS.get(joltDsl[newValue.intValue()]);
                });

        // 设置 codeArea 显示行号且高亮 JSON
        outputText.setParagraphGraphicFactory(LineNumberFactory.get(outputText));
        inputText.setParagraphGraphicFactory(LineNumberFactory.get(inputText));
        specText.setParagraphGraphicFactory(LineNumberFactory.get(specText));
        highLightJson(outputText);
        highLightJson(inputText);
        highLightJson(specText);
        buttonTooltip();
    }

    @FXML
    protected void validateSpec(ActionEvent event){
        // 校验 spec 格式
        String spec = specText.getText();
        JoltSpecificationDTO specificationDTO = new JoltSpecificationDTO(transform, spec);
        ValidationDTO validationDTO = transformJSONResource.validateSpec(specificationDTO);
        if (validationDTO.isValid()){
            Dialogs.showYesInfoDialog("JOLT Specification",validationDTO.getMessage(), dialogPane, validSpecButton, context);
        }else {
            Dialogs.showNoInfoDialog("JOLT Specification",validationDTO.getMessage(), dialogPane, validSpecButton, context);
        }

    }

    @FXML
    protected void transform(ActionEvent event) {
        //jolt转换json
        String spec = specText.getText();
        JoltSpecificationDTO joltSpecificationDTO = new JoltSpecificationDTO(transform, spec);
        joltSpecificationDTO.setInput(inputText.getText());
        joltSpecificationDTO.setExpressionLanguageAttributes(attributes);
        try {
            Object inputJson = JsonUtils.jsonToObject(joltSpecificationDTO.getInput());
            JoltTransform transform = transformJSONResource.getTransformation(joltSpecificationDTO,true);
            outputText.replaceText(JsonUtils.toPrettyJsonString(TransformUtils.transform(transform, inputJson)));
            outputText.setStyleSpans(0, HighlighterFactory.getHighlighter("JSON").computeHighlighting(outputText.getText()));
        } catch (final Exception e) {
            //logger.error("Execute Specification Failed - " + e.toString());
            outputText.replaceText("Execute Specification Failed - " + e.toString());
            outputText.setStyleSpans(0, HighlighterFactory.getHighlighter("JSON").computeHighlighting(outputText.getText()));
        }
    }

    @FXML
    protected void beautyInput(ActionEvent event) {
        beautyJson(inputText);
    }

    @FXML
    protected void beautySpec(ActionEvent event) {
        beautyJson(specText);
    }

    @FXML
    protected void setAttributes(ActionEvent event) {
        VBox vBox = new VBox();
        if (attributes != null && attributes.size() >0) {
            Iterator<Map.Entry<String,String>> iterable = attributes.entrySet().iterator();
            while (iterable.hasNext()) {
                Map.Entry<String,String> entry = iterable.next();

                HBox hBox = new HBox(30);
                hBox.setPadding(new Insets(10, 0, 0, 0));

                Label key = new Label(entry.getKey());
                key.setPrefWidth(150);


                Label value = new Label(entry.getValue());
                value.setPrefWidth(150);

                JFXButton delete = new JFXButton();
                delete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT));
                delete.setStyle("-fx-pref-width: 50px;-fx-font-size: 14px;-fx-background-color: #3986F7;");
                hBox.getChildren().addAll(key, value, delete);
                delete.setOnAction((ActionEvent e) -> {
                    attributes.remove(entry.getKey());
                    vBox.getChildren().remove(hBox);
                });
                vBox.getChildren().add(hBox);
            }
        }
        attributeTotal(attributes, dialogPane, attributeButton, context, vBox);
    }

    private void beautyJson(CodeArea codeArea) {
        if (StringUtils.isNoneEmpty(codeArea.getText())){
            codeArea.replaceText(JsonUtils.toPrettyJsonString(JsonUtils.jsonToObject(codeArea.getText())));
            codeArea.setStyleSpans(0, HighlighterFactory.getHighlighter("JSON").computeHighlighting(codeArea.getText()));
        }
    }

    private void highLightJson(CodeArea codeArea) {
        //json 高亮
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, HighlighterFactory.getHighlighter("JSON").computeHighlighting(newText));
        });
    }

    private void buttonTooltip() {
        validSpecButton.setTooltip(new Tooltip("校验 Specification 是否合法"));

        beautySpecButton.setTooltip(new Tooltip("格式化 Specification 字符串"));

        transButton.setTooltip(new Tooltip("执行 JOLT 转换"));

        attributeButton.setTooltip(new Tooltip("设置流文件属性键值对"));

        beautyInputButton.setTooltip(new Tooltip("格式化输入 JSON 字符串"));

    }

    private void attributeTotal(Map<String, String> attributes, StackPane dialogPane, Node btn, AnchorPane root, VBox vBox) {
        JFXDialogLayout content = new JFXDialogLayout();
        //显示提示框
        root.getChildren().add(dialogPane);

        HBox hBox = new HBox(30);

        JFXTextField attrKey = new JFXTextField();
        attrKey.setPromptText("Attribute");
        attrKey.setPrefWidth(150);

        JFXTextField attrValue = new JFXTextField();
        attrValue.setPromptText("Value");
        attrValue.setPrefWidth(150);

        JFXButton attrAdd = new JFXButton();
        attrAdd.setStyle("-fx-pref-width: 50px;-fx-font-size: 14px;-fx-background-color: #3986F7;");
        attrAdd.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        attrAdd.setOnAction((ActionEvent event) -> {
            if (attributes.get(attrKey.getText()) == null && StringUtils.isNotEmpty(attrKey.getText())) {
                attributes.put(attrKey.getText(), attrValue.getText());
                HBox h = new HBox(30);
                h.setPadding(new Insets(10, 0, 0, 0));

                Label key = new Label(attrKey.getText());
                key.setPrefWidth(150);
                h.getChildren().add(key);

                Label value = new Label(attrValue.getText());
                value.setPrefWidth(150);
                h.getChildren().add(value);

                JFXButton delete = new JFXButton();
                delete.setStyle("-fx-pref-width: 50px;-fx-font-size: 14px;-fx-background-color: #3986F7;");
                FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT);
                delete.setGraphic(iconView);
                delete.setOnAction((ActionEvent e) -> {
                    attributes.remove(attrKey.getText());
                    vBox.getChildren().remove(h);
                });
                h.getChildren().add(delete);

                vBox.getChildren().add(h);
            }
        });
        hBox.getChildren().addAll(attrKey, attrValue, attrAdd);

        content.setHeading(hBox);

        content.setBody(vBox);
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
