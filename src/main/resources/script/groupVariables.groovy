/**
 * 获取模块变量
 * @author zhongwei.long
 * @date 2022年04月22日 上午10:24
 */

import org.apache.commons.io.IOUtils
import org.apache.nifi.processor.io.StreamCallback

import java.nio.charset.StandardCharsets

def flowFile = session.get();
if (flowFile == null) {
    return;
}
def appKey = context.newPropertyValue('${appKey}').evaluateAttributeExpressions().getValue()
def appId = context.newPropertyValue('${appId}').evaluateAttributeExpressions().getValue()
def appSecret = context.newPropertyValue('${appSecret}').evaluateAttributeExpressions().getValue()
def querys = ''
try {

    flowFile = session.write(flowFile,
            { inputStream, outputStream ->
                def text = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
                TreeMap<String, String> params = new TreeMap<String, String>();
                params.put("dealerNo",flowFile.getAttribute("http.param.dealerNo"));
                params.put("timestamp", System.currentTimeMillis() + "");
                params.put("app_key", appKey);
                querys = params.toString()
                outputStream.write(text.getBytes(StandardCharsets.UTF_8))
            } as StreamCallback)
    flowFile = session.putAttribute(flowFile, "public.querys", querys)
    session.transfer(flowFile, REL_SUCCESS)
} catch (Exception e) {
    flowFile = session.putAttribute(flowFile, "public_param.error", e.getMessage())
    session.transfer(flowFile, REL_FAILURE)
}
