package com.lezw.controller;

import com.bazaarvoice.jolt.JoltTransform;
import com.bazaarvoice.jolt.JsonUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import com.lezw.codeeditor.CodeEditor;
import com.lezw.codeeditor.CodeMirrorEditor;
import com.lezw.db.Sample;
import com.lezw.db.SampleService;
import com.lezw.transformjson.TransformJSONResource;
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
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processors.standard.util.jolt.TransformUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    @FXML
    private StackPane context;

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
    private StackPane specText;

    @FXML
    private StackPane inputText;

    @FXML
    private StackPane outputText;

    @FXML
    private ChoiceBox<String> joltSample;

    @FXML
    private JFXButton saveSampleButton;

    @FXML
    private JFXButton deleteSampleButton;

    private List<Sample> sampleList;
    private Sample currentSample;

    private final CodeEditor spec = new CodeMirrorEditor();
    private final CodeEditor input = new CodeMirrorEditor();
    private final CodeEditor output = new CodeMirrorEditor();

    int x0, y0, x1, y1;

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
    private final String[] files = getFiles();
    String transform = "";
    TransformJSONResource transformJSONResource = new TransformJSONResource();
    HashMap<String, String> attributes = new HashMap<>();

    /**
     * 根据DSL名称选中对应的选项
     */
    private void selectDslByName(String dslName) {
        if (dslName == null) {
            dslChoice.getSelectionModel().select(1); // 默认Chain
            return;
        }
        for (int i = 0; i < joltDsl.length; i++) {
            if (joltDsl[i].equals(dslName)) {
                dslChoice.getSelectionModel().select(i);
                return;
            }
        }
        dslChoice.getSelectionModel().select(1); // 默认Chain
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        specText.getChildren().add(spec.getWidget());
        inputText.getChildren().add(input.getWidget());
        outputText.getChildren().add(output.getWidget());

        spec.init(
                () -> spec.setJsonLint(true),
                () -> spec.setMode("application/json"),
                () -> spec.setTheme("neat"));
        input.init(
                //() -> input.setContent("select * from T t where t.name = \"test\" limit 20;", true),
                () -> input.setJsonLint(true),
                () -> input.setMode("application/json"),
                () -> input.setTheme("neat"));
        output.init(
                () -> output.setMode("application/json"),
                () -> output.setReadOnly(true),
                () -> output.setTheme("neat"));

        //不显示提示框避免遮挡
        context.getChildren().remove(dialogPane);

        dslChoice.setItems(FXCollections.observableArrayList(joltDsl));

        // 从数据库加载样例
        sampleList = SampleService.getAllJoltSamples();
        List<String> sampleNames = sampleList.stream().map(Sample::getName).collect(Collectors.toList());
        joltSample.setItems(FXCollections.observableArrayList(sampleNames));
        // 默认选中第2个选项
        dslChoice.getSelectionModel().select(1);
        // 监听选中事件
        dslChoice.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    transform = STOCK_TRANSFORMS.get(joltDsl[newValue.intValue()]);
                });
        joltSample.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    int index = newValue.intValue();
                    if (index >= 0 && index < sampleList.size()) {
                        currentSample = sampleList.get(index);
                        Sample s = currentSample;

                        // 切换DSL
                        selectDslByName(s.getDsl());

                        input.setContent(s.getInputContent(), true);
                        spec.setContent(s.getSpecContent(), true);

                        // 加载属性
                        attributes.clear();
                        if (StringUtils.isNotEmpty(s.getAttributes())) {
                            try {
                                Map<String, Object> tmp = JsonUtils.jsonToMap(s.getAttributes());
                                tmp.forEach((k, v) -> {
                                    attributes.put(k, v.toString());
                                });
                            } catch (Exception e) {
                                // 忽略属性解析错误
                            }
                        }
                        spec.autoFormat();
                        input.autoFormat();
                    }
                });
        buttonTooltip();
    }

    @FXML
    protected void validateSpec(ActionEvent event){
        // 校验 spec 格式
        String sp = spec.getContent();
        JoltSpecificationDTO specificationDTO = new JoltSpecificationDTO(transform, sp);
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
        String sp = spec.getContent();
        JoltSpecificationDTO joltSpecificationDTO = new JoltSpecificationDTO(transform, sp);
        joltSpecificationDTO.setInput(input.getContent());
        joltSpecificationDTO.setExpressionLanguageAttributes(attributes);
        try {
            Object inputJson = JsonUtils.jsonToObject(joltSpecificationDTO.getInput());
            JoltTransform transform = transformJSONResource.getTransformation(joltSpecificationDTO,true);
            output.setContent(JsonUtils.toPrettyJsonString(TransformUtils.transform(transform, inputJson)),true);
            output.autoFormat();
        } catch (final Exception e) {
            //logger.error("Execute Specification Failed - " + e.toString());
            output.setContent("Execute Specification Failed - " + e.toString(),true);
        }
    }

    @FXML
    protected void beautyInput(ActionEvent event) {
        input.autoFormat();
    }

    public void fullScreenInput() {
        setSaveAccelerator(input);
    }

    public void fullScreenSpec() {
        setSaveAccelerator(spec);
    }

    protected void setSaveAccelerator(CodeEditor cm) {
        if (context.getChildren().contains(dialogPane)){
            context.getChildren().remove(dialogPane);
            if (dialogPane.getChildren().contains(spec.getWidget())) {
                dialogPane.getChildren().remove(spec.getWidget());
                specText.getChildren().add(spec.getWidget());
            }else {
                dialogPane.getChildren().remove(input.getWidget());
                inputText.getChildren().add(input.getWidget());
            }
        }else {
            context.getChildren().add(dialogPane);
            dialogPane.getChildren().add(cm.getWidget());
        }
    }

    @FXML
    protected void beautySpec(ActionEvent event) {
        spec.autoFormat();
    }

    @FXML
    protected void setAttributes(ActionEvent event) {
        VBox vBox = new VBox();
        Separator separator = new Separator();
        vBox.getChildren().addAll(separator);
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

    private void buttonTooltip() {
        validSpecButton.setTooltip(new Tooltip("校验 Specification 是否合法"));

        beautySpecButton.setTooltip(new Tooltip("格式化 Specification 字符串"));

        transButton.setTooltip(new Tooltip("执行 JOLT 转换"));

        attributeButton.setTooltip(new Tooltip("设置流文件属性键值对"));

        beautyInputButton.setTooltip(new Tooltip("格式化输入 JSON 字符串"));

    }

    private void attributeTotal(Map<String, String> attributes, StackPane dialogPane, Node btn, Pane root, VBox vBox) {
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

        JFXDialog dialog = new JFXDialog(dialogPane, content, JFXDialog.DialogTransition.CENTER, false);
        dialog.setPrefHeight(dialogPane.getMinHeight());
        dialog.setPrefWidth(dialogPane.getMinWidth());

        JFXButton button = new JFXButton("确定");
        button.setStyle("-fx-background-color:#3986F7;");
        button.setOnAction((ActionEvent event) -> {
            dialog.close();
            //不显示提示框避免遮挡
            root.getChildren().remove(dialogPane);
        });
        dialog.show();

        JFXButton attrAdd = new JFXButton();
        attrAdd.setStyle("-fx-pref-width: 50px;-fx-font-size: 14px;-fx-background-color: #3986F7;");
        attrAdd.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        attrAdd.setOnAction((ActionEvent event) -> {
            if (!attributes.containsKey(attrKey.getText()) && StringUtils.isNotEmpty(attrKey.getText())) {
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
            }else {
                if (attributes.containsKey(attrKey.getText()) && !attributes.get(attrKey.getText()).equals(attrValue.getText())) {
                    // 更新attributes map
                    attributes.replace(attrKey.getText(), attrValue.getText());
                }
            }
        });
        hBox.getChildren().addAll(attrKey, attrValue, attrAdd);

        content.setHeading(hBox);
        content.setBody(vBox);
        content.setActions(button);

    }

    private String[] getFiles(){
        String[] list = {"JSON-key加前缀","JSON对象原样输出","JSON数组原样输出","JSON对象新增字段","JSON数组新增字段","JSON对象替换value","JSON数组替换key","JSON对象转数组","JSON提取value部分值","JSON过滤(QueryRecord)","String函数示例","复杂数组转简单数组"};
        return list;
    }

    /**
     * 保存为样例
     */
    @FXML
    protected void saveSample(ActionEvent event) {
        JFXDialogLayout content = new JFXDialogLayout();
        content.setHeading(new Label("保存为样例"));

        VBox vBox = new VBox(10);
        JFXTextField nameField = new JFXTextField();
        nameField.setPromptText("请输入样例名称");
        nameField.setPrefWidth(300);
        vBox.getChildren().add(nameField);

        content.setBody(vBox);

        JFXButton saveBtn = new JFXButton("保存");
        saveBtn.setStyle("-fx-background-color:#3986F7;-fx-pref-width: 80px;");

        JFXButton cancelBtn = new JFXButton("取消");
        cancelBtn.setStyle("-fx-background-color:#999;-fx-pref-width: 80px;");

        JFXDialog dialog = new JFXDialog(dialogPane, content, JFXDialog.DialogTransition.CENTER, false);
        dialog.setPrefWidth(400);

        saveBtn.setOnAction((ActionEvent e) -> {
            String name = nameField.getText();
            if (StringUtils.isBlank(name)) {
                Dialogs.showNoInfoDialog("提示", "请输入样例名称", dialogPane, saveBtn, context);
                return;
            }

            // 保存到数据库
            String attrs = attributes.isEmpty() ? null : JsonUtils.toPrettyJsonString(attributes);
            String currentDsl = dslChoice.getValue();
            SampleService.saveJoltSample(name, input.getContent(), spec.getContent(), currentDsl, attrs);

            // 刷新样例列表
            refreshSampleList();

            dialog.close();
            context.getChildren().remove(dialogPane);
            Dialogs.showYesInfoDialog("成功", "样例保存成功", dialogPane, saveBtn, context);
        });

        cancelBtn.setOnAction((ActionEvent e) -> {
            dialog.close();
            context.getChildren().remove(dialogPane);
        });

        content.setActions(saveBtn, cancelBtn);

        context.getChildren().add(dialogPane);
        dialog.show();
    }

    /**
     * 删除样例
     */
    @FXML
    protected void deleteSample(ActionEvent event) {
        if (currentSample == null) {
            Dialogs.showNoInfoDialog("提示", "请先选择一个样例", dialogPane, deleteSampleButton, context);
            return;
        }

        if (currentSample.isBuiltin()) {
            Dialogs.showNoInfoDialog("提示", "内置样例不能删除", dialogPane, deleteSampleButton, context);
            return;
        }

        // 确认对话框
        JFXDialogLayout content = new JFXDialogLayout();
        content.setHeading(new Label("确认删除"));
        content.setBody(new Label("确定要删除样例 \"" + currentSample.getName() + "\" 吗？此操作不可恢复。"));

        JFXButton confirmBtn = new JFXButton("删除");
        confirmBtn.setStyle("-fx-background-color:#F44336;-fx-pref-width: 80px;");

        JFXButton cancelBtn = new JFXButton("取消");
        cancelBtn.setStyle("-fx-background-color:#999;-fx-pref-width: 80px;");

        JFXDialog dialog = new JFXDialog(dialogPane, content, JFXDialog.DialogTransition.CENTER, false);
        dialog.setPrefWidth(400);

        confirmBtn.setOnAction((ActionEvent e) -> {
            SampleService.deleteSample(currentSample.getId());
            currentSample = null;

            // 刷新样例列表
            refreshSampleList();

            dialog.close();
            context.getChildren().remove(dialogPane);
            Dialogs.showYesInfoDialog("成功", "样例删除成功", dialogPane, confirmBtn, context);
        });

        cancelBtn.setOnAction((ActionEvent e) -> {
            dialog.close();
            context.getChildren().remove(dialogPane);
        });

        content.setActions(confirmBtn, cancelBtn);

        context.getChildren().add(dialogPane);
        dialog.show();
    }

    /**
     * 刷新样例列表
     */
    private void refreshSampleList() {
        sampleList = SampleService.getAllJoltSamples();
        List<String> sampleNames = sampleList.stream().map(Sample::getName).collect(Collectors.toList());
        joltSample.setItems(FXCollections.observableArrayList(sampleNames));
    }
//public static String[] getFiles() {
//    List<String> filenames = new ArrayList<>();
//
//    // 获取资源目录的URL
//    URL resourceFolder = Controller.class.getClassLoader().getResource("jolt");
//
//    if (resourceFolder != null) {
//        try {
//            // 将URL转换为File对象
//            File folder = new File(resourceFolder.toURI());
//            // 获取文件列表
//            File[] files = folder.listFiles();
//
//            if (files != null) {
//                // 遍历文件列表，获取文件名
//                for (File file : files) {
//                    filenames.add(file.getName());
//                }
//            }
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//    } else {
//        System.err.println("Folder 'jolt' not found.");
//    }
//
//    // 将List转换为String数组并返回
//    return filenames.toArray(new String[0]);
//}

}
