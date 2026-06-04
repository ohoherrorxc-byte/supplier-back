package srm.portal.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            @RequestParam(required = false) String supplierOrderStatus
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
                    supplierId, orderNo, partsNumber, startDate, endDate, supplierOrderStatus
            );

            byte[] excelBytes = generatePurchasePlanExcel(items);

            String filename = "采购计划导出_" + System.currentTimeMillis() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private byte[] generatePurchasePlanExcel(List<Map<String, Object>> items) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("采购计划");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"预测单号", "预测释放时间", "供应商名称", "预测需求总数", "待确认需求数量", "已确认需求数量", "预测状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> item : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.get("order_no") != null ? item.get("order_no").toString() : "");
                row.createCell(1).setCellValue(item.get("forecast_release_date") != null ? item.get("forecast_release_date").toString() : "");
                row.createCell(2).setCellValue(item.get("supplier_name") != null ? item.get("supplier_name").toString() : "");
                row.createCell(3).setCellValue(item.get("forecast_total_qty") != null ? ((Number) item.get("forecast_total_qty")).doubleValue() : 0);
                row.createCell(4).setCellValue(item.get("forecast_pending_qty") != null ? ((Number) item.get("forecast_pending_qty")).doubleValue() : 0);
                row.createCell(5).setCellValue(item.get("forecast_confirmed_qty") != null ? ((Number) item.get("forecast_confirmed_qty")).doubleValue() : 0);

                Integer status = item.get("supplier_order_status") != null ? ((Number) item.get("supplier_order_status")).intValue() : 0;
                String statusText = switch (status) {
                    case 20 -> "已确认";
                    case 21 -> "部分确认";
                    default -> "待确认";
                };
                row.createCell(6).setCellValue(statusText);

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
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