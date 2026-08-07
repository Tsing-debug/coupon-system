# Aspire  Coupon System — 高并发微服务优惠券分发与核销平台

基于 Spring Cloud Alibaba 构建，覆盖**热点券秒杀兑换**、**百万级 Excel 批量发券**、**P99 < 2ms 结算查询**以及 **Redisson 分布式锁 + 乐观锁双防并发核销** 的全链路优惠券中台系统。

> **项目状态**：✅ 核心工程已闭环。包含 6 个核心 REST API、4 组 RocketMQ 消费者、3 个定时/延迟任务，共计 61 个源文件。

---

## 📖 目录
- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [系统架构与数据流](#系统架构与数据流)
- [API 文档](#api-文档)
- [关键设计决策与面试防守话术](#关键设计决策与面试防守话术)
- [压测数据与性能基线](#压测数据与性能基线)
- [项目目录结构](#项目目录结构)

---

## 🚀 核心特性

| 模块 | 核心能力 | 技术亮点 |
| :--- | :--- | :--- |
| **热点券兑换** | 高并发秒杀/兑换，防止超卖 | **Redis Lua** 原子扣库存 + **本地消息表(Outbox Pattern)** + **RocketMQ** 异步落库，保证最终一致性 |
| **批量发券** | 支持 10 万行 Excel 导入 | **EasyExcel** 流式解析（O(1) 内存），500 条/批打包 MQ，**唯一索引**防重复提交 |
| **结算页查询** | 毫秒级拉取用户可用券 | **Redis ZSet** 过期索引 + **Pipeline** 批量读缓存 + **CompletableFuture** 并行计算优惠 |
| **锁券/核销** | 并发抢券/支付核销/退款 | **Redisson 看门狗**（分布式锁）+ **DB Version 乐观锁**（双防 ABA）+ **状态机**强制校验 |
| **自动过期** | 券到期自动释放/作废 | **RocketMQ 延迟消息**（30 分钟）自动扫描回滚，避免僵尸锁 |
| **数据对账** | 系统自愈与监控 | 每 10 分钟自动核对 **Redis ZSet/DB 一致性**、**库存超卖**、**僵尸锁** |

---

## 🛠 技术栈

| 类别 | 技术 | 版本/说明 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 3, Spring Cloud Alibaba | 2023.0.x / 2022.0.x |
| **服务治理** | Nacos (服务注册/配置) | 2.2.x |
| **网关** | Spring Cloud Gateway | 路由分发 |
| **数据库** | MySQL 8.0 | 分库分表 (ShardingSphere) |
| **分库分表** | Apache ShardingSphere-JDBC | 分片键：`shop_number` / `user_id`，禁止全表扫描 |
| **缓存/分布式锁** | Redis (Lettuce) + Redisson | Lua 原子脚本 / 看门狗自动续期 |
| **消息队列** | Apache RocketMQ | 异步解耦 / 延迟消息 / 消费幂等 |
| **ORM** | MyBatis-Plus 3.5.x | 乐观锁 / 批量插入 |
| **Excel 处理** | EasyExcel | 流式解析大文件 |
| **并发工具** | CompletableFuture + 自定义线程池 | 并行计算隔离 |
| **定时任务** | Spring `@Scheduled` + XXL-Job (预留) | 补偿重试/对账 |

---

## ⚡️ 快速开始

### 1. 前置环境
- JDK 17+
- Docker & Docker Compose
- Maven 3.8+

### 2. 启动基础中间件
```bash
cd /path/to/Ali
docker-compose up -d
```
此命令会启动：
- Redis (端口 6379, 密码: redis123)
- RocketMQ Namesrv (9876) + Broker (10911)

### 3. 初始化数据库
```bash
# 连接 MySQL (需提前创建库 ali_coupon)
mysql -h127.0.0.1 -uroot -p < src/main/resources/db/schema.sql
```
*（若需分库分表，请根据 `application-dev.yml` 配置多数据源）*

### 4. 修改配置
打开 `src/main/resources/application-dev.yml`，修改：
- `spring.datasource.url` (MySQL 地址)
- `spring.redis.host` (Redis 地址)
- `rocketmq.name-server` (RocketMQ 地址)

### 5. 编译与启动
```bash
mvn clean compile -DskipTests
mvn spring-boot:run
```

### 6. 初始化测试库存（兑换场景）
```bash
docker exec ali-redis redis-cli -a redis123 SET "stock:template:100" "100"
```

---

## 🧩 系统架构与数据流

### 热点兑换（Sprint 2）
```text
POST /exchange
  -> ① Redis Lua (原子扣库存 + 限频)
  -> ② INSERT outbox_task (本地消息表, status=0)
  -> ③ RocketMQ 发送 (失败不抛异常, 等待补偿)
  -> ④ Consumer: INSERT user_coupon (唯一索引防重)
  -> ⑤ UPDATE outbox_task status=1 (标记完成)
  -> ⑥ (补偿) XXL-Job 定时扫表重试失败消息
```

### 批量发券（Sprint 3）
```text
POST /batch/upload (Excel)
  -> ① EasyExcel 流式解析 (500 行/批)
  -> ② Redis SADD 幂等过滤（可选，设计上已下沉至 DB）
  -> ③ 打包成 1 条 MQ 消息 (500 用户/条)
  -> ④ Consumer: INSERT IGNORE user_coupon (逐条, 适配分片路由)
  -> ⑤ 原子更新 batch_job_record 计数器
```

### 高性能结算查询（Sprint 4.1）
```text
GET /settlement/coupons?userId=1
  -> ① ZRANGEBYSCORE zset:user:1 (当前时间戳 ~ +inf)
  -> ② Pipeline GET coupon:detail:{id} (批量拉缓存)
  -> ③ CompletableFuture 并行计算优惠金额
  -> ④ (异常降级) exceptionally -> 返回 BigDecimal.ZERO
```

### 锁券与核销（Sprint 4.2）
```text
POST /coupon/{id}/lock
  -> ① 状态机校验 PENDING -> LOCKED
  -> ② Redisson tryLock (等待3s, 租期30s, 看门狗续期)
  -> ③ UPDATE ... SET status=2, version=version+1 WHERE status=1 AND version=?
  -> ④ (超时保护) DB version 乐观锁拦截脏写

POST /coupon/{id}/use
  -> ① 状态机校验 LOCKED -> USED
  -> ② UPDATE ... SET status=3 WHERE status=2 AND user_id=?
  -> ③ DEL cache + ZSet 移除
```

---

## 📡 API 文档

| Method | Endpoint | 描述 | 核心参数 |
| :--- | :--- | :--- | :--- |
| POST | `/api/exchange` | 热点券兑换 | `userId`, `activityId`, `templateId` |
| POST | `/api/batch/upload` | 批量上传 Excel 发券 | `file` (Multipart), `templateId` |
| GET | `/api/settlement/coupons` | 用户结算页查询 | `userId` |
| POST | `/api/coupon/{id}/lock` | 锁券（下单时） | `userId` (Header/Body) |
| POST | `/api/coupon/{id}/use` | 核销券（支付成功） | `userId` |
| POST | `/api/coupon/{id}/refund` | 退款回滚 | `userId` |

---

## 🔥 关键设计决策与面试防守话术

### 1. 最终一致性：Redis 扣库存成功，但 MQ 挂了怎么办？
- **Outbox Pattern**：扣减成功先落 `outbox_task` 本地表，再发 MQ。发送失败不抛异常，接口返回“兑换中”。
- **补偿机制**：`@Scheduled` 每分钟扫描待发送消息，指数退避重试。
- **幂等兜底**：消费端利用 `uk_user_template_batch` 唯一索引，捕获 `DuplicateKeyException` 直接 ACK。

### 2. 并发锁券如何防止超卖？
- **Redisson 分布式锁**：防多节点并发争抢（看门狗自动续期）。
- **DB 乐观锁 (Version)**：`UPDATE ... WHERE status=1 AND version=?`。即使 Redisson 锁超时异常释放，DB 层仍拒绝脏写。
- **状态机前置校验**：非法状态（如 EXPIRED -> LOCKED）直接在 Service 层抛出 `IllegalStateException`。

### 3. 10万行 Excel 导入为何内存不爆？
- **EasyExcel 流式读取**：逐行回调，不持有全量数据。
- **分批刷盘**：每 500 条触发一次 MQ 投递，`userIdBuffer.clear()` 及时释放堆内存。
- **MQ 压缩**：500 个用户打包成 1 条 MQ 消息，10 万人仅产生 200 条消息，规避 `MessageTooBigException`。

### 4. 结算页 P99 2ms 如何达到？
- **ZSet 范围查询**：按过期时间戳过滤，时间复杂度 O(logN + M)。
- **Pipeline 批量读**：N 张券仅需 1 次网络往返。
- **异步并行计算**：`CompletableFuture` + 自定义线程池，10 张券并行计算耗时 ≈ 最慢单张耗时。
- **连接池调优**：Redis `max-active=1000`，启用 JMeter Keep-Alive，内网压测并预热 JVM。

---

## 📊 压测数据与性能基线

| 链路 | 请求量 | QPS | 错误率 | P99 延迟 | 环境说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **结算查询** | 80,000 | ~7,600 | 0% | 2 ms | 纯 Redis 读 (ZSet+Pipeline)，缓存完全命中 |
| **异步发券** | 40,000 | ~1,838 | 0% | 12 ms | HTTP 响应仅代表 MQ 接收成功 |
| **批量导入** | 10 万行 | - | 0% | < 10s | 内存占用 < 200MB |

> **注意**：兑券全链路（Lua+MQ+DB落库）因异步解耦设计，同步响应 QPS 约为 800~1000，符合最终一致性的预期取舍。

---

## 📁 项目目录结构

```
Ali/
├── docker-compose.yml                         # Redis + RocketMQ 容器化
├── pom.xml                                    # Spring Boot 3.2.5 依赖管理
├── src/main/java/com/ali/coupon/
│   ├── CouponApplication.java                 # 启动类 (@MapperScan)
│   ├── common/
│   │   ├── enums/CouponStatus.java            # 5种状态枚举
│   │   └── guard/StateMachineGuard.java       # 状态流转校验
│   ├── config/                                 # Redis/Redisson/线程池配置
│   ├── controller/                             # 6个核心 REST Controller
│   ├── service/                                # 兑换、查询、锁券、过期服务
│   ├── mq/                                     # 4组生产者/消费者 (兑换/批量/过期)
│   ├── job/                                    # 补偿重试 & 数据对账 & 索引清理
│   ├── mapper/                                 # MyBatis-Plus Mapper (含乐观锁)
│   └── dto/entity/                             # 数据传输对象与实体
├── src/main/resources/
│   ├── application-dev.yml                    # 分库分表配置 (禁止全表扫描)
│   ├── lua/exchange.lua                       # Redis 原子扣减脚本
│   └── db/schema.sql                          # 5张核心表 DDL
├── jmeter/
│   └── coupon_mixed_test.jmx                  # 混合压测脚本 (7:3读写)
└── docs/
    └── performance_report.md                   # 详细压测报告模板
```

---

## 🤝 后续优化方向 (TODO)
- [ ] 集成 SkyWalking 全链路追踪
- [ ] 引入 Guava BloomFilter 强化缓存穿透防御（目前基于空值缓存+双检锁）
- [ ] ShardingSphere 读写分离配置
- [ ] 对接 Prometheus + Grafana 可视化监控
