package srm.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final TransactionTemplate transactionTemplate;

    public InvoiceController(InvoiceMapper invoiceMapper, FlowFileMapper flowFileMapper, OcrInvoiceService ocrInvoiceService, TransactionTemplate transactionTemplate) {
        this.invoiceMapper = invoiceMapper;
        this.flowFileMapper = flowFileMapper;
        this.ocrInvoiceService = ocrInvoiceService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 查询待开票清单 - 一般采购（按验收单号分页，同一验收单不会被拆散）
     */
    @GetMapping("/pending/general")
    public ResponseEntity<PendingInvoiceResponse> listPendingInvoiceGeneral(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String supplierNo,
            @RequestParam(required = false) Boolean isAdmin,
            @RequestParam(required = false) String acceptApplyNo,
            @RequestParam(required = false) String partsNo,
            @RequestParam(required = false) String partsName,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        try {
            // 如果前端没传，则后端查询
            if (supplierNo == null && userId != null) {
                supplierNo = getSupplierNoByUserId(userId);
            }
            boolean adminFlag = Boolean.TRUE.equals(isAdmin);
            if (isAdmin == null && userId != null) {
                adminFlag = Boolean.TRUE.equals(invoiceMapper.isAdminUser(userId));
            }
            int offset = (pageNum - 1) * pageSize;

            // 先按验收单号分页查询（不查明细）
            List<String> applyNos = invoiceMapper.listPendingInvoiceGeneralByPage(
                supplierNo, acceptApplyNo, partsNo, partsName, adminFlag, pageSize, offset);

            // 根据验收单号列表查询所有明细
            List<PendingInvoiceItem> items = invoiceMapper.listPendingInvoiceGeneralByAcceptApplyNosSimple(
                supplierNo, adminFlag, applyNos);

            // 总数使用专门的数量统计查询
            long total = invoiceMapper.countPendingInvoiceGeneral(supplierNo, adminFlag);
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
     * 查询待开票清单 - BOM采购（按验收单号分页，同一验收单不会被拆散）
     */
    @GetMapping("/pending/bom")
    public ResponseEntity<PendingInvoiceResponse> listPendingInvoiceBom(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String supplierNo,
            @RequestParam(required = false) Boolean isAdmin,
            @RequestParam(required = false) String acceptApplyNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String partsNo,
            @RequestParam(required = false) String partsName,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        try {
            // 如果前端没传，则后端查询
            if (supplierNo == null && userId != null) {
                supplierNo = getSupplierNoByUserId(userId);
            }
            boolean adminFlag = Boolean.TRUE.equals(isAdmin);
            if (isAdmin == null && userId != null) {
                adminFlag = Boolean.TRUE.equals(invoiceMapper.isAdminUser(userId));
            }
            int offset = (pageNum - 1) * pageSize;

            // 先按验收单号分页查询（不查明细）
            List<String> applyNos = invoiceMapper.listPendingInvoiceBomByPage(
                supplierNo, acceptApplyNo, orderNo, partsNo, partsName, adminFlag, pageSize, offset);

            // 根据验收单号列表查询所有明细
            List<PendingInvoiceItem> items = invoiceMapper.listPendingInvoiceBomByAcceptApplyNos(
                supplierNo, acceptApplyNo, orderNo, partsNo, partsName, adminFlag, applyNos);

            // 总数使用专门的数量统计查询
            long total = invoiceMapper.countPendingInvoiceBomDetails(supplierNo, adminFlag);
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
     * 获取待开票数量统计（按验收单号去重）
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingInvoiceCount(@RequestParam Long userId) {
        String supplierNo = getSupplierNoByUserId(userId);
        boolean isAdmin = Boolean.TRUE.equals(invoiceMapper.isAdminUser(userId));
        long generalCount = invoiceMapper.countPendingInvoiceGeneral(supplierNo, isAdmin);
        long bomCount = invoiceMapper.countPendingInvoiceBom(supplierNo, isAdmin);
        Map<String, Long> result = new HashMap<>();
        result.put("general", generalCount);
        result.put("bom", bomCount);
        result.put("total", generalCount + bomCount);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取工作台消息列表（待确认订单号 + 待开票验收单号）
     */
    @GetMapping("/dashboard/messages")
    public ResponseEntity<List<Map<String, Object>>> getDashboardMessages(@RequestParam Long userId) {
        String supplierNo = getSupplierNoByUserId(userId);
        boolean isAdmin = Boolean.TRUE.equals(invoiceMapper.isAdminUser(userId));
        List<Map<String, Object>> messages = new ArrayList<>();

        // 待确认订单
        List<String> orderNos = invoiceMapper.listPendingOrderApplyNos(supplierNo, 0, 5, 0);
        for (String no : orderNos) {
            messages.add(Map.of("type", "order", "no", no, "text", "订单 " + no + " 待确认"));
        }

        // 一般采购待开票
        List<String> generalNos = invoiceMapper.listPendingInvoiceGeneralApplyNos(supplierNo, isAdmin, 5);
        for (String no : generalNos) {
            messages.add(Map.of("type", "general", "no", no, "text", "一般采购 " + no + " 待开票"));
        }

        // BOM采购待开票
        List<String> bomNos = invoiceMapper.listPendingInvoiceBomApplyNos(supplierNo, isAdmin, 5);
        for (String no : bomNos) {
            messages.add(Map.of("type", "bom", "no", no, "text", "生产采购 " + no + " 待开票"));
        }

        return ResponseEntity.ok(messages);
    }

    /**
     * 生成结算单号
     * 规则：供应商编号 + 年月 + 4位序号（如 GYS-2022100001 + 202605 + 0001）
     */
    private String generateSettlementNo(String supplierNo) {
        String dateStr = new SimpleDateFormat("yyyyMM").format(new Date());
        int nextSeq = invoiceMapper.getMaxSettlementSeq(supplierNo, dateStr) + 1;
        return supplierNo + dateStr + String.format("%04d", nextSeq);
    }

    /**
     * 创建结算单
     */
    @PostMapping("/settlement")
    public ResponseEntity<Map<String, Object>> createSettlement(@RequestBody CreateSettlementRequest req) {
        try {
            System.out.println("=== createSettlement called ===");
            System.out.println("req=" + req);

            Long userId = req.getUserId();
            String supplierNo = getSupplierNoByUserId(userId);
            System.out.println("supplierNo=" + supplierNo);

            if (supplierNo == null || supplierNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "无法获取供应商编号，请确认已绑定供应商"));
            }

            // 验证验收明细
            List<Long> acceptDetailIds = req.getAcceptDetailIds();
            if (acceptDetailIds == null || acceptDetailIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "请选择要结算的明细"));
            }

            // 汇总发票数据
            BigDecimal sumTaxAmount = BigDecimal.ZERO;
            BigDecimal sumAmount = BigDecimal.ZERO;
            BigDecimal sumTaxRate = BigDecimal.ZERO;
            List<SettlementInvoiceLineRequest> invoiceLines = req.getInvoiceLines();
            if (invoiceLines != null && !invoiceLines.isEmpty()) {
                for (SettlementInvoiceLineRequest line : invoiceLines) {
                    if (sumTaxRate == BigDecimal.ZERO && line.getTaxRate() != null) {
                        sumTaxRate = line.getTaxRate();
                    }
                    if (line.getTaxAmount() != null) sumTaxAmount = sumTaxAmount.add(line.getTaxAmount());
                    if (line.getTotalAmount() != null) sumAmount = sumAmount.add(line.getTotalAmount());
                }
            }
            final BigDecimal totalTaxAmount = sumTaxAmount;
            final BigDecimal totalAmount = sumAmount;
            final BigDecimal taxRate = sumTaxRate;

            // 验收明细ID转JSON字符串
            String acceptDetailIdsJson;
            try {
                acceptDetailIdsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(acceptDetailIds);
            } catch (Exception e) {
                acceptDetailIdsJson = "[]";
                e.printStackTrace();
            }
            final String detailIdsJson = acceptDetailIdsJson;

            // 使用事务包装，并发冲突时重试
            Map<String, Object> result = transactionTemplate.execute(status -> {
                String settlementNo = generateSettlementNo(supplierNo);

                // 获取验收申请编号（从发票行中获取，所有行应一致）
                String acceptApplyNo = null;
                if (invoiceLines != null && !invoiceLines.isEmpty() && invoiceLines.get(0).getAcceptApplyNo() != null) {
                    acceptApplyNo = invoiceLines.get(0).getAcceptApplyNo();
                }

                // 插入结算单头
                invoiceMapper.insertSettlementOrder(
                    settlementNo,
                    supplierNo,
                    req.getSupplierName(),
                    totalAmount.subtract(totalTaxAmount),
                    taxRate,
                    totalTaxAmount,
                    totalAmount,
                    req.getRemark(),
                    userId,
                    req.getCreateDept(),
                    detailIdsJson,
                    acceptApplyNo
                );

                long settlementId = invoiceMapper.getLastInsertId();

                // 插入发票行（每行包含附件信息）
                if (invoiceLines != null && !invoiceLines.isEmpty()) {
                    for (SettlementInvoiceLineRequest reqLine : invoiceLines) {
                        Long attachmentId = null;
                        if (reqLine.getAttachmentUrl() != null && !reqLine.getAttachmentUrl().isEmpty()) {
                            FlowFile flowFile = new FlowFile();
                            flowFile.setId(System.currentTimeMillis());
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
                        line.setId(System.currentTimeMillis());
                        line.setSettlementId(settlementId);
                        line.setInvoiceNo(reqLine.getInvoiceNo());
                        line.setInvoiceDate(reqLine.getInvoiceDate() != null ? reqLine.getInvoiceDate().toString() : null);
                        line.setProjectName(reqLine.getProjectName());
                        line.setPartNo(reqLine.getPartNo());
                        line.setTaxRate(reqLine.getTaxRate());
                        line.setAmount(reqLine.getAmount());
                        line.setTaxAmount(reqLine.getTaxAmount());
                        line.setTotalAmount(reqLine.getTotalAmount());
                        line.setAttachmentId(attachmentId);
                        line.setAttachmentName(reqLine.getAttachmentName());
                        line.setAttachmentUrl(reqLine.getAttachmentUrl());
                        line.setAcceptApplyNo(reqLine.getAcceptApplyNo());
                        line.setQuantity(reqLine.getQuantity());
                        line.setUnit(reqLine.getUnit());
                        invoiceMapper.insertSettlementInvoiceLine(line);
                    }
                }

                // 插入完工单附件（新建记录）
                List<CompletionFileRequest> completionFiles = req.getCompletionFiles();
                if (completionFiles != null && !completionFiles.isEmpty()) {
                    for (CompletionFileRequest file : completionFiles) {
                        invoiceMapper.insertCompletionFile(
                            System.currentTimeMillis(),
                            settlementId,
                            "completionDoc",
                            file.getUrl(),
                            file.getName(),
                            userId
                        );
                    }
                }

                // 插入订单附件（新建记录）
                List<CompletionFileRequest> orderFiles = req.getOrderFiles();
                if (orderFiles != null && !orderFiles.isEmpty()) {
                    for (CompletionFileRequest file : orderFiles) {
                        invoiceMapper.insertCompletionFile(
                            System.currentTimeMillis(),
                            settlementId,
                            "orderDoc",
                            file.getUrl(),
                            file.getName(),
                            userId
                        );
                    }
                }

                Map<String, Object> r = new HashMap<>();
                r.put("settlementId", settlementId);
                r.put("settlementNo", settlementNo);
                r.put("message", "结算单创建成功");
                return r;
            });

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "创建结算单失败: " + e.getMessage()));
        }
    }

    /**
     * 查询结算单列表
     */
    @GetMapping("/settlement/list")
    public ResponseEntity<SettlementOrderResponse> listSettlementOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) String settlementNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String acceptApplyNo,
            @RequestParam(required = false) String bomOrderNo,
            @RequestParam(required = false) String contractNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        String supplierNo = getSupplierNoByUserId(userId);
        int offset = (pageNum - 1) * pageSize;

        List<SettlementOrderItem> items = invoiceMapper.listSettlementOrders(
            supplierNo, settlementNo, status, acceptApplyNo, bomOrderNo, contractNo, pageSize, offset);
        long total = invoiceMapper.countSettlementOrders(supplierNo);

        SettlementOrderResponse resp = new SettlementOrderResponse();
        resp.setItems(items);
        resp.setTotal(total);
        return ResponseEntity.ok(resp);
    }

    /**
     * 获取结算单明细（发票行 + 验收明细）
     */
    @GetMapping("/settlement/{id}")
    public ResponseEntity<Map<String, Object>> getSettlementDetails(@PathVariable Long id) {
        try {
            List<SettlementInvoiceLine> invoiceLines = invoiceMapper.getSettlementInvoiceLines(id);
            List<SettlementDetailItem> details = invoiceMapper.getSettlementDetails(id);
            List<Map<String, Object>> completionFiles = invoiceMapper.getCompletionFiles(id);
            List<Map<String, Object>> orderFiles = invoiceMapper.getOrderFiles(id);
            String remark = invoiceMapper.getSettlementRemark(id);
            Map<String, Object> result = new HashMap<>();
            result.put("invoiceLines", invoiceLines);
            result.put("details", details);
            result.put("completionFiles", completionFiles);
            result.put("orderFiles", orderFiles);
            result.put("remark", remark);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "获取结算单明细失败: " + e.getMessage()));
        }
    }

    /**
     * 提交结算单（草稿 -> 已提交）
     */
    @PostMapping("/settlement/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitSettlement(@PathVariable Long id) {
        try {
            invoiceMapper.updateSettlementStatus(id, 1);
            return ResponseEntity.ok(Map.of("success", true, "message", "提交成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "提交失败: " + e.getMessage()));
        }
    }

    /**
     * 删除草稿结算单
     */
    @PostMapping("/settlement/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteSettlement(@PathVariable Long id) {
        try {
            invoiceMapper.deleteSettlementOrder(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    /**
     * 检查是否有草稿结算单包含指定验收明细
     */
    @PostMapping("/settlement/check")
    public ResponseEntity<Map<String, Object>> checkDraftSettlement(@RequestBody List<Long> acceptDetailIds) {
        try {
            List<Map<String, Object>> drafts = invoiceMapper.findDraftSettlements(acceptDetailIds);
            if (!drafts.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "hasDraft", true,
                    "settlementNo", drafts.get(0).get("settlementNo"),
                    "message", "已有草稿结算单 " + drafts.get(0).get("settlementNo") + "，请勿重复操作"
                ));
            }
            return ResponseEntity.ok(Map.of("hasDraft", false));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "检查失败: " + e.getMessage()));
        }
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