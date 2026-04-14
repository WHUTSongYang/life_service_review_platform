package com.lifereview.service.impl;

import com.lifereview.dto.CaptchaResponse;
import com.lifereview.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现。
 * <p>
 * 生成随机字母数字验证码、绘制干扰线与旋转字符、输出 PNG；验证码原文大写存入 Redis 并设置 TTL。
 * 校验时忽略大小写，成功后删除 Redis 键（一次性消费）。
 */
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    static {
        // 服务端无显示器环境下启用 AWT 无头模式
        System.setProperty("java.awt.headless", "true");
    }

    /** 验证码字符集（大写字母与数字） */
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /** 验证码长度 */
    private static final int LEN = 4;
    /** 图片宽度（像素） */
    private static final int WIDTH = 120;
    /** 图片高度（像素） */
    private static final int HEIGHT = 44;

    private final StringRedisTemplate stringRedisTemplate;
    /** 密码学安全随机数，用于验证码与干扰元素 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** Redis key 前缀，配置项 {@code app.captcha.redis-prefix} */
    @Value("${app.captcha.redis-prefix:captcha:}")
    private String redisPrefix;

    /** 验证码存活时间（秒），配置项 {@code app.captcha.ttl-seconds} */
    @Value("${app.captcha.ttl-seconds:300}")
    private int ttlSeconds;

    /**
     * 生成新验证码：随机串写入 Redis，返回 id 与 Base64(PNG)。
     *
     * @return 包含 {@code captchaId} 与 {@code imageBase64} 的响应体
     * @throws IllegalStateException 图片编码 IO 失败时由 {@link #renderPng} 抛出
     * @throws org.springframework.dao.DataAccessException 写入 Redis 失败时由 Spring Data 抛出
     */
    @Override
    public CaptchaResponse generate() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String key = redisPrefix + captchaId;
        stringRedisTemplate.opsForValue().set(key, code.toUpperCase(), ttlSeconds, TimeUnit.SECONDS);

        byte[] png = renderPng(code);
        String b64 = Base64.getEncoder().encodeToString(png);
        return new CaptchaResponse(captchaId, b64);
    }

    /**
     * 校验用户输入是否与 Redis 中一致（忽略大小写）；成功则删除键。
     *
     * @param captchaId 生成接口返回的 id
     * @param userInput 用户填写的验证码
     * @return {@code true} 表示校验通过并已消费；否则 {@code false}
     * @throws org.springframework.dao.DataAccessException 与 Redis 通信失败等场景下由 Spring Data 抛出
     */
    @Override
    public boolean verifyAndConsume(String captchaId, String userInput) {
        if (captchaId == null || captchaId.isBlank() || userInput == null || userInput.isBlank()) {
            return false;
        }
        String key = redisPrefix + captchaId.trim();
        String expected = stringRedisTemplate.opsForValue().get(key);
        if (expected == null) {
            return false;
        }
        if (!expected.equalsIgnoreCase(userInput.trim())) {
            return false;
        }
        stringRedisTemplate.delete(key);
        return true;
    }

    /**
     * 从字符集中均匀抽取 {@link #LEN} 位构成验证码明文（展示用大小写混合，存 Redis 时转大写）。
     *
     * @return 验证码字符串
     */
    private String randomCode() {
        StringBuilder sb = new StringBuilder(LEN);
        for (int i = 0; i < LEN; i++) {
            sb.append(CHARSET.charAt(secureRandom.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    /**
     * 将验证码绘制为 PNG 字节数组（干扰线、旋转字符、噪点）。
     *
     * @param code 待绘制的明文
     * @return PNG 二进制
     * @throws IllegalStateException {@link ImageIO#write} 失败时包装原始 {@link IOException}
     */
    private byte[] renderPng(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线
            for (int i = 0; i < 8; i++) {
                g.setColor(randomColor(120, 200));
                g.drawLine(secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT),
                        secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT));
            }
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 28);
            g.setFont(font);
            char[] chars = code.toCharArray();
            int x = 12;
            for (char c : chars) {
                g.setColor(randomColor(20, 100));
                AffineTransform old = g.getTransform();
                double theta = (secureRandom.nextDouble() - 0.5) * 0.5;
                g.rotate(theta, x, 30);
                g.drawString(String.valueOf(c), x, 32);
                g.setTransform(old);
                x += 22;
            }
            // 噪点
            for (int i = 0; i < 30; i++) {
                g.setColor(randomColor(100, 180));
                g.fillOval(secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT), 2, 2);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
        return baos.toByteArray();
    }

    /**
     * 在 [{@code min}, {@code max}) 区间内为 RGB 三分量分别取随机值。
     *
     * @param min 下界（含）
     * @param max 上界（不含）
     * @return 合成后的 {@link Color}
     */
    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + secureRandom.nextInt(range), min + secureRandom.nextInt(range), min + secureRandom.nextInt(range));
    }
}
