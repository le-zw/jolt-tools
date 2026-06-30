package com.lezw.db;

import java.time.LocalDateTime;

/**
 * 样例实体类
 */
public class Sample {
    private Long id;
    private String name;
    private String type;              // 'jolt' 或 'script'
    private String inputContent;      // 输入JSON（jolt用）
    private String specContent;       // Jolt Spec 或 Script内容
    private String scriptType;        // 脚本语言类型（script用: groovy/js/python等）
    private String dsl;               // Jolt DSL类型（jolt用）
    private String processor;         // Processor类型（script用）
    private String attributes;        // 扩展属性（JSON格式）
    private boolean builtin;          // 是否内置样例
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Sample() {}

    public Sample(String name, String type, String inputContent, String specContent,
                  String scriptType, String attributes, boolean builtin) {
        this.name = name;
        this.type = type;
        this.inputContent = inputContent;
        this.specContent = specContent;
        this.scriptType = scriptType;
        this.attributes = attributes;
        this.builtin = builtin;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getInputContent() { return inputContent; }
    public void setInputContent(String inputContent) { this.inputContent = inputContent; }

    public String getSpecContent() { return specContent; }
    public void setSpecContent(String specContent) { this.specContent = specContent; }

    public String getScriptType() { return scriptType; }
    public void setScriptType(String scriptType) { this.scriptType = scriptType; }

    public String getDsl() { return dsl; }
    public void setDsl(String dsl) { this.dsl = dsl; }

    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
