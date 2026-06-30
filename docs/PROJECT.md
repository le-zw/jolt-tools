# Jolt-Tools 项目文档

## 1. 项目概述

**项目名称**: Jolt-Tools (DevTools)
**项目类型**: JavaFX 桌面应用程序
**主要功能**: 用于本地测试 Apache NiFi 的 JOLT 转换和 Groovy Script 工具

## 2. 技术栈

| 类别 | 技术/版本 |
|------|-----------|
| UI 框架 | JavaFX 8 |
| UI 组件库 | JFXTextField 8.0.10, FontAwesomeFX 8.9 |
| 代码编辑器 | CodeMirror 5.x (嵌入 WebView) |
| JOLT 转换 | jolt-core 0.1.0, json-utils 0.1.0 |
| 脚本引擎 | Groovy 2.5.4, JavaScript, Python, Ruby, Lua, Clojure |
| NiFi 集成 | nifi-api 1.7.1, nifi-expression-language 1.7.1, nifi-standard-utils 1.7.1 |
| 构建工具 | Maven 3 |
| JDK 要求 | Java 8 |

## 3. 项目结构

```
jolt-tools/
├── pom.xml                          # Maven 配置
├── readme.md                        # 项目说明
└── src/
    ├── main/
    │   ├── java/com/lezw/
    │   │   ├── App.java             # JavaFX 应用入口
    │   │   ├── controller/          # FX控制器
    │   │   │   ├── Controller.java      # JOLT转换控制器
    │   │   │   └── ScriptController.java # 脚本执行控制器
    │   │   ├── transformjson/       # JOLT转换核心
    │   │   │   ├── TransformJSONResource.java  # 转换资源类
    │   │   │   └── dto/
    │   │   │       ├── JoltSpecificationDTO.java  # 转换规格DTO
    │   │   │       └── ValidationDTO.java         # 验证结果DTO
    │   │   ├── codeeditor/          # 代码编辑器
    │   │   │   ├── CodeEditor.java          # 编辑器接口
    │   │   │   ├── CodeMirrorEditor.java    # CodeMirror实现
    │   │   │   └── JsString.java            # JS字符串工具
    │   │   ├── script/              # 脚本执行相关
    │   │   │   ├── ExecuteScript.java
    │   │   │   ├── ScriptEngineConfigurator.java
    │   │   │   └── impl/            # 各种脚本引擎配置器
    │   │   └── util/
    │   │       ├── Dialogs.java     # 对话框工具
    │   │       └── StringUtils.java # 字符串工具
    │   └── resources/
    │       ├── fxml/                # FXML 布局文件
    │       │   ├── main.fxml        # 主窗口(标签页)
    │       │   ├── sample.fxml      # JOLT工具页面
    │       │   └── script.fxml      # Script工具页面
    │       ├── css/                 # 样式文件
    │       ├── html/
    │       │   └── editor.html      # CodeMirror编辑器HTML
    │       ├── codemirror/          # CodeMirror 5.x 库
    │       ├── jolt/                # JOLT示例JSON文件
    │       └── script/              # 脚本示例文件
    └── test/                        # 单元测试
```

## 4. 功能模块

### 4.1 JOLT Tool (JOLT 转换工具)

**功能**:
- 支持 10 种 JOLT 转换类型:
  - Shift (移动/重命名)
  - Default (默认值)
  - Modify - Overwrite (覆盖修改)
  - Modify - Default (默认修改)
  - Modify - Define (定义修改)
  - Remove (删除)
  - Sort (排序)
  - Chain (链式转换)
  - Custom (自定义)
  - Cardinality (基数转换)

- 支持 NiFi 表达式语言 (Expression Language) 属性
- 支持设置 FlowFile 属性键值对
- JSON 格式化和校验
- 内置 12 个 JOLT 示例

**核心类**: `Controller.java`

### 4.2 Script Tool (脚本执行工具)

**功能**:
- 支持 6 种脚本语言:
  - Groovy (默认)
  - ECMAScript (JavaScript)
  - Python
  - Ruby
  - Lua
  - Clojure

- 支持两种 NiFi 处理器:
  - ExecuteScript
  - ExecuteGroovyScript

- 支持脚本文件和模块目录配置
- 支持设置 FlowFile 属性
- 内置 25+ 个脚本示例

**核心类**: `ScriptController.java`

## 5. 核心代码流程

### 5.1 JOLT 转换流程

```
用户输入 → Controller.transform()
  ↓
JoltSpecificationDTO (包含 transform, specification, input, attributes)
  ↓
TransformJSONResource.getTransformation()
  ↓
NiFi TransformFactory 创建 JoltTransform
  ↓
TransformUtils.transform() 执行转换
  ↓
输出结果到 CodeMirror 编辑器
```

### 5.2 脚本执行流程

```
用户输入 → ScriptController.transform()
  ↓
选择处理器 (ExecuteScript / ExecuteGroovyScript)
  ↓
TestRunner 创建测试运行环境
  ↓
设置脚本内容/文件路径/模块路径
  ↓
runner.enqueue() 输入 FlowFile
  ↓
runner.run() 执行脚本
  ↓
outputFlowFilesForRelationship() 输出结果
```

## 6. UI 布局

### 主窗口 (main.fxml)
- JFXTabPane 包含两个 Tab:
  - "Jolt Tool" → sample.fxml
  - "Script Tool" → script.fxml

### JOLT 工具页面 (sample.fxml)
- 垂直 SplitPane:
  - 上半部分: JOLT Spec 编辑器 + DSL选择 + 示例选择
  - 下半部分: 水平 SplitPane
    - 左侧: JSON Input 编辑器
    - 右侧: JSON Output 编辑器

### Script 工具页面 (script.fxml)
- 垂直 SplitPane:
  - 上半部分: 三列布局
    - 左侧: FlowFile Input 编辑器 + 处理器选择
    - 中间: Script Body 编辑器 + 脚本类型/示例选择
    - 右侧: 脚本文件路径 + 模块目录 + 属性按钮 + 运行按钮
  - 下半部分: Output 编辑器

## 7. 快捷键

- `Ctrl + N`: 打开新的主窗口实例

## 8. 打包配置

- 默认打包为 macOS dmg 文件
- 可修改 pom.xml 改为 Windows exe
