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

@Service
@RequiredArgsConstructor
public class WalletCommunityImageStorage {

    private final WalletAppAvatarProperties props;

    public String save(MultipartFile file, Long userId, String extension) throws IOException {
        String ext = StringUtils.isBlank(extension) ? "jpg" : extension.toLowerCase();
        Path root = Paths.get(props.getStoragePath()).toAbsolutePath().normalize().resolve("community");
        Path userDir = root.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = userDir.resolve(fileName);
        file.transferTo(dest);
        String path = normalizeWebPrefix(props.getUrlPathPrefix()) + "/community/" + userId + "/" + fileName;
        return buildAbsoluteUrl(path);
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
