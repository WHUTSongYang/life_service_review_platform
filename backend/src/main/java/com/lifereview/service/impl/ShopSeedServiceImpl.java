package com.lifereview.service.impl;

import com.lifereview.entity.Shop;
import com.lifereview.repository.ShopRepository;
import com.lifereview.service.ShopCategoryCacheService;
import com.lifereview.service.ShopGeoService;
import com.lifereview.service.ShopSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 店铺种子数据服务实现类。
 * 初始化示例店铺数据，供管理员快速搭建测试环境。
 */
@Service
@RequiredArgsConstructor
public class ShopSeedServiceImpl implements ShopSeedService {

    private final ShopRepository shopRepository;
    private final ShopGeoService shopGeoService;
    private final ShopCategoryCacheService shopCategoryCacheService;

    @Override
    @Transactional
    public SeedResult seedShops() {
        List<SeedShop> seedShops = List.of(
                new SeedShop("云水足疗", "足疗", "上海市静安区南京西路108号", 121.458732, 31.228921),
                new SeedShop("泰禾足道会馆", "足疗", "上海市徐汇区漕溪北路66号", 121.443281, 31.197665),
                new SeedShop("古法养生馆", "足疗", "上海市浦东新区张杨路520号", 121.523112, 31.234781),
                new SeedShop("小南国本帮菜", "美食", "上海市黄浦区淮海中路288号", 121.472846, 31.222917),
                new SeedShop("江边小馆", "美食", "上海市杨浦区长阳路819号", 121.540117, 31.261203),
                new SeedShop("海味食堂", "美食", "上海市虹口区四川北路1388号", 121.488501, 31.262849),
                new SeedShop("星河KTV", "娱乐", "上海市长宁区天山路789号", 121.390284, 31.217112),
                new SeedShop("乐次元电玩", "娱乐", "上海市闵行区莘庄地铁南广场", 121.381221, 31.112904),
                new SeedShop("奇妙桌游社", "娱乐", "上海市普陀区曹杨路1568号", 121.407332, 31.246119),
                new SeedShop("橙子酒店", "酒店", "上海市浦东新区世纪大道268号", 121.525691, 31.239571),
                new SeedShop("花园民宿", "酒店", "上海市宝山区友谊路99号", 121.488191, 31.404381),
                new SeedShop("云镜美发", "丽人", "上海市徐汇区衡山路42号", 121.448611, 31.207514),
                new SeedShop("轻氧健身", "运动", "上海市长宁区仙霞路350号", 121.395833, 31.212477),
                new SeedShop("观影小镇影城", "电影院", "上海市浦东新区金桥路1888号", 121.610733, 31.247295),
                new SeedShop("舒心推拿馆", "按摩", "上海市黄浦区福州路699号", 121.478939, 31.233109)
        );
        int created = 0;
        int skipped = 0;
        for (SeedShop seed : seedShops) {
            // 同名同址已存在则跳过
            if (shopRepository.existsByNameAndAddress(seed.name(), seed.address())) {
                skipped++;
                continue;
            }
            Shop shop = new Shop();
            shop.setName(seed.name());
            shop.setType(shopCategoryCacheService.validateAndNormalizeCategory(seed.type()));
            shop.setAddress(seed.address());
            shop.setLongitude(seed.longitude());
            shop.setLatitude(seed.latitude());
            Shop saved = shopRepository.save(shop);
            shopGeoService.syncShopLocation(saved);
            created++;
        }
        return new SeedResult(created, skipped, seedShops.size());
    }

    private record SeedShop(String name, String type, String address, Double longitude, Double latitude) {
    }
}
