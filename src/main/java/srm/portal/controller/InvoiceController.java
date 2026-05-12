package srm.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import srm.portal.dto.*;
import srm.portal.entity.FlowFile;
import srm.portal.mapper.InvoiceMapper;
import srm.portal.mapper.FlowFileMapper;
import srm.portal.service.OcrInvoiceService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {
    private final InvoiceMapper invoiceMapper;
    private final FlowFileMapper flowFileMapper;
    private final OcrInvoiceService ocrInvoiceService;

    public InvoiceController(InvoiceMapper invoiceMapper, FlowFileMapper flowFileMapper, OcrInvoiceService ocrInvoiceService) {
        this.invoiceMapper = invoiceMapper;
        this.flowFileMapper = flowFileMapper;
        this.ocrInvoiceService = ocrInvoiceService;
    }

    /**
     * 查询待开票清单 - 一般采购
     */
    @GetMapping("/pending/general")
    public ResponseEntity<PendingInvoiceResponse> listPendingInvoiceGeneral(
            @RequestParam Long userId,
            @RequestParam(required = false) String partsNo,
            @RequestParam(required = false) String partsName,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        try {
            String supplierNo = getSupplierNoByUserId(userId);
            int offset = (pageNum - 1) * pageSize;

            List<PendingInvoiceItem> items = invoiceMapper.listPendingInvoiceGeneral(
                supplierNo, partsNo, partsName, pageSize, offset);
            long total = invoiceMapper.countPendingInvoiceGeneral(supplierNo);

            PendingInvoiceResponse resp = new PendingInvoiceResponse();
            resp.setItems(items);
            resp.setTotal(total);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 查询待开票清单 - BOM采购
     */
    @GetMapping("/pending/bom")
    public ResponseEntity<PendingInvoiceResponse> listPendingInvoiceBom(
            @RequestParam Long userId,
            @RequestParam(required = false) String partsNo,
            @RequestParam(required = false) String partsName,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        String supplierNo = getSupplierNoByUserId(userId);
        int offset = (pageNum - 1) * pageSize;

        List<PendingInvoiceItem> items = invoiceMapper.listPendingInvoiceBom(
            supplierNo, partsNo, partsName, pageSize, offset);
        long total = invoiceMapper.countPendingInvoiceBom(supplierNo);

        PendingInvoiceResponse resp = new PendingInvoiceResponse();
        resp.setItems(items);
        resp.setTotal(total);
        return ResponseEntity.ok(resp);
    }

    /**
     * 获取供应商编号（根据用户ID）
     */
    private String getSupplierNoByUserId(Long userId) {
        Long supplierId = invoiceMapper.findSupplierCompanyIdByUserId(userId);
        if (supplierId == null) {
            return null;
        }
        return invoiceMapper.getSupplierNoBySupplierId(supplierId);
    }

    /**
     * 生成结算单号
     */
    private String generateSettlementNo() {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int nextSeq = invoiceMapper.getMaxSettlementSeq(dateStr) + 1;
        return "S" + dateStr + String.format("%04d", nextSeq);
    }

    /**
     * 创建结算单
     */
    @PostMapping("/settlement")
    public ResponseEntity<Map<String, Object>> createSettlement(@RequestBody CreateSettlementRequest req) {
        System.out.println("=== createSettlement called ===");
        System.out.println("req=" + req);
        System.out.println("userId=" + req.getUserId());
        System.out.println("acceptDetailIds=" + req.getAcceptDetailIds());
        System.out.println("invoiceLines=" + req.getInvoiceLines());

        Long userId = req.getUserId();
        String supplierNo = getSupplierNoByUserId(userId);
        System.out.println("supplierNo=" + supplierNo);

        // 验证验收明细
        List<Long> acceptDetailIds = req.getAcceptDetailIds();
        if (acceptDetailIds == null || acceptDetailIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请选择要结算的明细"));
        }

        // 生成结算单号
        String settlementNo = generateSettlementNo();

        // 汇总发票数据
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;

        List<SettlementInvoiceLineRequest> invoiceLines = req.getInvoiceLines();
        if (invoiceLines != null && !invoiceLines.isEmpty()) {
            for (SettlementInvoiceLineRequest line : invoiceLines) {
                if (taxRate == BigDecimal.ZERO && line.getTaxRate() != null) {
                    taxRate = line.getTaxRate();
                }
                if (line.getTaxAmount() != null) totalTaxAmount = totalTaxAmount.add(line.getTaxAmount());
                if (line.getTotalAmount() != null) totalAmount = totalAmount.add(line.getTotalAmount());
            }
        }

        // 验收明细ID转JSON字符串
        String acceptDetailIdsJson;
        try {
            acceptDetailIdsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(acceptDetailIds);
        } catch (Exception e) {
            acceptDetailIdsJson = "[]";
            e.printStackTrace();
        }

        // 插入结算单头
        invoiceMapper.insertSettlementOrder(
            settlementNo,
            supplierNo,
            req.getSupplierName(),
            totalAmount.subtract(totalTaxAmount), // 不含税金额
            taxRate,
            totalTaxAmount,
            totalAmount,
            req.getRemark(),
            userId,
            req.getCreateDept(),
            acceptDetailIdsJson
        );

        long settlementId = invoiceMapper.getLastInsertId();

        // 插入发票行（每行包含附件信息）
        if (invoiceLines != null && !invoiceLines.isEmpty()) {
            for (SettlementInvoiceLineRequest reqLine : invoiceLines) {
                // 先保存附件到tb_flow_file
                Long attachmentId = null;
                if (reqLine.getAttachmentUrl() != null && !reqLine.getAttachmentUrl().isEmpty()) {
                    FlowFile flowFile = new FlowFile();
                    flowFile.setBusinessId(settlementId);
                    flowFile.setBusinessName("supplierInvoice");
                    flowFile.setUrl(reqLine.getAttachmentUrl());
                    flowFile.setName(reqLine.getAttachmentName());
                    flowFile.setCreateUser(userId);
                    flowFile.setStatus("1");
                    flowFileMapper.insert(flowFile);
                    attachmentId = flowFile.getId();
                }

                SettlementInvoiceLine line = new SettlementInvoiceLine();
                line.setSettlementId(settlementId);
                line.setInvoiceNo(reqLine.getInvoiceNo());
                line.setInvoiceDate(reqLine.getInvoiceDate() != null ? reqLine.getInvoiceDate().toString() : null);
                line.setProjectName(reqLine.getProjectName());
                line.setPartNo(reqLine.getPartNo());
                line.setTaxRate(reqLine.getTaxRate());
                line.setTaxAmount(reqLine.getTaxAmount());
                line.setTotalAmount(reqLine.getTotalAmount());
                line.setAttachmentId(attachmentId);
                line.setAttachmentName(reqLine.getAttachmentName());
                line.setAttachmentUrl(reqLine.getAttachmentUrl());

                invoiceMapper.insertSettlementInvoiceLine(line);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlementId);
        result.put("settlementNo", settlementNo);
        result.put("message", "结算单创建成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 查询结算单列表
     */
    @GetMapping("/settlement/list")
    public ResponseEntity<SettlementOrderResponse> listSettlementOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) String settlementNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        String supplierNo = getSupplierNoByUserId(userId);
        int offset = (pageNum - 1) * pageSize;

        List<SettlementOrderItem> items = invoiceMapper.listSettlementOrders(
            supplierNo, settlementNo, status, pageSize, offset);
        long total = invoiceMapper.countSettlementOrders(supplierNo);

        SettlementOrderResponse resp = new SettlementOrderResponse();
        resp.setItems(items);
        resp.setTotal(total);
        return ResponseEntity.ok(resp);
    }

    /**
     * OCR识别接口
     */
    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> ocrRecognize(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = ocrInvoiceService.recognizeInvoice(file);
        return ResponseEntity.ok(result);
    }
}