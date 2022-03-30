flowFile = session.create()
session.transfer(flowFile, REL_FAILURE)