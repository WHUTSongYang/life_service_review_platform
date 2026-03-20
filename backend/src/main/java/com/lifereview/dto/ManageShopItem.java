// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/** 可管理店铺展示项：管理端店铺列表 */
@Data
@Builder
public class ManageShopItem {
    // 字段说明：店铺主键 ID
    private Long id;
    // 字段说明：店铺名称
    private String name;
    // 字段说明：店铺类型
    private String type;
}
