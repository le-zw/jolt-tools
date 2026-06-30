package com.lezw.db;

import com.bazaarvoice.jolt.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLite数据库工具类
 */
public class DatabaseUtil {
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + ".jolt-tools" + File.separator + "samples.db";
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".jolt-tools";

    private static Connection connection;

    static {
        initDatabase();
    }

    /**
     * 初始化数据库连接和表结构
     */
    private static void initDatabase() {
        try {
            // 确保目录存在
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 连接数据库
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            connection.setAutoCommit(true);

            // 创建表
            createTable();

            // 初始化内置样例
            initBuiltinSamples();
        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建样例表
     */
    private static void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS samples (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name VARCHAR(255) NOT NULL, " +
                "type VARCHAR(50) NOT NULL, " +
                "input_content TEXT, " +
                "spec_content TEXT, " +
                "script_type VARCHAR(50), " +
                "dsl VARCHAR(50), " +
                "processor VARCHAR(50), " +
                "attributes TEXT, " +
                "is_builtin INTEGER DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        Statement stmt = connection.createStatement();
        stmt.execute(sql);

        // 数据库升级：添加新字段
        upgradeDatabase(stmt);
        stmt.close();
    }

    /**
     * 数据库升级
     */
    private static void upgradeDatabase(Statement stmt) throws SQLException {
        try {
            stmt.execute("ALTER TABLE samples ADD COLUMN dsl VARCHAR(50)");
        } catch (SQLException e) {
            // 字段已存在，忽略
        }
        try {
            stmt.execute("ALTER TABLE samples ADD COLUMN processor VARCHAR(50)");
        } catch (SQLException e) {
            // 字段已存在，忽略
        }
    }

    /**
     * 初始化内置样例
     */
    private static void initBuiltinSamples() throws SQLException {
        // 检查是否已有内置样例
        String checkSql = "SELECT COUNT(*) FROM samples WHERE is_builtin = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // 已有内置样例，跳过
            }
        }

        // 初始化Jolt内置样例
        initJoltBuiltinSamples();

        // 初始化Script内置样例
        initScriptBuiltinSamples();
    }

    /**
     * 初始化Jolt内置样例
     */
    private static void initJoltBuiltinSamples() throws SQLException {
        String[] joltFiles = {
                "JSON-key加前缀", "JSON对象原样输出", "JSON数组原样输出",
                "JSON对象新增字段", "JSON数组新增字段", "JSON对象替换value",
                "JSON数组替换key", "JSON对象转数组", "JSON提取value部分值",
                "JSON过滤(QueryRecord)", "String函数示例", "复杂数组转简单数组",
                "JSON提取对象中数组"
        };

        for (String fileName : joltFiles) {
            try {
                String path = "/jolt/" + fileName + ".json";
                Map<String, Object> data = JsonUtils.classpathToMap(path);

                String inputContent = JsonUtils.toPrettyJsonString(data.get("input"));
                String specContent = JsonUtils.toPrettyJsonString(data.get("spec"));
                String attributes = data.get("attributes") != null ?
                        JsonUtils.toPrettyJsonString(data.get("attributes")) : null;

                String sql = "INSERT INTO samples (name, type, input_content, spec_content, attributes, is_builtin) VALUES (?, ?, ?, ?, ?, 1)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, fileName);
                    ps.setString(2, "jolt");
                    ps.setString(3, inputContent);
                    ps.setString(4, specContent);
                    ps.setString(5, attributes);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("加载内置样例失败: " + fileName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 初始化Script内置样例
     */
    private static void initScriptBuiltinSamples() throws SQLException {
        String[] scriptFiles = {
                "attributes.groovy", "attributes.js", "attributes.py",
                "datetime.groovy", "datetime.js", "json2json.groovy", "json2json.py",
                "log.groovy", "log.js", "log.py", "padleft.groovy", "split.js", "split.py",
                "state.groovy", "state.js", "state.py", "transform.groovy", "transform.js", "transform.py",
                "xmlToJson.groovy", "convertTimestampToDate.groovy", "fileLine.groovy",
                "groupVariables.groovy", "json_to_Attribute.groovy", "jsonbuilder.groovy"
        };

        for (String fileName : scriptFiles) {
            try {
                String path = "/script/" + fileName;
                String specContent = getResourceContent(path);
                String scriptType = getScriptType(fileName);

                String sql = "INSERT INTO samples (name, type, spec_content, script_type, is_builtin) VALUES (?, ?, ?, ?, 1)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, fileName);
                    ps.setString(2, "script");
                    ps.setString(3, specContent);
                    ps.setString(4, scriptType);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("加载内置样例失败: " + fileName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 获取资源文件内容
     */
    private static String getResourceContent(String path) throws Exception {
        try (java.io.InputStream is = DatabaseUtil.class.getResourceAsStream(path)) {
            if (is == null) {
                return "";
            }
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        }
    }

    /**
     * 根据文件名获取脚本类型
     */
    private static String getScriptType(String fileName) {
        String postfix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (postfix) {
            case "cljs": return "Clojure";
            case "js": return "ECMAScript";
            case "groovy": return "Groovy";
            case "lua": return "lua";
            case "py": return "python";
            case "rb": return "ruby";
            default: return "Groovy";
        }
    }

    /**
     * 获取所有样例
     */
    public static List<Sample> getAllSamples(String type) {
        List<Sample> list = new ArrayList<>();
        String sql = "SELECT * FROM samples WHERE type = ? ORDER BY is_builtin DESC, id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(resultSetToSample(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询样例失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 根据ID获取样例
     */
    public static Sample getSampleById(Long id) {
        String sql = "SELECT * FROM samples WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToSample(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询样例失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 保存样例
     */
    public static long saveSample(Sample sample) {
        String sql = "INSERT INTO samples (name, type, input_content, spec_content, script_type, dsl, processor, attributes, is_builtin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sample.getName());
            ps.setString(2, sample.getType());
            ps.setString(3, sample.getInputContent());
            ps.setString(4, sample.getSpecContent());
            ps.setString(5, sample.getScriptType());
            ps.setString(6, sample.getDsl());
            ps.setString(7, sample.getProcessor());
            ps.setString(8, sample.getAttributes());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("保存样例失败: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * 删除样例
     */
    public static boolean deleteSample(Long id) {
        // 检查是否为内置样例
        String checkSql = "SELECT is_builtin FROM samples WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("is_builtin") == 1) {
                    throw new RuntimeException("内置样例不能删除");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("检查样例失败: " + e.getMessage(), e);
        }

        String sql = "DELETE FROM samples WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("删除样例失败: " + e.getMessage(), e);
        }
    }

    /**
     * ResultSet转换为Sample对象
     */
    private static Sample resultSetToSample(ResultSet rs) throws SQLException {
        Sample sample = new Sample();
        sample.setId(rs.getLong("id"));
        sample.setName(rs.getString("name"));
        sample.setType(rs.getString("type"));
        sample.setInputContent(rs.getString("input_content"));
        sample.setSpecContent(rs.getString("spec_content"));
        sample.setScriptType(rs.getString("script_type"));
        sample.setDsl(rs.getString("dsl"));
        sample.setProcessor(rs.getString("processor"));
        sample.setAttributes(rs.getString("attributes"));
        sample.setBuiltin(rs.getInt("is_builtin") == 1);
        sample.setCreatedAt(rs.getTimestamp("created_at") != null ?
                rs.getTimestamp("created_at").toLocalDateTime() : null);
        sample.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return sample;
    }

    /**
     * 关闭数据库连接
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库连接失败: " + e.getMessage());
        }
    }
}
