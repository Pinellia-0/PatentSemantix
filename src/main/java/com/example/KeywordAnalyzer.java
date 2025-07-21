package com.example;

import java.sql.ResultSet;
import java.sql.SQLException;

public class KeywordAnalyzer {
    private final DatabaseManager dbManager;        // 数据库管理器
    private final DeepSeekClient deepSeekClient;    // API客户端
    private final int batchSize;                    // API批量处理大小
    private final int pageSize;                     // 数据库分页大小

    public KeywordAnalyzer(DatabaseManager dbManager, DeepSeekClient deepSeekClient,
                           int batchSize, int pageSize) {
        this.dbManager = dbManager;
        this.deepSeekClient = deepSeekClient;
        this.batchSize = batchSize;
        this.pageSize = pageSize;
    }

    // 专利处理主流程
    public void processPatents(int maxPatents) throws Exception {
        int totalCount = dbManager.getPatentCount();
        int processed = 0;
        int apiCalls = 0;

        System.out.printf("📊 共发现 %d 项专利待处理%n", totalCount);
        System.out.printf("⏳ 开始处理 (最大处理: %d 项)%n", maxPatents);

        for (int offset = 0; offset < totalCount && processed < maxPatents; offset += pageSize) {
            int currentLimit = Math.min(pageSize, maxPatents - processed);

            try {
                System.out.printf("\n📖 读取专利数据 [%d-%d]...%n", offset, offset + currentLimit);
                try (ResultSet rs = dbManager.getPatentsByPage(offset, currentLimit)) {
                    while (rs.next() && processed < maxPatents) {
                        processed++;
                        apiCalls++;

                        // 获取专利数据
                        String patentName = rs.getString("专利名称");
                        String mainClass = rs.getString("主分类号");
                        String classes = rs.getString("分类号");

                        System.out.printf("\n🔍 处理专利 #%d: %s%n", offset + processed, patentName);
                        System.out.println("├─ 主分类号: " + mainClass);
                        System.out.println("└─ 分类号: " + truncateString(classes, 50));

                        // 调用DeepSeek API
                        System.out.println("🚀 调用DeepSeek API...");
                        long startTime = System.currentTimeMillis();
                        String keywords = deepSeekClient.analyzeClassification(classes);
                        long duration = System.currentTimeMillis() - startTime;

                        System.out.println("✅ 分析完成 (耗时: " + duration + "ms)");
                        System.out.println("📌 关键词: " + keywords);

                        // 保存到数据库（使用拆分单个词的方法）
                        dbManager.insertKeywords(patentName, mainClass, classes, keywords);
                        System.out.println("💾 数据已保存");

                        // API调用频率控制
                        if (apiCalls % batchSize == 0) {
                            System.out.printf("\n⏳ 已处理 %d/%d，等待10秒...%n", processed, maxPatents);
                            Thread.sleep(10000);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ 数据库错误: " + e.getMessage());
                System.out.println("🔄 重试当前分页...");
                offset -= pageSize; // 回退一页重试
                Thread.sleep(5000);
            } catch (Exception e) {
                System.err.println("⚠️ 处理错误: " + e.getMessage());
                System.out.println("⏸️ 跳过当前专利继续处理...");
            }

            // 显示进度
            double progress = (double) processed / Math.min(totalCount, maxPatents) * 100;
            System.out.printf("\n📈 进度: %d/%d (%.1f%%)%n",
                    processed, Math.min(totalCount, maxPatents), progress);
        }
        System.out.printf("\n🎉 完成! 共处理 %d/%d 项专利%n", processed, Math.min(totalCount, maxPatents));
    }

    // 字符串截断显示
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
