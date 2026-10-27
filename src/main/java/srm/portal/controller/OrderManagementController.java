package srm.portal.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class OrderManagementController {

    @Autowired
    private srm.portal.repository.GenericTableLookupRepository lookupRepo;

    @Autowired
    private srm.portal.repository.OrderCollaborationRepository orderRepo;

    @GetMapping("/sales-forecast")
    public ResponseEntity<Map<String, Object>> listSalesForecast(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看销售预测"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listSalesForecast(
                    supplierId.get(), partsNumber, startDate, endDate, l, o
            );
            
            long total = orderRepo.getOrderMapper().countSalesForecast(
                    supplierId.get(), partsNumber, startDate, endDate
            );
            
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

    @GetMapping("/purchase-plan")
    public ResponseEntity<Map<String, Object>> listPurchasePlan(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierOrderStatus,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Long supplierId = null;
        if (!isAdmin) {
            Optional<Long> supplierIdOpt = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierIdOpt.isEmpty()) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看采购计划"));
            }
            supplierId = supplierIdOpt.get();
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listPurchasePlan(
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus, l, o
            );

            long total = orderRepo.getOrderMapper().countPurchasePlan(
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus
            );

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

    @GetMapping("/purchase-plan/details")
    public ResponseEntity<Map<String, Object>> listPurchasePlanDetails(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierOrderStatus,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Long supplierId = null;
        if (!isAdmin) {
            Optional<Long> supplierIdOpt = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierIdOpt.isEmpty()) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看采购计划"));
            }
            supplierId = supplierIdOpt.get();
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listPurchasePlanDetails(
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus, l, o
            );

            long total = orderRepo.getOrderMapper().countPurchasePlanDetails(
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus
            );

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

    @PostMapping("/purchase-plan/{id}/feedback")
    public ResponseEntity<Map<String, Object>> submitPurchasePlanFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer committedQty = Integer.valueOf(request.get("committedQty").toString());
            String remark = request.get("remark") != null ? request.get("remark").toString() : "";

            Map<String, Object> result = orderRepo.submitPurchasePlanFeedback(id, userId, committedQty, remark);

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "message",
                    "提交失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/purchase-plan/{id}")
    public ResponseEntity<Map<String, Object>> getPurchasePlanOrder(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        if (!isAdmin) {
            Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierId.isEmpty()) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
        }

        try {
            Optional<Map<String, Object>> orderOpt = orderRepo.getOrderById(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "预测单不存在"));
            }

            Map<String, Object> order = orderOpt.get();
            List<Map<String, Object>> details = orderRepo.getOrderDetailsByOrderId(id);

            return ResponseEntity.ok(Map.of("order", order, "details", details));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "message",
                    "查询失败：" + ex.getMessage()
            ));
        }
    }

    @GetMapping("/purchase-plan/export")
    public ResponseEntity<byte[]> exportPurchasePlan(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierOrderStatus,
            @RequestParam(required = false) List<String> detailIds
    ) {
        boolean isAdmin = lookupRepo.isAdminUser(String.valueOf(userId));
        Long supplierId = null;
        if (!isAdmin) {
            Optional<Long> supplierIdOpt = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
            if (supplierIdOpt.isEmpty()) {
                return ResponseEntity.status(403).build();
            }
            supplierId = supplierIdOpt.get();
        }

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listPurchasePlanForExport(
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus, detailIds
            );

            byte[] excelBytes = generatePurchasePlanExcel(items);

            String filename = "采购计划导出_" + System.currentTimeMillis() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            // filename*=UTF-8''... 才能带中文；form-data 里的中文文件名会被 Tomcat 丢掉，响应写到一半就断，Excel 打开就是乱码。
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(("导出失败: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private byte[] generatePurchasePlanExcel(List<Map<String, Object>> items) throws IOException {
        // 在无头容器（无 AWT 字体）中显式禁用 POI 的字体初始化
        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.apache.poi.scratch.avoid", "true");
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("采购计划");

            // 为样式显式指定一个内置字体，避免 POI 在某些无字体容器里隐式初始化字体导致崩溃。
            org.apache.poi.ss.usermodel.Font baseFont = workbook.createFont();
            baseFont.setFontName("宋体");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(baseFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(baseFont);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "预测单号", "物料号", "物料名称", "需求日期", "预测释放时间",
                "供应商名称", "需求数量", "待确认数量", "已确认数量", "状态"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> item : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(strValue(item.get("order_no")));
                row.createCell(1).setCellValue(strValue(item.get("parts_number")));
                row.createCell(2).setCellValue(strValue(item.get("parts_name")));
                row.createCell(3).setCellValue(strValue(item.get("plan_period")));
                row.createCell(4).setCellValue(strValue(item.get("forecast_release_date")));
                row.createCell(5).setCellValue(strValue(item.get("supplier_name")));
                row.createCell(6).setCellValue(numValue(item.get("forecast_total_qty")));
                row.createCell(7).setCellValue(numValue(item.get("forecast_pending_qty")));
                row.createCell(8).setCellValue(numValue(item.get("forecast_confirmed_qty")));

                Integer status = 0;
                Object statusObj = item.get("supplier_order_status");
                if (statusObj instanceof Number ns) {
                    status = ns.intValue();
                } else if (statusObj instanceof String ss && !ss.isEmpty()) {
                    try { status = Integer.parseInt(ss.trim()); } catch (NumberFormatException ignored) {}
                }
                String statusText = switch (status) {
                    case 20 -> "已确认";
                    case 21 -> "部分确认";
                    default -> "待确认";
                };
                row.createCell(9).setCellValue(statusText);

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 不使用 autoSizeColumn：在无字体子系统的容器里它会触发 FontManagerFactory 初始化并报错。
            // 直接用固定列宽，避免任何 AWT 字体调用。
            int[] columnWidths = {20, 18, 30, 14, 14, 30, 12, 12, 12, 12};
            for (int i = 0; i < columnWidths.length; i++) {
                sheet.setColumnWidth(i, columnWidths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static String strValue(Object v) {
        return v != null ? v.toString() : "";
    }

    private static double numValue(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return 0;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @GetMapping("/jit-orders")
    public ResponseEntity<Map<String, Object>> listJitOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看 JIT 订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listJitOrders(
                    supplierId.get(), orderNo, partsNumber, startDate, endDate, l, o
            );
            
            long total = orderRepo.getOrderMapper().countJitOrders(
                    supplierId.get(), orderNo, partsNumber, startDate, endDate
            );
            
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

    @GetMapping("/outsourcing-orders")
    public ResponseEntity<Map<String, Object>> listOutsourcingOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) Integer supplierOrderStatus,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看委外加工订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listOutsourcingOrders(
                    supplierId.get(), orderNo, partsNumber, supplierOrderStatus, l, o
            );
            
            long total = orderRepo.getOrderMapper().countOutsourcingOrders(
                    supplierId.get(), orderNo, partsNumber, supplierOrderStatus
            );
            
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

    @GetMapping("/assr-orders")
    public ResponseEntity<Map<String, Object>> listAssrOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) List<String> orderNo,
            @RequestParam(required = false) List<String> partsNumber,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商，无法查看 ASSR 订单"));
        }
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        try {
            List<Map<String, Object>> items = orderRepo.getOrderMapper().listAssrOrders(
                    supplierId.get(), orderNo, partsNumber, urgencyLevel, l, o
            );
            
            long total = orderRepo.getOrderMapper().countAssrOrders(
                    supplierId.get(), orderNo, partsNumber, urgencyLevel
            );
            
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
}