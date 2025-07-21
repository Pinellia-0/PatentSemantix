package com.example;

import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PatentAnalysis {
    public static void main(String[] args) {
        final int MAX_RETRIES = 5;
        int retryCount = 0;
        boolean success = false;

        while (retryCount < MAX_RETRIES && !success) {
            try {
                // 1. 加载配置
                Yaml yaml = new Yaml();
                InputStream in = Files.newInputStream(Paths.get("src/main/resources/application.yml"));
                Map<String, Object> config = yaml.load(in);

                Map<String, String> dbConfig = (Map<String, String>) config.get("database");
                Map<String, Object> deepseekConfig = (Map<String, Object>) config.get("deepseek");
                Map<String, Integer> analysisConfig = (Map<String, Integer>) config.get("analysis");

                // 2. 初始化组件
                DatabaseManager dbManager = new DatabaseManager(
                        dbConfig.get("url"),
                        dbConfig.get("username"),
                        dbConfig.get("password"),
                        dbConfig.get("patentTable"),
                        dbConfig.get("keywordTable")
                );

                DeepSeekClient deepSeekClient = new DeepSeekClient(
                        (String) deepseekConfig.get("apiKey"),
                        (String) deepseekConfig.get("endpoint"),
                        (String) deepseekConfig.get("model"),
                        (Integer) deepseekConfig.get("maxTokens"),
                        (Double) deepseekConfig.get("temperature")
                );

                KeywordAnalyzer analyzer = new KeywordAnalyzer(
                        dbManager,
                        deepSeekClient,
                        analysisConfig.get("batchSize"),
                        analysisConfig.get("pageSize")
                );

                // 3. 创建关键词表（使用新方法）
                dbManager.createKeywordTables();

                // 4. 处理专利数据
                analyzer.processPatents(analysisConfig.get("maxPatents"));

                // 5. 输出关键词统计
                dbManager.printTopKeywords(20);

                // 6. 清理资源
                dbManager.close();

                success = true;
                System.out.println("\n✅ 程序执行成功");
            } catch (Exception e) {
                retryCount++;
                System.err.println("⚠️ 程序执行错误 (" + retryCount + "/" + MAX_RETRIES + "): " + e.getMessage());
                e.printStackTrace();

                try {
                    int delay = 5000 * retryCount;
                    System.out.println("⏳ 等待 " + delay/1000 + " 秒后重试...");
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!success) {
            System.err.println("\n❌ 程序失败，达到最大重试次数");
            System.exit(1);
        }
    }
}
