package srm.portal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import srm.portal.mapper.GenericMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private GenericMapper genericMapper;

    @Autowired
    private srm.portal.service.TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String password = body.get("password");

        if (account == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "账号和密码不能为空"));
        }

        // 查询用户
        Map<String, Object> user = genericMapper.findBladeUserByAccount(account, 629965L);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "账号不存在"));
        }

        // 验证密码
        Object dbPasswordObj = getValueIgnoreCase(user, "password");
        String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);
        String newPasswordHash = sha1(password);
        if (!dbPassword.equalsIgnoreCase(newPasswordHash)) {
            return ResponseEntity.status(401).body(Map.of("message", "密码错误"));
        }

        // 获取用户ID
        Object userIdObj = getValueIgnoreCase(user, "id");
        Long userId = convertToLong(userIdObj);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "用户数据异常（ID缺失）"));
        }

        // 获取用户名（优先从 blade_user.real_name 取）
        String userName = "";
        Object realNameObj = getValueIgnoreCase(user, "real_name");
        if (realNameObj != null && !String.valueOf(realNameObj).isBlank()) {
            userName = String.valueOf(realNameObj);
        } else {
            // 备用：从 tb_user_detail.name 取
            List<Map<String, Object>> userDetails = genericMapper.findUserDetailsByUserId(userId);
            if (userDetails != null && !userDetails.isEmpty()) {
                Object nameObj = userDetails.get(0).get("name");
                if (nameObj != null) {
                    userName = String.valueOf(nameObj);
                }
            }
        }

        // 判断是否管理员
        boolean isAdmin = isAdminUser(userId);

        // 判断是否供应商客户
        boolean isSupplierClient = isSupplierClient(userId);

        // 判断是否只允许开票
        boolean onlyOpenInvoice = isOnlyOpenInvoice(userId);

        // 获取供应商编号
        String supplierNo = null;
        Map<String, Object> supplier = genericMapper.findSupplierByUserId(userId);
        if (supplier != null) {
            Object supplierNoObj = getValueIgnoreCase(supplier, "supplier_no");
            if (supplierNoObj != null) {
                supplierNo = String.valueOf(supplierNoObj);
            }
        }

        // 生成 token 并保存到缓存
        String token = tokenService.generateToken(userId, userName, isAdmin, isSupplierClient);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", String.valueOf(userId),
                "userName", userName,
                "isAdmin", isAdmin,
                "isSupplierClient", isSupplierClient,
                "onlyOpenInvoice", onlyOpenInvoice,
                "supplierNo", supplierNo != null ? supplierNo : ""
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (userIdStr == null || oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户ID、旧密码和新密码不能为空"));
        }

        try {
            Long userId = Long.parseLong(userIdStr);

            // 查询用户
            Map<String, Object> user = genericMapper.findBladeUserByAccount(null, 629965L);
            // 通过ID查询
            String account = genericMapper.findAccountByUserId(userId);
            if (account == null) {
                return ResponseEntity.status(400).body(Map.of("message", "用户不存在"));
            }
            user = genericMapper.findBladeUserByAccount(account, 629965L);
            if (user == null) {
                return ResponseEntity.status(400).body(Map.of("message", "用户不存在"));
            }

            // 验证旧密码
            Object dbPasswordObj = getValueIgnoreCase(user, "password");
            String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);
            boolean pwdMatch = dbPassword.equalsIgnoreCase(sha1(oldPassword));
            if (!pwdMatch) {
                return ResponseEntity.status(400).body(Map.of("message", "旧密码错误"));
            }

            // 更新密码
            String encryptedNewPassword = sha1(newPassword);
            int updated = genericMapper.updatePassword(userId, encryptedNewPassword);

            if (updated == 0) {
                return ResponseEntity.status(400).body(Map.of("message", "密码更新失败"));
            }

            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户ID格式错误"));
        }
    }

    private Object getValueIgnoreCase(Map<String, Object> map, String key) {
        if (map.containsKey(key)) return map.get(key);
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key)) return map.get(k);
        }
        return null;
    }

    private Long convertToLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return input;
        }
    }

    /**
     * 判断用户是否为管理员
     */
    private boolean isAdminUser(Long userId) {
        // 1. 通过账号名判断是否为管理员
        String account = genericMapper.findAccountByUserId(userId);
        if (account != null) {
            String lower = account.toLowerCase();
            if ("admin".equals(lower) || "管理员".equals(account)) {
                return true;
            }
        }
        // 2. 通过 tb_user_detail.IS_SUPPLIER_ADMIN 判断是否为管理员
        String isAdmin = genericMapper.findSupplierAdminByUserId(userId);
        if (isAdmin != null) {
            String trimmed = isAdmin.trim();
            if ("是".equals(trimmed) || "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户是否为供应商客户
     */
    private boolean isSupplierClient(Long userId) {
        String client = genericMapper.findSupplierClientByUserId(userId);
        if (client == null) return false;
        String trimmed = client.trim();
        return "是".equals(trimmed) || "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed);
    }

    /**
     * 判断用户是否只允许开票
     */
    private boolean isOnlyOpenInvoice(Long userId) {
        String val = genericMapper.findOnlyOpenInvoiceByUserId(userId);
        if (val == null) return false;
        String trimmed = val.trim();
        return "是".equals(trimmed) || "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> body,
                                                      jakarta.servlet.http.HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            tokenService.removeToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }
}
