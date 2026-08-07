# Ali Coupon System - 性能压测报告

## 测试环境

| 项目 | 配置 |
|:---|:---|
| CPU | Intel i7-13700K (16C/24T) |
| 内存 | 32GB DDR5 |
| JVM 堆 | -Xms2g -Xmx2g -XX:+UseG1GC |
| Redis | 7.2-alpine (Docker, 2C/4G) |
| RocketMQ | 5.2.0 (Docker, 2C/2G) |
| MySQL | 8.0 (本地, 4C/8G) |
| 网络 | 内网 (localhost) |

## 压测场景

```xml
<!-- JMeter 混合场景 -->
线程组: 1000 线程, Ramp-up 60s, 循环 100 次
请求比例: 70% GET /api/settlement/coupons + 30% POST /api/coupon/{id}/lock + use
预热: 前 1000 次请求不计入结果
持续: 5 分钟
```

## 压测结果

| 指标 | 目标值 | 实际值 | 判定 |
|:---|:---|:---|:---|
| 结算查询 QPS | ≥7600 | ______ | ⬜ |
| 结算查询 P99 | ≤2ms | ______ms | ⬜ |
| 锁券成功率 | ≥99% | ______% | ⬜ |
| 错误率 | <0.01% | ______% | ⬜ |
| Full GC 次数 | 0 | ______ | ⬜ |
| Redis 连接池超时 | 0 | ______ | ⬜ |
| DB 连接池泄露 | 0 | ______ | ⬜ |

## 数据对账结果

| 检查项 | 结果 | 说明 |
|:---|:---|:---|
| ZSet vs DB 一致性 | ⬜ PASS / FAIL | ZCARD total = DB COUNT where status IN (1,2) |
| Redis 库存 vs DB 核销数 | ⬜ PASS / FAIL | DECR count = USE count |
| 僵尸锁 (lock_time > 10min) | ⬜ PASS / FAIL | COUNT where status=2 and lock_time old |
| 超卖检查 | ⬜ PASS / FAIL | COUNT per activity ≤ stock |

## GC 日志摘要

```
[粘贴 GC 日志关键行]
```

## 结论

- [ ] 系统达到 7600+ QPS，P99 ≤ 2ms
- [ ] 无数据不一致
- [ ] 无超卖、无僵尸锁
- [ ] 系统可通过生产级验收
