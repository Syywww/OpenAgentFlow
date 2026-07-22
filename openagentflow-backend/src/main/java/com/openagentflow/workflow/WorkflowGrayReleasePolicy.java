package com.openagentflow.workflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 工作流灰度版本稳定路由策略。
 */
public final class WorkflowGrayReleasePolicy {

    private WorkflowGrayReleasePolicy() {
    }

    /**
     * 根据稳定哈希判断是否使用当前版本。
     *
     * @param workflowId 工作流ID
     * @param userId 调用用户ID
     * @param requestKey 幂等键或请求特征
     * @param grayPercent 当前版本灰度比例
     * @return 是否选择当前版本
     */
    public static boolean useCurrentVersion(String workflowId, String userId, String requestKey, int grayPercent) {
        int percent = Math.max(0, Math.min(100, grayPercent));
        if (percent == 0) {
            return false;
        }
        if (percent == 100) {
            return true;
        }
        String source = safe(workflowId) + '|' + safe(userId) + '|' + safe(requestKey);
        byte[] digest = sha256(source);
        int bucket = ((digest[0] & 0xff) << 8 | (digest[1] & 0xff)) % 100;
        return bucket < percent;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

