/**
 * 时区转换
 * FlowFile Context：2022/01/05 00:00:00
 * FlowFile Attribute：Null
 * @author zhongwei.long
 * @date 2021年08月31日 上午11:24
 */
var flowFile = session.get();

if (flowFile !== null) {

    var StreamCallback = Java.type("org.apache.nifi.processor.io.StreamCallback");
    var IOUtils = Java.type("org.apache.commons.io.IOUtils");
    var StandardCharsets = Java.type("java.nio.charset.StandardCharsets");
    var SimpleDateFormat = Java.type("java.text.SimpleDateFormat")
    var TimeZone = Java.type("java.util.TimeZone")

    flowFile = session.write(flowFile, new StreamCallback(function(inputStream, outputStream) {

            var inputDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var utcZone = TimeZone.getTimeZone("UTC")
            inputDateFormat.setTimeZone(utcZone)

            var outputDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var outputZone = TimeZone.getTimeZone("America/New_York")
            outputDateFormat.setTimeZone(outputZone)

            var inputDateTime = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            var utcDate = inputDateFormat.parse(inputDateTime)
            var outputDateTime = outputDateFormat.format(utcDate)
            IOUtils.write(outputDateTime, outputStream, StandardCharsets.UTF_8)
    }));

    session.transfer(flowFile, REL_SUCCESS);
}
