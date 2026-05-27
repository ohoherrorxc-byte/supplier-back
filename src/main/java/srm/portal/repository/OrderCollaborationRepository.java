package srm.portal.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import srm.portal.mapper.OrderMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderCollaborationRepository {
    private final OrderMapper orderMapper;

    public OrderCollaborationRepository(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public OrderMapper getOrderMapper() {
        return orderMapper;
    }

    /**
     * 查询订单详情与统计
     */
    public Map<String, Object> listNewOrderDetailsWithCount(
            Long supplierId,
            List<String> orderNo,
            List<String> partsNumber,
            Integer supplierOrderStatus,
            List<String> deliveryStatus,
            String startDate,
            String endDate,
            String requireAttr,
            int limit,
            int offset,
            String sortField,
            String sortOrder
    ) {
        long total = orderMapper.countOrderDetails(supplierId, orderNo, partsNumber, supplierOrderStatus, deliveryStatus, startDate, endDate, requireAttr);
        List<Map<String, Object>> items = orderMapper.listOrderDetails(
                supplierId, orderNo, partsNumber, supplierOrderStatus, deliveryStatus, startDate, endDate, requireAttr, limit, offset, sortField, sortOrder
        );
        return Map.of("items", items, "total", total);
    }

    /**
     * 获取订单详情
     */
    public List<Map<String, Object>> getOrderDetails(Long orderId) {
        return orderMapper.getOrderDetails(orderId);
    }

    /**
     * 根据订单号获取订单详情
     */
    public List<Map<String, Object>> getOrderDetailsByOrderNo(String orderNo) {
        return orderMapper.getOrderDetailsByOrderNo(orderNo);
    }

    /**
     * 根据 ID 获取订单
     */
    public Optional<Map<String, Object>> getOrderById(Long orderId) {
        return Optional.ofNullable(orderMapper.getOrderById(orderId));
    }

    /**
     * 列出供应商的订单
     */
    public List<Map<String, Object>> listNewOrdersBySupplier(Long supplierId, int limit, int offset) {
        return orderMapper.listOrderHeaders(supplierId, null, null, null, null, null, null, limit, offset, null, null);
    }

    /**
     * 根据订单 ID 查询供应商 ID
     */
    public Optional<Long> findSupplierIdByOrderId(Long orderId) {
        return Optional.ofNullable(orderMapper.findSupplierIdByOrderId(orderId));
    }

    /**
     * 更新订单状态及其详情
     */
    @Transactional
    public int updatesupplierOrderStatusForSupplier(Long orderId, Long supplierId, int status, Long operatorUserId, String actionLog) {
        //1. 更新订单头
        int affected = orderMapper.updatesupplierOrderStatus(orderId, supplierId, status, operatorUserId, actionLog);
        
        //2. 同步更新订单详情
        if (affected > 0) {
            orderMapper.updateOrderDetailStatus(orderId, status, operatorUserId);
        }
        
        return affected;
    }

    /**
     * 检查订单是否全部属于该供应商
     */
    public boolean allOrdersBelongToSupplier(List<Long> orderIds, Long supplierId) {
        if (orderIds == null || orderIds.isEmpty()) {
            return false;
        }
        int cnt = orderMapper.countOrdersBySupplier(supplierId, orderIds);
        return cnt == orderIds.size();
    }

    /**
     * 检查订单是否全部已确认
     */
    public boolean allOrdersConfirmedForSupplier(List<Long> orderIds, Long supplierId, int confirmedStatus) {
        // 这个方法需要延伸 Mapper，这里先用基础方法实现
        for (Long orderId : orderIds) {
            Optional<Map<String, Object>> order = getOrderById(orderId);
            if (order.isEmpty()) {
                return false;
            }
            Object status = order.get().get("supplier_order_status");
            if (status == null || !status.toString().equals(String.valueOf(confirmedStatus))) {
                return false;
            }
            Object suppId = order.get().get("supplier_id");
            if (suppId == null || !suppId.toString().equals(String.valueOf(supplierId))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 提交采购计划反馈
     */
    @Transactional
    public Map<String, Object> submitPurchasePlanFeedback(Long planId, Long userId, Integer committedQty, String remark) {
        // 这里需要实现采购计划反馈的逻辑
        // 由于目前还没有采购计划表，这里先返回一个模拟结果
        return Map.of(
                "success", true,
                "message", "产能反馈成功"
        );
    }

    /**
     * 根据订单详情ID获取订单详情
     */
    public Optional<Map<String, Object>> getOrderDetailById(Long orderDetailId) {
        return Optional.ofNullable(orderMapper.getOrderDetailById(orderDetailId));
    }

    /**
     * 根据订单ID获取所有订单详情
     */
    public List<Map<String, Object>> getOrderDetailsByOrderId(Long orderId) {
        return orderMapper.getOrderDetails(orderId);
    }

    /**
     * 更新订单详情的已发货数量
     */
    public void updateOrderDetailShippedQty(Long orderDetailId, Integer shippedQty) {
        orderMapper.updateOrderDetailShippedQty(orderDetailId, shippedQty);
    }

    /**
     * 更新订单详情状态（按详情ID）
     */
    public void updateOrderDetailStatus(Long orderDetailId, int status, Long operatorUserId) {
        orderMapper.updateOrderDetailStatusById(orderDetailId, status, operatorUserId);
    }

    /**
     * 更新订单状态
     */
    public void updatesupplierOrderStatus(Long orderId, int status) {
        orderMapper.updatesupplierOrderStatusOnly(orderId, status);
    }

    /**
     * 只更新订单状态
     */
    public void updatesupplierOrderStatusOnly(Long orderId, int status) {
        orderMapper.updatesupplierOrderStatusOnly(orderId, status);
    }

    /**
     * 获取历史采购需求数据
     */
    public Map<String, Object> getHistoricalDemands(
            Long supplierId,
            String partsNumber,
            String demandFactory,
            String startDate,
            String endDate,
            int limit,
            int offset
    ) {
        long total = orderMapper.countHistoricalDemands(supplierId, partsNumber, demandFactory, startDate, endDate);
        List<Map<String, Object>> records = orderMapper.getHistoricalDemands(supplierId, partsNumber, demandFactory, startDate, endDate, limit, offset);
        return Map.of("records", records, "total", total);
    }

    /**
     * 获取历史采购需求数据数量
     */
    public int getHistoricalDemandsCount(
            Long supplierId,
            String partsNumber,
            String demandFactory,
            String startDate,
            String endDate
    ) {
        return (int) orderMapper.countHistoricalDemands(supplierId, partsNumber, demandFactory, startDate, endDate);
    }

    /**
     * 历史需求分析 - 按月/年统计订单数量
     */
    public Map<String, Object> analyzeHistoricalDemands(
            Long supplierId,
            String granularity,
            String startDate,
            String endDate,
            List<String> demandFactories,
            String partsNumbers
    ) {
        List<Map<String, Object>> aggregatedData = orderMapper.analyzeHistoricalDemands(
                supplierId, granularity, startDate, endDate, demandFactories, partsNumbers
        );
        
        // 生成时间标签
        List<String> timeLabels = orderMapper.getTimeLabels(granularity, startDate, endDate);
        
        // 生成图表数据
        Map<String, Object> chartData = generateChartData(aggregatedData, timeLabels);
        
        return Map.of(
                "aggregatedData", aggregatedData,
                "timeLabels", timeLabels,
                "chartData", chartData
        );
    }

    /**
     * 生成图表数据
     */
    private Map<String, Object> generateChartData(List<Map<String, Object>> aggregatedData, List<String> timeLabels) {
        List<String> xAxis = timeLabels;
        List<Map<String, Object>> series = new java.util.ArrayList<>();

        // 按零件号+需求工厂分组
        Map<String, List<Map<String, Object>>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : aggregatedData) {
            String key = row.get("parts_number") + "_" + row.get("demand_factory");
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(row);
        }

        // 为每个分组生成一个系列
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            if (group.isEmpty()) continue;

            Map<String, Object> firstRow = group.get(0);
            String name = firstRow.get("parts_number") + " (" + firstRow.get("demand_factory") + ")";

            List<Integer> data = new java.util.ArrayList<>();
            for (String timeLabel : timeLabels) {
                int sum = 0;
                for (Map<String, Object> row : group) {
                    Object value = row.get(timeLabel);
                    if (value != null) {
                        sum += ((Number) value).intValue();
                    }
                }
                data.add(sum);
            }

            series.add(Map.of("name", name, "data", data));
        }

        return Map.of("xAxis", xAxis, "series", series);
    }

    // ============ 供应商历史模块方法 ============

    /**
     * 供应商历史需求分析 - 按月/年统计订单数量
     */
    public Map<String, Object> analyzeSupplierHistory(
            Long supplierId,
            String granularity,
            String startDate,
            String endDate,
            List<String> demandFactories,
            List<String> partsNumbers
    ) {
        // 处理空值
        if (demandFactories == null) {
            demandFactories = new java.util.ArrayList<>();
        }
        if (partsNumbers == null) {
            partsNumbers = new java.util.ArrayList<>();
        }

        List<Map<String, Object>> aggregatedData = orderMapper.analyzeSupplierHistory(
                supplierId, granularity, startDate, endDate, demandFactories, partsNumbers
        );

        // 生成完整的时间标签序列
        List<String> timeLabels = generateTimeLabels(granularity, startDate, endDate);

        // 转换聚合数据为动态透视表格式
        List<Map<String, Object>> pivotData = convertToPivotData(aggregatedData, timeLabels);

        // 生成图表数据
        Map<String, Object> chartData = generateSupplierHistoryChartData(pivotData, timeLabels);

        return Map.of(
                "aggregatedData", pivotData,
                "timeLabels", timeLabels,
                "chartData", chartData
        );
    }

    /**
     * 根据起止日期生成完整的时间标签序列
     */
    private List<String> generateTimeLabels(String granularity, String startDate, String endDate) {
        List<String> labels = new java.util.ArrayList<>();
        if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
            return labels;
        }

        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate + "-01");
            java.time.LocalDate end = java.time.LocalDate.parse(endDate + "-01");

            if ("year".equals(granularity)) {
                // 按年生成
                int startYear = start.getYear();
                int endYear = end.getYear();
                for (int year = startYear; year <= endYear; year++) {
                    labels.add(String.valueOf(year));
                }
            } else {
                // 按月生成
                java.time.YearMonth startYm = java.time.YearMonth.from(start);
                java.time.YearMonth endYm = java.time.YearMonth.from(end);
                java.time.YearMonth current = startYm;
                while (!current.isAfter(endYm)) {
                    labels.add(current.toString());
                    current = current.plusMonths(1);
                }
            }
        } catch (Exception e) {
            // 解析失败时返回空列表
        }
        return labels;
    }

    /**
     * 供应商历史下钻明细查询
     */
    public List<Map<String, Object>> getSupplierHistoryDrillDown(
            Long supplierId,
            String partsNumber,
            String demandFactory,
            String timeKey,
            String granularity
    ) {
        return orderMapper.getSupplierHistoryDrillDown(
                supplierId, partsNumber, demandFactory, timeKey, granularity
        );
    }

    /**
     * 转换聚合数据为动态透视表格式
     */
    private List<Map<String, Object>> convertToPivotData(List<Map<String, Object>> rawData, List<String> timeLabels) {
        // 按零件号+需求工厂分组
        Map<String, Map<String, Object>> pivotMap = new java.util.LinkedHashMap<>();

        for (Map<String, Object> row : rawData) {
            String partsNumber = (String) row.get("parts_number");
            String partsName = (String) row.get("parts_name");
            String demandFactory = (String) row.get("demand_factory");
            String timeKey = (String) row.get("time_key");

            // 跳过无效数据
            if (partsNumber == null || partsNumber.isEmpty()) {
                continue;
            }

            // 处理 null 的 demandFactory
            String safeDemandFactory = (demandFactory != null && !demandFactory.isEmpty()) ? demandFactory : "未知工厂";
            String key = partsNumber + "_" + safeDemandFactory;

            Map<String, Object> pivotRow = pivotMap.computeIfAbsent(key, k -> {
                Map<String, Object> newRow = new java.util.LinkedHashMap<>();
                newRow.put("partsNumber", partsNumber);
                newRow.put("partsName", partsName != null ? partsName : "");
                newRow.put("demandFactory", safeDemandFactory);
                newRow.put("total", 0);
                return newRow;
            });

            // 获取数量
            int totalQty = 0;
            Object totalQtyObj = row.get("total_qty");
            if (totalQtyObj != null) {
                totalQty = ((Number) totalQtyObj).intValue();
            }

            // 设置时间列的值
            if (timeKey != null && !timeKey.isEmpty()) {
                Object existing = pivotRow.get(timeKey);
                int existingValue = (existing != null) ? ((Number) existing).intValue() : 0;
                pivotRow.put(timeKey, existingValue + totalQty);
            }

            // 累加总数
            Object totalObj = pivotRow.get("total");
            int currentTotal = (totalObj != null) ? ((Number) totalObj).intValue() : 0;
            pivotRow.put("total", currentTotal + totalQty);
        }

        // 确保所有时间标签列都存在
        for (Map<String, Object> row : pivotMap.values()) {
            for (String timeLabel : timeLabels) {
                row.putIfAbsent(timeLabel, 0);
            }
        }

        return new java.util.ArrayList<>(pivotMap.values());
    }

    /**
     * 生成供应商历史图表数据
     */
    private Map<String, Object> generateSupplierHistoryChartData(List<Map<String, Object>> pivotData, List<String> timeLabels) {
        List<String> xAxis = timeLabels;
        List<Map<String, Object>> series = new java.util.ArrayList<>();

        for (Map<String, Object> row : pivotData) {
            String partsNumber = (String) row.get("partsNumber");   
            String name = partsNumber ;

            List<Integer> data = new java.util.ArrayList<>();
            for (String timeLabel : timeLabels) {
                Object value = row.get(timeLabel);
                data.add(value != null ? ((Number) value).intValue() : 0);
            }

            series.add(Map.of("name", name, "data", data));
        }

        return Map.of("xAxis", xAxis, "series", series);
    }
}
