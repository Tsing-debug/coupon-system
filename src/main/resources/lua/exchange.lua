-- KEYS[1]: 模板库存 key, 如 "stock:template:{templateId}"
-- KEYS[2]: 用户活动参与记录 key, 如 "user_act:{userId}:{activityId}"
-- ARGV[1]: 用户ID (用于存入 Set 记录)
-- 返回值: 1=兑换成功, -1=库存不足, -2=重复领取

local stock = redis.call('GET', KEYS[1])
if not stock or tonumber(stock) <= 0 then
    return -1
end

local alreadyExists = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if alreadyExists == 1 then
    return -2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
