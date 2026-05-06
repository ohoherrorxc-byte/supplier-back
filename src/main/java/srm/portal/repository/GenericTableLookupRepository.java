package srm.portal.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import srm.portal.config.SrmDbProperties;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Repository
public class GenericTableLookupRepository {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final SrmDbProperties props;

    public GenericTableLookupRepository(JdbcTemplate jdbcTemplate, SrmDbProperties props) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
    }

    public Optional<Map<String, Object>> findByPk(String tableName, String pkValue, String pkColumn) {
        String safeTable = safeIdentifier(tableName);
        String safePkColumn = safeIdentifier(pkColumn);

        String sql = "SELECT * FROM " + safeTable + " WHERE " + safePkColumn + " = ?";
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, pkValue);
            return Optional.ofNullable(convertKeysToCamelCase(row));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findByColumn(String tableName, String columnName, String value) {
        String safeTable = safeIdentifier(tableName);
        String safeColumn = safeIdentifier(columnName);

        String sql = "SELECT * FROM " + safeTable + " WHERE " + safeColumn + " = ?";
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, value);
            return Optional.ofNullable(convertKeysToCamelCase(row));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String safeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe identifier: " + identifier);
        }
        return identifier;
    }

    public Optional<Map<String, Object>> findSupplierById(String supplierId) {
        String pkColumn = props.getPkColumns().getOrDefault("tb_contract_supplier", "ID");
        return findByPk("tb_contract_supplier", supplierId, pkColumn);
    }

    public Optional<Map<String, Object>> findUserBaseById(String userId) {
        String pkColumn = props.getPkColumns().getOrDefault("blade_user", "id");
        return findByPk("blade_user", userId, pkColumn);
    }

    public Optional<Map<String, Object>> findUserDetailByUserId(String userId) {
        String pkColumn = props.getUserDetailFilterColumn();
        if (pkColumn == null || pkColumn.isBlank()) {
            pkColumn = "user_id";
        }
        return findByColumn("tb_user_detail", pkColumn, userId);
    }

    public Optional<Map<String, Object>> findOrderById(String orderId) {
        String pkColumn = props.getPkColumns().getOrDefault("tb_purchase_parts_order", "id");
        return findByPk("tb_purchase_parts_order", orderId, pkColumn);
    }

    public Optional<Map<String, Object>> findInvoiceById(String invoiceId) {
        String pkColumn = props.getPkColumns().getOrDefault("tb_invoice", "id");
        return findByPk("tb_invoice", invoiceId, pkColumn);
    }

    public Optional<Map<String, Object>> findSupplierByUserId(String userId) {
        // 供应商归属：tb_user_detail.USER_ID -> tb_user_detail.SUPPLIER_COMPANY_ID -> tb_contract_supplier.ID
        String userDetailFilterColumn = props.getUserDetailFilterColumn();
        if (userDetailFilterColumn == null || userDetailFilterColumn.isBlank()) {
            userDetailFilterColumn = "USER_ID";
        }
        String supplierIdFromDetailColumn = props.getSupplierIdFromUserDetailColumn();
        if (supplierIdFromDetailColumn == null || supplierIdFromDetailColumn.isBlank()) {
            supplierIdFromDetailColumn = "SUPPLIER_COMPANY_ID";
        }

        String supplierPkColumn = props.getPkColumns().getOrDefault("tb_contract_supplier", "ID");

        String safeUserDetailFilterColumn = safeIdentifier(userDetailFilterColumn);
        String safeSupplierIdFromDetailColumn = safeIdentifier(supplierIdFromDetailColumn);
        String safeSupplierPkColumn = safeIdentifier(supplierPkColumn);

        String sql = "SELECT " + safeSupplierIdFromDetailColumn + " AS supplier_pk " +
                "FROM tb_user_detail WHERE " + safeUserDetailFilterColumn + " = ?";
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, userId);
            Object supplierPk = row.get("supplier_pk");
            if (supplierPk == null) {
                return Optional.empty();
            }
            return findByPk("tb_contract_supplier", String.valueOf(supplierPk), supplierPkColumn);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 当前登录用户（blade_user.id）在 tb_user_detail 中绑定的供应商公司 ID，对应 tb_contract_supplier.ID。
     */
    public Optional<Long> findSupplierCompanyIdByUserId(String userId) {
        // 管理员用户返回 empty，表示不限制供应商
        if (isAdminUser(userId)) {
            return Optional.empty();
        }
        String userDetailFilterColumn = props.getUserDetailFilterColumn();
        if (userDetailFilterColumn == null || userDetailFilterColumn.isBlank()) {
            userDetailFilterColumn = "USER_ID";
        }
        String supplierIdFromDetailColumn = props.getSupplierIdFromUserDetailColumn();
        if (supplierIdFromDetailColumn == null || supplierIdFromDetailColumn.isBlank()) {
            supplierIdFromDetailColumn = "SUPPLIER_COMPANY_ID";
        }
        String safeUserDetailFilterColumn = safeIdentifier(userDetailFilterColumn);
        String safeSupplierIdFromDetailColumn = safeIdentifier(supplierIdFromDetailColumn);
        String sql = "SELECT " + safeSupplierIdFromDetailColumn + " AS supplier_pk " +
                "FROM tb_user_detail WHERE " + safeUserDetailFilterColumn + " = ? AND IS_DELETED = 0";
        System.err.println("[DEBUG] findSupplierCompanyIdByUserId: sql=" + sql + ", userId=" + userId);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            System.err.println("[DEBUG] findSupplierCompanyIdByUserId: rows.size=" + rows.size());
            if (rows.isEmpty()) {
                System.err.println("[DEBUG] findSupplierCompanyIdByUserId: no rows found, returning empty");
                return Optional.empty();
            }
            Object supplierPk = rows.get(0).get("supplier_pk");
            System.err.println("[DEBUG] findSupplierCompanyIdByUserId: supplier_pk=" + supplierPk);
            return Optional.ofNullable(convertToLong(supplierPk));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * 判断用户是否为管理员（角色名包含"管理员"）
     */
    public boolean isAdminUser(String userId) {
        try {
            // 优先检查 tb_user_detail.IS_SUPPLIER_ADMIN（最快，不依赖其他表）
            try {
                String adminSql = "SELECT IS_SUPPLIER_ADMIN FROM tb_user_detail WHERE USER_ID = ? AND IFNULL(IS_DELETED, 0) = 0";
                List<Map<String, Object>> adminRows = jdbcTemplate.queryForList(adminSql, userId);
                if (!adminRows.isEmpty()) {
                    Object adminObj = adminRows.get(0).get("IS_SUPPLIER_ADMIN");
                    if (adminObj != null) {
                        String val = adminObj.toString().trim();
                        if ("是".equals(val) || "1".equals(val) || "true".equalsIgnoreCase(val)) {
                            return true;
                        }
                    }
                }
            } catch (Exception ex) {
                // IS_SUPPLIER_ADMIN 字段不存在时忽略，继续其他检查
            }

            // 检查 blade_user 表中的账号名是否为 admin 或 管理员
            String userPkCol = safeIdentifier(props.getPkColumns().getOrDefault("blade_user", "id"));
            String accountSql = "SELECT account FROM blade_user WHERE " + userPkCol + " = ? AND IFNULL(is_deleted, 0) = 0";
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(accountSql, userId);
            if (!userRows.isEmpty()) {
                Object accountObj = userRows.get(0).get("account");
                if (accountObj != null) {
                    String account = accountObj.toString().toLowerCase();
                    if ("admin".equals(account) || "管理员".equals(account)) {
                        return true;
                    }
                }
            }

            // 检查用户角色是否包含"管理员"
            try {
                String userRoleSql = """
                    SELECT r.role_name FROM blade_user_role ur
                    INNER JOIN blade_role r ON ur.role_id = r.id
                    WHERE ur.user_id = ? AND IFNULL(ur.is_deleted, 0) = 0 AND IFNULL(r.is_deleted, 0) = 0
                    """;
                List<Map<String, Object>> roleRows = jdbcTemplate.queryForList(userRoleSql, userId);
                for (Map<String, Object> row : roleRows) {
                    Object roleNameObj = row.get("role_name");
                    if (roleNameObj != null && roleNameObj.toString().contains("管理员")) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                // blade_user_role 表结构异常时忽略
            }

            return false;
        } catch (Exception ex) {
            System.err.println("isAdminUser error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * 判断用户是否为供应商客户（SUPPLIER_CLIENT 为"是"）
     */
    public boolean isSupplierClient(String userId) {
        try {
            String sql = "SELECT SUPPLIER_CLIENT FROM tb_user_detail WHERE USER_ID = ? AND IFNULL(IS_DELETED, 0) = 0";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            if (rows.isEmpty()) return false;
            Object valueObj = rows.get(0).get("SUPPLIER_CLIENT");
            if (valueObj == null) return false;
            String value = valueObj.toString().trim();
            return "是".equals(value) || "1".equals(value) || "true".equalsIgnoreCase(value);
        } catch (Exception ex) {
            System.err.println("isSupplierClient error: " + ex.getMessage());
            return false;
        }
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

    private Object getValueIgnoreCase(Map<String, Object> map, String key) {
        if (map.containsKey(key)) return map.get(key);
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key)) return map.get(k);
        }
        return null;
    }

    public Map<String, Object> getDashboardStats(Long supplierId) {
        // 统计待确认订单 (status = 1)
        String pendingOrdersSql = "SELECT COUNT(*) FROM tb_purchase_parts_order WHERE supplier_id = ? AND supplier_order_status = 0 AND order_status = 1  AND is_deleted = 0";
        Long pendingOrders = jdbcTemplate.queryForObject(pendingOrdersSql, Long.class, supplierId);

        // 统计在途 ASN (SHIPPED / CREATED)，排除已删除
        String activeAsnSql = "SELECT COUNT(*) FROM tb_srm_asn WHERE supplier_id = ? AND status IN ('CREATED', 'SHIPPED') AND IFNULL(is_deleted, 0) = 0";
        Long activeAsn = jdbcTemplate.queryForObject(activeAsnSql, Long.class, supplierId);

        // 统计今日到货 (实际到货日期 = 今天 且 已确认发货，统计ship_qty总和)，排除已删除
        String todayArrivingSql = "SELECT COALESCE(SUM(ship_qty), 0) FROM tb_srm_asn WHERE supplier_id = ? AND actual_eta_date = CURDATE() AND status = 'SHIPPED' AND IFNULL(is_deleted, 0) = 0";
        Long todayArriving = jdbcTemplate.queryForObject(todayArrivingSql, Long.class, supplierId);

        return Map.of(
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "activeAsn", activeAsn != null ? activeAsn : 0,
                "todayArriving", todayArriving != null ? todayArriving : 0
        );
    }

    /**
     * 管理员用的汇总统计（不限供应商）
     */
    public Map<String, Object> getDashboardStatsAll() {
        // 统计待确认订单 (status = 1)
        String pendingOrdersSql = "SELECT COUNT(*) FROM tb_purchase_parts_order WHERE supplier_order_status = 0 AND order_status = 1 AND is_deleted = 0";
        Long pendingOrders = jdbcTemplate.queryForObject(pendingOrdersSql, Long.class);

        // 统计在途 ASN (SHIPPED / CREATED)，排除已删除
        String activeAsnSql = "SELECT COUNT(*) FROM tb_srm_asn WHERE status IN ('CREATED', 'SHIPPED') AND IFNULL(is_deleted, 0) = 0";
        Long activeAsn = jdbcTemplate.queryForObject(activeAsnSql, Long.class);

        // 统计今日到货
        String todayArrivingSql = "SELECT COALESCE(SUM(ship_qty), 0) FROM tb_srm_asn WHERE actual_eta_date = CURDATE() AND status = 'SHIPPED' AND IFNULL(is_deleted, 0) = 0";
        Long todayArriving = jdbcTemplate.queryForObject(todayArrivingSql, Long.class);

        return Map.of(
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "activeAsn", activeAsn != null ? activeAsn : 0,
                "todayArriving", todayArriving != null ? todayArriving : 0
        );
    }

    public List<Map<String, Object>> listOrdersBySupplier(String supplierId, int limit, int offset) {
        String filterColumn = props.getPurchaseOrderSupplierFilterColumn();
        if (filterColumn == null || filterColumn.isBlank()) {
            filterColumn = "supplier_id";
        }

        String safeTable = safeIdentifier("tb_purchase_parts_order");
        String safeColumn = safeIdentifier(filterColumn);
        String safePk = safeIdentifier(props.getPkColumns().getOrDefault("tb_purchase_parts_order", "id"));

        String sql = "SELECT * FROM " + safeTable + " WHERE " + safeColumn + " = ? " +
                "ORDER BY " + safePk + " DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, supplierId, limit, offset);
    }

    public List<Map<String, Object>> listInvoicesByOrder(String orderId, int limit, int offset) {
        // 若配置了 tb_invoice 直连字段（如 ORDER_ID），优先走直连；未配置则跳过
        String invoiceOrderIdColumn = props.getInvoiceOrderIdColumn();
        String safeInvoiceTable = safeIdentifier("tb_invoice");
        String safeInvoicePk = safeIdentifier(props.getPkColumns().getOrDefault("tb_invoice", "ID"));
        if (invoiceOrderIdColumn != null && !invoiceOrderIdColumn.isBlank()) {
            String safeInvoiceOrderIdColumn = safeIdentifier(invoiceOrderIdColumn);
            String directSql = "SELECT * FROM " + safeInvoiceTable + " WHERE " + safeInvoiceOrderIdColumn + " = ? " +
                    "ORDER BY " + safeInvoicePk + " DESC LIMIT ? OFFSET ?";
            try {
                return jdbcTemplate.queryForList(directSql, orderId, limit, offset);
            } catch (BadSqlGrammarException ex) {
                // 字段尚未加到数据库时，回退到 SALES_ORDER_NO 兼容逻辑
            }
        }

        // tb_purchase_parts_order.order_no -> tb_invoice.SALES_ORDER_NO
        String purchaseOrderNoColumn = props.getPurchaseOrderNoColumn();
        if (purchaseOrderNoColumn == null || purchaseOrderNoColumn.isBlank()) {
            purchaseOrderNoColumn = "order_no";
        }

        String invoiceSalesOrderNoColumn = props.getInvoiceSalesOrderNoColumn();
        if (invoiceSalesOrderNoColumn == null || invoiceSalesOrderNoColumn.isBlank()) {
            invoiceSalesOrderNoColumn = "SALES_ORDER_NO";
        }

        String safePurchaseOrderNoColumn = safeIdentifier(purchaseOrderNoColumn);
        String safeInvoiceSalesOrderNoColumn = safeIdentifier(invoiceSalesOrderNoColumn);
        String safeOrderPk = safeIdentifier(props.getPkColumns().getOrDefault("tb_purchase_parts_order", "id"));

        // 先取订单号
        String orderNoSql = "SELECT " + safePurchaseOrderNoColumn + " AS order_no " +
                "FROM tb_purchase_parts_order WHERE " + safeOrderPk + " = ?";
        Map<String, Object> orderRow;
        try {
            orderRow = jdbcTemplate.queryForMap(orderNoSql, orderId);
        } catch (EmptyResultDataAccessException ex) {
            return List.of();
        }
        Object orderNo = orderRow.get("order_no");
        if (orderNo == null) {
            return List.of();
        }

        String sql = "SELECT * FROM " + safeInvoiceTable + " WHERE " + safeInvoiceSalesOrderNoColumn + " = ? " +
                "ORDER BY " + safeInvoicePk + " DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, orderNo, limit, offset);
    }

    public int normalizeLimit(int limit) {
        if (limit <= 0) return props.getDefaultPageSize();
        if (limit > 200) return 200;
        return limit;
    }

    public int normalizeOffset(int offset) {
        if (offset < 0) return 0;
        return offset;
    }

    private String md5(String input) {
        return hash(input, "MD5");
    }

    private String sha1(String input) {
        return hash(input, "SHA-1");
    }

    private String hash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
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

    public AuthResult authenticateUser(String account, String password) {
        if (account == null || password == null) return AuthResult.error("账号和密码不能为空");
        
        try {
            String acc = account.trim();
            String pwd = password.trim();
            
            String authAccCol = props.getAuthAccountColumn();
            String authPwdCol = props.getAuthPasswordColumn();
            
            if (authAccCol == null || authPwdCol == null) {
                return AuthResult.error("系统配置错误：未指定认证字段列名");
            }

            String safeTable = safeIdentifier("blade_user");
            String safeAccountCol = safeIdentifier(authAccCol);
            String userPkCol = safeIdentifier(props.getPkColumns().getOrDefault("blade_user", "id"));

            // 1. 查找用户是否存在
            String userSql = "SELECT * FROM " + safeTable + " WHERE " + safeAccountCol + " = ? AND tenant_id = ? AND IFNULL(is_deleted, 0) = 0";
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(userSql, acc, 629965);
            
            if (userRows.isEmpty()) {
                System.err.println("[Auth] User not found: " + acc);
                return AuthResult.error("账号不存在");
            }

            Map<String, Object> userRow = userRows.get(0);
            Object dbPasswordObj = getValueIgnoreCase(userRow, authPwdCol);
            String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);
            String newPasswordHash = sha1(pwd);
            // 前端发 md5(密码)，后端 sha1(前端发的) 对比数据库
            boolean pwdMatch = dbPassword.equalsIgnoreCase(newPasswordHash);
            
            if (!pwdMatch) {
                return AuthResult.error("密码错误");
            }

            Object userIdObj = getValueIgnoreCase(userRow, userPkCol);
            Long userId = convertToLong(userIdObj);
            if (userId == null) return AuthResult.error("用户数据异常（ID缺失）");

            // 3. 查找 tb_user_detail 关联
            String userDetailTable = safeIdentifier("tb_user_detail");
            String userDetailUserCol = safeIdentifier(props.getUserDetailFilterColumn() == null ? "USER_ID" : props.getUserDetailFilterColumn());
            String supplierIdCol = safeIdentifier(props.getSupplierIdFromUserDetailColumn() == null ? "SUPPLIER_COMPANY_ID" : props.getSupplierIdFromUserDetailColumn());

            String detailSql = "SELECT * FROM " + userDetailTable + " WHERE " + userDetailUserCol + " = ? AND IFNULL(IS_DELETED, 0) = 0";
            List<Map<String, Object>> detailRows = jdbcTemplate.queryForList(detailSql, userId);
            
            if (detailRows.isEmpty()) {
                System.err.println("[Auth] No tb_user_detail for userId: " + userId);
                // 开发环境下，允许用户登录，即使没有绑定供应商
                // return AuthResult.error("该用户尚未绑定供应商公司（详情资料缺失）");
            }

            Map<String, Object> detailRow = null;
            Object supplierIdObj = null;
            if (!detailRows.isEmpty()) {
                detailRow = detailRows.get(0);
                supplierIdObj = getValueIgnoreCase(detailRow, supplierIdCol);
            }
            
            if (supplierIdObj == null) {
                System.err.println("[Auth] SUPPLIER_COMPANY_ID is null for userId: " + userId);
                // 开发环境下，允许用户登录，即使没有绑定供应商
                // return AuthResult.error("该用户尚未绑定供应商公司，无法使用门户功能");
            }
            
            return AuthResult.success(userId);
        } catch (Exception e) {
            System.err.println("[Auth] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return AuthResult.error("登录服务内部异常: " + e.getMessage());
        }
    }

    public AuthResult changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null) {
            return AuthResult.error("用户ID、旧密码和新密码不能为空");
        }

        try {
            String authAccCol = props.getAuthAccountColumn();
            String authPwdCol = props.getAuthPasswordColumn();

            if (authAccCol == null || authPwdCol == null) {
                return AuthResult.error("系统配置错误：未指定认证字段列名");
            }

            String safeTable = safeIdentifier("blade_user");
            String safeAccountCol = safeIdentifier(authAccCol);
            String userPkCol = safeIdentifier(props.getPkColumns().getOrDefault("blade_user", "id"));
            String safePwdCol = safeIdentifier(authPwdCol);

            // 1. 查找用户是否存在
            String userSql = "SELECT * FROM " + safeTable + " WHERE " + userPkCol + " = ? AND IFNULL(is_deleted, 0) = 0";
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(userSql, userId);

            if (userRows.isEmpty()) {
                System.err.println("[Change Password] User not found: " + userId);
                return AuthResult.error("用户不存在");
            }

            Map<String, Object> userRow = userRows.get(0);
            Object dbPasswordObj = getValueIgnoreCase(userRow, authPwdCol);
            String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);

            // 前端发 md5(密码)，后端 sha1(前端发的) 对比数据库
            boolean pwdMatch = dbPassword.equalsIgnoreCase(sha1(oldPassword));

            if (!pwdMatch) {
                return AuthResult.error("旧密码错误");
            }

            // 新密码：前端发 md5(明文)，后端 sha1 后存数据库
            String encryptedNewPassword = sha1(newPassword);

            // 4. 更新密码
            String updateSql = "UPDATE " + safeTable + " SET " + safePwdCol + " = ? WHERE " + userPkCol + " = ?";
            int updated = jdbcTemplate.update(updateSql, encryptedNewPassword, userId);

            if (updated == 0) {
                return AuthResult.error("密码更新失败");
            }

            return AuthResult.success(userId);
        } catch (Exception e) {
            System.err.println("[Change Password] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return AuthResult.error("修改密码服务内部异常: " + e.getMessage());
        }
    }

    public static class AuthResult {
        public final boolean success;
        public final Long userId;
        public final String message;

        private AuthResult(boolean success, Long userId, String message) {
            this.success = success;
            this.userId = userId;
            this.message = message;
        }

        public static AuthResult success(Long userId) { return new AuthResult(true, userId, "登录成功"); }
        public static AuthResult error(String msg) { return new AuthResult(false, null, msg); }
    }

    // 下划线转驼峰工具方法
    private Map<String, Object> convertKeysToCamelCase(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            result.put(toCamelCase(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String toCamelCase(String str) {
        if (str == null) return str;
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}

