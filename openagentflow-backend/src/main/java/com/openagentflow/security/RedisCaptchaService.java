package com.openagentflow.security;

import com.openagentflow.domain.auth.CaptchaResponse;
import com.openagentflow.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Redis 图形验证码服务。
 */
@Service
public class RedisCaptchaService {

    /** Redis 中验证码 key 的统一前缀。 */
    private static final String CAPTCHA_KEY_PREFIX = "oaf:auth:captcha:";

    /** 验证码字符集，去掉容易混淆的 0、O、1、I。 */
    private static final char[] CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    /** 验证码有效期，避免长期占用 Redis。 */
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    /** 验证码图片宽度。 */
    private static final int IMAGE_WIDTH = 112;

    /** 验证码图片高度。 */
    private static final int IMAGE_HEIGHT = 42;

    /** 安全随机数生成器。 */
    private final SecureRandom random = new SecureRandom();

    /** Redis 字符串操作模板。 */
    private final StringRedisTemplate redisTemplate;

    public RedisCaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 创建图形验证码，并把正确答案写入 Redis。
     *
     * @return 图形验证码响应
     */
    public CaptchaResponse createCaptcha() {
        String captchaCode = randomCode(4);
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 只把验证码答案存入 Redis，前端拿不到明文答案。
        redisTemplate.opsForValue().set(redisKey(captchaKey), captchaCode, CAPTCHA_TTL);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey(captchaKey);
        response.setImageBase64("data:image/png;base64," + renderImage(captchaCode));
        response.setExpireSeconds(CAPTCHA_TTL.toSeconds());
        return response;
    }

    /**
     * 校验用户输入的验证码，成功或失败都会删除 Redis 中的旧验证码。
     *
     * @param captchaKey 验证码唯一标识
     * @param captcha 用户输入的验证码
     */
    public void validateCaptcha(String captchaKey, String captcha) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captcha)) {
            throw new BusinessException("CAPTCHA_INVALID", "请输入验证码");
        }

        String redisKey = redisKey(captchaKey);
        String expectedCode = redisTemplate.opsForValue().get(redisKey);
        // 验证码一次性使用，校验后立即删除，防止重放。
        redisTemplate.delete(redisKey);

        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException("CAPTCHA_EXPIRED", "验证码已过期，请刷新后重试");
        }
        if (!expectedCode.equalsIgnoreCase(captcha.trim())) {
            throw new BusinessException("CAPTCHA_INVALID", "验证码错误");
        }
    }

    /**
     * 生成指定长度的随机验证码。
     *
     * @param length 验证码长度
     * @return 随机验证码
     */
    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CAPTCHA_CHARS[random.nextInt(CAPTCHA_CHARS.length)]);
        }
        return builder.toString();
    }

    /**
     * 把验证码渲染成 PNG 图片并编码为 Base64。
     *
     * @param captchaCode 验证码明文
     * @return Base64 图片内容
     */
    private String renderImage(String captchaCode) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRoundRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, 10, 10);

            // 绘制干扰线，降低简单 OCR 识别成功率。
            for (int index = 0; index < 5; index++) {
                graphics.setColor(randomSoftColor());
                graphics.setStroke(new BasicStroke(1.4f));
                graphics.drawLine(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                        random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT));
            }

            graphics.setFont(new Font("Arial", Font.BOLD, 24));
            for (int index = 0; index < captchaCode.length(); index++) {
                graphics.setColor(randomTextColor());
                int x = 13 + index * 24;
                int y = 29 + random.nextInt(5);
                double angle = Math.toRadians(random.nextInt(28) - 14);
                graphics.rotate(angle, x, y);
                graphics.drawString(String.valueOf(captchaCode.charAt(index)), x, y);
                graphics.rotate(-angle, x, y);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception exception) {
            throw new BusinessException("CAPTCHA_RENDER_FAILED", "验证码生成失败");
        } finally {
            graphics.dispose();
        }
    }

    /**
     * 生成浅色干扰色。
     *
     * @return 颜色对象
     */
    private Color randomSoftColor() {
        return new Color(160 + random.nextInt(70), 170 + random.nextInt(60), 185 + random.nextInt(55));
    }

    /**
     * 生成偏深的验证码文字色。
     *
     * @return 颜色对象
     */
    private Color randomTextColor() {
        return new Color(20 + random.nextInt(70), 70 + random.nextInt(80), 90 + random.nextInt(90));
    }

    /**
     * 组装 Redis 验证码 key。
     *
     * @param captchaKey 验证码唯一标识
     * @return Redis key
     */
    private String redisKey(String captchaKey) {
        return CAPTCHA_KEY_PREFIX + captchaKey;
    }
}
