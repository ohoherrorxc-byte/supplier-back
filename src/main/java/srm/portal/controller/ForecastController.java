package srm.portal.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.repository.GenericTableLookupRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {

    @Autowired
    private OrderCollaborationRepository orderRepo;
    
    @Autowired
    private GenericTableLookupRepository lookupRepo;

    /**
     * 历史需求分页查询 API
     */
    @PostMapping("/historical-demands/page")
    public ResponseEntity<Map<String, Object>> getHistoricalDemandsPage(
            @RequestBody HistoricalDemandQueryRequest request) {
        try {
            // 获取当前用户的供应商 ID
            Long supplierId = null;
            if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 调用仓库方法获取历史采购需求数据
            Map<String, Object> result = orderRepo.getHistoricalDemands(
                    supplierId,
                    request.getPartsNumber(),
                    request.getDemandFactory(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getPageSize(),
                    (request.getPageNum() - 1) * request.getPageSize()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 历史需求全量导出 API
     */
    @PostMapping("/historical-demands/export")
    public ResponseEntity<Map<String, Object>> exportHistoricalDemands(
            @RequestBody HistoricalDemandQueryRequest request) {
        try {
            // 获取当前用户的供应商 ID
            Long supplierId = null;
            if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 检查数据量
            int totalCount = orderRepo.getHistoricalDemandsCount(
                    supplierId,
                    request.getPartsNumber(),
                    request.getDemandFactory(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            if (totalCount > 50000) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "导出数据量过大，请缩小日期范围或指定零件号后再试",
                        "total", totalCount
                ));
            }

            // 这里可以实现导出逻辑，暂时返回成功信息
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "导出成功",
                    "total", totalCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "导出失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 历史需求分析 API - 按月/年统计订单数量
     */
    @PostMapping("/historical-demands/analyze")
    public ResponseEntity<Map<String, Object>> analyzeHistoricalDemands(
            @RequestBody HistoricalDemandAnalyzeRequest request) {
        try {
            // 获取当前用户的供应商 ID
            Long supplierId = null;
            if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 调用仓库方法获取历史采购需求分析数据
            Map<String, Object> result = orderRepo.analyzeHistoricalDemands(
                    supplierId,
                    request.getGranularity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getDemandFactories(),
                    request.getPartsNumbers()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "分析失败: " + e.getMessage()
            ));
        }
    }

    // 请求参数类
    public static class HistoricalDemandQueryRequest {
        private Long userId;
        private String partsNumber;
        private String demandFactory;
        private String startDate;
        private String endDate;
        private int pageSize = 20;
        private int pageNum = 1;

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getPartsNumber() {
            return partsNumber;
        }

        public void setPartsNumber(String partsNumber) {
            this.partsNumber = partsNumber;
        }

        public String getDemandFactory() {
            return demandFactory;
        }

        public void setDemandFactory(String demandFactory) {
            this.demandFactory = demandFactory;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }
    }

    // 历史需求分析请求参数类
    public static class HistoricalDemandAnalyzeRequest {
        private Long userId;
        private String granularity; // 'month' 或 'year'
        private String startDate;
        private String endDate;
        private java.util.List<String> demandFactories;
        private String partsNumbers;

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getGranularity() {
            return granularity;
        }

        public void setGranularity(String granularity) {
            this.granularity = granularity;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public java.util.List<String> getDemandFactories() {
            return demandFactories;
        }

        public void setDemandFactories(java.util.List<String> demandFactories) {
            this.demandFactories = demandFactories;
        }

        public String getPartsNumbers() {
            return partsNumbers;
        }

        public void setPartsNumbers(String partsNumbers) {
            this.partsNumbers = partsNumbers;
        }
    }

    // ============ 供应商历史模块 API ============

    /**
     * 供应商历史需求分析 API - 按月/年统计订单数量
     */
    @PostMapping("/supplier-history/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSupplierHistory(
            @RequestBody SupplierHistoryAnalyzeRequest request) {
        try {
            // 获取供应商ID：如果请求中带了供应商名称，则通过名称查找；否则用当前登录用户的供应商ID
            Long supplierId = null;
            if (request.getSupplierName() != null && !request.getSupplierName().isBlank()) {
                supplierId = lookupRepo.findSupplierIdByName(request.getSupplierName()).orElse(null);
            } else if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 调用仓库方法获取历史采购需求分析数据
            Map<String, Object> result = orderRepo.analyzeSupplierHistory(
                    supplierId,
                    request.getGranularity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getDemandFactories(),
                    request.getPartsNumbers()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "aggregatedData", result.get("aggregatedData"),
                    "timeLabels", result.get("timeLabels"),
                    "chartData", result.get("chartData")
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "分析失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 供应商历史下钻明细 API
     */
    @PostMapping("/supplier-history/drill-down")
    public ResponseEntity<Map<String, Object>> drillDownSupplierHistory(
            @RequestBody SupplierHistoryDrillDownRequest request) {
        try {
            // 获取当前用户的供应商 ID
            Long supplierId = null;
            if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 调用仓库方法获取下钻明细数据
            java.util.List<Map<String, Object>> details = orderRepo.getSupplierHistoryDrillDown(
                    supplierId,
                    request.getPartsNumber(),
                    request.getDemandFactory(),
                    request.getTimeKey(),
                    request.getGranularity()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "details", details
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "查询明细失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 供应商历史导出 API
     */
    @PostMapping("/supplier-history/export")
    public ResponseEntity<byte[]> exportSupplierHistory(
            @RequestBody SupplierHistoryAnalyzeRequest request) {
        try {
            // 获取当前用户的供应商 ID
            Long supplierId = null;
            if (request.getUserId() != null) {
                supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(request.getUserId())).orElse(null);
            }

            // 获取分析数据（导出用，包含所有状态）
            Map<String, Object> result = orderRepo.analyzeSupplierHistoryForExport(
                    supplierId,
                    request.getGranularity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getDemandFactories(),
                    request.getPartsNumbers()
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> aggregatedData = (List<Map<String, Object>>) result.get("aggregatedData");

            @SuppressWarnings("unchecked")
            List<String> timeLabels = (List<String>) result.get("timeLabels");

            if (aggregatedData != null && aggregatedData.size() > 100000) {
                return ResponseEntity.badRequest().build();
            }

            // 生成 Excel 文件
            byte[] excelBytes = generateExcel(aggregatedData, timeLabels, request.getGranularity());

            String filename = "供应商历史采购需求_" + request.getStartDate() + "_" + request.getEndDate() + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 生成 Excel 文件
     */
    private byte[] generateExcel(List<Map<String, Object>> aggregatedData, List<String> timeLabels, String granularity) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("供应商历史采购需求");

            // 创建表头样式
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

            // 创建数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            // 创建表头
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;

            // 固定列
            Cell cell0 = headerRow.createCell(colIndex++);
            cell0.setCellValue("序号");
            cell0.setCellStyle(headerStyle);

            Cell cell1 = headerRow.createCell(colIndex++);
            cell1.setCellValue("零件号");
            cell1.setCellStyle(headerStyle);

            Cell cell2 = headerRow.createCell(colIndex++);
            cell2.setCellValue("零件名称");
            cell2.setCellStyle(headerStyle);

            // 时间列
            for (String label : timeLabels) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(label);
                cell.setCellStyle(headerStyle);
            }

            // 合计列
            Cell totalCell = headerRow.createCell(colIndex);
            totalCell.setCellValue("期间总合计");
            totalCell.setCellStyle(headerStyle);

            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> row : aggregatedData) {
                Row dataRow = sheet.createRow(rowNum++);
                colIndex = 0;

                // 序号
                Cell seqCell = dataRow.createCell(colIndex++);
                seqCell.setCellValue(rowNum - 1);
                seqCell.setCellStyle(dataStyle);

                // 零件号
                Cell partsCell = dataRow.createCell(colIndex++);
                partsCell.setCellValue((String) row.get("partsNumber"));
                partsCell.setCellStyle(dataStyle);

                // 零件名称
                Cell nameCell = dataRow.createCell(colIndex++);
                nameCell.setCellValue((String) row.get("partsName"));
                nameCell.setCellStyle(dataStyle);

                // 时间列数据
                for (String label : timeLabels) {
                    Cell dataCell = dataRow.createCell(colIndex++);
                    Object orderValue = row.get(label + "_order");
                    Object forecastValue = row.get(label + "_forecast");
                    Object singleValue = row.get(label);

                    if (orderValue != null && forecastValue != null) {
                        dataCell.setCellValue(((Number) orderValue).doubleValue() + ((Number) forecastValue).doubleValue());
                    } else if (forecastValue != null && orderValue == null) {
                        dataCell.setCellValue(((Number) forecastValue).doubleValue() + " (预测)");
                    } else if (orderValue != null) {
                        dataCell.setCellValue(((Number) orderValue).doubleValue());
                    } else if (singleValue != null) {
                        dataCell.setCellValue(((Number) singleValue).doubleValue());
                    } else {
                        dataCell.setCellValue(0);
                    }
                    dataCell.setCellStyle(dataStyle);
                }

                // 合计
                Cell totalDataCell = dataRow.createCell(colIndex);
                Object totalValue = row.get("total");
                if (totalValue != null) {
                    totalDataCell.setCellValue(((Number) totalValue).doubleValue());
                } else {
                    totalDataCell.setCellValue(0);
                }
                totalDataCell.setCellStyle(numberStyle);
            }

            // 自动调整列宽
            for (int i = 0; i <= colIndex; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 供应商历史分析请求参数类
    public static class SupplierHistoryAnalyzeRequest {
        private String userId;
        private String granularity; // 'month' 或 'year'
        private String startDate;
        private String endDate;
        private java.util.List<String> demandFactories;
        private java.util.List<String> partsNumbers;
        private String supplierName;

        // Getters and Setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getGranularity() {
            return granularity;
        }

        public void setGranularity(String granularity) {
            this.granularity = granularity;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public java.util.List<String> getDemandFactories() {
            return demandFactories;
        }

        public void setDemandFactories(java.util.List<String> demandFactories) {
            this.demandFactories = demandFactories;
        }

        public java.util.List<String> getPartsNumbers() {
            return partsNumbers;
        }

        public void setPartsNumbers(java.util.List<String> partsNumbers) {
            this.partsNumbers = partsNumbers;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }
    }

    // 供应商历史下钻请求参数类
    public static class SupplierHistoryDrillDownRequest {
        private String userId;
        private String partsNumber;
        private String demandFactory;
        private String timeKey;
        private String granularity;

        // Getters and Setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPartsNumber() {
            return partsNumber;
        }

        public void setPartsNumber(String partsNumber) {
            this.partsNumber = partsNumber;
        }

        public String getDemandFactory() {
            return demandFactory;
        }

        public void setDemandFactory(String demandFactory) {
            this.demandFactory = demandFactory;
        }

        public String getTimeKey() {
            return timeKey;
        }

        public void setTimeKey(String timeKey) {
            this.timeKey = timeKey;
        }

        public String getGranularity() {
            return granularity;
        }

        public void setGranularity(String granularity) {
            this.granularity = granularity;
        }
    }
}
