package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import srm.portal.config.SrmOrderProperties;
import srm.portal.dto.ConfirmAndSplitRequest;
import srm.portal.dto.OrderActionRequest;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.service.OrderSplitFlowService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/collaboration/orders")
public class OrderCollaborationController {
    private final OrderCollaborationRepository orderRepo;
    private final GenericTableLookupRepository lookupRepo;
    private final SrmOrderProperties orderProps;
    private final JdbcTemplate jdbcTemplate;
    private final OrderSplitFlowService orderSplitFlowService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderCollaborationController(
            OrderCollaborationRepository orderRepo,
            GenericTableLookupRepository lookupRepo,
            SrmOrderProperties orderProps,
            JdbcTemplate jdbcTemplate,
            OrderSplitFlowService orderSplitFlowService
    ) {
        this.orderRepo = orderRepo;
        this.lookupRepo = lookupRepo;
        this.orderProps = orderProps;
        this.jdbcTemplate = jdbcTemplate;
        this.orderSplitFlowService = orderSplitFlowService;
    }

    // 解析 plan_user_name JSON 数组，提取所有 userName 用逗号连接
    private Map<String, Object> processOrderUserName(Map<String, Object> order) {
        Object userNameObj = order.get("plan_user_name");
        if (userNameObj != null) {
            try {
                String userNameStr = userNameObj.toString();
                if (userNameStr.startsWith("[")) {
                    List<Map<String, Object>> users = objectMapper.readValue(
                            userNameStr, new TypeReference<List<Map<String, Object>>>() {});
                    String displayName = users.stream()
                            .map(u -> String.valueOf(u.get("userName")))
                            .filter(n -> n != null && !"null".equals(n))
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
                    order.put("plan_user_name", displayName);
                }
            } catch (Exception e) {
                // 解析失败，保持原值
            }
        }
        return order;
    }

