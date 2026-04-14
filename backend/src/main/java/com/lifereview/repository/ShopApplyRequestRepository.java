package com.lifereview.repository;

import com.lifereview.entity.ShopApplyRequest;
import com.lifereview.enums.ShopApplyStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 店铺入驻申请数据访问：对应表 {@code shop_apply_requests}（及 XML），支持申请人列表、待审队列、状态机更新与重复申请校验。
 */
@Mapper
public interface ShopApplyRequestRepository extends MybatisBaseRepository<ShopApplyRequest> {
    /**
     * 按申请人用户 id 查询其全部申请，按创建时间从新到旧排序。
     *
     * @param applicantUserId 申请人用户 id
     * @return 申请记录列表
     */
    List<ShopApplyRequest> findByApplicantUserIdOrderByCreatedAtDesc(Long applicantUserId);

    /**
     * 按申请状态查询记录，按创建时间从早到晚排序（便于 FIFO 审核）。
     *
     * @param status 申请状态枚举
     * @return 申请列表
     */
    List<ShopApplyRequest> findByStatusOrderByCreatedAtAsc(ShopApplyStatus status);

    /**
     * 按主键与期望状态查询单条申请，常用于审核时锁定「待处理」状态。
     *
     * @param id     申请 id
     * @param status 状态
     * @return 匹配实体或 {@code null}
     */
    ShopApplyRequest findOneByIdAndStatus(@Param("id") Long id, @Param("status") ShopApplyStatus status);

    /**
     * 统计同一申请人在相同店铺名、地址与状态下的申请条数，用于防止重复提交。
     *
     * @param applicantUserId 申请人用户 id
     * @param name            店铺名称
     * @param address         店铺地址
     * @param status          申请状态
     * @return 记录数
     */
    long countByApplicantUserIdAndNameAndAddressAndStatus(@Param("applicantUserId") Long applicantUserId,
                                                          @Param("name") String name,
                                                          @Param("address") String address,
                                                          @Param("status") ShopApplyStatus status);

    /**
     * 乐观锁式更新审核结果：仅当当前状态等于期望状态时写入新状态、审核人、备注与时间。
     *
     * @param id              申请 id
     * @param expectedStatus  更新前期望的当前状态
     * @param status          更新后的目标状态
     * @param reviewerUserId  审核人用户 id
     * @param reviewNote      审核备注
     * @param reviewedAt      审核完成时间
     * @return 受影响行数，0 表示状态不匹配未更新
     */
    int updateReviewResult(
            @Param("id") Long id, @Param("expectedStatus") ShopApplyStatus expectedStatus, @Param("status") ShopApplyStatus status,
            @Param("reviewerUserId") Long reviewerUserId, @Param("reviewNote") String reviewNote, @Param("reviewedAt") LocalDateTime reviewedAt
    );

    /**
     * 按 id 与状态查询申请，封装为 {@link Optional}。
     *
     * @param id     申请 id
     * @param status 状态
     * @return 存在则非空，否则为空
     */
    default Optional<ShopApplyRequest> findByIdAndStatus(Long id, ShopApplyStatus status) {
        return Optional.ofNullable(findOneByIdAndStatus(id, status));
    }

    /**
     * 判断是否存在指定申请人、同名、同址且处于给定状态的申请。
     *
     * @param applicantUserId 申请人用户 id
     * @param name            店铺名称
     * @param address         店铺地址
     * @param status          状态
     * @return 存在为 {@code true}
     */
    default boolean existsByApplicantUserIdAndNameAndAddressAndStatus(Long applicantUserId, String name, String address, ShopApplyStatus status) {
        return countByApplicantUserIdAndNameAndAddressAndStatus(applicantUserId, name, address, status) > 0;
    }
}
