package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 上传文件类型、压缩炸弹和恶意特征扫描服务。 */
@Service
public class FileSecurityScanService {

    /** ZIP 最大条目数量。 */
    private static final int MAX_ZIP_ENTRIES = 10_000;
    /** ZIP 解压后最大允许体积。 */
    private static final long MAX_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024;
    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public FileSecurityScanService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 扫描对象存储中的文件，阻断伪造扩展名、压缩炸弹和已知危险脚本特征。 */
    public void scan(String workspaceId, String documentId, String bucket, String objectKey,
                     String fileHash, String extension, long compressedSize, SharedObjectStorageService storage) {
        String status = "clean";
        String threat = null;
        String detectedType = "unknown";
        Map<String, Object> detail = Map.of();
        try (InputStream raw = storage.open(bucket, objectKey); BufferedInputStream input = new BufferedInputStream(raw)) {
            input.mark(16);
            byte[] magic = input.readNBytes(8);
            input.reset();
            detectedType = detectType(magic);
            if (!extensionMatches(extension, detectedType)) {
                status = "blocked";
                threat = "文件扩展名与真实类型不匹配";
            } else if ("zip".equals(detectedType)) {
                detail = inspectZip(input, Math.max(1L, compressedSize));
            } else {
                byte[] prefix = input.readNBytes(64 * 1024);
                String text = new String(prefix, java.nio.charset.StandardCharsets.ISO_8859_1).toLowerCase();
                if (text.contains("<script") || text.contains("/javascript") || text.contains("powershell -enc")) {
                    status = "blocked";
                    threat = "检测到可执行脚本特征";
                }
            }
        } catch (BusinessException exception) {
            status = "blocked";
            threat = exception.getMessage();
        } catch (Exception exception) {
            status = "error";
            threat = exception.getMessage();
        }
        save(workspaceId, documentId, bucket, objectKey, fileHash, detectedType, status, threat, detail);
        if (!"clean".equals(status)) {
            throw new BusinessException("DOCUMENT_SECURITY_BLOCKED", "文件安全扫描未通过：" + threat);
        }
    }

    /** 流式检查 ZIP 条目与膨胀比例，避免把整份压缩包加载到堆内存。 */
    private Map<String, Object> inspectZip(InputStream input, long compressedSize) throws Exception {
        int entries = 0;
        long expanded = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) throw new BusinessException("ZIP_BOMB", "压缩包条目数量超限");
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".exe") || name.endsWith(".dll") || name.endsWith(".bat") || name.endsWith(".ps1")) {
                    throw new BusinessException("ARCHIVE_EXECUTABLE_FORBIDDEN", "压缩包包含可执行文件");
                }
                int read;
                while ((read = zip.read(buffer)) > 0) {
                    expanded += read;
                    if (expanded > MAX_UNCOMPRESSED_BYTES || expanded > compressedSize * 200L) {
                        throw new BusinessException("ZIP_BOMB", "压缩包解压体积或膨胀比例超限");
                    }
                }
            }
        }
        return Map.of("entryCount", entries, "uncompressedBytes", expanded,
                "expansionRatio", (double) expanded / compressedSize);
    }

    private String detectType(byte[] magic) {
        String hex = HexFormat.of().formatHex(magic);
        if (hex.startsWith("25504446")) return "pdf";
        if (hex.startsWith("504b0304") || hex.startsWith("504b0506")) return "zip";
        if (hex.startsWith("d0cf11e0")) return "ole";
        return "text";
    }

    private boolean extensionMatches(String extension, String type) {
        String ext = extension == null ? "" : extension.toLowerCase();
        if ("pdf".equals(type)) return "pdf".equals(ext);
        if ("zip".equals(type)) return java.util.Set.of("docx", "xlsx", "pptx", "zip").contains(ext);
        if ("ole".equals(type)) return java.util.Set.of("doc", "xls", "ppt").contains(ext);
        return java.util.Set.of("txt", "md", "csv", "json", "html", "htm").contains(ext);
    }

    private void save(String workspaceId, String documentId, String bucket, String key, String hash,
                      String detectedType, String status, String threat, Map<String, Object> detail) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO file_security_scan
                      (id,workspace_id,document_id,object_bucket,object_key,file_hash,detected_type,scan_engine,
                       scan_status,threat_name,detail_json,scanned_at,created_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,CAST(? AS JSON),NOW(3),NOW(3))
                    """, UUID.randomUUID().toString(), workspaceId, documentId, bucket, key, hash, detectedType,
                    "oaf-stream-scan-v1", status, threat, objectMapper.writeValueAsString(detail));
        } catch (Exception exception) {
            throw new IllegalStateException("文件安全扫描结果保存失败", exception);
        }
    }
}
