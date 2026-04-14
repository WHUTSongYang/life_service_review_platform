package com.lifereview.enums;

/**
 * 点赞功能的目标类型枚举。
 * <p>与 {@link com.lifereview.entity.LikeRecord} 配合使用，区分被点赞对象是博客动态还是店铺点评。</p>
 */
public enum LikeTargetType {
    /** 博客/动态 */
    BLOG,
    /** 店铺点评 */
    REVIEW
}
