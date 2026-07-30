package org.dromara.web3.wallet.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.web3.wallet.config.WalletAppAvatarProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * APP 头像保存到应用服务器本地磁盘，并生成可访问的 URL。
 */
@Service
@RequiredArgsConstructor
public class WalletLocalAvatarStorage {

    private final WalletAppAvatarProperties props;

    /**
     * 保存文件并返回完整可访问 URL（写入 app_user.avatar）。
     */
    public String save(MultipartFile file, Long userId, String extension) throws IOException {
        String ext = extension == null ? "" : extension.toLowerCase();
        if (StringUtils.isBlank(ext)) {
            ext = "jpg";
        }
        Path root = Paths.get(props.getStoragePath()).toAbsolutePath().normalize();
        Path userDir = root.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = userDir.resolve(fileName);
        file.transferTo(dest);
        try (Stream<Path> stream = Files.list(userDir)) {
            stream.filter(p -> !p.getFileName().equals(dest.getFileName())).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 尽力清理旧头像，失败不影响新文件
                }
            });
        }
        String webPath = normalizeWebPrefix(props.getUrlPathPrefix()) + "/" + userId + "/" + fileName;
        return buildAbsoluteUrl(webPath);
    }

    private static String normalizeWebPrefix(String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return StringUtils.stripEnd(p, "/");
    }

    private String buildAbsoluteUrl(String pathFromRoot) {
        String configured = props.getPublicBaseUrl();
        if (StringUtils.isNotBlank(configured)) {
            return StringUtils.stripEnd(configured.trim(), "/") + pathFromRoot;
        }
        HttpServletRequest req = ServletUtils.getRequest();
        if (req == null) {
            return pathFromRoot;
        }
        String scheme = StringUtils.blankToDefault(req.getHeader("X-Forwarded-Proto"), req.getScheme());
        String forwardedHost = req.getHeader("X-Forwarded-Host");
        String authority;
        if (StringUtils.isNotBlank(forwardedHost)) {
            authority = forwardedHost.split(",")[0].trim();
        } else {
            int port = req.getServerPort();
            String host = req.getServerName();
            boolean defPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
            authority = defPort ? host : host + ":" + port;
        }
        String ctx = StringUtils.defaultString(req.getContextPath());
        return scheme + "://" + authority + ctx + pathFromRoot;
    }
}
