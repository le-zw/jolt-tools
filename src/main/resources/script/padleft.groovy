import org.apache.commons.io.IOUtils;
import org.apache.nifi.processor.io.StreamCallback
import java.nio.charset.StandardCharsets

/**
 * 字符串前置补指定个数的 0 (长度固定，不足的部分补0)
 * FlowFile Context:
     {
        "CODE": "RY0233"
     }
 * FlowFile Attribute：
     executesql.row.count 模拟数据库查询条数，最终生成的值在原来的基础上加上查询条数
 * @author zhongwei.long
 * @date 2021/8/31 上午11:36
 * @param null
 * @return null
 */

// 同时获取多个流文件
// flowFileList = session.get(100)
// if(!flowFileList.isEmpty()) {
//     flowFileList.each { flowFile ->
//     // Process each FlowFile here
//     }
// }

def flowFile = session.get();
if (flowFile == null) {
    return;
}
def count = flowFile.getAttribute("executesql.row.count") as Integer
def slurper = new groovy.json.JsonSlurper()
def new_code
flowFile = session.write(flowFile,
        { inputStream, outputStream ->
            def builder = count
            if(count != 0){
                def text = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
                def obj = slurper.parseText(text)
                builder= "${obj.CODE}"[2..-1]
            }
            def code = (builder as Integer) + 1
            new_code = "RY" + String.format("%09d", code) // RY000000001(固定长度为9，不补前置补0)
            outputStream.write(new_code.getBytes(StandardCharsets.UTF_8))
        } as StreamCallback)
session.putAttribute(flowFile, "CODE", new_code)
session.transfer(flowFile, ExecuteScript.REL_SUCCESS)