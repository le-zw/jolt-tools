import org.apache.commons.io.IOUtils
import org.apache.nifi.processor.io.StreamCallback
import java.nio.charset.StandardCharsets

// 脚本实现Jolt JSON转换功能
// FlowFile Input：
/*
{
    "rating": {
        "primary": {
            "value": 3
        },
        "quality": {
            "value": 3
        },
        "metric": {
            "value": 6
        }
    }
 }
*/

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
                'Range' 5
                'Rating' "${obj.rating.primary.value}"
                'SecondaryRatings' {
                    obj.rating.findAll {it.key != "primary"}.each {k,v ->
                        "$k" {
                            'Id' "$k"
                            'Range' 5
                            'Value' v.value
                        }
                    }
                }
            }
            outputStream.write(builder.toPrettyString().getBytes(StandardCharsets.UTF_8))
        } as StreamCallback)
flowFile = session.putAttribute(flowFile, "filename", flowFile.getAttribute('filename').tokenize('.')[0]+'_translated.json')
session.transfer(flowFile, REL_SUCCESS)
