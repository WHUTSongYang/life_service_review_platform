local stockKey = KEYS[1]
local buyerSetKey = KEYS[2]
local userId = ARGV[1]

local stock = redis.call('GET', stockKey)
if (not stock) then
    return -1
end
if (tonumber(stock) <= 0) then
    return 1
end
if (redis.call('SISMEMBER', buyerSetKey, userId) == 1) then
    return 2
end

redis.call('DECR', stockKey)
redis.call('SADD', buyerSetKey, userId)
return 0
