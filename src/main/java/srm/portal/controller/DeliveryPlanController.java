package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import srm.portal.dto.DeliveryPlanCreateRequest;
import srm.portal.dto.DeliveryPlanShipRequest;
import srm.portal.service.DeliveryPlanDbService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration/delivery-plan")
public class DeliveryPlanController {
    private final DeliveryPlanDbService deliveryPlanService;

    public DeliveryPlanController(DeliveryPlanDbService deliveryPlanService) {
        this.deliveryPlanService = deliveryPlanService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDeliveryPlan(@Valid @RequestBody DeliveryPlanCreateRequest req) {
        try {
            return ResponseEntity.ok(deliveryPlanService.createDeliveryPlan(req));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("ORDER_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("message", "订单不存在"));
            }
            if ("ORDER_NOT_BELONG_TO_SUPPLIER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "该订单不属于当前供应商"));
            }
            if ("PLAN_QUANTITY_EXCEEDS_ORDER".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "计划数量超过订单数量"));
            }
            if ("INVALID_ORDER".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "订单数据无效"));
            }
            if ("ORDER_DETAIL_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "订单明细不存在"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            System.err.println("================= DataAccessException in createDeliveryPlan =================");
            System.err.println("Message: " + ex.getMessage());
            if (ex.getCause() != null) {
                System.err.println("Cause: " + ex.getCause().getMessage());
            }
            ex.printStackTrace();
            String actualMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            return ResponseEntity.status(503).body(Map.of("message", "数据库错误: " + actualMsg));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> listDeliveryPlanByOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        try {
            List<Map<String, Object>> items = deliveryPlanService.listDeliveryPlanByOrder(orderId);
            return ResponseEntity.ok(Map.of("items", items));
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "数据库错误"));
        }
    }

    @GetMapping("/supplier")
    public ResponseEntity<Map<String, Object>> listDeliveryPlanBySupplier(
            @RequestParam Long userId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String partsNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        try {
            int limit = pageSize;
            int offset = Math.max(0, (pageNum - 1)) * pageSize;
            List<String> partsNoList = (partsNo != null && !partsNo.isBlank())
                ? java.util.Arrays.asList(partsNo.replace('，', ',').split(","))
                : null;
            List<Map<String, Object>> items = deliveryPlanService.listDeliveryPlanBySupplier(userId, orderNo, partsNoList, startDate, endDate, status, limit, offset);
            int total = deliveryPlanService.countDeliveryPlanBySupplier(userId, orderNo, partsNoList, startDate, endDate, status);
            return ResponseEntity.ok(Map.of("items", items, "total", total));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            System.err.println("================= DataAccessException in listDeliveryPlanBySupplier =================");
            System.err.println("Message: " + ex.getMessage());
            if (ex.getCause() != null) {
                System.err.println("Cause: " + ex.getCause().getMessage());
            }
            ex.printStackTrace();
            return ResponseEntity.status(503).body(Map.of("message", "数据库错误: " + ex.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> listUpcomingDeliveryPlan(
            @RequestParam Long userId
    ) {
        try {
            List<Map<String, Object>> items = deliveryPlanService.listUpcomingDeliveryPlan(userId);
            return ResponseEntity.ok(Map.of("items", items));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "数据库错误"));
        }
    }

    @PutMapping("/{planId}/ship")
    public ResponseEntity<Map<String, Object>> updateDeliveryPlanShipInfo(
            @PathVariable Long planId,
            @Valid @RequestBody DeliveryPlanShipRequest req
    ) {
        try {
            req.setPlanId(planId);
            return ResponseEntity.ok(deliveryPlanService.updateDeliveryPlanShipInfo(req));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("PLAN_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("message", "交付计划不存在"));
            }
            if ("NO_PLAN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权操作该交付计划"));
            }
            if ("INVALID_STATUS_FOR_SHIP".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "只有状态为计划中的交付计划才能发货"));
            }
            if ("ACTUAL_QUANTITY_EXCEEDS_PLAN".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "实际发货数量不能超过计划数量"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "数据库错误"));
        }
    }
}
