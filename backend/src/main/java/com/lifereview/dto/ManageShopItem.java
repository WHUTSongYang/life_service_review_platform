package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 管理端可选中或可操作的店铺摘要，用于店铺列表等场景。
 */
@Data
@Builder
public class ManageShopItem {
    /** 店铺主键 ID */
    private Long id;
    /** 店铺名称 */
    private String name;
    /** 店铺类型或业态 */
    private String type;
}
