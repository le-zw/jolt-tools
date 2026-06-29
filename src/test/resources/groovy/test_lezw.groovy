def flowFileList = session.get(2)
if(!flowFileList.isEmpty()) {
    def flowFile_success=session.create()
    //合并流文件
    session.merge(flowFileList,flowFile_success)
    //必须先移除原始flowFile，才能transfer创建的flowFile
    session.remove(flowFileList)
    session.transfer(flowFile_success, REL_SUCCESS)
}