package srm.portal.repository;

import org.springframework.stereotype.Repository;
import srm.portal.dto.DeliveryPlanInsertDto;
import srm.portal.mapper.DeliveryPlanMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class DeliveryPlanRepository {
    private final DeliveryPlanMapper deliveryPlanMapper;

    public DeliveryPlanRepository(DeliveryPlanMapper deliveryPlanMapper) {
        this.deliveryPlanMapper = deliveryPlanMapper;
    }

    public long insertDeliveryPlan(Long orderId, Long orderDetailId, Long supplierId, Integer planQuantity, LocalDate planDate) {
        // Generate plan number: DP + supplier_id + yyyyMMdd + 4-digit sequence
        String dateStr = planDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String planNoPrefix = "DP" + supplierId + dateStr;

        // Retry with lock to handle concurrent inserts
        for (int retry = 0; retry < 3; retry++) {
            try {
                int maxSeq = deliveryPlanMapper.getMaxPlanSeqBySupplierAndDate(supplierId, planNoPrefix);
                int nextSeq = maxSeq + 1;
                String planNo = planNoPrefix + String.format("%04d", nextSeq);

                DeliveryPlanInsertDto dto = new DeliveryPlanInsertDto();
                dto.setPlanNo(planNo);
                dto.setOrderId(orderId);
                dto.setOrderDetailId(orderDetailId);
                dto.setSupplierId(supplierId);
                dto.setPlanQuantity(planQuantity);
                dto.setPlanDate(planDate);
                deliveryPlanMapper.insertDeliveryPlan(dto);
                return dto.getId();
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
                    // Another thread inserted the same plan_no, retry with new sequence
                    continue;
                }
                throw ex;
            }
        }
        throw new RuntimeException("Failed to generate unique plan_no after 3 retries");
    }

    public List<Map<String, Object>> listDeliveryPlanByOrder(Long orderId) {
        return deliveryPlanMapper.listDeliveryPlanByOrder(orderId);
    }

    public List<Map<String, Object>> listDeliveryPlanBySupplier(Long supplierId, String orderNo, List<String> partsNo, String startDate, String endDate, String status, int limit, int offset) {
        return deliveryPlanMapper.listDeliveryPlanBySupplier(supplierId, orderNo, partsNo, startDate, endDate, status, limit, offset);
    }

    public int countDeliveryPlanBySupplier(Long supplierId, String orderNo, List<String> partsNo, String startDate, String endDate, String status) {
        return deliveryPlanMapper.countDeliveryPlanBySupplier(supplierId, orderNo, partsNo, startDate, endDate, status);
    }

    public List<Map<String, Object>> listUpcomingDeliveryPlan(Long supplierId) {
        return deliveryPlanMapper.listUpcomingDeliveryPlan(supplierId);
    }

    public Optional<Map<String, Object>> findDeliveryPlanById(Long planId) {
        Map<String, Object> row = deliveryPlanMapper.findDeliveryPlanById(planId);
        return Optional.ofNullable(row);
    }

    public int updateDeliveryPlanStatus(Long planId, String newStatus) {
        return deliveryPlanMapper.updateDeliveryPlanStatus(planId, newStatus);
    }

    public int updateDeliveryPlanShipInfo(Long planId, Integer actualQuantity, LocalDate actualEtaDate,
                                          String trackingNo, String logisticsCompany, LocalDate shipDate) {
        return deliveryPlanMapper.updateDeliveryPlanShipInfo(planId, actualQuantity, actualEtaDate, trackingNo, logisticsCompany, shipDate);
    }

    public int updateDeliveryPlanAsnId(Long planId, Long asnId) {
        return deliveryPlanMapper.updateDeliveryPlanAsnId(planId, asnId);
    }

    public int queryPlannedQuantityByOrderDetail(Long orderDetailId) {
        return deliveryPlanMapper.queryPlannedQuantityByOrderDetail(orderDetailId);
    }
}