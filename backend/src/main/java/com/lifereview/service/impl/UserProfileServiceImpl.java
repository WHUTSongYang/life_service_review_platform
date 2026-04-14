package com.lifereview.service.impl;

import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.UserProfileItem;
import com.lifereview.dto.UserProfileUpdateRequest;
import com.lifereview.entity.Review;
import com.lifereview.entity.Shop;
import com.lifereview.entity.User;
import com.lifereview.repository.ReviewRepository;
import com.lifereview.repository.ShopRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户资料服务实现类。
 * <p>负责用户资料查询与更新（昵称、手机、邮箱占用校验），以及「我的点评」列表组装。</p>
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    /** 用户仓储 */
    private final UserRepository userRepository;
    /** 点评仓储 */
    private final ReviewRepository reviewRepository;
    /** 店铺仓储 */
    private final ShopRepository shopRepository;

    /**
     * 查询用户资料。
     *
     * @param userId 用户主键
     * @return 资料 DTO
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public UserProfileItem getProfile(Long userId) {
        User user = ensureUserExists(userId);
        return mapProfile(user);
    }

    /**
     * 更新昵称、手机、邮箱；手机与邮箱与「其他用户」冲突时拒绝。
     *
     * @param userId 用户主键
     * @param req    可选字段更新请求
     * @return 更新后的资料
     * @throws IllegalArgumentException 用户不存在、昵称为空或联系方式被占用时抛出
     */
    @Override
    @Transactional
    public UserProfileItem updateProfile(Long userId, UserProfileUpdateRequest req) {
        User user = ensureUserExists(userId);
        if (req.getNickname() != null) {
            String nickname = req.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new IllegalArgumentException("昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (req.getPhone() != null) {
            String phone = cleanOptional(req.getPhone());
            // 校验手机号是否被其他用户占用
            if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
                throw new IllegalArgumentException("手机号已被占用");
            }
            user.setPhone(phone);
        }
        if (req.getEmail() != null) {
            String email = cleanOptional(req.getEmail());
            // 校验邮箱是否被其他用户占用
            if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new IllegalArgumentException("邮箱已被占用");
            }
            user.setEmail(email);
        }
        return mapProfile(userRepository.save(user));
    }

    /**
     * 查询当前用户发表的点评列表（店铺信息批量填充）。
     *
     * @param userId 用户主键
     * @return 与热门列表同结构的展示项（用户昵称字段此处置空由调用方决定展示）
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public List<HotReviewItem> listMyReviews(Long userId) {
        ensureUserExists(userId);
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (reviews.isEmpty()) {
            return List.of();
        }
        Set<Long> shopIds = reviews.stream().map(Review::getShopId).collect(Collectors.toSet());
        Map<Long, Shop> shopMap = shopRepository.findAllById(shopIds).stream().collect(Collectors.toMap(Shop::getId, s -> s));
        return reviews.stream().map(review -> {
            Shop shop = shopMap.get(review.getShopId());
            return HotReviewItem.builder()
                    .id(review.getId())
                    .shopId(review.getShopId())
                    .shopName(shop != null ? shop.getName() : "未知店铺")
                    .shopType(shop != null ? shop.getType() : "")
                    .shopAddress(shop != null ? shop.getAddress() : "")
                    .userId(review.getUserId())
                    .userNickname("")
                    .content(review.getContent())
                    .images(review.getImages())
                    .score(review.getScore())
                    .likeCount(review.getLikeCount())
                    .createdAt(review.getCreatedAt())
                    .build();
        }).toList();
    }

    /**
     * 可选字符串去空白；空串视为 null。
     *
     * @param value 原始字符串（调用方保证非 null 时再 trim）
     * @return 去空白后的非空串，或 null
     */
    private String cleanOptional(String value) {
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * 用户实体转资料 DTO。
     *
     * @param user 用户实体
     * @return 资料项
     */
    private UserProfileItem mapProfile(User user) {
        return UserProfileItem.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    /**
     * 按 ID 查询用户，不存在抛异常。
     *
     * @param userId 用户主键
     * @return 用户实体
     * @throws IllegalArgumentException 不存在时抛出
     */
    private User ensureUserExists(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
