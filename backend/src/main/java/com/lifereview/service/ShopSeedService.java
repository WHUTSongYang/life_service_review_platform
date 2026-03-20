package com.lifereview.service;

/**
 * 店铺种子数据服务接口。
 * 初始化分类与示例店铺，供管理员快速搭建测试数据。
 */
public interface ShopSeedService {

    // 执行种子数据初始化，返回创建数、跳过数、总数
    SeedResult seedShops();

    // 种子执行结果：新建数、跳过数、总数
    record SeedResult(int created, int skipped, int total) {
    }
}
