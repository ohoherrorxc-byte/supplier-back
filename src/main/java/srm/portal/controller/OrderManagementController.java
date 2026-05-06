package srm.portal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class OrderManagementController {

    @Autowired
    private srm.portal.repository.GenericTableLookupRepository lookupRepo;

    @Autowired
    private srm.portal.repository.OrderCollaborationRepository orderRepo;

    @GetMapping("/sales-forecast")
    public ResponseEntity<Map<String, Object>> listSalesForecast(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看销售预测"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listSalesForecast(
                    supplierId.get(), partsNumber, startDate, endDate, l, o
            );
            
            long total = orderRepo.getOrderMapper().countSalesForecast(
                    supplierId.get(), partsNumber, startDate, endDate
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/purchase-plan")
    public ResponseEntity<Map<String, Object>> listPurchasePlan(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String feedbackStatus,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看采购计划"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listPurchasePlan(
                    supplierId.get(), partsNumber, startDate, endDate, feedbackStatus, l, o
            );
            
            long total = orderRepo.getOrderMapper().countPurchasePlan(
                    supplierId.get(), partsNumber, startDate, endDate, feedbackStatus
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }

    @PostMapping("/purchase-plan/{id}/feedback")
    public ResponseEntity<Map<String, Object>> submitPurchasePlanFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer committedQty = Integer.valueOf(request.get("committedQty").toString());
            String remark = request.get("remark") != null ? request.get("remark").toString() : "";
            
            Map<String, Object> result = orderRepo.submitPurchasePlanFeedback(id, userId, committedQty, remark);
            
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "message",
                    "提交失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/jit-orders")
    public ResponseEntity<Map<String, Object>> listJitOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看 JIT 订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listJitOrders(
                    supplierId.get(), orderNo, partsNumber, startDate, endDate, l, o
            );
            
            long total = orderRepo.getOrderMapper().countJitOrders(
                    supplierId.get(), orderNo, partsNumber, startDate, endDate
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/outsourcing-orders")
    public ResponseEntity<Map<String, Object>> listOutsourcingOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) Integer supplierOrderStatus,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看委外加工订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listOutsourcingOrders(
                    supplierId.get(), orderNo, partsNumber, supplierOrderStatus, l, o
            );
            
            long total = orderRepo.getOrderMapper().countOutsourcingOrders(
                    supplierId.get(), orderNo, partsNumber, supplierOrderStatus
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/assr-orders")
    public ResponseEntity<Map<String, Object>> listAssrOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看 ASSR 订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listAssrOrders(
                    supplierId.get(), orderNo, partsNumber, urgencyLevel, l, o
            );
            
            long total = orderRepo.getOrderMapper().countAssrOrders(
                    supplierId.get(), orderNo, partsNumber, urgencyLevel
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }
}