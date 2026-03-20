local stockKey = KEYS[1]
local buyerSetKey = KEYS[2]
local userId = ARGV[1]

if (redis.call('SISMEMBER', buyerSetKey, userId) == 1) then
    redis.call('SREM', buyerSetKey, userId)
    redis.call('INCR', stockKey)
    return 1
end
return 0
