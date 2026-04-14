package com.lifereview.service;



import com.lifereview.dto.CaptchaResponse;



/**

 * 业务职责说明：图形验证码的生成（Base64 图片）与 Redis 存储；登录流程中的校验与一次性消费。

 */

public interface CaptchaService {



    /**

     * 生成新的图形验证码并写入 Redis。

     *

     * @return 含验证码 ID、Base64 编码 PNG 等信息的响应体

     */

    CaptchaResponse generate();



    /**

     * 校验用户输入是否与 Redis 中存储的答案一致（通常忽略大小写），成功后删除该验证码键。

     *

     * @param captchaId  验证码唯一标识

     * @param userInput  用户输入的验证码文本

     * @return {@code true} 表示校验通过；{@code false} 表示为空、过期或答案错误

     */

    boolean verifyAndConsume(String captchaId, String userInput);

}

