package srm.portal.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 查询订单列表（带分页）
     */
    List<Map<String, Object>> listOrderHeaders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("supplierOrderStatus") List<Integer> supplierOrderStatus,
            @Param("supplierName") String supplierName,
            @Param("deliveryStatus") List<String> deliveryStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("requireAttr") String requireAttr,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 统计订单数量
     */
    long countOrderHeaders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("supplierOrderStatus") List<Integer> supplierOrderStatus,
            @Param("supplierName") String supplierName,
            @Param("deliveryStatus") List<String> deliveryStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("requireAttr") String requireAttr
    );

    /**
     * 查询订单汇总统计；已交付/未交付数量仅按订单头 {@code delivery_status}
     *（值为 {@code "已交付"} 视为已交付，否则为未交付）
     * 汇总行需求数量，不看 ASN。{@code pending_ship_qty} 仍为 ASN「未发运」口径。
     */
    Map<String, Object> getOrderSummaryStats(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("supplierOrderStatus") List<Integer> supplierOrderStatus,
            @Param("deliveryStatus") List<String> deliveryStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询订单详情
     */
    Map<String, Object> getOrderById(@Param("orderId") Long orderId);

    /**
     * 查询订单详情行
     */
    List<Map<String, Object>> getOrderDetails(@Param("orderId") Long orderId);

    /**
     * 根据订单号查询订单详情行
     */
    List<Map<String, Object>> getOrderDetailsByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询供应商的订单 - 合并订单头和明细
     */
    List<Map<String, Object>> listOrderDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("supplierOrderStatus") Integer supplierOrderStatus,
            @Param("deliveryStatus") List<String> deliveryStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("requireAttr") String requireAttr,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 统计合并查询的记录数
     */
    long countOrderDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("supplierOrderStatus") Integer supplierOrderStatus,
            @Param("deliveryStatus") List<String> deliveryStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("requireAttr") String requireAttr
    );

    /**
     * 获取供应商 ID
     */
    Long findSupplierIdByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新订单状态
     */
    int updatesupplierOrderStatus(
            @Param("orderId") Long orderId,
            @Param("supplierId") Long supplierId,
            @Param("status") int status,
            @Param("operatorUserId") Long operatorUserId,
            @Param("actionLog") String actionLog
    );

    /**
     * 更新订单详情状态（按订单ID）
     */
    int updateOrderDetailStatus(
            @Param("orderId") Long orderId,
            @Param("status") int status,
            @Param("operatorUserId") Long operatorUserId
    );

    /**
     * 更新订单详情状态（按详情ID）
     */
    int updateOrderDetailStatusById(
            @Param("orderDetailId") Long orderDetailId,
            @Param("status") int status,
            @Param("operatorUserId") Long operatorUserId
    );

    /**
     * 根据订单详情ID获取订单详情
     */
    Map<String, Object> getOrderDetailById(@Param("orderDetailId") Long orderDetailId);

    /**
     * 更新订单详情的已发货数量
     */
    void updateOrderDetailShippedQty(
            @Param("orderDetailId") Long orderDetailId,
            @Param("shippedQty") Integer shippedQty
    );

    /**
     * 只更新订单状态
     */
    void updatesupplierOrderStatusOnly(
            @Param("orderId") Long orderId,
            @Param("status") int status
    );

    /**
     * 查询订单详情行的已发运数量
     */
    int getOrderDetailShippedQty(
            @Param("orderDetailId") Long orderDetailId
    );

    /**
     * 检查订单是否全部属于该供应商
     */
    int countOrdersBySupplier(
            @Param("supplierId") Long supplierId,
            @Param("orderIds") List<Long> orderIds
    );

    /**
     * 查询待发货订单列表
     */
    List<Map<String, Object>> listPendingShipmentOrderDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") String orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计待发货订单数量
     */
    long countPendingShipmentOrderDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") String orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询销售预测列表
     */
    List<Map<String, Object>> listSalesForecast(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计销售预测数量
     */
    long countSalesForecast(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询采购计划列表
     */
    List<Map<String, Object>> listPurchasePlan(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("feedbackStatus") String feedbackStatus,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计采购计划数量
     */
    long countPurchasePlan(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("feedbackStatus") String feedbackStatus
    );

    /**
     * 查询采购计划明细列表
     */
    List<Map<String, Object>> listPurchasePlanDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("feedbackStatus") String feedbackStatus,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计采购计划明细数量
     */
    long countPurchasePlanDetails(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("feedbackStatus") String feedbackStatus
    );

    /**
     * 查询 JIT 订单列表
     */
    List<Map<String, Object>> listJitOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计 JIT 订单数量
     */
    long countJitOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询委外加工订单列表
     */
    List<Map<String, Object>> listOutsourcingOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("supplierOrderStatus") Integer supplierOrderStatus,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计委外加工订单数量
     */
    long countOutsourcingOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("supplierOrderStatus") Integer supplierOrderStatus
    );

    /**
     * 查询 ASSR 订单列表
     */
    List<Map<String, Object>> listAssrOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("urgencyLevel") String urgencyLevel,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计 ASSR 订单数量
     */
    long countAssrOrders(
            @Param("supplierId") Long supplierId,
            @Param("orderNo") List<String> orderNo,
            @Param("partsNumber") List<String> partsNumber,
            @Param("urgencyLevel") String urgencyLevel
    );

    /**
     * 查询经销商历史采购需求列表
     */
    List<Map<String, Object>> listDealerHistory(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("region") String region,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计经销商历史采购需求数量
     */
    long countDealerHistory(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") List<String> partsNumber,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("region") String region
    );

    /**
     * 获取历史采购需求数据
     */
    List<Map<String, Object>> getHistoricalDemands(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") String partsNumber,
            @Param("demandFactory") String demandFactory,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计历史采购需求数据数量
     */
    long countHistoricalDemands(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") String partsNumber,
            @Param("demandFactory") String demandFactory,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 历史需求分析 - 按月/年统计订单数量
     */
    List<Map<String, Object>> analyzeHistoricalDemands(
            @Param("supplierId") Long supplierId,
            @Param("granularity") String granularity,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("demandFactories") List<String> demandFactories,
            @Param("partsNumbers") String partsNumbers
    );

    /**
     * 获取时间标签
     */
    List<String> getTimeLabels(
            @Param("granularity") String granularity,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 计算订单的总需求数量
     */
    int getOrderTotalNeedQuantity(@Param("orderId") Long orderId);

    /**
     * 计算订单相关的所有ASN的ship_qty总和
     */
    int getOrderTotalShippedQuantity(@Param("orderId") Long orderId);

    // ============ 供应商历史模块方法 ============

    /**
     * 供应商历史需求分析 - 按月/年统计订单数量
     */
    List<Map<String, Object>> analyzeSupplierHistory(
            @Param("supplierId") Long supplierId,
            @Param("granularity") String granularity,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("demandFactories") List<String> demandFactories,
            @Param("partsNumbers") List<String> partsNumbers
    );

    /**
     * 供应商历史需求分析 - 导出用（包含所有状态）
     */
    List<Map<String, Object>> analyzeSupplierHistoryForExport(
            @Param("supplierId") Long supplierId,
            @Param("granularity") String granularity,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("demandFactories") List<String> demandFactories,
            @Param("partsNumbers") List<String> partsNumbers
    );

    /**
     * 供应商历史下钻明细查询
     */
    List<Map<String, Object>> getSupplierHistoryDrillDown(
            @Param("supplierId") Long supplierId,
            @Param("partsNumber") String partsNumber,
            @Param("demandFactory") String demandFactory,
            @Param("timeKey") String timeKey,
            @Param("granularity") String granularity
    );

    /**
     * 获取供应商历史时间标签
     */
    List<String> getSupplierHistoryTimeLabels(
            @Param("supplierId") Long supplierId,
            @Param("granularity") String granularity,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
