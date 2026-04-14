package com.lifereview.service.impl;

import com.lifereview.dto.ShopApplyCreateRequest;
import com.lifereview.dto.ShopApplyItem;
import com.lifereview.entity.Shop;
import com.lifereview.entity.ShopApplyRequest;
import com.lifereview.entity.User;
import com.lifereview.enums.ShopApplyStatus;
import com.lifereview.repository.ShopApplyRequestRepository;
import com.lifereview.repository.ShopRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.CacheOpsService;
import com.lifereview.service.ShopApplyService;
import com.lifereview.service.ShopCategoryCacheService;
import com.lifereview.service.ShopGeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 店铺入驻申请服务实现类。
 * <p>负责商户提交申请、查看我的申请、超级管理员审核（通过/驳回），并与 GEO、分类缓存联动。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopApplyServiceImpl implements ShopApplyService {

    /** 入驻申请仓储 */
    private final ShopApplyRequestRepository shopApplyRequestRepository;
    /** 店铺仓储 */
    private final ShopRepository shopRepository;
    /** 用户仓储 */
    private final UserRepository userRepository;
    /** 店铺地理位置同步 */
    private final ShopGeoService shopGeoService;
    /** 店铺分类校验与缓存 */
    private final ShopCategoryCacheService shopCategoryCacheService;
    /** 通用缓存操作（双删、bump 版本等） */
    private final CacheOpsService cacheOpsService;

    /**
     * 提交店铺入驻申请。
     *
     * @param userId 申请人用户 ID
     * @param req    店铺名称、类型、图片、地址与经纬度等
     * @return 申请展示项
     * @throws IllegalArgumentException 用户不存在、参数不合法、重复申请或商铺已存在时抛出
     */
    @Override
    @Transactional
    public ShopApplyItem submitApply(Long userId, ShopApplyCreateRequest req) {
        ensureUserExists(userId);
        validateLocation(req.getLongitude(), req.getLatitude());
        // 校验并规范化店铺分类
        String normalizedType = shopCategoryCacheService.validateAndNormalizeCategory(req.getType());
        String name = req.getName().trim();
        String image = req.getImage() == null ? "" : req.getImage().trim();
        if (image.isEmpty()) {
            throw new IllegalArgumentException("请先上传商铺图片");
        }
        String address = req.getAddress().trim();
        if (shopRepository.existsByNameAndAddress(name, address)) {
            throw new IllegalArgumentException("该商铺已存在，无需重复申请");
        }
        // 检查是否存在同名同址的待审核申请
        boolean duplicatePending = shopApplyRequestRepository.existsByApplicantUserIdAndNameAndAddressAndStatus(userId, name, address, ShopApplyStatus.PENDING);
        if (duplicatePending) {
            throw new IllegalArgumentException("你已提交同名同址申请，请等待审核");
        }
        ShopApplyRequest entity = new ShopApplyRequest();
        entity.setApplicantUserId(userId);
        entity.setName(name);
        entity.setType(normalizedType);
        entity.setImage(image);
        entity.setAddress(address);
        entity.setLongitude(req.getLongitude());
        entity.setLatitude(req.getLatitude());
        entity.setStatus(ShopApplyStatus.PENDING);
        ShopApplyRequest saved = shopApplyRequestRepository.save(entity);
        return mapItems(List.of(saved)).get(0);
    }

    /**
     * 查询当前用户的申请列表（按创建时间倒序）。
     *
     * @param userId 用户主键
     * @return 申请展示项列表
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public List<ShopApplyItem> listMine(Long userId) {
        ensureUserExists(userId);
        return mapItems(shopApplyRequestRepository.findByApplicantUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * 超级管理员查看待审核申请列表。
     *
     * @param operatorId  操作者 ID（当前实现未用于过滤，预留）
     * @param superAdmin  是否为超级管理员
     * @return 待审核申请列表
     * @throws IllegalArgumentException 非超级管理员时抛出
     */
    @Override
    public List<ShopApplyItem> listPending(Long operatorId, boolean superAdmin) {
        assertSuperAdmin(superAdmin);
        return mapItems(shopApplyRequestRepository.findByStatusOrderByCreatedAtAsc(ShopApplyStatus.PENDING));
    }

    /**
     * 通过申请：创建店铺、同步 GEO、更新申请状态并清理相关缓存。
     *
     * @param operatorId  审核人用户 ID
     * @param superAdmin  是否为超级管理员
     * @param applyId     申请主键
     * @param reviewNote  审核备注，可为 null
     * @return 更新后的申请展示项
     * @throws IllegalArgumentException 权限不足、申请状态非法、并发更新失败或同名同址已存在（自动驳回）等
     */
    @Override
    @Transactional
    public ShopApplyItem approve(Long operatorId, boolean superAdmin, Long applyId, String reviewNote) {
        assertSuperAdmin(superAdmin);
        ShopApplyRequest request = shopApplyRequestRepository.findByIdAndStatus(applyId, ShopApplyStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("申请不存在或已处理"));
        // 若同名同址商铺已存在，自动驳回
        if (shopRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
            int rows = shopApplyRequestRepository.updateReviewResult(request.getId(), ShopApplyStatus.PENDING, ShopApplyStatus.REJECTED, operatorId, "系统检测到同名同址商铺已存在，自动驳回", LocalDateTime.now());
            if (rows != 1) {
                throw new IllegalArgumentException("审批状态更新失败，请重试");
            }
            ShopApplyRequest rejected = shopApplyRequestRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("申请不存在或已处理"));
            return mapItems(List.of(rejected)).get(0);
        }
        // 创建店铺实体并写入数据库
        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setType(shopCategoryCacheService.validateAndNormalizeCategory(request.getType()));
        shop.setOwnerUserId(request.getApplicantUserId());
        shop.setImage(request.getImage());
        shop.setPromotion(false);
        shop.setAddress(request.getAddress());
        shop.setLongitude(request.getLongitude());
        shop.setLatitude(request.getLatitude());
        Shop savedShop = shopRepository.saveAndFlush(shop);
        shopGeoService.syncShopLocation(savedShop);
        // 删除店铺详情缓存并递增附近店铺版本号
        cacheOpsService.deleteWithDoubleDelete("cache:shop:detail:" + savedShop.getId());
        cacheOpsService.bumpNearbyVersion();
        int rows = shopApplyRequestRepository.updateReviewResult(request.getId(), ShopApplyStatus.PENDING, ShopApplyStatus.APPROVED, operatorId, reviewNote == null ? "" : reviewNote.trim(), LocalDateTime.now());
        if (rows != 1) {
            throw new IllegalArgumentException("审批状态更新失败，请重试");
        }
        ShopApplyRequest approved = shopApplyRequestRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("申请不存在或已处理"));
        return mapItems(List.of(approved)).get(0);
    }

    /**
     * 驳回申请并更新状态与审核信息。
     *
     * @param operatorId  审核人用户 ID
     * @param superAdmin  是否为超级管理员
     * @param applyId     申请主键
     * @param reviewNote  驳回原因或备注，可为 null
     * @return 更新后的申请展示项
     * @throws IllegalArgumentException 权限不足、申请状态非法或并发更新失败时抛出
     */
    @Override
    @Transactional
    public ShopApplyItem reject(Long operatorId, boolean superAdmin, Long applyId, String reviewNote) {
        assertSuperAdmin(superAdmin);
        ShopApplyRequest request = shopApplyRequestRepository.findByIdAndStatus(applyId, ShopApplyStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("申请不存在或已处理"));
        int rows = shopApplyRequestRepository.updateReviewResult(request.getId(), ShopApplyStatus.PENDING, ShopApplyStatus.REJECTED, operatorId, reviewNote == null ? "" : reviewNote.trim(), LocalDateTime.now());
        if (rows != 1) {
            throw new IllegalArgumentException("审批状态更新失败，请重试");
        }
        ShopApplyRequest rejected = shopApplyRequestRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("申请不存在或已处理"));
        return mapItems(List.of(rejected)).get(0);
    }

    /**
     * 将申请实体列表转为展示项（填充申请人、审核人昵称）。
     *
     * @param requests 申请实体列表
     * @return 展示项列表
     */
    private List<ShopApplyItem> mapItems(List<ShopApplyRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        for (ShopApplyRequest req : requests) {
            userIds.add(req.getApplicantUserId());
            if (req.getReviewerUserId() != null) {
                userIds.add(req.getReviewerUserId());
            }
        }
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, v -> v));
        List<ShopApplyItem> result = new ArrayList<>();
        for (ShopApplyRequest req : requests) {
            User applicant = userMap.get(req.getApplicantUserId());
            User reviewer = req.getReviewerUserId() == null ? null : userMap.get(req.getReviewerUserId());
            result.add(ShopApplyItem.builder()
                    .id(req.getId())
                    .applicantUserId(req.getApplicantUserId())
                    .applicantNickname(applicant != null ? applicant.getNickname() : "未知用户")
                    .name(req.getName())
                    .type(req.getType())
                    .image(req.getImage())
                    .address(req.getAddress())
                    .longitude(req.getLongitude())
                    .latitude(req.getLatitude())
                    .status(req.getStatus())
                    .reviewerUserId(req.getReviewerUserId())
                    .reviewerNickname(reviewer != null ? reviewer.getNickname() : "")
                    .reviewNote(req.getReviewNote())
                    .createdAt(req.getCreatedAt())
                    .reviewedAt(req.getReviewedAt())
                    .build());
        }
        return result;
    }

    /**
     * 校验用户存在。
     *
     * @param userId 用户主键
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    /**
     * 校验超级管理员权限。
     *
     * @param superAdmin 是否为超级管理员
     * @throws IllegalArgumentException 非超级管理员时抛出
     */
    private void assertSuperAdmin(boolean superAdmin) {
        if (!superAdmin) {
            throw new IllegalArgumentException("仅超级管理员可操作");
        }
    }

    /**
     * 校验经纬度：允许不传；若传则必须经纬度同时存在且范围合法。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @throws IllegalArgumentException 仅传其一或超出 WGS84 合法范围时抛出
     */
    private void validateLocation(Double longitude, Double latitude) {
        // 允许前端不上传定位信息
        if (longitude == null && latitude == null) {
            return;
        }
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("经纬度需同时传入");
        }
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("经纬度不合法");
        }
    }
}
