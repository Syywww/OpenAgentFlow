package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.notification.NotificationDtos;
import com.openagentflow.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人通知中心接口。
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    /** 通知中心服务。 */
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 分页查询当前用户通知。 */
    @GetMapping
    public ApiResponse<PageResult<NotificationDtos.NotificationItem>> list(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(notificationService.list(status, notificationType, severity, keyword, pageNo, pageSize));
    }

    /** 查询当前用户通知数量汇总。 */
    @GetMapping("/overview")
    public ApiResponse<NotificationDtos.NotificationOverview> overview() {
        return ApiResponse.ok(notificationService.overview());
    }

    /** 标记单条通知已读。 */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id) {
        notificationService.markRead(id);
        return ApiResponse.ok(null);
    }

    /** 批量标记通知已读。 */
    @PatchMapping("/read")
    public ApiResponse<Void> markReadBatch(@RequestBody NotificationDtos.BatchActionRequest request) {
        notificationService.markReadBatch(request.getNotificationIds());
        return ApiResponse.ok(null);
    }

    /** 标记全部通知已读。 */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok(null);
    }

    /** 归档单条通知。 */
    @PatchMapping("/{id}/archive")
    public ApiResponse<Void> archive(@PathVariable String id) {
        notificationService.archive(id);
        return ApiResponse.ok(null);
    }

    /** 批量归档通知。 */
    @PatchMapping("/archive")
    public ApiResponse<Void> archiveBatch(@RequestBody NotificationDtos.BatchActionRequest request) {
        notificationService.archiveBatch(request.getNotificationIds());
        return ApiResponse.ok(null);
    }

    /** 查询当前用户通知偏好。 */
    @GetMapping("/preference")
    public ApiResponse<NotificationDtos.Preference> preference() {
        return ApiResponse.ok(notificationService.preference());
    }

    /** 保存当前用户通知偏好。 */
    @PutMapping("/preference")
    public ApiResponse<NotificationDtos.Preference> savePreference(@RequestBody NotificationDtos.Preference request) {
        return ApiResponse.ok(notificationService.savePreference(request));
    }

    /** 管理员向指定范围发布通知。 */
    @PostMapping("/publish")
    @PreAuthorize("hasAnyAuthority('notification:manage','ROLE_admin','ROLE_super_admin')")
    public ApiResponse<NotificationDtos.PublishResult> publish(@RequestBody NotificationDtos.PublishRequest request) {
        return ApiResponse.ok(notificationService.publish(request));
    }
}
