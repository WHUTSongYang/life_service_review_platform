package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ShopApplyCreateRequest;
import com.lifereview.dto.ShopApplyItem;
import com.lifereview.service.ShopApplyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家端店铺入驻申请控制器。
 * <p>URL 前缀：{@code /api/merchant/shops/apply}。提交与查询我的申请需普通用户登录。</p>
 */
@RestController
@RequestMapping("/api/merchant/shops/apply")
@RequiredArgsConstructor
public class MerchantShopApplyController {

    /** 店铺入驻申请业务服务 */
    private final ShopApplyService shopApplyService;

    /**
     * 提交一条新的店铺入驻申请。
     *
     * @param req     店铺名称、资质说明等
     * @param request 当前 HTTP 请求
     * @return 创建后的申请详情
     * @throws IllegalArgumentException 未登录或业务校验失败
     */
    @PostMapping
    public ApiResponse<ShopApplyItem> submitApply(@Valid @RequestBody ShopApplyCreateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.submitApply(currentUserId(request), req));
    }

    /**
     * 查询当前登录用户提交过的所有入驻申请。
     *
     * @param request 当前 HTTP 请求
     * @return 申请列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/mine")
    public ApiResponse<List<ShopApplyItem>> listMine(HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.listMine(currentUserId(request)));
    }

    /** 从请求属性解析当前登录用户 ID，未登录则抛出异常。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
