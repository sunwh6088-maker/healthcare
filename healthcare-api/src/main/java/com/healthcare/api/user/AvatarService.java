package com.healthcare.api.user;

import com.healthcare.api.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 头像存储：文件保存在 {@code app.upload-dir/avatars/<userId>.<ext>}。
 * 只接受真实图片（按文件头识别 JPEG/PNG/WebP），限制 2MB。
 */
@Service
public class AvatarService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "png", "webp");

    private final Path avatarDir;

    public AvatarService(@Value("${app.upload-dir:./data/uploads}") String uploadDir) {
        this.avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建头像目录: " + avatarDir, ex);
        }
    }

    /** 保存头像，返回文件扩展名；旧头像文件会被替换。 */
    public String store(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(413, "头像不能超过 2MB");
        }
        String extension = detectExtension(file);
        if (extension == null) {
            throw new ApiException(400, "仅支持 JPG、PNG 或 WebP 图片");
        }
        Path target = avatarDir.resolve(userId + "." + extension).normalize();
        if (!target.getParent().equals(avatarDir)) {
            throw new ApiException(400, "非法的文件名");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(500, "头像保存失败，请稍后重试");
        }
        deleteExcept(userId, extension);
        return extension;
    }

    /** 查找用户头像文件，不存在时返回 null。 */
    public Path find(Long userId) {
        try (Stream<Path> files = Files.list(avatarDir)) {
            return files
                    .filter(path -> isAvatarOf(path, userId))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    public void delete(Long userId) {
        try (Stream<Path> files = Files.list(avatarDir)) {
            for (Path path : files.filter(path -> isAvatarOf(path, userId)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new ApiException(500, "头像删除失败，请稍后重试");
        }
    }

    public static String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void deleteExcept(Long userId, String keepExtension) {
        try (Stream<Path> files = Files.list(avatarDir)) {
            for (Path path : files.filter(path -> isAvatarOf(path, userId)).toList()) {
                boolean keep = path.getFileName().toString().equals(userId + "." + keepExtension);
                if (!keep) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException ignored) {
            // 旧文件清理失败不影响本次上传
        }
    }

    private boolean isAvatarOf(Path path, Long userId) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot <= 0) return false;
        return name.substring(0, dot).equals(String.valueOf(userId))
                && ALLOWED_EXTENSIONS.contains(name.substring(dot + 1));
    }

    /** 按文件头识别真实图片格式，避免仅依赖客户端 Content-Type。 */
    private String detectExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException ex) {
            return null;
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8
                && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N'
                && head[3] == 'G' && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
