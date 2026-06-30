package com.lezw.db;

import java.util.List;

/**
 * 样例业务服务类
 */
public class SampleService {

    /**
     * 获取所有Jolt样例
     */
    public static List<Sample> getAllJoltSamples() {
        return DatabaseUtil.getAllSamples("jolt");
    }

    /**
     * 获取所有Script样例
     */
    public static List<Sample> getAllScriptSamples() {
        return DatabaseUtil.getAllSamples("script");
    }

    /**
     * 根据ID获取样例
     */
    public static Sample getSampleById(Long id) {
        return DatabaseUtil.getSampleById(id);
    }

    /**
     * 保存Jolt样例
     */
    public static long saveJoltSample(String name, String inputContent, String specContent, String dsl, String attributes) {
        Sample sample = new Sample();
        sample.setName(name);
        sample.setType("jolt");
        sample.setInputContent(inputContent);
        sample.setSpecContent(specContent);
        sample.setDsl(dsl);
        sample.setAttributes(attributes);
        sample.setBuiltin(false);
        return DatabaseUtil.saveSample(sample);
    }

    /**
     * 保存Script样例
     */
    public static long saveScriptSample(String name, String specContent, String scriptType, String processor, String inputContent) {
        Sample sample = new Sample();
        sample.setName(name);
        sample.setType("script");
        sample.setInputContent(inputContent);
        sample.setSpecContent(specContent);
        sample.setScriptType(scriptType);
        sample.setProcessor(processor);
        sample.setBuiltin(false);
        return DatabaseUtil.saveSample(sample);
    }

    /**
     * 删除样例
     */
    public static boolean deleteSample(Long id) {
        return DatabaseUtil.deleteSample(id);
    }

    /**
     * 检查样例是否为内置样例
     */
    public static boolean isBuiltin(Long id) {
        Sample sample = DatabaseUtil.getSampleById(id);
        return sample != null && sample.isBuiltin();
    }
}
