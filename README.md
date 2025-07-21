# 专利语义解析工具
## 核心功能
这是一个基于deepseek大模型的专利分析工具，自动将MySQL数据库的专利数据提取关键技术创新点，整理成相关数据表，并输出数量最多的top20热词。
## 使用说明
### 使用环境
jdk 11+

MySQL 8.0+数据库

### 基本配置
修改 ``src/main/resources/application.yml``
````
database:
  url: "jdbc:mysql://您的IP:3306/数据库名"
  username: "您的用户名"
  password: "您的密码"
  patentTable: "专利表名" 

deepseek:
  apiKey: "您的API密钥"
````
### 输出
#### 1.两张表
关键词整理：存储原始分析结果，包含id、ipc分类号、专利名称、技术关键词

关键词整理_stats：将全部关键词拆分成单个词并统计数量
#### 2.控制台输出
控制台输出大模型分析进度和top20关键词

#### 配套前端后续开发。
