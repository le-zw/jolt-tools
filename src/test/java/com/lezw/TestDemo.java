package com.lezw;

import groovy.json.JsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processors.groovyx.ExecuteGroovyScript;
import org.apache.nifi.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TestDemo {
    protected static TestRunner runner;
    protected ExecuteGroovyScript proc;
    public final String TEST_RESOURCE_LOCATION = "target/test/resources/groovy/";
    Map<String, String> incomingAttributes = new HashMap<>();
    @BeforeAll
    public static void setupBeforeClass() throws Exception {
        FileUtils.copyDirectory(new File("src/test/resources"), new File("target/test/resources"));
    }

    @BeforeEach
    public void setup() throws Exception {
        //init processor
        proc = new ExecuteGroovyScript();
        MockProcessContext context = new MockProcessContext(proc);
        MockProcessorInitializationContext initContext = new MockProcessorInitializationContext(proc, context);
        proc.initialize(initContext);
        runner = TestRunners.newTestRunner(proc);

        // Get incoming attributes from file (if specified)
        String attrFile = "src/test/resources/attrfiles/incoming_attributes.properties";
        Path attrFilePath = Paths.get(attrFile);
        if (!Files.exists(attrFilePath)) {
            System.err.println("Attribute file does not exist: " + attrFile);
            System.exit(5);
        } else {
            try {
                Properties p = new Properties();
                p.load(Files.newBufferedReader(attrFilePath));
                p.forEach((k, v) -> incomingAttributes.put(k.toString(), v.toString()));
            } catch (IOException ioe) {
                System.err.println("Could not read properties file: " + attrFile + ", reason: " + ioe.getLocalizedMessage());
                System.exit(5);
            }
        }

    }

    /**
     * 格式化输出Relationship上的流文件内容和属性.
     *
     * @param relationship relationship of flowFile
     */
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

    @Test
    public void test_onTrigger_groovy() throws IOException {
        runner.setProperty(proc.SCRIPT_FILE, TEST_RESOURCE_LOCATION + "test_lezw.groovy");
        //runner.setProperty(proc.FAIL_STRATEGY, "rollback");
        runner.assertValid();

        runner.enqueue(Paths.get("src/test/resources/input_files/kong_orchsym.log"),incomingAttributes);
        runner.enqueue(Paths.get("src/test/resources/input_files/kong_orchsym.log"),incomingAttributes);
        runner.run(2);

        runner.assertAllFlowFilesTransferred(proc.REL_SUCCESS.getName());

        outputFlowFilesForRelationship(proc.REL_SUCCESS);
    }
}