    @GetMapping("/new")
    public ResponseEntity<Map<String, Object>> listNewOrdersForUser(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) Integer supplierOrderStatus,
            @RequestParam(required = false) List<String> deliveryStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder
    ) {
        // 检查是否为管理员用户
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));

        // 非管理员用户必须绑定供应商
        if (!isAdmin && supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看订单"));
        }

        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        // 管理员用户不限制供应商
        Long supplierIdValue = isAdmin ? null : supplierId.orElse(null);

        Map<String, Object> queryResult = orderRepo.listNewOrderDetailsWithCount(
                supplierIdValue, orderNo, partsNumber, supplierOrderStatus, deliveryStatus, startDate, endDate, l, o, sortField, sortOrder
        );

        // 创建可变的结果 Map
        Map<String, Object> result = new HashMap<>(queryResult);

        // 转换 items 中的 id 字段为字符串
        if (result.containsKey("items") && result.get("items") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
            result.put("items", items.stream().map(this::convertIdToString).toList());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> listOrderHeadersForUser(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<Integer> supplierOrderStatus,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) List<String> deliveryStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder
    ) {
        // 检查是否为管理员用户
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));

        // 非管理员用户必须绑定供应商
        if (!isAdmin && supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看订单"));
        }

        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        // 管理员用户不限制供应商
        Long supplierIdValue = isAdmin ? null : supplierId.orElse(null);

        long total = orderRepo.getOrderMapper().countOrderHeaders(
                supplierIdValue, orderNo, supplierOrderStatus, supplierName, deliveryStatus, startDate, endDate
        );

        List<Map<String, Object>> items = orderRepo.getOrderMapper().listOrderHeaders(
                supplierIdValue, orderNo, supplierOrderStatus, supplierName, deliveryStatus, startDate, endDate, l, o, sortField, sortOrder
        );

        // 将 id 字段转换为字符串，防止 JavaScript 精度丢失
        List<Map<String, Object>> convertedItems = items.stream()
                .map(this::convertIdToString)
                .toList();

        return ResponseEntity.ok(Map.of("items", convertedItems, "total", total));
    }

    @GetMapping("/headers/summary")
    public ResponseEntity<Map<String, Object>> getOrderHeadersSummary(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) List<Integer> supplierOrderStatus,
            @RequestParam(required = false) List<String> deliveryStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        // 检查是否为管理员用户
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));

        // 非管理员用户必须绑定供应商
        if (!isAdmin && supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看订单"));
        }

        // 管理员用户不限制供应商
        Long supplierIdValue = isAdmin ? null : supplierId.orElse(null);

        Map<String, Object> stats = orderRepo.getOrderMapper().getOrderSummaryStats(
                supplierIdValue, orderNo, partsNumber, supplierOrderStatus, deliveryStatus, startDate, endDate
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * 将 Map 中的 id 字段(Long)转换为字符串，防止 JavaScript 大整数精度丢失
     */
    private Map<String, Object> convertIdToString(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>(map);
        
        // 转换 id 字段
        if (result.containsKey("id") && result.get("id") != null) {
            Object id = result.get("id");
            result.put("id", String.valueOf(id));
        }
        
        // 转换 order_id, order_pk 等其他可能的 ID 字段
        if (result.containsKey("order_id") && result.get("order_id") != null) {
            result.put("order_id", String.valueOf(result.get("order_id")));
        }
        if (result.containsKey("order_pk") && result.get("order_pk") != null) {
            result.put("order_pk", String.valueOf(result.get("order_pk")));
        }
        if (result.containsKey("supplier_id") && result.get("supplier_id") != null) {
            result.put("supplier_id", String.valueOf(result.get("supplier_id")));
        }
        
        return result;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Optional<Long> userSupplier = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));

        // 管理员用户可以查看所有订单
        if (isAdmin) {
            Optional<Map<String, Object>> order = orderRepo.getOrderById(orderId);
            if (order.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<Map<String, Object>> details = orderRepo.getOrderDetailsByOrderNo((String) order.get().get("order_no"));
            return ResponseEntity.ok(Map.of(
                    "order", processOrderUserName(order.get()),
                    "details", details
            ));
        }

        // 非管理员用户：如果绑定了供应商，则只能查看自己供应商的订单
        if (userSupplier.isPresent()) {
            Optional<Map<String, Object>> order = orderRepo.getOrderById(orderId);
            if (order.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Object orderSupplierId = order.get().get("supplier_id");
            if (orderSupplierId == null || !userSupplier.get().equals(Long.valueOf(orderSupplierId.toString()))) {
                return ResponseEntity.status(403).body(Map.of("message", "无权查看该订单"));
            }
            List<Map<String, Object>> details = orderRepo.getOrderDetailsByOrderNo((String) order.get().get("order_no"));
            return ResponseEntity.ok(Map.of(
                    "order", processOrderUserName(order.get()),
                    "details", details
            ));
        }

        // 没有绑定供应商的用户也可以查看所有订单（用于演示/测试）
        Optional<Map<String, Object>> order = orderRepo.getOrderById(orderId);
        if (order.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> details = orderRepo.getOrderDetailsByOrderNo((String) order.get().get("order_no"));
        return ResponseEntity.ok(Map.of(
                "order", processOrderUserName(order.get()),
                "details", details
        ));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderActionRequest req
    ) {
        ResponseEntity<Map<String, Object>> denied = assertSupplierOwnsOrder(orderId, req.getOperatorUserId());
        if (denied != null) {
            return denied;
        }
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()));
        String actionLog = buildActionLog("确认订单", req.getOperatorUserId(), req.getReason());
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId,
                supplierId.get(),
                orderProps.getConfirmedStatus(),
                req.getOperatorUserId(),
                actionLog
        );
        if (affected == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", orderProps.getConfirmedStatus(),
                "message", "订单已确认"
        ));
    }

    @PostMapping("/details/{orderDetailId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmOrderDetail(
            @PathVariable Long orderDetailId,
            @Valid @RequestBody OrderActionRequest req
    ) {
        // 获取订单详情信息
        Optional<Map<String, Object>> detailOpt = orderRepo.getOrderDetailById(orderDetailId);
        if (detailOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> detail = detailOpt.get();
        Long orderId = Long.valueOf(detail.get("order_id").toString());

        // 校验供应商权限
        ResponseEntity<Map<String, Object>> denied = assertSupplierOwnsOrder(orderId, req.getOperatorUserId());
        if (denied != null) {
            return denied;
        }

        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
        }

        // 校验订单详情确实属于该供应商
        Object detailSupplierId = detail.get("supplier_id");
        if (detailSupplierId == null || !supplierId.get().equals(Long.valueOf(detailSupplierId.toString()))) {
            return ResponseEntity.status(403).body(Map.of("message", "该订单详情不属于当前供应商"));
        }

        // 更新订单详情的确认状态为20
        String actionLog = buildActionLog("确认零件", req.getOperatorUserId(), req.getReason());
        orderRepo.updateOrderDetailStatus(orderDetailId, 20, req.getOperatorUserId());

        // 重新计算订单状态：根据所有订单详情的状态决定订单状态
        List<Map<String, Object>> allDetails = orderRepo.getOrderDetailsByOrderId(orderId);
        boolean allConfirmed = true;
        boolean anyConfirmed = false;
        for (Map<String, Object> d : allDetails) {
            Object statusObj = d.get("supplier_order_status");
            int status = statusObj != null ? Integer.parseInt(statusObj.toString()) : 0;
            if (status == 0) {
                allConfirmed = false;
            } else {
                anyConfirmed = true;
            }
        }

        int newOrderStatus;
        if (allConfirmed) {
            newOrderStatus = 20; // 全部确认
        } else if (anyConfirmed) {
            newOrderStatus = 21; // 部分确认
        } else {
            newOrderStatus = 0; // 全部未确认
        }

        // 更新订单状态
        orderRepo.updatesupplierOrderStatusOnly(orderId, newOrderStatus);

        return ResponseEntity.ok(Map.of(
                "orderDetailId", orderDetailId,
                "orderId", orderId,
                "status", newOrderStatus,
                "message", newOrderStatus == 20 ? "零件已确认" : "零件已确认，订单"+ (newOrderStatus == 21 ? "部分确认" : "未确认")
        ));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderActionRequest req
    ) {
        if (req.getReason() == null || req.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "驳回订单必须填写原因"));
        }
        ResponseEntity<Map<String, Object>> denied = assertSupplierOwnsOrder(orderId, req.getOperatorUserId());
        if (denied != null) {
            return denied;
        }
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()));
        String actionLog = buildActionLog("驳回订单", req.getOperatorUserId(), req.getReason());
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId,
                supplierId.get(),
                orderProps.getRejectedStatus(),
                req.getOperatorUserId(),
                actionLog
        );
        if (affected == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", orderProps.getRejectedStatus(),
                "message", "订单已驳回"
        ));
    }

    @PostMapping("/detail/{detailId}/update")
    public ResponseEntity<Map<String, Object>> updateOrderDetail(
            @PathVariable Long detailId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作"));
        }
        
        // 校验该明细是否属于该供应商的订单
        String checkSql = """
                SELECT COUNT(*) FROM tb_purchase_parts_order_detail d
                JOIN tb_purchase_parts_order o ON d.order_id = o.id
                WHERE d.id = ? AND o.supplier_id = ? AND d.is_deleted = 0
                """;
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, detailId, supplierId.get());
        if (count == null || count == 0) {
            return ResponseEntity.status(403).body(Map.of("message", "无权修改该明细或明细不存在"));
        }

        String needDate = (String) body.get("need_date");
        Object needNumberObj = body.get("need_number");
        
        StringBuilder updateSql = new StringBuilder("UPDATE tb_purchase_parts_order_detail SET update_time = ?, update_user = ? ");
        List<Object> params = new ArrayList<>();
        params.add(LocalDateTime.now());
        params.add(userId);

        if (needDate != null) {
            updateSql.append(", need_date = ? ");
            params.add(needDate);
        }
        if (needNumberObj != null) {
            updateSql.append(", need_number = ? ");
            params.add(needNumberObj);
        }
        
        updateSql.append(" WHERE id = ?");
        params.add(detailId);
        
        jdbcTemplate.update(updateSql.toString(), params.toArray());
        return ResponseEntity.ok(Map.of("message", "修改成功"));
    }

    @GetMapping("/{orderId}/asn")
    public ResponseEntity<List<Map<String, Object>>> getOrderAsnList(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        // 校验订单权限
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            return ResponseEntity.status(403).build();
        }

        // 查询ASN列表（排除已删除），带上零件信息
        String sql = """
                SELECT a.id, a.asn_no, a.create_time, a.ship_qty as total_quantity,
                       a.logistics_company as carrier, a.tracking_no, a.eta_date, a.status,
                       a.ship_date, a.actual_eta_date, a.warehouse_in_no,
                       d.parts_number AS parts_no, d.parts_name
                FROM tb_srm_asn a
                INNER JOIN tb_srm_asn_order ao ON a.id = ao.asn_id
                LEFT JOIN tb_purchase_parts_order_detail d ON a.order_detail_id = d.id
                WHERE ao.order_id = ? AND IFNULL(a.is_deleted, 0) = 0
                ORDER BY a.create_time DESC
                """;
        List<Map<String, Object>> asnList = jdbcTemplate.queryForList(sql, orderId);
        return ResponseEntity.ok(asnList);
    }

    @GetMapping("/{orderId}/receipts")
    public ResponseEntity<List<Map<String, Object>>> getOrderReceipts(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        // 校验订单权限
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            return ResponseEntity.status(403).build();
        }

        // 查询收货记录（这里需要根据实际的收货表结构调整SQL）
        String sql = """
                SELECT r.id, r.receipt_no, r.asn_no, r.receipt_date,
                       r.parts_name, r.qualified_quantity, r.rejected_quantity, r.warehouse
                FROM tb_receipt r
                WHERE r.order_id = ?
                  AND r.is_deleted = 0
                ORDER BY r.receipt_date DESC
                """;
        List<Map<String, Object>> receipts = jdbcTemplate.queryForList(sql, orderId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{orderId}/timeline")
    public ResponseEntity<List<Map<String, Object>>> getOrderTimeline(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        // 校验订单权限
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            return ResponseEntity.status(403).build();
        }

        // 查询时间轴数据（这里可以从订单备注或其他日志表中提取）
        List<Map<String, Object>> timeline = new ArrayList<>();

        // 添加订单创建事件
        Optional<Map<String, Object>> order = orderRepo.getOrderById(orderId);
        if (order.isPresent()) {
            timeline.add(Map.of(
                    "title", "订单由ERP创建并下达",
                    "time", order.get().get("order_time"),
                    "color", "blue",
                    "icon", "clock-circle-o",
                    "description", "订单号：" + order.get().get("order_no")
            ));

            // 解析订单备注中的操作日志
            String remark = (String) order.get().get("order_remark");
            if (remark != null && !remark.isBlank()) {
                String[] entries = remark.split("\n");
                for (String entry : entries) {
                    if (entry.contains("[SRM-")) {
                        // 解析日志格式：[SRM-确认订单] time=2024-04-08T14:30:00, userId=123, reason=-
                        String[] parts = entry.split("time=|userId=|reason=");
                        if (parts.length >= 4) {
                            String action = parts[0].replace("[SRM-", "").replace("]", "");
                            String time = parts[1].split(",")[0];
                            String userIdStr = parts[2].split(",")[0];
                            String reason = parts[3];

                            String color = "green";
                            String iconType = "check-circle-o";
                            if (action.contains("驳回")) {
                                color = "red";
                                iconType = "close-circle-o";
                            }

                            timeline.add(Map.of(
                                    "title", "供应商" + action,
                                    "time", time,
                                    "color", color,
                                    "icon", iconType,
                                    "description", "操作人ID：" + userIdStr + (reason.equals("-") ? "" : "，原因：" + reason)
                            ));
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/{orderId}/audit-log")
    public ResponseEntity<List<Map<String, Object>>> getOrderAuditLog(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        // 校验订单权限
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            return ResponseEntity.status(403).build();
        }

        // 查询审计日志（这里可以从专门的审计表或订单备注中提取）
        List<Map<String, Object>> auditLog = new ArrayList<>();

        Optional<Map<String, Object>> order = orderRepo.getOrderById(orderId);
        if (order.isPresent()) {
            String remark = (String) order.get().get("order_remark");
            if (remark != null && !remark.isBlank()) {
                String[] entries = remark.split("\n");
                for (String entry : entries) {
                    if (entry.contains("[SRM-")) {
                        String[] parts = entry.split("time=|userId=|reason=");
                        if (parts.length >= 4) {
                            String action = parts[0].replace("[SRM-", "").replace("]", "");
                            String time = parts[1].split(",")[0];
                            String userIdStr = parts[2].split(",")[0];
                            String reason = parts[3];

                            auditLog.add(Map.of(
                                    "operation_time", time,
                                    "operator", "用户" + userIdStr,
                                    "operation_type", action,
                                    "old_value", "-",
                                    "new_value", reason.equals("-") ? "已" + action : reason
                            ));
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(auditLog);
    }

    @PostMapping("/detail/{detailId}/split")
    public ResponseEntity<Map<String, Object>> splitOrderDetail(
            @PathVariable Long detailId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作"));
        }

        // 校验该明细是否属于该供应商的订单
        String checkSql = """
                SELECT d.*, o.order_no FROM tb_purchase_parts_order_detail d
                JOIN tb_purchase_parts_order o ON d.order_id = o.id
                WHERE d.id = ? AND o.supplier_id = ? AND d.is_deleted = 0
                """;
        List<Map<String, Object>> details = jdbcTemplate.queryForList(checkSql, detailId, supplierId.get());
        if (details.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作该明细或明细不存在"));
        }

        Map<String, Object> originalDetail = details.get(0);
        Integer originalQuantity = (Integer) originalDetail.get("need_number");

        Integer quantity1 = Integer.valueOf(body.get("quantity1").toString());
        Integer quantity2 = Integer.valueOf(body.get("quantity2").toString());
        String expectedDate1 = (String) body.get("expectedDate1");
        String expectedDate2 = (String) body.get("expectedDate2");

        if (quantity1 + quantity2 != originalQuantity) {
            return ResponseEntity.badRequest().body(Map.of("message", "拆分数量总和必须等于原始数量"));
        }

        // 插入新的明细行（不指定id，让数据库自动生成）
        String insertSql = """
                INSERT INTO tb_purchase_parts_order_detail
                (order_id, parts_number, parts_name, need_number, need_date, expected_delivery_date,
                 create_time, create_user, update_time, update_user, status, is_deleted)
                SELECT order_id, parts_number, parts_name, ?, ?, ?,
                       ?, ?, ?, ?, 0, 0
                FROM tb_purchase_parts_order_detail
                WHERE id = ?
                """;

        jdbcTemplate.update(insertSql,
                quantity1, expectedDate1, expectedDate1,
                LocalDateTime.now(), userId, LocalDateTime.now(), userId,
                detailId);

        // 更新原始明细行
        String updateSql = """
                UPDATE tb_purchase_parts_order_detail
                SET need_number = ?, expected_delivery_date = ?,
                    update_time = ?, update_user = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(updateSql, quantity2, expectedDate2, LocalDateTime.now(), userId, detailId);

        return ResponseEntity.ok(Map.of("message", "拆分成功"));
    }

    /**
     * ASN.md：订单确认与拆单（数量守恒、防已发后重拆）。
     */
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
                    "拆单表未就绪，请在库中执行 sql/create_tb_srm_split_asn_line.sql"
            ));
        }
    }

    /**
     * 待发货数据源：按订单列出拆分行及剩余可发数量。
     */
    @GetMapping("/{orderId}/split-lines")
    public ResponseEntity<Map<String, Object>> listSplitLines(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        ResponseEntity<Map<String, Object>> denied = assertSupplierOwnsOrder(orderId, userId);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(Map.of("items", orderSplitFlowService.listSplitLinesForUser(orderId, userId)));
        } catch (IllegalStateException ex) {
            if ("ORDER_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("message", "订单不存在"));
            }
            throw ex;
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "拆单表未就绪，请在库中执行 sql/create_tb_srm_split_asn_line.sql"
            ));
        }
    }

    /**
     * 待发货订单列表。
     */
    @GetMapping("/pending-shipment")
    public ResponseEntity<Map<String, Object>> listPendingShipmentOrderDetails(
            @RequestParam Long userId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        // 检查是否为管理员用户
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));

        // 非管理员用户必须绑定供应商
        if (!isAdmin && supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看订单"));
        }

        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        // 管理员用户不限制供应商
        Long supplierIdValue = isAdmin ? null : supplierId.orElse(null);

        try {
            // 解析零件号列表（支持中英文逗号分隔）
            List<String> partsNumberList = (partsNumber != null && !partsNumber.isBlank())
                ? java.util.Arrays.asList(partsNumber.replace('，', ',').split(","))
                : null;

            // 查询待发货订单列表
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listPendingShipmentOrderDetails(
                    supplierIdValue, orderNo, partsNumberList, startDate, endDate, l, o
            );

            // 统计待发货订单数量
            long total = orderRepo.getOrderMapper().countPendingShipmentOrderDetails(
                    supplierIdValue, orderNo, partsNumberList, startDate, endDate
            );

            // 返回结果
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

    /**
     * 根据订单ID列表查询订单信息
     */
    @GetMapping("/details/by-ids")
    public ResponseEntity<Map<String, Object>> getOrderDetailsByIds(
            @RequestParam String ids,
            @RequestParam Long userId
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "用户未绑定供应商"));
        }

        // 解析ID列表
        List<String> orderIdList = new ArrayList<>();
        for (String id : ids.split(",")) {
            String trimmedId = id.trim();
            if (!trimmedId.isEmpty()) {
                orderIdList.add(trimmedId);
            }
        }

        if (orderIdList.isEmpty()) {
            return ResponseEntity.ok(Map.of("orders", new ArrayList<>(), "contactInfo", new HashMap<>()));
        }

        // 获取用户联系信息
        Map<String, Object> contactInfo = new HashMap<>();
        Optional<Map<String, Object>> userDetail = lookupRepo.findUserDetailByUserId(String.valueOf(userId));
        if (userDetail.isPresent()) {
            Map<String, Object> detail = userDetail.get();
            contactInfo.put("contactName", detail.get("realName"));
            contactInfo.put("contactPhone", detail.get("phone"));
            contactInfo.put("contactEmail", detail.get("email"));
            contactInfo.put("address", detail.get("address"));
        }
        // 获取供应商名称
        Optional<Map<String, Object>> supplier = lookupRepo.findSupplierByUserId(String.valueOf(userId));
        if (supplier.isPresent()) {
            contactInfo.put("supplierName", supplier.get().get("supplierName"));
        }

        // 查询订单信息及发运明细
        List<Map<String, Object>> ordersWithShipments = new ArrayList<>();
        for (String orderId : orderIdList) {
            try {
                // 尝试将订单号转换为Long类型
                Long orderIdLong = Long.valueOf(orderId);
                Optional<Map<String, Object>> orderOpt = orderRepo.getOrderById(orderIdLong);
                if (orderOpt.isPresent()) {
                    Map<String, Object> order = orderOpt.get();
                    // 校验该订单是否属于该供应商
                    Object orderSupplierId = order.get("supplier_id");
                    if (orderSupplierId != null && supplierId.get().equals(Long.valueOf(orderSupplierId.toString()))) {
                        // 获取该订单的订单明细（发运明细）
                        List<Map<String, Object>> orderDetails = orderRepo.getOrderDetailsByOrderNo((String) order.get("order_no"));
                        order.put("orderDetails", orderDetails);

                        // 获取ASN发运信息（排除已删除）
                        String asnSql = """
                            SELECT a.id, a.asn_no, a.create_time, a.ship_qty, a.logistics_company,
                                   a.tracking_no, a.eta_date, a.status
                            FROM tb_srm_asn a
                            INNER JOIN tb_srm_asn_order ao ON a.id = ao.asn_id
                            WHERE ao.order_id = ? AND IFNULL(a.is_deleted, 0) = 0
                            ORDER BY a.create_time DESC
                            LIMIT 10
                            """;
                        List<Map<String, Object>> asnList = jdbcTemplate.queryForList(asnSql, orderIdLong);
                        order.put("asnList", asnList);

                        ordersWithShipments.add(order);
                    }
                }
            } catch (NumberFormatException e) {
                // 如果订单号不是数字，可能是字符串格式的订单号，尝试按订单号查询
                List<Map<String, Object>> orders = orderRepo.getOrderDetailsByOrderNo(orderId);
                for (Map<String, Object> ord : orders) {
                    Object orderSupplierId = ord.get("supplier_id");
                    if (orderSupplierId != null && supplierId.get().equals(Long.valueOf(orderSupplierId.toString()))) {
                        // 获取该订单的订单明细
                        List<Map<String, Object>> orderDetails = orderRepo.getOrderDetailsByOrderNo(orderId);
                        ord.put("orderDetails", orderDetails);

                        // 获取ASN发运信息（排除已删除）
                        Object actualOrderId = ord.get("order_id");
                        if (actualOrderId != null) {
                            String asnSql = """
                                SELECT a.id, a.asn_no, a.create_time, a.ship_qty, a.logistics_company,
                                       a.tracking_no, a.eta_date, a.status
                                FROM tb_srm_asn a
                                INNER JOIN tb_srm_asn_order ao ON a.id = ao.asn_id
                                WHERE ao.order_id = ? AND IFNULL(a.is_deleted, 0) = 0
                                ORDER BY a.create_time DESC
                                LIMIT 10
                                """;
                            List<Map<String, Object>> asnList = jdbcTemplate.queryForList(asnSql, actualOrderId);
                            ord.put("asnList", asnList);
                        }

                        ordersWithShipments.add(ord);
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "orders", ordersWithShipments,
                "contactInfo", contactInfo
        ));
    }

    private String buildActionLog(String action, Long userId, String reason) {
        String safeReason = (reason == null || reason.isBlank()) ? "-" : reason.trim();
        return "\n[SRM-" + action + "] time=" + LocalDateTime.now() + ", userId=" + userId + ", reason=" + safeReason;
    }

    /**
     * @return null 表示校验通过；否则返回 403/404 响应
     */
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

