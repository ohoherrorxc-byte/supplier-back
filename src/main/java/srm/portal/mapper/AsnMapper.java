package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AsnMapper {

    // ============ 查询方法 (XML) ============

    List<Map<String, Object>> listAsnBySupplier(@Param("supplierId") Long supplierId, @Param("limit") int limit, @Param("offset") int offset);

    long countAsnBySupplier(@Param("supplierId") Long supplierId);

    List<Map<String, Object>> listUpcomingAsnBySupplier(@Param("supplierId") Long supplierId);

    Map<String, Object> findHeaderById(@Param("asnId") long asnId);

    Long findSupplierIdByAsnId(@Param("asnId") long asnId);

    List<Long> findOrderIdsByAsnId(@Param("asnId") long asnId);

    List<Map<String, Object>> findAsnLinesByAsnId(@Param("asnId") long asnId);

    List<Map<String, Object>> findTimelineByAsnId(@Param("asnId") long asnId);

    // ============ 插入方法 (XML) ============

    int insertHeader(@Param("supplierId") Long supplierId, @Param("logisticsCompany") String logisticsCompany, @Param("trackingNo") String trackingNo, @Param("etaDate") LocalDate etaDate, @Param("status") String status);

    int updateAsnNo(@Param("asnId") long asnId, @Param("asnNo") String asnNo);

    int insertOrderLink(@Param("asnId") long asnId, @Param("orderId") long orderId);

    int insertTimeline(@Param("asnId") long asnId, @Param("eventCode") String eventCode, @Param("description") String description, @Param("eventTime") LocalDateTime eventTime);

    int updateAsnStatus(@Param("asnId") long asnId, @Param("newStatus") String newStatus);

    int insertAsnLineNoAsnNo(@Param("supplierId") Long supplierId, @Param("orderDetailId") Long orderDetailId, @Param("shipQty") int shipQty, @Param("shipDate") LocalDate shipDate, @Param("actualEtaDate") LocalDate actualEtaDate, @Param("warehouseInNo") String warehouseInNo);

    int insertAsnLine(@Param("supplierId") Long supplierId, @Param("asnNo") String asnNo, @Param("orderDetailId") Long orderDetailId, @Param("shipQty") int shipQty, @Param("shipDate") LocalDate shipDate, @Param("actualEtaDate") LocalDate actualEtaDate, @Param("warehouseInNo") String warehouseInNo);

    int insertAsnLineWithAsnId(@Param("asnId") Long asnId, @Param("supplierId") Long supplierId, @Param("orderDetailId") Long orderDetailId, @Param("shipQty") int shipQty, @Param("shipDate") LocalDate shipDate, @Param("actualEtaDate") LocalDate actualEtaDate, @Param("warehouseInNo") String warehouseInNo);

    int updateAsnSelfReference(@Param("asnId") long asnId);

    int updateAsnOrderDetailAndShipQty(@Param("asnId") long asnId, @Param("orderDetailId") long orderDetailId, @Param("shipQty") int shipQty, @Param("shipDate") String shipDate, @Param("actualEtaDate") String actualEtaDate, @Param("warehouseInNo") String warehouseInNo);

    int updateAsnShipInfo(@Param("asnId") long asnId, @Param("shippedQty") Integer shippedQty, @Param("shipDate") String shipDate, @Param("actualEtaDate") String actualEtaDate, @Param("warehouseInNo") String warehouseInNo);

    int getMaxSeqBySupplierAndDate(@Param("supplierId") Long supplierId, @Param("shipDate") LocalDate shipDate);

    int cleanupPlaceholderAsn(@Param("supplierId") Long supplierId, @Param("shipDate") LocalDate shipDate);

    int softDeleteAsn(@Param("asnId") long asnId);

    int insertAsnPlaceholder(@Param("asnNo") String asnNo, @Param("supplierId") Long supplierId, @Param("shipDate") LocalDate shipDate);

    Long lockAsnNoAndGetId(@Param("asnNo") String asnNo, @Param("supplierId") Long supplierId);
}