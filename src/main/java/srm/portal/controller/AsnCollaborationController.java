package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import srm.portal.dto.CreateAsnFromOrdersRequest;
import srm.portal.service.AsnDbService;

import java.util.List;
import java.util.Map;

/**
 * 处理协作相关的 ASN 接口
 */
@RestController
@RequestMapping("/api/collaboration/asn")
public class AsnCollaborationController {
    private final AsnDbService asnService;

    public AsnCollaborationController(AsnDbService asnService) {
        this.asnService = asnService;
    }

    @PostMapping("/from-orders")
    public ResponseEntity<List<Map<String, Object>>> createFromOrders(@Valid @RequestBody CreateAsnFromOrdersRequest req) {
        System.err.println("================= AsnCollaborationController.createFromOrders() 开始执行 =================");
        System.err.println("请求内容: " + req);
        try {
            List<Map<String, Object>> result = asnService.createAsnFromOrders(req);
            System.err.println("================= AsnCollaborationController.createFromOrders() 执行成功 =================");
            return ResponseEntity.ok(result);
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
            if ("OVER_SHIP".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "发运数量不可超过订单剩余可发量")));
            }
            if ("LINES_EMPTY".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(List.of(Map.of("message", "请至少选择一条待发货订单")));
            }
            throw ex;
        } catch (DataAccessException ex) {
            System.err.println("================= AsnCollaborationController.createFromOrders() 执行出错 (DataAccessException) =================");
            ex.printStackTrace();
            return ResponseEntity.status(503).body(List.of(Map.of(
                    "message",
                    "ASN表未就绪，请执行 sql/create_tb_srm_asn.sql",
                    "error",
                    ex.getMessage()
            )));
        }
    }
}
