package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import srm.portal.dto.ConfirmAndSplitRequest;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.service.OrderSplitFlowService;

import java.util.Map;
import java.util.Optional;

/**
 * ASN.md 文档中的 {@code POST /api/v1/orders/{po_id}/confirm-and-split} 别名。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class V1OrderSplitController {
    private final OrderSplitFlowService orderSplitFlowService;
    private final OrderCollaborationRepository orderRepo;
    private final GenericTableLookupRepository lookupRepo;

    public V1OrderSplitController(
            OrderSplitFlowService orderSplitFlowService,
            OrderCollaborationRepository orderRepo,
            GenericTableLookupRepository lookupRepo
    ) {
        this.orderSplitFlowService = orderSplitFlowService;
        this.orderRepo = orderRepo;
        this.lookupRepo = lookupRepo;
    }

    @PostMapping("/{orderId}/confirm-and-split")
    public ResponseEntity<Map<String, Object>> confirmAndSplit(
            @PathVariable Long orderId,
            @Valid @RequestBody ConfirmAndSplitRequest req
    ) {
        ResponseEntity<Map<String, Object>> denied = assertSupplierOwnsOrder(orderId, req.getOperatorUserId());
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(orderSplitFlowService.confirmAndSplit(orderId, req));
        } catch (IllegalStateException ex) {
            String code = ex.getMessage();
            if ("SPLIT_QTY_MISMATCH".equals(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "拆分行数量之和必须等于该明细需求数量"));
            }
            if ("SPLIT_LINES_REQUIRED".equals(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "SPLIT_AND_CONFIRM 时必须提交 splitLines"));
            }
            if ("UNKNOWN_ACTION".equals(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "action 须为 CONFIRM_ONLY 或 SPLIT_AND_CONFIRM"));
            }
            if ("CANNOT_REDEFINE_SPLITS_WITH_SHIPMENTS".equals(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "该明细已有发货记录，不可重新拆单"));
            }
            if ("DETAIL_NOT_FOUND".equals(code)) {
                return ResponseEntity.badRequest().body(Map.of("message", "明细不存在或不属于该订单"));
            }
            if ("ORDER_NOT_FOUND".equals(code)) {
                return ResponseEntity.status(404).body(Map.of("message", "订单不存在"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "拆单表未就绪，请执行 sql/create_tb_srm_split_asn_line.sql"
            ));
        }
    }

    private ResponseEntity<Map<String, Object>> assertSupplierOwnsOrder(Long orderId, Long operatorUserId) {
        Optional<Long> userSupplier = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId));
        if (userSupplier.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
        }
        Optional<Long> orderSupplier = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplier.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "订单不存在或已删除"));
        }
        if (!userSupplier.get().equals(orderSupplier.get())) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作该订单"));
        }
        return null;
    }
}
