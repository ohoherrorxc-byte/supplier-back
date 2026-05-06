package srm.portal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import srm.portal.config.SrmOrderProperties;
import srm.portal.dto.ConfirmAndSplitRequest;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.repository.PoSplitAsnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单确认与拆单：数量守恒、1:1 默认拆分行（ASN.md 模块 A）。
 */
@Service
public class OrderSplitFlowService {
    private final PoSplitAsnRepository splitAsnRepo;
    private final OrderCollaborationRepository orderRepo;
    private final GenericTableLookupRepository lookupRepo;
    private final SrmOrderProperties orderProps;

    public OrderSplitFlowService(
            PoSplitAsnRepository splitAsnRepo,
            OrderCollaborationRepository orderRepo,
            GenericTableLookupRepository lookupRepo,
            SrmOrderProperties orderProps
    ) {
        this.splitAsnRepo = splitAsnRepo;
        this.orderRepo = orderRepo;
        this.lookupRepo = lookupRepo;
        this.orderProps = orderProps;
    }

    @Transactional
    public Map<String, Object> confirmAndSplit(long orderId, ConfirmAndSplitRequest req) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Long orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("ORDER_NOT_FOUND"));
        if (!orderSupplierId.equals(supplierId)) {
            throw new IllegalStateException("ORDER_NOT_BELONG");
        }

        Map<String, Object> detail = splitAsnRepo.findDetailRow(req.getDetailId(), orderId)
                .orElseThrow(() -> new IllegalStateException("DETAIL_NOT_FOUND"));

        if (splitAsnRepo.existsPositiveShippedForDetail(req.getDetailId())) {
            throw new IllegalStateException("CANNOT_REDEFINE_SPLITS_WITH_SHIPMENTS");
        }

        splitAsnRepo.deleteSplitsWithZeroShippedByDetail(req.getDetailId());

        int needQty = parseNeedQty(detail);
        LocalDate needDate = parseNeedDate(detail);

        String action = req.getAction() == null ? "" : req.getAction().trim().toUpperCase();
        List<Long> createdIds = new ArrayList<>();

        if ("CONFIRM_ONLY".equals(action)) {
            long id = splitAsnRepo.insertSplitLine(orderId, req.getDetailId(), needQty, needDate, "PLANNED");
            createdIds.add(id);
        } else if ("SPLIT_AND_CONFIRM".equals(action)) {
            if (req.getSplitLines() == null || req.getSplitLines().isEmpty()) {
                throw new IllegalStateException("SPLIT_LINES_REQUIRED");
            }
            int sum = req.getSplitLines().stream().mapToInt(ConfirmAndSplitRequest.SplitLineDraft::getSplitQty).sum();
            if (sum != needQty) {
                throw new IllegalStateException("SPLIT_QTY_MISMATCH");
            }
            for (ConfirmAndSplitRequest.SplitLineDraft sl : req.getSplitLines()) {
                long id = splitAsnRepo.insertSplitLine(
                        orderId,
                        req.getDetailId(),
                        sl.getSplitQty(),
                        sl.getCommittedDate(),
                        "PLANNED"
                );
                createdIds.add(id);
            }
        } else {
            throw new IllegalStateException("UNKNOWN_ACTION");
        }

        boolean confirmHeader = req.getConfirmOrderHeader() == null || Boolean.TRUE.equals(req.getConfirmOrderHeader());
        if (confirmHeader) {
            orderRepo.updatesupplierOrderStatusForSupplier(
                    orderId,
                    supplierId,
                    orderProps.getConfirmedStatus(),
                    req.getOperatorUserId(),
                    "|CONFIRM_AND_SPLIT"
            );
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderId", orderId);
        out.put("detailId", req.getDetailId());
        out.put("splitLineIds", createdIds);
        out.put("needQty", needQty);
        return out;
    }

    public List<Map<String, Object>> listSplitLinesForUser(long orderId, long operatorUserId) {
        Long supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        Long orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("ORDER_NOT_FOUND"));
        if (!orderSupplierId.equals(supplierId)) {
            throw new IllegalStateException("ORDER_NOT_BELONG");
        }
        return splitAsnRepo.listSplitLinesByOrder(orderId);
    }

    private static int parseNeedQty(Map<String, Object> detail) {
        Object n = detail.get("need_number");
        if (n == null) {
            return 0;
        }
        if (n instanceof Number num) {
            return num.intValue();
        }
        if (n instanceof BigDecimal bd) {
            return bd.intValue();
        }
        return new BigDecimal(n.toString()).intValue();
    }

    private static LocalDate parseNeedDate(Map<String, Object> detail) {
        Object d = detail.get("need_date");
        if (d == null) {
            return null;
        }
        if (d instanceof LocalDate ld) {
            return ld;
        }
        if (d instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        if (d instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        String dateStr = d.toString();
        // 处理包含时间部分的日期字符串，如 '2025-12-22T00:00'
        if (dateStr.contains("T")) {
            dateStr = dateStr.substring(0, dateStr.indexOf("T"));
        }
        return LocalDate.parse(dateStr);
    }
}
