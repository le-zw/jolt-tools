package com.lezw.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.lezw.codeeditor.CodeEditor;
import com.lezw.codeeditor.CodeMirrorEditor;
import com.lezw.db.Sample;
import com.lezw.db.SampleService;
import com.lezw.script.AccessibleExecuteScript;
import com.lezw.script.AccessibleScriptingComponentHelper;
import com.lezw.script.ExecuteScript;
import com.lezw.script.ScriptingComponentUtils;
import com.lezw.util.Dialogs;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import groovy.json.JsonBuilder;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processors.groovyx.ExecuteGroovyScript;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author zhongwei.long
 * @date 2022年03月02日 17:16
 */
public class ScriptController implements Initializable {

    @FXML
    private StackPane context;

    @FXML
    private StackPane dialogPane;

    @FXML
    private StackPane inputText;

    @FXML
    private StackPane scriptText;

    @FXML
    private StackPane outputText;

    @FXML
    private JFXButton transButton;

    @FXML
    private JFXButton attributeButton;

    @FXML
    private ChoiceBox<String> scriptType;

    @FXML
    private ChoiceBox<String> scriptExample;

    @FXML
    private JFXButton saveSampleButton;

    @FXML
    private JFXButton deleteSampleButton;

    @FXML
    private ChoiceBox<String> processType;

    private List<Sample> sampleList;
    private Sample currentSample;

    @FXML
    private JFXTextArea scriptPath;

    @FXML
    private JFXTextArea modulePath;

    private final CodeEditor input = new CodeMirrorEditor();
    private final CodeEditor output = new CodeMirrorEditor();
    private final CodeEditor scriptContent = new CodeMirrorEditor();
    private final String[] type = { "Clojure","ECMAScript","Groovy","lua","python","ruby"};
    private final String[] files = getFiles();

    private final String[] processors = { "ExecuteScript","ExecuteGroovyScript"};
    /**
     * Map transform "script theme" names
     */
    public static final Map<String, String> SCRIPT_THEME;

    static {
        HashMap<String, String> temp = new HashMap<>();
        temp.put("Clojure", "text/x-clojure");
        temp.put("ECMAScript", "application/javascript");
        temp.put("Groovy", "text/x-groovy");
        temp.put("lua", "text/x-lua");
        temp.put("python", "text/x-python");
        temp.put("ruby", "text/x-ruby");

        SCRIPT_THEME = Collections.unmodifiableMap( temp );
    }

    private String scriptEngineName = "Groovy";

    private String processor = "ExecuteScript";

    HashMap<String, String> attributes = new HashMap<>();

    public static String DASHED_LINE = "---------------------------------------------------------";

    private static TestRunner runner;
    private static AccessibleScriptingComponentHelper scriptingComponent;
    private static boolean outputAttributes = true;
    private static boolean outputContent = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //不显示提示框避免遮挡
        context.getChildren().remove(dialogPane);

        //初始化选择框
        scriptType.setItems(FXCollections.observableArrayList(type));

        // 从数据库加载样例
        sampleList = SampleService.getAllScriptSamples();
        List<String> sampleNames = sampleList.stream().map(Sample::getName).collect(Collectors.toList());
        scriptExample.setItems(FXCollections.observableArrayList(sampleNames));

        processType.setItems(FXCollections.observableArrayList(processors));

