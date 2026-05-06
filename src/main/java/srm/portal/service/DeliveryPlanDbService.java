package srm.portal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import srm.portal.dto.DeliveryPlanCreateRequest;
import srm.portal.dto.DeliveryPlanShipRequest;
import srm.portal.repository.DeliveryPlanRepository;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.OrderCollaborationRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeliveryPlanDbService {
    private final DeliveryPlanRepository deliveryPlanRepository;
    private final OrderCollaborationRepository orderRepository;
    private final GenericTableLookupRepository lookupRepository;
    private final JdbcTemplate jdbcTemplate;

    public DeliveryPlanDbService(
            DeliveryPlanRepository deliveryPlanRepository,
            OrderCollaborationRepository orderRepository,
            GenericTableLookupRepository lookupRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.deliveryPlanRepository = deliveryPlanRepository;
        this.orderRepository = orderRepository;
        this.lookupRepository = lookupRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> createDeliveryPlan(DeliveryPlanCreateRequest req) {
        Long operatorUserId = req.getOperatorUserId();
        System.err.println("================= DeliveryPlanDbService.createDeliveryPlan() 开始执行 =================");
        System.err.println("operatorUserId: " + operatorUserId);
Long supplierId = lookupRepository.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        System.err.println("supplierId: " + supplierId);

        Long orderId = req.getOrderId();
        Long orderDetailId = Long.valueOf(req.getOrderDetailId());

        // 检查订单是否属于该供应商
        Optional<Map<String, Object>> order = orderRepository.getOrderById(orderId);
        if (order.isEmpty()) {
            throw new IllegalStateException("ORDER_NOT_FOUND");
        }
        Object orderSupplierId = order.get().get("supplier_id");
        if (orderSupplierId == null || !supplierId.equals(Long.valueOf(orderSupplierId.toString()))) {
            throw new IllegalStateException("ORDER_NOT_BELONG_TO_SUPPLIER");
        }

        // 检查计划数量是否超过订单明细的需求数量（按零件维度）
        String detailSql = """
                SELECT need_number FROM tb_purchase_parts_order_detail WHERE id = ? AND order_id = ? AND IFNULL(is_deleted, 0) = 0
                """;
        List<Integer> needQtyList = jdbcTemplate.queryForList(detailSql, Integer.class, orderDetailId, orderId);
        if (needQtyList.isEmpty()) {
            throw new IllegalStateException("ORDER_DETAIL_NOT_FOUND");
        }
        Integer needQty = needQtyList.get(0);
        if (needQty == null || needQty == 0) {
            throw new IllegalStateException("INVALID_ORDER");
        }
        int plannedQty = deliveryPlanRepository.queryPlannedQuantityByOrderDetail(orderDetailId);
        if (plannedQty + req.getPlanQuantity() > needQty) {
            throw new IllegalStateException("PLAN_QUANTITY_EXCEEDS_ORDER");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate planDate = LocalDate.parse(req.getPlanDate(), formatter);

        long planId = deliveryPlanRepository.insertDeliveryPlan(
                orderId,
                orderDetailId,
                supplierId,
                req.getPlanQuantity(),
                planDate
        );

        return buildDeliveryPlanView(planId);
    }

    public List<Map<String, Object>> listDeliveryPlanByOrder(Long orderId) {
        return deliveryPlanRepository.listDeliveryPlanByOrder(orderId);
    }

    public List<Map<String, Object>> listDeliveryPlanBySupplier(Long userId, String orderNo, List<String> partsNo, String startDate, String endDate, String status, int limit, int offset) {
        System.err.println("================= listDeliveryPlanBySupplier called with userId=" + userId + " limit=" + limit + " offset=" + offset);

        // 检查是否为管理员用户
        boolean isAdmin = lookupRepository.isAdminUser(String.valueOf(userId));
        Long supplierId = null;

        if (!isAdmin) {
            supplierId = lookupRepository.findSupplierCompanyIdByUserId(String.valueOf(userId))
                    .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        }

        System.err.println("supplierId: " + supplierId + " (isAdmin: " + isAdmin + ")");
        try {
            return deliveryPlanRepository.listDeliveryPlanBySupplier(supplierId, orderNo, partsNo, startDate, endDate, status, limit, offset);
        } catch (Exception ex) {
            System.err.println("================= Exception in listDeliveryPlanBySupplier =================");
            System.err.println("Message: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    public int countDeliveryPlanBySupplier(Long userId, String orderNo, List<String> partsNo, String startDate, String endDate, String status) {
        System.err.println("================= countDeliveryPlanBySupplier called with userId=" + userId);

        // 检查是否为管理员用户
        boolean isAdmin = lookupRepository.isAdminUser(String.valueOf(userId));
        Long supplierId = null;

        if (!isAdmin) {
            supplierId = lookupRepository.findSupplierCompanyIdByUserId(String.valueOf(userId))
                    .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        }

        return deliveryPlanRepository.countDeliveryPlanBySupplier(supplierId, orderNo, partsNo, startDate, endDate, status);
    }

    public List<Map<String, Object>> listUpcomingDeliveryPlan(Long userId) {
        // 检查是否为管理员用户
        boolean isAdmin = lookupRepository.isAdminUser(String.valueOf(userId));
        Long supplierId = null;

        if (!isAdmin) {
            supplierId = lookupRepository.findSupplierCompanyIdByUserId(String.valueOf(userId))
                    .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        }

        return deliveryPlanRepository.listUpcomingDeliveryPlan(supplierId);
    }

    @Transactional
    public Map<String, Object> updateDeliveryPlanShipInfo(DeliveryPlanShipRequest req) {
        Long operatorUserId = req.getOperatorUserId();

        // 检查是否为管理员用户
        boolean isAdmin = lookupRepository.isAdminUser(String.valueOf(operatorUserId));
        Long supplierId = null;

        if (!isAdmin) {
            supplierId = lookupRepository.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId))
                    .orElseThrow(() -> new IllegalStateException("NO_SUPPLIER_FOR_USER"));
        }

        Optional<Map<String, Object>> plan = deliveryPlanRepository.findDeliveryPlanById(req.getPlanId());
        if (plan.isEmpty()) {
            throw new IllegalStateException("PLAN_NOT_FOUND");
        }

        Map<String, Object> planMap = plan.get();
        Object planSupplierId = planMap.get("supplier_id");

        // 非管理员用户必须匹配供应商
        if (!isAdmin && supplierId != null) {
            if (planSupplierId == null || !supplierId.equals(Long.valueOf(planSupplierId.toString()))) {
                throw new IllegalStateException("NO_PLAN_ACCESS");
            }
        }

        String currentStatus = (String) planMap.get("status");
        if (!"PLAN".equals(currentStatus)) {
            throw new IllegalStateException("INVALID_STATUS_FOR_SHIP");
        }

        // 校验实际发货数量不能超过计划数量
        Object planQtyObj = planMap.get("plan_quantity");
        int planQty = Integer.parseInt(planQtyObj.toString());
        if (req.getActualQuantity() > planQty) {
            throw new IllegalStateException("ACTUAL_QUANTITY_EXCEEDS_PLAN");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate actualEtaDate = LocalDate.parse(req.getActualEtaDate(), formatter);
        LocalDate shipDate = LocalDate.parse(req.getShipDate(), formatter);

        deliveryPlanRepository.updateDeliveryPlanShipInfo(
                req.getPlanId(),
                req.getActualQuantity(),
                actualEtaDate,
                req.getTrackingNo(),
                req.getLogisticsCompany(),
                shipDate
        );

        return buildDeliveryPlanView(req.getPlanId());
    }

    private Map<String, Object> buildDeliveryPlanView(Long planId) {
        Optional<Map<String, Object>> plan = deliveryPlanRepository.findDeliveryPlanById(planId);
        if (plan.isEmpty()) {
            throw new IllegalStateException("PLAN_NOT_FOUND");
        }

        Map<String, Object> result = new LinkedHashMap<>(plan.get());
        return result;
    }
}
