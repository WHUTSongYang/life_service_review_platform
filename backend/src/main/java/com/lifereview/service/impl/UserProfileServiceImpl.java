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
 * 负责用户资料查询、更新，支持手机/邮箱重复校验。
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;

    @Override
    public UserProfileItem getProfile(Long userId) {
        User user = ensureUserExists(userId);
        return mapProfile(user);
    }

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

    private String cleanOptional(String value) {
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private UserProfileItem mapProfile(User user) {
        return UserProfileItem.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private User ensureUserExists(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