        // 默认选中第3个选项
        scriptType.getSelectionModel().select(2);
        // 监听选中事件
        scriptType.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    scriptEngineName = type[newValue.intValue()];
                    scriptContent.setMode(SCRIPT_THEME.get(scriptEngineName));
                });
        scriptExample.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    int index = newValue.intValue();
                    if (index >= 0 && index < sampleList.size()) {
                        currentSample = sampleList.get(index);
                        Sample s = currentSample;

                        // 切换Script Type
                        scriptType.getSelectionModel().select(getScriptTypeByName(s.getScriptType()));

                        // 切换Processor
                        if (StringUtils.isNotEmpty(s.getProcessor())) {
                            for (int i = 0; i < processors.length; i++) {
                                if (processors[i].equals(s.getProcessor())) {
                                    processType.getSelectionModel().select(i);
                                    break;
                                }
                            }
                        }

                        // 加载Input
                        if (StringUtils.isNotEmpty(s.getInputContent())) {
                            input.setContent(s.getInputContent(), true);
                        }

                        // 加载Script
                        scriptContent.setContent(s.getSpecContent(), true);
                    }
                });
        //默认选中第1个
        processType.getSelectionModel().select(0);
        processType.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, newValue) -> {
                    processor = processors[newValue.intValue()];
                });
        //初始化文本框
        inputText.getChildren().add(input.getWidget());
        scriptText.getChildren().add(scriptContent.getWidget());
        outputText.getChildren().add(output.getWidget());
        scriptContent.init(
                () -> scriptContent.setTheme("neat"),
                () -> scriptContent.setMode("text/x-groovy"));
        input.init(
                () -> input.setMode("application/json"));
        output.init(
                () -> output.setMode("application/json"),
                () -> output.setTheme("neat"));

    }

    @FXML
    protected void transform(ActionEvent event) {
        // 将控制台输出打印到 output 上
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream(1024);
        //临时输出
        PrintStream cacheStream = new PrintStream(baoStream);
        System.setOut(cacheStream);
        System.setErr(cacheStream);
        try {
            switch (processor){
                case "ExecuteScript":
                    execute1();
                    break;
                case "ExecuteGroovyScript":
                    execute2();
                    break;
                default:
                    return;
            }

        }catch (Throwable exc){
            exc.printStackTrace();
        }
        //存放控制台输出的字符串
        String result = baoStream.toString();
        output.setContent(result, true);
    }

    private void execute2() {
        final ExecuteGroovyScript executeScript = new ExecuteGroovyScript();;
        // Need to do something to initialize the properties, like retrieve the list of properties

        runner = TestRunners.newTestRunner(executeScript);

        if (StringUtils.isNotEmpty(scriptContent.getContent())) {
            runner.setProperty(ExecuteGroovyScript.SCRIPT_BODY, scriptContent.getContent());
        }else {
            if (StringUtils.isNotEmpty(scriptPath.getText())) {
                File scriptFile = new File(scriptPath.getText());
                if (!scriptFile.exists()) {
                    System.err.println("Script file not found: " + scriptPath.getText());
                    System.exit(2);
                }
                runner.setProperty(ExecuteGroovyScript.SCRIPT_FILE, scriptPath.getText());
            }
        }
        if (StringUtils.isNotEmpty(modulePath.getText())) {
            runner.setProperty(ExecuteGroovyScript.ADD_CLASSPATH, modulePath.getText());
        }
        runner.assertValid();
        runner.enqueue(input.getContent(), attributes);
        runner.run();
        outputFlowFilesForRelationship(ExecuteGroovyScript.REL_SUCCESS);
        outputFlowFilesForRelationship(ExecuteGroovyScript.REL_FAILURE);

    }

    private void execute1() {

        final ExecuteScript executeScript = new AccessibleExecuteScript();
        // Need to do something to initialize the properties, like retrieve the list of properties
        executeScript.getSupportedPropertyDescriptors();
        runner = TestRunners.newTestRunner(executeScript);
        scriptingComponent = (AccessibleScriptingComponentHelper) executeScript;
        runner.setValidateExpressionUsage(false);
        runner.setProperty(scriptingComponent.getScriptingComponentHelper().SCRIPT_ENGINE, scriptEngineName);
        if (StringUtils.isNotEmpty(scriptContent.getContent())) {
            runner.setProperty(ScriptingComponentUtils.SCRIPT_BODY, scriptContent.getContent());
        }else {
            if (StringUtils.isNotEmpty(scriptPath.getText())) {
                File scriptFile = new File(scriptPath.getText());
                if (!scriptFile.exists()) {
                    System.err.println("Script file not found: " + scriptPath.getText());
                    System.exit(2);
                }
                runner.setProperty(ScriptingComponentUtils.SCRIPT_FILE, scriptPath.getText());
            }
        }
        if (StringUtils.isNotEmpty(modulePath.getText())) {
            runner.setProperty(ScriptingComponentUtils.MODULES, modulePath.getText());
        }
        runner.assertValid();
        runner.enqueue(input.getContent(), attributes);
        runner.run();
        outputFlowFilesForRelationship(ExecuteScript.REL_SUCCESS);
        outputFlowFilesForRelationship(ExecuteScript.REL_FAILURE);

    }

