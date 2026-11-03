-- 单接口滑动窗口限流
-- KEYS[1] 限流键
-- ARGV[1] 当前时间戳（毫秒）
-- ARGV[2] 窗口长度（毫秒）
-- ARGV[3] 窗口内允许的最大请求数
-- ARGV[4] 本次请求的唯一标识

local key = KEYS[1]
local nowMs = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local requestId = ARGV[4]

-- 1. 清掉窗口外的历史记录
redis.call("ZREMRANGEBYSCORE", key, 0, nowMs - windowMs)

-- 2. 数一下窗口内还剩几次
local used = redis.call("ZCARD", key)

-- 3. 超限直接返回，不写入
if used >= limit then
    return 0
end

-- 4. 记下这次请求，并把整个集合的过期时间设为窗口长度
redis.call("ZADD", key, nowMs, requestId)
redis.call("PEXPIRE", key, windowMs)

return 1