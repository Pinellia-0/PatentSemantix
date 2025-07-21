package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class DeepSeekClient {
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(String apiKey, String endpoint, String model,
                          int maxTokens, double temperature) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // 分析分类号获取关键词（优化提示词）
    public String analyzeClassification(String classification) throws Exception {
        // 优化提示词：要求返回单个关键词
        String prompt = "分析以下专利分类号，提取核心技术创新点和技术关键词。要求：\n" +
                "1. 每个关键词必须是独立的单个词或短语\n" +
                "2. 不要组成句子或复合词\n" +
                "3. 只输出逗号分隔的关键词列表\n" +
                "4. 示例格式：太阳能,光伏转换,可再生能源\n" +
                "5. 确保关键词简洁且具有代表性\n\n" +
                "分类号：" + classification;

        // 构建请求体
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", new Object[] {
                        Map.of("role", "user", "content", prompt)
                },
                "max_tokens", maxTokens,
                "temperature", temperature
        ));

        // 创建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        // 解析响应
        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode choices = rootNode.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                return content.replace("\n", "").trim(); // 清理格式
            }
            throw new Exception("API响应格式错误");
        } else {
            throw new Exception("API错误: " + response.statusCode() + " - " + response.body());
        }
    }
}