//    private static void outputFlowFilesForRelationship(Relationship relationship) {
//
//        List<MockFlowFile> files = runner.getFlowFilesForRelationship(relationship);
//        if (files != null) {
//            for (MockFlowFile flowFile : files) {
//                if (outputAttributes) {
//                    final StringBuilder message = new StringBuilder();
//                    message.append("Flow file ").append(flowFile);
//                    message.append("\n");
//                    message.append(DASHED_LINE);
//                    message.append("\nFlowFile Attributes:\n");
//                    for (final String key : flowFile.getAttributes().keySet()) {
//                        assert !key.equals("path");
//                        message.append(String.format("\nKey: '%1$s'\n\tValue: '%2$s'", key, flowFile.getAttribute(key)));
//                    }
//                    message.append("\n");
//                    message.append(DASHED_LINE);
//                    System.out.println(message);
//                }
//                if (outputContent) {
//                    System.out.println("FlowFile Content:");
//                    System.out.println("");
//                    System.out.println(new String(flowFile.toByteArray()));
//                    System.out.println(DASHED_LINE);
//                }
//                System.out.println("");
//            }
//            System.out.println("Flow Files transferred to " + relationship.getName() + ": " + files.size() + "\n");
//        }
//    }
private static void outputFlowFilesForRelationship(Relationship relationship) {
    List<MockFlowFile> files = runner.getFlowFilesForRelationship(relationship);
    if (files != null) {
        System.err.println("Flow Files transferred to " + relationship.getName() + ": " + files.size());
        for (MockFlowFile flowFile : files) {
            final StringBuilder message = new StringBuilder();
            message.append("--------------------------------------------------------------\n");
            message.append(flowFile);
            message.append("\nFlowFile Attributes:\n");
            message.append(new JsonBuilder(flowFile.getAttributes()).toPrettyString());
            message.append("\n");
            System.out.println(message.toString());
            System.out.println("FlowFile Content:");
            System.out.println(new String(flowFile.toByteArray()));
        }
    }
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
        String[] list = {"attributes.groovy","attributes.js","attributes.py","datetime.groovy","datetime.js","json2json.groovy","json2json.py","log" +
                ".groovy","log.js","log.py","padleft.groovy","split.js","split.py","state.groovy","state.js","state.py","transform.groovy",
                "transform.js","transform.py","xmlToJson.groovy","convertTimestampToDate.groovy","fileLine.groovy","groupVariables.groovy",
        "json_to_Attribute.groovy","jsonbuilder.groovy"};
        return list;
    }

    /**
     * 根据脚本类型名称获取索引
     */
    private int getScriptTypeByName(String scriptTypeName) {
        if (scriptTypeName == null) return 2;
        switch (scriptTypeName) {
            case "Clojure": return 0;
            case "ECMAScript": return 1;
            case "Groovy": return 2;
            case "lua": return 3;
            case "python": return 4;
            case "ruby": return 5;
            default: return 2;
        }
    }

    private int getScriptType(String script){
        String postfix = script.substring(script.lastIndexOf(".")+1).toLowerCase();
        int type = 2;
        // { "Clojure","ECMAScript","Groovy","lua","python","ruby"};
        switch (postfix){
            case "cljs": type = 0;break;
            case "js": type =1;break;
            case "groovy": type =2;break;
            case "lua": type =3;break;
            case "py": type =4;break;
            case "rb": type =5;break;
            default: break;
        }
        return type;
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
            SampleService.saveScriptSample(name, scriptContent.getContent(), scriptEngineName, processor, input.getContent());

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
        sampleList = SampleService.getAllScriptSamples();
        List<String> sampleNames = sampleList.stream().map(Sample::getName).collect(Collectors.toList());
        scriptExample.setItems(FXCollections.observableArrayList(sampleNames));
    }

    private String getStringFromResource(String path){
        StringBuffer sbf = new StringBuffer();
        try (InputStream inputStream = this.getClass().getResourceAsStream(path)){
            byte[] inputbyte=new byte[inputStream.available()];
            //循环读取
            while (inputStream.read(inputbyte)!=-1) {
                //将byte数组中的内容转换为String内容字符串输出
                String s = new String(inputbyte, 0, inputbyte.length);
                sbf.append(s);
            }
            return sbf.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return sbf.toString();

    }
}