package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import srm.portal.dto.AsnCreateFromSplitsRequest;
import srm.portal.dto.CreateAsnFromOrdersRequest;
import srm.portal.service.AsnDbService;

import java.util.List;
import java.util.Map;

/**
 * ASN.md 文档中的 {@code POST /api/v1/asn/create} 别名（请求体兼容 carrier / split_line_id 等 snake_case）。
 */
@RestController
@RequestMapping("/api/v1/asn")
public class V1AsnCreateController {
    private final AsnDbService asnService;

    public V1AsnCreateController(AsnDbService asnService) {
        this.asnService = asnService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AsnCreateFromSplitsRequest req) {
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

    @PostMapping({"/create-from-orders", "/collaboration/asn/from-orders"})
    public ResponseEntity<List<Map<String, Object>>> createFromOrders(@Valid @RequestBody CreateAsnFromOrdersRequest req) {
        try {
            return ResponseEntity.ok(asnService.createAsnFromOrders(req));
        } catch (IllegalStateException ex) {
            if ("NO_SUPPLIER_FOR_USER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(List.of(Map.of("message", "当前用户未绑定供应商")));
            }
            if ("ORDER_NOT_BELONG_TO_SUPPLIER".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(List.of(Map.of("message", "订单不属于当前供应商")));
            }
            if ("ORDER_DETAIL_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "订单详情不存在")));
            }
            if ("DIFFERENT_SHIP_TO_PLANT".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "所选订单的送货工厂不同，无法合并生成同一张发货单，请重新选择！")));
            }
            if ("OVER_SHIP".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "发运数量不可超过订单剩余可发量")));
            }
            if ("LINES_EMPTY".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "请至少选择一条待发货订单")));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(List.of(Map.of(
                    "message",
                    "ASN表未就绪，请执行 sql/create_tb_srm_asn.sql"
            )));
        }
    }
}
