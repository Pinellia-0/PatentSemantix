package com.example;

import java.sql.*;
import java.util.Map;

public class DatabaseManager {
    private Connection connection;
    private final String url;
    private final String username;
    private final String password;
    private final String patentTable;
    private final String keywordTable;

    public DatabaseManager(String url, String username, String password,
                           String patentTable, String keywordTable) throws SQLException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.patentTable = patentTable;
        this.keywordTable = keywordTable;
        reconnect();
    }

    // 重连数据库
    public void reconnect() throws SQLException {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
        this.connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(true);
        System.out.println("✅ 数据库连接成功");
    }

    // 检查连接是否有效
    public boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    // 创建关键词表
    public void createKeywordTables() throws SQLException {
        // 原始结果表
        String resultTableSql = "CREATE TABLE IF NOT EXISTS " + keywordTable + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "patent_name VARCHAR(255) NOT NULL, " +
                "main_class VARCHAR(50), " +
                "classes TEXT, " +
                "keywords TEXT" +
                ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        // 关键词统计表
        String statsTableSql = "CREATE TABLE IF NOT EXISTS " + keywordTable + "_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "keyword VARCHAR(100) NOT NULL, " +
                "count INT DEFAULT 0, " +
                "UNIQUE KEY unique_keyword (keyword)" +
                ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(resultTableSql);
            stmt.execute(statsTableSql);
            System.out.println("✅ 关键词表创建成功");
        }
    }

    // 获取专利总数
    public int getPatentCount() throws SQLException {
        checkConnection();
        String sql = "SELECT COUNT(*) FROM " + patentTable;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // 分页获取专利数据
    public ResultSet getPatentsByPage(int offset, int limit) throws SQLException {
        checkConnection();
        String sql = "SELECT 专利名称, 主分类号, 分类号 FROM " + patentTable + " LIMIT ? OFFSET ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, limit);
        stmt.setInt(2, offset);
        return stmt.executeQuery();
    }

    // 插入关键词记录（拆分为单个词）
    public void insertKeywords(String patentName, String mainClass, String classes, String keywords) throws SQLException {
        checkConnection();

        // 1. 插入原始记录
        String sql = "INSERT INTO " + keywordTable + " (patent_name, main_class, classes, keywords) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, patentName);
            stmt.setString(2, mainClass);
            stmt.setString(3, classes);
            stmt.setString(4, keywords);
            stmt.executeUpdate();
        }

        // 2. 拆分并统计单个关键词
        String[] words = keywords.split(",");
        for (String word : words) {
            String cleanedWord = word.trim().toLowerCase();
            if (!cleanedWord.isEmpty()) {
                updateKeywordStats(cleanedWord);
            }
        }
    }

    // 更新关键词统计
    private void updateKeywordStats(String keyword) throws SQLException {
        String tableName = keywordTable + "_stats";

        // 尝试更新现有记录
        String updateSql = "UPDATE " + tableName + " SET count = count + 1 WHERE keyword = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, keyword);
            int updated = stmt.executeUpdate();

            // 如果没有更新任何记录，则插入新记录
            if (updated == 0) {
                String insertSql = "INSERT INTO " + tableName + " (keyword, count) VALUES (?, 1)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, keyword);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    // 获取关键词统计（单个词）
    public void printTopKeywords(int topN) throws SQLException {
        checkConnection();
        String tableName = keywordTable + "_stats";
        String sql = "SELECT keyword, count FROM " + tableName +
                " ORDER BY count DESC, keyword ASC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, topN);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n🔝 Top " + topN + " 关键词:");
            System.out.println("--------------------------");
            System.out.println("关键词\t\t数量");
            System.out.println("--------------------------");
            while (rs.next()) {
                String keyword = rs.getString("keyword");
                int count = rs.getInt("count");
                System.out.printf("%-20s %d%n", keyword, count);
            }
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    // 检查连接状态
    private void checkConnection() throws SQLException {
        if (!isConnectionValid()) {
            System.out.println("⚠️ 连接失效，尝试重连...");
            reconnect();
        }
    }
}
