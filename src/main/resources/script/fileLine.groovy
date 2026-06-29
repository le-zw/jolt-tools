import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import org.apache.nifi.logging.ComponentLog
import encoders.*

// 按行读取文件指定区间的字符集合，最后拼接成 JSON 返回

def flowFile = session.get()
if (flowFile == null) {
    return
}
def pricefile = flowFile.getAttribute('pricefile')
def descfile = flowFile.getAttribute('descfile')
def descCNList = []

try {
    new File(descfile).eachLine {
        text -> FindMatchLine: {
            def itemDescriptionV = ''
            if (text.length() < 54) {
                return
            }else if (text.length() < 92 && text.length() > 54) {
                itemDescriptionV = text[54..text.length() - 1]
                log.warn("this line is shorter than 92 bit: ${text}")
            }else {
                itemDescriptionV = text[54..91]
            }
            descCNList.add(itemDescriptionV)
        }
    }
}catch (FileNotFoundException e) {
    flowFile = session.putAttribute(flowFile, 'msg', 'no file descfile for requested date')
    log.error("descfile FileNotFound: ${ e }")
    session.transfer(flowFile, REL_FAILURE)
    session.commit()
}catch (Exception e) {
    flowFile = session.putAttribute(flowFile, 'msg', 'descfile parse field: ' + e.getMessage())
    log.error('Something went wrong', e)
    session.transfer(flowFile, REL_FAILURE)
    session.commit()
}

try {
    def matchedList = []

    new File(pricefile).eachLine {
text,lineNumber -> FindMatchLine: {
            def divisionV = text[97..98]
            if (divisionV in ['10', '11', '12']) {
                def itemNoV = text[0..17]
                def descriptionCN = descCNList.get(lineNumber - 1).trim()
                def descriptionEN = text[18..57]
                def assortment = text[76..77]
                def sap_MSRP_incl_VAT = text[112..124]
                def sap_MSRP_valid_from = text[159..166]

                def builder = new groovy.json.JsonBuilder()

                def map = new HashMap<>()
                map.put('partsNo', itemNoV.trim())
                map.put('descriptionCN', descriptionCN.trim())
                map.put('descriptionEN', descriptionEN.trim())
                map.put('division', divisionV.trim())
                map.put('assortment', assortment.trim())
                def vat = Integer.parseInt(sap_MSRP_incl_VAT.trim()).toString()
                if (vat.length() >= 3) {
                    vat = vat[0..-3] + '.' + vat[-3, -1]
    }else {
                    vat = (Integer.parseInt(vat) == 0) ? vat : ('0.' + vat)
                }
                map.put('sap_MSRP_incl_VAT', vat)
                map.put('sap_MSRP_valid_from', sap_MSRP_valid_from.trim())

                // println(matchedLine);
                matchedList.add(map)
            }
            log.error('lineNumber: ' + lineNumber)
}
    }
    def output = JsonOutput.toJson(matchedList)

    flowFile = session.write(flowFile, { outputStream ->
        outputStream.write(output.toString().getBytes('utf-8'))
} as OutputStreamCallback)

    session.transfer(flowFile, REL_SUCCESS)
    session.commit()
}catch (FileNotFoundException e) {
    flowFile = session.putAttribute(flowFile, 'msg', 'no file pricefile for requested date')
    log.error("pricefile FileNotFound: ${ e }")
    session.transfer(flowFile, REL_FAILURE)
    session.commit()
}catch (Exception e) {
    log.error('Something went wrong', e)
    session.transfer(flowFile, REL_FAILURE)
    session.commit()
}
