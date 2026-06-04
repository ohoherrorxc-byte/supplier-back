package srm.portal.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Repository;
import srm.portal.config.SrmDbProperties;
import srm.portal.mapper.SupplierMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Repository
public class GenericTableLookupRepository {
    private final SupplierMapper supplierMapper;
    private final SrmDbProperties props;

    public GenericTableLookupRepository(SupplierMapper supplierMapper, SrmDbProperties props) {
        this.supplierMapper = supplierMapper;
        this.props = props;
    }

    public Optional<Map<String, Object>> findByPk(String tableName, String pkValue, String pkColumn) {
        Map<String, Object> row = supplierMapper.findByPk(tableName, pkColumn, pkValue);
        return Optional.ofNullable(row);
    }

    public Optional<Map<String, Object>> findByColumn(String tableName, String columnName, String value) {
        Map<String, Object> row = supplierMapper.findByColumn(tableName, columnName, value);
        return Optional.ofNullable(row);
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

    public Optional<Long> findSupplierIdByName(String supplierName) {
        Long id = supplierMapper.findSupplierIdByName(supplierName);
        return Optional.ofNullable(id);
    }

    public Optional<Map<String, Object>> findSupplierByUserId(String userId) {
        String userDetailFilterColumn = props.getUserDetailFilterColumn();
        if (userDetailFilterColumn == null || userDetailFilterColumn.isBlank()) {
            userDetailFilterColumn = "USER_ID";
        }
        String supplierIdFromDetailColumn = props.getSupplierIdFromUserDetailColumn();
        if (supplierIdFromDetailColumn == null || supplierIdFromDetailColumn.isBlank()) {
            supplierIdFromDetailColumn = "SUPPLIER_COMPANY_ID";
        }
        String supplierPkColumn = props.getPkColumns().getOrDefault("tb_contract_supplier", "ID");

        Map<String, Object> row = supplierMapper.findSupplierPkByUserId(userId, userDetailFilterColumn, supplierIdFromDetailColumn);
        if (row == null) {
            return Optional.empty();
        }
        Object supplierPk = row.get("supplier_pk");
        if (supplierPk == null) {
            return Optional.empty();
        }
        return findByPk("tb_contract_supplier", String.valueOf(supplierPk), supplierPkColumn);
    }

    public Optional<Long> findSupplierCompanyIdByUserId(String userId) {
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
        try {
            Long supplierId = supplierMapper.findSupplierCompanyIdByUserId(userId, userDetailFilterColumn, supplierIdFromDetailColumn);
            return Optional.ofNullable(supplierId);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public boolean isAdminUser(String userId) {
        try {
            String adminResult = supplierMapper.findIsSupplierAdmin(userId);
            if (adminResult != null) {
                String val = adminResult.toString().trim();
                if ("是".equals(val) || "1".equals(val) || "true".equalsIgnoreCase(val)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // IS_SUPPLIER_ADMIN 字段不存在时忽略，继续其他检查
        }

        String userPkCol = props.getPkColumns().getOrDefault("blade_user", "id");
        String account = supplierMapper.findAccountByUserId(userId, userPkCol);
        if (account != null) {
            String lower = account.toLowerCase();
            if ("admin".equals(lower) || "管理员".equals(account)) {
                return true;
            }
        }

        try {
            List<String> roleNames = supplierMapper.findRoleNamesByUserId(userId);
            for (String roleName : roleNames) {
                if (roleName != null && roleName.contains("管理员")) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // 表结构异常时忽略
        }

        return false;
    }

    public boolean isSupplierClient(String userId) {
        try {
            String value = supplierMapper.findSupplierClient(userId);
            if (value == null) return false;
            String trimmed = value.toString().trim();
            return "是".equals(trimmed) || "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed);
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
        Long pendingOrders = supplierMapper.countPendingOrders(supplierId);
        Long activeAsn = supplierMapper.countActiveAsn(supplierId);
        Long todayArriving = supplierMapper.countTodayArriving(supplierId);
        Long pendingForecast = supplierMapper.countPendingForecast(supplierId);

        return Map.of(
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "activeAsn", activeAsn != null ? activeAsn : 0,
                "todayArriving", todayArriving != null ? todayArriving : 0,
                "pendingForecast", pendingForecast != null ? pendingForecast : 0
        );
    }

    public Map<String, Object> getDashboardStatsAll() {
        Long pendingOrders = supplierMapper.countPendingOrdersAll();
        Long activeAsn = supplierMapper.countActiveAsnAll();
        Long todayArriving = supplierMapper.countTodayArrivingAll();

        return Map.of(
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "activeAsn", activeAsn != null ? activeAsn : 0,
                "todayArriving", todayArriving != null ? todayArriving : 0,
                "pendingForecast", 0L
        );
    }

    public List<Map<String, Object>> listOrdersBySupplier(String supplierId, int limit, int offset) {
        String filterColumn = props.getPurchaseOrderSupplierFilterColumn();
        if (filterColumn == null || filterColumn.isBlank()) {
            filterColumn = "supplier_id";
        }
        String tableName = "tb_purchase_parts_order";
        String pkColumn = props.getPkColumns().getOrDefault(tableName, "id");

        return supplierMapper.listOrdersBySupplier(supplierId, tableName, filterColumn, pkColumn, limit, offset);
    }

    public List<Map<String, Object>> listInvoicesByOrder(String orderId, int limit, int offset) {
        String invoiceOrderIdColumn = props.getInvoiceOrderIdColumn();
        String safeInvoiceTable = "tb_invoice";
        String safeInvoicePk = props.getPkColumns().getOrDefault(safeInvoiceTable, "ID");
        if (invoiceOrderIdColumn != null && !invoiceOrderIdColumn.isBlank()) {
            try {
                return supplierMapper.listInvoicesByOrderDirect(orderId, safeInvoiceTable, invoiceOrderIdColumn, safeInvoicePk, limit, offset);
            } catch (BadSqlGrammarException ex) {
                // 字段尚未加到数据库时，回退到 SALES_ORDER_NO 兼容逻辑
            }
        }

        String purchaseOrderNoColumn = props.getPurchaseOrderNoColumn();
        if (purchaseOrderNoColumn == null || purchaseOrderNoColumn.isBlank()) {
            purchaseOrderNoColumn = "order_no";
        }

        String invoiceSalesOrderNoColumn = props.getInvoiceSalesOrderNoColumn();
        if (invoiceSalesOrderNoColumn == null || invoiceSalesOrderNoColumn.isBlank()) {
            invoiceSalesOrderNoColumn = "SALES_ORDER_NO";
        }

        String safePurchaseOrderNoColumn = purchaseOrderNoColumn;
        String safeOrderPk = props.getPkColumns().getOrDefault("tb_purchase_parts_order", "id");

        String orderNo = supplierMapper.findOrderNoById(orderId, "tb_purchase_parts_order", safePurchaseOrderNoColumn, safeOrderPk);
        if (orderNo == null) {
            return List.of();
        }

        return supplierMapper.listInvoicesBySalesOrderNo(orderNo, safeInvoiceTable, invoiceSalesOrderNoColumn, safeInvoicePk, limit, offset);
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

            String userPkCol = props.getPkColumns().getOrDefault("blade_user", "id");

            Map<String, Object> userRow = supplierMapper.findBladeUserByAccount(acc, 629965L, authAccCol);
            if (userRow == null || userRow.isEmpty()) {
                System.err.println("[Auth] User not found: " + acc);
                return AuthResult.error("账号不存在");
            }

            Object dbPasswordObj = getValueIgnoreCase(userRow, authPwdCol);
            String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);
            String newPasswordHash = sha1(pwd);
            boolean pwdMatch = dbPassword.equalsIgnoreCase(newPasswordHash);

            if (!pwdMatch) {
                return AuthResult.error("密码错误");
            }

            Object userIdObj = getValueIgnoreCase(userRow, userPkCol);
            Long userId = convertToLong(userIdObj);
            if (userId == null) return AuthResult.error("用户数据异常（ID缺失）");

            String userDetailUserCol = props.getUserDetailFilterColumn() == null ? "USER_ID" : props.getUserDetailFilterColumn();
            String supplierIdCol = props.getSupplierIdFromUserDetailColumn() == null ? "SUPPLIER_COMPANY_ID" : props.getSupplierIdFromUserDetailColumn();

            List<Map<String, Object>> detailRows = supplierMapper.findUserDetailsByUserId(String.valueOf(userId), userDetailUserCol);

            if (detailRows.isEmpty()) {
                System.err.println("[Auth] No tb_user_detail for userId: " + userId);
            }

            Object supplierIdObj = null;
            if (!detailRows.isEmpty()) {
                supplierIdObj = getValueIgnoreCase(detailRows.get(0), supplierIdCol);
            }

            if (supplierIdObj == null) {
                System.err.println("[Auth] SUPPLIER_COMPANY_ID is null for userId: " + userId);
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
            String authPwdCol = props.getAuthPasswordColumn();
            if (authPwdCol == null) {
                return AuthResult.error("系统配置错误：未指定认证字段列名");
            }

            String userPkCol = props.getPkColumns().getOrDefault("blade_user", "id");

            List<Map<String, Object>> userRows = supplierMapper.findUserDetailsByUserId(String.valueOf(userId), userPkCol);
            if (userRows == null || userRows.isEmpty()) {
                return AuthResult.error("用户不存在");
            }

            Map<String, Object> userRow = userRows.get(0);
            Object dbPasswordObj = getValueIgnoreCase(userRow, authPwdCol);
            String dbPassword = dbPasswordObj == null ? "" : String.valueOf(dbPasswordObj);

            boolean pwdMatch = dbPassword.equalsIgnoreCase(sha1(oldPassword));
            if (!pwdMatch) {
                return AuthResult.error("旧密码错误");
            }

            String encryptedNewPassword = sha1(newPassword);
            int updated = supplierMapper.updatePassword(userId, encryptedNewPassword, authPwdCol, userPkCol);

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
}