package srm.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import srm.portal.repository.GenericTableLookupRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LookupController {
    private final GenericTableLookupRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public LookupController(GenericTableLookupRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/suppliers/{supplierId}")
    public ResponseEntity<Map<String, Object>> getSupplier(@PathVariable String supplierId) {
        return repo.findSupplierById(supplierId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBase(@PathVariable String userId) {
        return repo.findUserBaseById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/detail")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable String userId) {
        return repo.findUserDetailByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return repo.findOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<Map<String, Object>> getInvoice(@PathVariable String invoiceId) {
        return repo.findInvoiceById(invoiceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam Long userId) {
        if (repo.isAdminUser(String.valueOf(userId))) {
            return ResponseEntity.ok(repo.getDashboardStatsAll());
        }
        Optional<Long> supplierId = repo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(repo.getDashboardStats(supplierId.get()));
    }

    @GetMapping("/users/{userId}/supplier-profile")
    public ResponseEntity<Map<String, Object>> getSupplierProfile(@PathVariable String userId) {
        var user = repo.findUserBaseById(userId).orElse(null);
        var detail = repo.findUserDetailByUserId(userId).orElse(null);
        var supplier = repo.findSupplierByUserId(userId).orElse(null);

        if (user == null && supplier == null && detail == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> res = new HashMap<>();
        res.put("user", user);
        res.put("userDetail", detail);
        res.put("supplier", supplier);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@PathVariable String userId) {
        boolean isAdmin = repo.isAdminUser(userId);
        boolean isSupplierClient = repo.isSupplierClient(userId);
        return ResponseEntity.ok(Map.of(
                "isAdmin", isAdmin,
                "isSupplierClient", isSupplierClient
        ));
    }

    @GetMapping("/suppliers/{supplierId}/orders")
    public ResponseEntity<Map<String, Object>> listOrdersBySupplier(
            @PathVariable String supplierId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int l = repo.normalizeLimit(limit);
        int o = repo.normalizeOffset(offset);
        return ResponseEntity.ok(Map.of("items", repo.listOrdersBySupplier(supplierId, l, o)));
    }

    @GetMapping("/orders/{orderId}/invoices")
    public ResponseEntity<Map<String, Object>> listInvoicesByOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int l = repo.normalizeLimit(limit);
        int o = repo.normalizeOffset(offset);
        return ResponseEntity.ok(Map.of("items", repo.listInvoicesByOrder(orderId, l, o)));
    }

    @GetMapping("/debug/order-id-mapping")
    public ResponseEntity<Map<String, Object>> debugOrderIdMapping(@RequestParam Long userId) {
        // 尝试先找到用户绑定的供应商ID
        try {
            Optional<Long> supplierId = repo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierId.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "用户未绑定供应商",
                    "userId", userId
                ));
            }
            
            // 直接用供应商ID查询订单
            String sql = "SELECT po.id, po.supplier_id FROM tb_purchase_parts_order po " +
                    "WHERE po.supplier_id = ? AND IFNULL(po.is_deleted, 0) = 0 " +
                    "ORDER BY po.id DESC LIMIT 10";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, supplierId.get());
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "supplierId", supplierId.get(),
                "orderCount", results.size(),
                "orders", results
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/order-id-mapping-all-fields")
    public ResponseEntity<Map<String, Object>> debugOrderIdMappingAllFields(@RequestParam Long userId) {
        // 用SELECT *查询所有字段，和API接口返回一致
        try {
            Optional<Long> supplierId = repo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierId.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "用户未绑定供应商",
                    "userId", userId
                ));
            }
            
            // 用SELECT * 查询，和接口完全一样
            String sql = "SELECT * FROM tb_purchase_parts_order po " +
                    "WHERE po.supplier_id = ? AND IFNULL(po.is_deleted, 0) = 0 " +
                    "ORDER BY po.id DESC LIMIT 10";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, supplierId.get());
            
            // 只提取ID用于对比
            List<Object> ids = results.stream().map(r -> r.get("id")).toList();
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "supplierId", supplierId.get(),
                "orderCount", results.size(),
                "ids", ids,
                "firstRecord", results.isEmpty() ? null : results.get(0)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

