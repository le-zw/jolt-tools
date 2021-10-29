![下载 (5)](https://gitee.com/lezww/le.zw/raw/master/img/下载 (5).png)

# Jolt-Tools 

基于 NiFi-1.7 版本开发的 Jolt 工具，默认打包成 Mac APP，如果需要 Windows 版本 ，可自行修改POM文件。软件界面如下图（支持定义 NiFi 流文件属性和 NiFi 表达式）:

![image-20211029175643500](https://gitee.com/lezww/le.zw/raw/master/img/image-20211029175643500.png)

![image-20211029175708136](https://gitee.com/lezww/le.zw/raw/master/img/image-20211029175708136.png)

![image-20211029175723742](https://gitee.com/lezww/le.zw/raw/master/img/image-20211029175723742.png)

![image-20211029175911011](https://gitee.com/lezww/le.zw/raw/master/img/image-20211029175911011.png)

## 开发测试

### 简介

* Java FX 8 + NiFi 1.7 (依赖 nifi-expression-language 和 nifi-standard-utils) + JOLT 0.1.0

### 目录结构

```text
jolt-tools                      root
	└── src
      └── main
          ├── java
          │   └── com
          │       └── lezw
          │           ├── controller				javafx controller
          │           ├── transformjson			jolt transform class
          │           └── util							utils
          └── resources
              ├── css												fxml css
              ├── fxml											javfx fxml
              └── image											app icon and background images
        |_ ...                  						other modules, to be extended
	|_ pom                  									root pom
```

### 安装准备

* Java 8
* Maven 3

### 打包

```bash
jolt-tools> mvn install
jolt-tools> mvn jfx:native
```

### 说明

```bash
# 默认打包为 Mac 版本，打包后文件位于
${project.build.directory}/native

# 若需要打包 Windows 版本，修改POM javafx-maven-plugin配置即可：
<plugin>
                <groupId>com.zenjava</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>8.8.3</version>
                <configuration>
                    <!-- 指明javafx的入口类 -->
                    <mainClass>com.lezw.App</mainClass>
                    <!-- 在MAC系统进行打包,所以这里是dmg文件 -->
                    <bundler>dmg</bundler>
                    <!-- 指明打包后文件的存储位置 -->
                    <jfxAppOutputDir>${project.build.directory}/app</jfxAppOutputDir>
                    <nativeOutputDir>${project.build.directory}/native</nativeOutputDir>
                    <appName>Jolt Tools</appName>
                    <bundleArguments>
                        <icon>${project.basedir}/src/main/resources/image/icon.icns</icon>
                    </bundleArguments>
                    <vendor>le.zw@qq.com</vendor>
                </configuration>
</plugin>
```
