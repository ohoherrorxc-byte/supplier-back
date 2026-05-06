package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import srm.portal.dto.AsnCreateFromSplitsRequest;
import srm.portal.dto.AsnCreateRequest;
import srm.portal.dto.ReceiveAsnLineRequest;
import srm.portal.service.AsnDbService;

import java.util.Map;

@RestController
@RequestMapping("/api/collaboration/asn")
public class AsnController {
    private final AsnDbService asnService;

    public AsnController(AsnDbService asnService) {
        this.asnService = asnService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAsn(@Valid @RequestBody AsnCreateRequest req) {
        try {
            return ResponseEntity.ok(asnService.createAsn(req));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法创建 ASN"));
            }
            if ("ORDER_NOT_BELONG_TO_SUPPLIER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "存在不属于当前供应商的订单"));
            }
            if ("ORDER_NOT_CONFIRMED".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "只有已确认的订单才可创建 ASN"));
            }
            if ("ORDER_IDS_EMPTY".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "请至少选择一个订单创建 ASN"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN 表未就绪，请在 license 库执行 sql/create_tb_srm_asn.sql 后重试"
            ));
        }
    }

    /**
     * ASN.md：按拆分行创建发货单，明细行记录 split_line_id，防超发。
     */
    @PostMapping("/from-split-lines")
    public ResponseEntity<Map<String, Object>> createAsnFromSplits(@Valid @RequestBody AsnCreateFromSplitsRequest req) {
        try {
            return ResponseEntity.ok(asnService.createAsnFromSplitLines(req));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("ORDER_NOT_BELONG_TO_SUPPLIER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "拆分行所属订单不属于当前供应商"));
            }
            if ("ORDER_NOT_CONFIRMED".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "关联订单须为已确认状态"));
            }
            if ("OVER_SHIP".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "发货数量超过拆分行剩余可发数量"));
            }
            if ("SPLIT_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "拆分行不存在"));
            }
            if ("LINES_EMPTY".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "请至少选择一行拆分明细"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN/拆单表未就绪，请执行 sql/create_tb_srm_asn.sql 与 sql/create_tb_srm_split_asn_line.sql"
            ));
        }
    }

    /**
     * ASN.md：对 ASN 明细行收货，回写拆分行已收与状态卷加。
     */
    @PostMapping("/lines/{asnLineId}/receive")
    public ResponseEntity<Map<String, Object>> receiveAsnLine(
            @PathVariable Long asnLineId,
            @Valid @RequestBody ReceiveAsnLineRequest req
    ) {
        try {
            asnService.receiveAsnLine(asnLineId, req);
            return ResponseEntity.ok(Map.of("message", "收货已登记"));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("NO_ASN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权操作该 ASN"));
            }
            if ("ASN_LINE_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            if ("OVER_RECEIVE".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "收货数量超过该发货行已发数量"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "ASN 明细表未就绪"));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAsn(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            return ResponseEntity.ok(asnService.listAsnForUser(userId, limit, offset));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "ASN 表未就绪"));
        }
    }

    @GetMapping("/{asnId}")
    public ResponseEntity<Map<String, Object>> getAsn(
            @PathVariable Long asnId,
            @RequestParam Long userId
    ) {
        try {
            return asnService.getAsnForUser(asnId, userId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("NO_ASN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权查看该 ASN"));
            }
            if ("ASN_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN 表未就绪，请在 license 库执行 sql/create_tb_srm_asn.sql 后重试"
            ));
        }
    }

    @GetMapping("/{asnId}/timeline")
    public ResponseEntity<Map<String, Object>> getTimeline(
            @PathVariable Long asnId,
            @RequestParam Long userId
    ) {
        try {
            return ResponseEntity.ok(Map.of("items", asnService.getTimelineForUser(asnId, userId)));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("NO_ASN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权查看该 ASN"));
            }
            if ("ASN_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN 表未就绪，请在 license 库执行 sql/create_tb_srm_asn.sql 后重试"
            ));
        }
    }

    @GetMapping("/plan")
    public ResponseEntity<Map<String, Object>> getDeliveryPlan(
            @RequestParam Long userId
    ) {
        try {
            return ResponseEntity.ok(Map.of("items", asnService.listUpcomingDeliveryPlan(userId)));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN 表未就绪，请在 license 库执行 sql/create_tb_srm_asn.sql 后重试"
            ));
        }
    }

    @PostMapping("/{asnId}/ship")
    public ResponseEntity<Map<String, Object>> shipAsn(
            @PathVariable Long asnId,
            @RequestParam Long userId
    ) {
        try {
            asnService.shipAsn(asnId, userId);
            return ResponseEntity.ok(Map.of("message", "ASN 已标记为已发货"));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("NO_ASN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权操作该 ASN"));
            }
            if ("ASN_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            if ("INVALID_STATUS_FOR_SHIP".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "只有状态为已创建的 ASN 才能标记发货"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "ASN 表未就绪，请在 license 库执行 sql/create_tb_srm_asn.sql 后重试"
            ));
        }
    }

    /**
     * 软删除 ASN（设置 is_deleted = 1）
     */
    @DeleteMapping("/{asnId}")
    public ResponseEntity<Map<String, Object>> deleteAsn(
            @PathVariable Long asnId,
            @RequestParam Long userId
    ) {
        try {
            asnService.deleteAsn(asnId, userId);
            return ResponseEntity.ok(Map.of("message", "ASN 已删除"));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            if ("NO_ASN_ACCESS".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message", "无权操作该 ASN"));
            }
            if ("ASN_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            if ("CANNOT_DELETE_SHIPPED_ASN".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "已发货或已到货的 ASN 不可删除"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "ASN 表未就绪"));
        }
    }
}
