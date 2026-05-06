package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import srm.portal.dto.DeliveryPlanInsertDto;

@Mapper
public interface DeliveryPlanMapper {

    // ============ 查询方法 ============

    List<Map<String, Object>> listDeliveryPlanByOrder(@Param("orderId") Long orderId);

    List<Map<String, Object>> listDeliveryPlanBySupplier(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") String orderNo,
            @Param("partsNo") List<String> partsNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int countDeliveryPlanBySupplier(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") String orderNo,
            @Param("partsNo") List<String> partsNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status
    );

    List<Map<String, Object>> listUpcomingDeliveryPlan(@Param("supplierId") Long supplierId);

    Map<String, Object> findDeliveryPlanById(@Param("planId") Long planId);

    int queryPlannedQuantityByOrderDetail(@Param("orderDetailId") Long orderDetailId);

    int getMaxPlanSeqBySupplierAndDate(@Param("supplierId") Long supplierId, @Param("planNoPrefix") String planNoPrefix);

    int getMaxPlanSeqWithSupplier(@Param("supplierId") Long supplierId);

    // ============ 插入方法 ============

    @Insert("INSERT INTO tb_delivery_plan (plan_no, order_id, order_detail_id, supplier_id, plan_quantity, plan_date, status) VALUES (#{dto.planNo}, #{dto.orderId}, #{dto.orderDetailId}, #{dto.supplierId}, #{dto.planQuantity}, #{dto.planDate}, 'PLAN')")
    @Options(useGeneratedKeys = true, keyProperty = "dto.id")
    int insertDeliveryPlan(@Param("dto") DeliveryPlanInsertDto dto);

    // ============ 更新方法 ============

    int updateDeliveryPlanStatus(@Param("planId") Long planId, @Param("newStatus") String newStatus);

    int updateDeliveryPlanShipInfo(
            @Param("planId") Long planId,
            @Param("actualQuantity") Integer actualQuantity,
            @Param("actualEtaDate") LocalDate actualEtaDate,
            @Param("trackingNo") String trackingNo,
            @Param("logisticsCompany") String logisticsCompany,
            @Param("shipDate") LocalDate shipDate
    );

    int updateDeliveryPlanAsnId(@Param("planId") Long planId, @Param("asnId") Long asnId);
}