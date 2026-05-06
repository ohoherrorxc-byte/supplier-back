package srm.portal.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    /**
     * 生成 token 并保存到缓存
     */
    public String generateToken(Long userId, String userName, boolean isAdmin, boolean isSupplierClient) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, new TokenInfo(userId, userName, isAdmin, isSupplierClient, System.currentTimeMillis()));
        return token;
    }

    /**
     * 验证 token，返回对应的用户信息
     */
    public TokenInfo validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return null;
        }
        // 7 天过期
        if (System.currentTimeMillis() - info.createTime > 7 * 24 * 60 * 60 * 1000L) {
            tokenStore.remove(token);
            return null;
        }
        return info;
    }

    /**
     * 清除 token（登出）
     */
    public void removeToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }

    public static class TokenInfo {
        public Long userId;
        public String userName;
        public boolean isAdmin;
        public boolean isSupplierClient;
        public long createTime;

        public TokenInfo(Long userId, String userName, boolean isAdmin, boolean isSupplierClient, long createTime) {
            this.userId = userId;
            this.userName = userName;
            this.isAdmin = isAdmin;
            this.isSupplierClient = isSupplierClient;
            this.createTime = createTime;
        }
    }
}