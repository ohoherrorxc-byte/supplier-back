package srm.portal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import srm.portal.config.SrmOrderProperties;
import srm.portal.dto.AsnCreateFromSplitsRequest;
import srm.portal.dto.AsnCreateRequest;
import srm.portal.dto.CreateAsnFromOrdersRequest;
import srm.portal.dto.ReceiveAsnLineRequest;
import srm.portal.repository.AsnRepository;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.repository.PoSplitAsnRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AsnDbService {
    private final AsnRepository asnRepository;
    private final PoSplitAsnRepository splitAsnRepo;
    private final GenericTableLookupRepository lookupRepo;
    private final OrderCollaborationRepository orderRepo;
    private final SrmOrderProperties orderProps;

    public AsnDbService(
            AsnRepository asnRepository,
            PoSplitAsnRepository splitAsnRepo,
            GenericTableLookupRepository lookupRepo,
            OrderCollaborationRepository orderRepo,
            SrmOrderProperties orderProps
    ) {
        this.asnRepository = asnRepository;
        this.splitAsnRepo = splitAsnRepo;
        this.lookupRepo = lookupRepo;
        this.orderRepo = orderRepo;
        this.orderProps = orderProps;
    }

    @Transactional
    public Map<String, Object> createAsn(AsnCreateRequest req) {
        Long operatorUserId = req.getOperatorUserId();
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        if (req.getOrderIds() == null || req.getOrderIds().isEmpty()) {
            throw new IllegalStateException("ORDER_IDS_EMPTY");
        }
        if (!orderRepo.allOrdersBelongToSupplier(req.getOrderIds(), supplierId)) {
            throw new IllegalStateException("ORDER_NOT_BELONG_TO_SUPPLIER");
        }
        if (!orderRepo.allOrdersConfirmedForSupplier(req.getOrderIds(), supplierId, orderProps.getConfirmedStatus())) {
            throw new IllegalStateException("ORDER_NOT_CONFIRMED");
        }

        long asnId = asnRepository.insertHeader(
                supplierId,
                req.getLogisticsCompany(),
                req.getTrackingNo(),
                req.getEtaDate(),
                "SHIPPED"
        );
        for (Long orderId : req.getOrderIds()) {
            asnRepository.insertOrderLink(asnId, orderId);
        }
        LocalDateTime now = LocalDateTime.now();
        asnRepository.insertTimeline(asnId, "ASN_CREATED", "供应商创建发货通知", now);

        return buildAsnView(asnId);
    }

    /**
     * 按拆分行创建 ASN：每行绑定 split_line_id，防超发（ASN.md 模块 B）。
     */
    @Transactional
    public Map<String, Object> createAsnFromSplitLines(AsnCreateFromSplitsRequest req) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        if (req.getLines() == null || req.getLines().isEmpty()) {
            throw new IllegalStateException("LINES_EMPTY");
        }

        List<Long> splitIds = req.getLines().stream()
                .map(AsnCreateFromSplitsRequest.AsnSplitShipLine::getSplitLineId)
                .sorted()
                .distinct()
                .toList();
        Map<Long, Map<String, Object>> locked = new LinkedHashMap<>();
        for (Long sid : splitIds) {
            Map<String, Object> row = splitAsnRepo.lockSplitLine(sid)
                    .orElseThrow(() -> new IllegalStateException("SPLIT_NOT_FOUND"));
            locked.put(sid, row);
        }

        Map<Long, Integer> remaining = new HashMap<>();
        Set<Long> orderIds = new LinkedHashSet<>();
        for (Map.Entry<Long, Map<String, Object>> e : locked.entrySet()) {
            Map<String, Object> row = e.getValue();
            int splitQty = toInt(row.get("split_qty"));
            int shipped = toInt(row.get("shipped_qty"));
            remaining.put(e.getKey(), splitQty - shipped);
            long oid = ((Number) row.get("order_id")).longValue();
            orderIds.add(oid);
        }

        for (Long oid : orderIds) {
            Long sup = orderRepo.findSupplierIdByOrderId(oid).orElse(null);
            if (sup == null || !sup.equals(supplierId)) {
                throw new IllegalStateException("ORDER_NOT_BELONG_TO_SUPPLIER");
            }
        }
        if (!orderRepo.allOrdersConfirmedForSupplier(new ArrayList<>(orderIds), supplierId, orderProps.getConfirmedStatus())) {
            throw new IllegalStateException("ORDER_NOT_CONFIRMED");
        }

        Map<Long, Integer> temp = new HashMap<>(remaining);
        for (AsnCreateFromSplitsRequest.AsnSplitShipLine line : req.getLines()) {
            int left = temp.getOrDefault(line.getSplitLineId(), -1);
            if (left < 0) {
                throw new IllegalStateException("SPLIT_NOT_FOUND");
            }
            if (line.getShippedQty() > left) {
                throw new IllegalStateException("OVER_SHIP");
            }
            temp.put(line.getSplitLineId(), left - line.getShippedQty());
        }

        long asnId = asnRepository.insertHeader(
                supplierId,
                req.getLogisticsCompany(),
                req.getTrackingNo(),
                req.getEtaDate(),
                "0"
        );
        for (Long oid : orderIds) {
            asnRepository.insertOrderLink(asnId, oid);
        }
        LocalDateTime now = LocalDateTime.now();
        for (AsnCreateFromSplitsRequest.AsnSplitShipLine line : req.getLines()) {
            splitAsnRepo.insertAsnLine(asnId, line.getSplitLineId(), line.getShippedQty());
            splitAsnRepo.addSplitShippedQty(line.getSplitLineId(), line.getShippedQty());
            splitAsnRepo.refreshSplitRollupStatus(line.getSplitLineId());
        }
        asnRepository.insertTimeline(asnId, "ASN_CREATED", "按拆分行创建发货通知", now);
        return buildAsnView(asnId);
    }

    @Transactional
    public void receiveAsnLine(long asnLineId, ReceiveAsnLineRequest req) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Map<String, Object> al = splitAsnRepo.lockAsnLine(asnLineId)
                .orElseThrow(() -> new IllegalStateException("ASN_LINE_NOT_FOUND"));
        long asnId = ((Number) al.get("asn_id")).longValue();
        Optional<Long> asnSupplierId = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplierId.isEmpty() || !asnSupplierId.get().equals(supplierId)) {
            throw new IllegalStateException("NO_ASN_ACCESS");
        }
        int lineShipped = toInt(al.get("shipped_qty"));
        int lineReceived = toInt(al.get("received_qty"));
        if (lineReceived + req.getReceivedQty() > lineShipped) {
            throw new IllegalStateException("OVER_RECEIVE");
        }
        long splitLineId = ((Number) al.get("split_line_id")).longValue();
        LocalDateTime now = LocalDateTime.now();
        splitAsnRepo.addAsnLineReceived(asnLineId, req.getReceivedQty(), now);
        splitAsnRepo.addSplitReceivedQty(splitLineId, req.getReceivedQty());
        splitAsnRepo.refreshSplitRollupStatus(splitLineId);
        asnRepository.insertTimeline(asnId, "GOODS_RECEIPT", "ASN 明细行收货 " + req.getReceivedQty(), now);
    }

    public Optional<Map<String, Object>> getAsn(long asnId) {
        if (asnRepository.findHeaderById(asnId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildAsnView(asnId));
    }

    public Optional<Map<String, Object>> getAsnForUser(long asnId, Long operatorUserId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Optional<Long> asnSupplierId = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplierId.isEmpty()) {
            return Optional.empty();
        }
        if (!asnSupplierId.get().equals(supplierId)) {
            throw new IllegalStateException("NO_ASN_ACCESS");
        }
        return getAsn(asnId);
    }

    public List<Map<String, Object>> getTimelineForUser(long asnId, Long operatorUserId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Optional<Long> asnSupplierId = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplierId.isEmpty()) {
            throw new IllegalStateException("ASN_NOT_FOUND");
        }
        if (!asnSupplierId.get().equals(supplierId)) {
            throw new IllegalStateException("NO_ASN_ACCESS");
        }
        return getTimeline(asnId);
    }

    public List<Map<String, Object>> getTimeline(long asnId) {
        return asnRepository.findTimelineByAsnId(asnId);
    }

    public Map<String, Object> listAsnForUser(Long userId, int limit, int offset) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));

        List<Map<String, Object>> items = asnRepository.listAsnBySupplier(supplierId, limit, offset);
        long total = asnRepository.countAsnBySupplier(supplierId);

        return Map.of("items", items, "total", total);
    }

    public List<Map<String, Object>> listUpcomingDeliveryPlan(Long userId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        return asnRepository.listUpcomingAsnBySupplier(supplierId);
    }

    @Transactional
    public void shipAsn(long asnId, Long operatorUserId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Optional<Long> asnSupplierId = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplierId.isEmpty()) {
            throw new IllegalStateException("ASN_NOT_FOUND");
        }
        if (!asnSupplierId.get().equals(supplierId)) {
            throw new IllegalStateException("NO_ASN_ACCESS");
        }
        Optional<Map<String, Object>> header = asnRepository.findHeaderById(asnId);
        if (header.isEmpty()) {
            throw new IllegalStateException("ASN_NOT_FOUND");
        }
        String currentStatus = (String) header.get().get("status");
        if (!"CREATED".equals(currentStatus)) {
            throw new IllegalStateException("INVALID_STATUS_FOR_SHIP");
        }
        asnRepository.updateAsnStatus(asnId, "SHIPPED");
        LocalDateTime now = LocalDateTime.now();
        asnRepository.insertTimeline(asnId, "ASN_SHIPPED", "供应商标记为已发货", now);
    }

    /**
     * 软删除 ASN（设置 is_deleted = 1）
     */
    @Transactional
    public void deleteAsn(long asnId, Long operatorUserId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Optional<Long> asnSupplierId = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplierId.isEmpty()) {
            throw new IllegalStateException("ASN_NOT_FOUND");
        }
        if (!asnSupplierId.get().equals(supplierId)) {
            throw new IllegalStateException("NO_ASN_ACCESS");
        }
        Optional<Map<String, Object>> header = asnRepository.findHeaderById(asnId);
        if (header.isEmpty()) {
            throw new IllegalStateException("ASN_NOT_FOUND");
        }
        String currentStatus = (String) header.get().get("status");
        if ("SHIPPED".equals(currentStatus) || "RECEIVED".equals(currentStatus)) {
            throw new IllegalStateException("CANNOT_DELETE_SHIPPED_ASN");
        }
        asnRepository.softDeleteAsn(asnId);
    }

    private Map<String, Object> buildAsnView(long asnId) {
        Map<String, Object> header = asnRepository.findHeaderById(asnId).orElseThrow();
        List<Long> orderIds = asnRepository.findOrderIdsByAsnId(asnId);
        List<Map<String, Object>> asnLines = asnRepository.findAsnLinesByAsnId(asnId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("asnId", header.get("id"));
        out.put("asnNo", header.get("asn_no"));
        out.put("supplierId", header.get("supplier_id"));
        out.put("orderIds", orderIds);
        out.put("logisticsCompany", header.get("logistics_company"));
        out.put("trackingNo", header.get("tracking_no"));
        out.put("etaDate", header.get("eta_date"));
        out.put("status", header.get("status"));
        out.put("asnLines", asnLines);
        Object ct = header.get("create_time");
        if (ct instanceof java.sql.Timestamp ts) {
            out.put("createdAt", ts.toLocalDateTime());
        } else {
            out.put("createdAt", ct);
        }
        return out;
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString());
    }

    /**
     * 从订单创建 ASN（每行独立生成 ASN 编号）
     */
    @Transactional
    public List<Map<String, Object>> createAsnFromOrders(CreateAsnFromOrdersRequest req) {
        Long operatorUserId = req.getOperatorUserId();
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));

        if (req.getLines() == null || req.getLines().isEmpty()) {
            throw new IllegalStateException("LINES_EMPTY");
        }

        // 预先查询所有订单详情，缓存起来
        Map<Long, Map<String, Object>> detailCache = new LinkedHashMap<>();
        for (CreateAsnFromOrdersRequest.AsnOrderLine line : req.getLines()) {
            Map<String, Object> orderDetail = orderRepo.getOrderDetailById(line.getOrderDetailId())
                    .orElseThrow(() -> new IllegalStateException("ORDER_DETAIL_NOT_FOUND"));
            detailCache.put(line.getOrderDetailId(), orderDetail);

            // 校验供应商
            Object supplierIdObj = orderDetail.get("supplier_id");
            if (supplierIdObj == null) {
                throw new IllegalStateException("ORDER_DETAIL_MISSING_SUPPLIER_ID");
            }
            Long orderSupplierId = Long.valueOf(supplierIdObj.toString());
            if (!orderSupplierId.equals(supplierId)) {
                throw new IllegalStateException("ORDER_NOT_BELONG_TO_SUPPLIER");
            }

            // 校验发货数量
            int needNumber = toInt(orderDetail.get("order_qty"));
            int shippedNumber = toInt(orderDetail.get("shipped_qty"));
            int remainingQty = needNumber - shippedNumber;
            if (line.getShippedQty() > remainingQty) {
                throw new IllegalStateException("OVER_SHIP");
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 每行独立生成 ASN
        for (CreateAsnFromOrdersRequest.AsnOrderLine line : req.getLines()) {
            LocalDate shipDate = line.getShipDate() != null && !line.getShipDate().isEmpty()
                    ? LocalDate.parse(line.getShipDate())
                    : LocalDate.now();

            // 生成唯一 ASN 编号并插入占位记录
            String asnNo = asnRepository.generateAsnNo(supplierId, shipDate);
            long asnId = asnRepository.lockAsnNoAndGetId(asnNo, supplierId);

            // 更新为真实数据
            asnRepository.updateAsnOrderDetailAndShipQty(
                    asnId,
                    line.getOrderDetailId(),
                    line.getShippedQty(),
                    line.getShipDate(),
                    line.getActualEtaDate(),
                    line.getWarehouseInNo()
            );

            // 关联到订单
            Map<String, Object> orderDetail = detailCache.get(line.getOrderDetailId());
            Long orderId = Long.valueOf(orderDetail.get("order_id").toString());
            asnRepository.insertOrderLink(asnId, orderId);

            // 更新订单状态
            updatesupplierOrderStatus(orderId);

            // 记录时间轴
            asnRepository.insertTimeline(asnId, "ASN_CREATED", "从订单创建发货通知", now);

            // 生成返回视图
            result.add(buildAsnView(asnId));
        }

        return result;
    }

    /**
     * 更新订单状态
     */
    private void updatesupplierOrderStatus(Long orderId) {
        // 计算订单的总需求数量
        int totalNeedQuantity = orderRepo.getOrderMapper().getOrderTotalNeedQuantity(orderId);
        // 计算订单相关的所有ASN的ship_qty总和
        int totalShippedQuantity = orderRepo.getOrderMapper().getOrderTotalShippedQuantity(orderId);

        // 判断发运状态
        if (totalNeedQuantity > 0) {
            if (totalShippedQuantity >= totalNeedQuantity) {
                orderRepo.updatesupplierOrderStatus(orderId, 30); // 全部发运
            } else if (totalShippedQuantity > 0) {
                orderRepo.updatesupplierOrderStatus(orderId, 25); // 部分发运
            }
        }
    }
}
