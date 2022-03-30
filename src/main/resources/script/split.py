'''
  将JSON数组分割成多个流文件，兼容fragment合并
  FlowFile Context：
    [
       {"color":"b"},
       {"color":"d"}
    ]
  FlowFile Attribute：Null
  @author zhongwei.long
  @date 2021年08月31日 上午11:24
'''


import json
import sys
import traceback
from java.nio.charset import StandardCharsets
from org.apache.commons.io import IOUtils
from org.apache.nifi.processor.io import InputStreamCallback, OutputStreamCallback
from org.python.core.util import StringUtil


class WriteCallback(OutputStreamCallback):
    def __init__(self):
        self.content = None
        self.charset = StandardCharsets.UTF_8

    def process(self, outputStream):
        bytes = bytearray(self.content.encode('utf-8'))
        outputStream.write(bytes)


class SplitCallback(InputStreamCallback):
    def __init__(self):
        self.parentFlowFile = None

    def process(self, inputStream):
        try:
            # Read input FlowFile content
            input_text = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            input_list = json.loads(input_text)

            # Create FlowFiles for array items
            splits = []
            fragment_index = 0
            for item in input_list:
                color = item['color']
                splitFlowFile = session.create(self.parentFlowFile)
                writeCallback = WriteCallback()
                writeCallback.content = json.dumps(item)
                splitFlowFile = session.write(splitFlowFile, writeCallback)
                splitFlowFile = session.putAllAttributes(splitFlowFile, {
                    'fragment.index': fragment_index,
                    'color': color
                })
                splits.append(splitFlowFile)
                log.info(color)
                fragment_index += 1

            for splitFlowFile in splits:
                session.transfer(splitFlowFile, REL_SUCCESS)
        except:
            traceback.print_exc(file=sys.stdout)
            raise


parentFlowFile = session.get()
if parentFlowFile != None:
    splitCallback = SplitCallback()
    splitCallback.parentFlowFile = parentFlowFile
    session.read(parentFlowFile, splitCallback)
    session.remove(parentFlowFile)
