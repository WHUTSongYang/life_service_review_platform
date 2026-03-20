package com.lifereview.repository;

import com.lifereview.entity.ShopApplyRequest;
import com.lifereview.enums.ShopApplyStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 店铺入驻申请表 Mapper */
@Mapper
public interface ShopApplyRequestRepository extends MybatisBaseRepository<ShopApplyRequest> {
    List<ShopApplyRequest> findByApplicantUserIdOrderByCreatedAtDesc(Long applicantUserId);

    List<ShopApplyRequest> findByStatusOrderByCreatedAtAsc(ShopApplyStatus status);

    ShopApplyRequest findOneByIdAndStatus(@Param("id") Long id, @Param("status") ShopApplyStatus status);

    long countByApplicantUserIdAndNameAndAddressAndStatus(@Param("applicantUserId") Long applicantUserId,
                                                          @Param("name") String name,
                                                          @Param("address") String address,
                                                          @Param("status") ShopApplyStatus status);

    int updateReviewResult(
            @Param("id") Long id, @Param("expectedStatus") ShopApplyStatus expectedStatus, @Param("status") ShopApplyStatus status,
            @Param("reviewerUserId") Long reviewerUserId, @Param("reviewNote") String reviewNote, @Param("reviewedAt") LocalDateTime reviewedAt
    );

    default Optional<ShopApplyRequest> findByIdAndStatus(Long id, ShopApplyStatus status) {
        return Optional.ofNullable(findOneByIdAndStatus(id, status));
    }

    default boolean existsByApplicantUserIdAndNameAndAddressAndStatus(Long applicantUserId, String name, String address, ShopApplyStatus status) {
        return countByApplicantUserIdAndNameAndAddressAndStatus(applicantUserId, name, address, status) > 0;
    }
}
