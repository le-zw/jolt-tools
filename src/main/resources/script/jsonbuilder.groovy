import org.apache.commons.io.IOUtils;
/**
 * JSONBuilder构建返回JSON
 * FlowFile Context：
     {
        "value": 2
     }
 * FlowFile Attribute：CODE、UUID
 * @author zhongwei.long
 * @date 2021年08月31日 上午11:32
 */

import org.apache.nifi.processor.io.StreamCallback

import java.nio.charset.StandardCharsets

def flowFile = session.get();
if (flowFile == null) {
    return;
}
def slurper = new groovy.json.JsonSlurper()

flowFile = session.write(flowFile,
        { inputStream, outputStream ->
            def text = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            def obj = slurper.parseText(text)
            def builder = new groovy.json.JsonBuilder()
            builder.call {
                'CODE' flowFile.getAttribute("CODE")
                'UUID' flowFile.getAttribute("UUID")
                'CODEVALUE' obj
            }
            outputStream.write(builder.toPrettyString().getBytes(StandardCharsets.UTF_8))
        } as StreamCallback)
session.transfer(flowFile, REL_SUCCESS)