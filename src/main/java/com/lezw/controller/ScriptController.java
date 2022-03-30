package com.lezw.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.lezw.codeeditor.CodeEditor;
import com.lezw.codeeditor.CodeMirrorEditor;
import com.lezw.script.AccessibleExecuteScript;
import com.lezw.script.AccessibleScriptingComponentHelper;
import com.lezw.script.ExecuteScript;
import com.lezw.script.ScriptingComponentUtils;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processor.Relationship;
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
    private JFXTextArea scriptPath;

    @FXML
    private JFXTextArea modulePath;

    private final CodeEditor input = new CodeMirrorEditor();
    private final CodeEditor output = new CodeMirrorEditor();
    private final CodeEditor scriptContent = new CodeMirrorEditor();
    private final String[] type = { "Clojure","ECMAScript","Groovy","lua","python","ruby"};
    private final String[] files = getFiles();
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
        scriptExample.setItems(FXCollections.observableArrayList(files));
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
                    String val = files[newValue.intValue()];
                    String pos = val.substring(val.lastIndexOf(".")+1);
                    scriptType.getSelectionModel().select(getScriptType(pos));
                    scriptContent.setContent(getStringFromResource("/script/"+val), true);
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
        PrintStream cacheStream = new PrintStream(baoStream);//临时输出
        System.setOut(cacheStream);
        System.setErr(cacheStream);
        try {
            execute1();
        }catch (Throwable exc){
            exc.printStackTrace();
        }
        String result = baoStream.toString();//存放控制台输出的字符串
        output.setContent(result, true);
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

    private static void outputFlowFilesForRelationship(Relationship relationship) {

        List<MockFlowFile> files = runner.getFlowFilesForRelationship(relationship);
        if (files != null) {
            for (MockFlowFile flowFile : files) {
                if (outputAttributes) {
                    final StringBuilder message = new StringBuilder();
                    message.append("Flow file ").append(flowFile);
                    message.append("\n");
                    message.append(DASHED_LINE);
                    message.append("\nFlowFile Attributes:\n");
                    for (final String key : flowFile.getAttributes().keySet()) {
                        assert !key.equals("path");
                        message.append(String.format("\nKey: '%1$s'\n\tValue: '%2$s'", key, flowFile.getAttribute(key)));
                    }
                    message.append("\n");
                    message.append(DASHED_LINE);
                    System.out.println(message);
                }
                if (outputContent) {
                    System.out.println("FlowFile Content:");
                    System.out.println("");
                    System.out.println(new String(flowFile.toByteArray()));
                    System.out.println(DASHED_LINE);
                }
                System.out.println("");
            }
            System.out.println("Flow Files transferred to " + relationship.getName() + ": " + files.size() + "\n");
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
        String[] list = {"attributes.groovy","attributes.js","attributes.py","datetime.groovy","datetime.js","json2json.groovy","json2json.py","log.groovy","log.js","log.py","padleft.groovy","split.js","split.py","state.groovy","state.js","state.py","transform.groovy","transform.js","transform.py","xmlToJson.groovy","convertTimestampToDate.groovy","fileLine.groovy","json_to_Attribute.groovy","jsonbuilder.groovy"};
        return list;
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