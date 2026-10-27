package srm.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 发票识别服务 - 使用百度智能云「增值税发票识别」（免费额度）。
 * 不调用欠费的智能财务票据识别（multiple_invoice）。
 * PDF 优先 pdf_file 直传；失败再转图片。品名用 PDF 文字层校准。
 */
@Service
public class OcrInvoiceService {

    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String VAT_INVOICE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice";

    // 增值税发票识别 Key
    @Value("${baidu.ocr.api-key:}")
    private String apiKey;

    @Value("${baidu.ocr.secret-key:}")
    private String secretKey;

    // 通用识别 Key（用于 PDF）
    @Value("${baidu.ocr.general-api-key:}")
    private String generalApiKey;

    @Value("${baidu.ocr.general-secret-key:}")
    private String generalSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存 access token
    private volatile String accessToken = null;
    private volatile String generalAccessToken = null;
    private volatile long tokenExpireTime = 0;
    private volatile long generalTokenExpireTime = 0;

    public Map<String, Object> recognizeInvoice(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            boolean isPdf = fileName != null && fileName.toLowerCase().endsWith(".pdf");

            if (isPdf) {
                // 英文商业发票（如 Cerence）有文字层，直接解析，不走国内增值税 OCR
                String pdfText = null;
                try {
                    pdfText = extractPdfText(fileBytes);
                } catch (Exception ignored) {
                    // 抽文本失败再走 OCR
                }
                Map<String, Object> english = tryParseEnglishCommercialInvoice(pdfText);
                if (english != null) {
                    return english;
                }
            }

            if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                Map<String, Object> result = createEmptyResult("增值税发票 OCR 未配置 API Key");
                result.put("success", false);
                return result;
            }

            if (isPdf) {
                // 百度 vat_invoice 对 PDF 默认只认第 1 页，多页必须带 pdf_file_num 逐页识别后合并
                Map<String, Object> result = recognizeMultiPagePdfVatInvoice(fileBytes, fileName);
                if (!Boolean.TRUE.equals(result.get("success"))) {
                    System.out.println("=== vat_invoice(PDF多页) 失败，回退 PDF转图片+vat_invoice: " + result.get("message") + " ===");
                    result = fallbackPdfToVatInvoice(fileBytes);
                }
                enrichProjectNamesFromPdf(result, fileBytes);
                sanitizeProjectNamesWithRowFields(result);
                finalizeCommodityRows(result);
                return result;
            } else {
                // 图片直接用增值税发票识别
                Map<String, Object> result = callVatInvoiceOcr(fileBytes, fileName);
                sanitizeProjectNamesWithRowFields(result);
                finalizeCommodityRows(result);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = createEmptyResult("OCR识别失败: " + e.getMessage());
            result.put("success", false);
            return result;
        }
    }

    private synchronized String getAccessToken(boolean isGeneral) throws Exception {
        String key = isGeneral ? generalApiKey : apiKey;
        String secret = isGeneral ? generalSecretKey : secretKey;
        long expireTime = isGeneral ? generalTokenExpireTime : tokenExpireTime;
        String cachedToken = isGeneral ? generalAccessToken : accessToken;

        if (cachedToken != null && System.currentTimeMillis() < expireTime - 300000) {
            return cachedToken;
        }

        String urlStr = ACCESS_TOKEN_URL + "?grant_type=client_credentials" +
            "&client_id=" + key +
            "&client_secret=" + secret;

        String response = doGet(urlStr);
        JsonNode json = objectMapper.readTree(response);
        if (json.has("access_token")) {
            String token = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 2592000;
            long newExpireTime = System.currentTimeMillis() + expiresIn * 1000L;

            if (isGeneral) {
                generalAccessToken = token;
                generalTokenExpireTime = newExpireTime;
            } else {
                accessToken = token;
                tokenExpireTime = newExpireTime;
            }
            return token;
        } else {
            throw new Exception("获取百度 Access Token 失败: " + response);
        }
    }

    private Map<String, Object> callVatInvoiceOcr(byte[] fileBytes, String fileName) throws Exception {
        return callVatInvoiceOcr(fileBytes, fileName, null);
    }

    /**
     * @param pdfPageNum 1-based PDF 页码；null 表示图片或不指定页（百度默认第 1 页）
     */
    private Map<String, Object> callVatInvoiceOcr(byte[] fileBytes, String fileName, Integer pdfPageNum) throws Exception {
        String token = getAccessToken(false);
        String requestUrl = VAT_INVOICE_URL + "?access_token=" + token;

        String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);
        boolean isPdf = fileName != null && fileName.toLowerCase().endsWith(".pdf");
        // 增值税发票识别支持 image / pdf_file；PDF 走 pdf_file 更准，避免先转图丢清晰度
        StringBuilder formData = new StringBuilder();
        formData.append(isPdf ? "pdf_file=" : "image=")
                .append(java.net.URLEncoder.encode(fileBase64, StandardCharsets.UTF_8));
        if (isPdf && pdfPageNum != null && pdfPageNum > 0) {
            formData.append("&pdf_file_num=").append(pdfPageNum);
        }

        String response = doPost(requestUrl, formData.toString());
        System.out.println("=== vat_invoice(" + (isPdf ? "PDF page=" + pdfPageNum : "image") + ") 原始响应前500字: "
            + response.substring(0, Math.min(500, response.length())) + " ===");
        return parseVatInvoiceResponse(response);
    }

    private int getPdfPageCount(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            return document.getNumberOfPages();
        }
    }

    /**
     * 多页电子发票：逐页调用 vat_invoice（pdf_file_num），合并明细行。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> recognizeMultiPagePdfVatInvoice(byte[] fileBytes, String fileName) throws Exception {
        int pageCount = getPdfPageCount(fileBytes);
        System.out.println("=== PDF pages=" + pageCount + "，逐页 vat_invoice ===");
        if (pageCount <= 1) {
            return callVatInvoiceOcr(fileBytes, fileName, 1);
        }

        List<Map<String, Object>> allRows = new ArrayList<>();
        String lastError = null;
        for (int page = 1; page <= pageCount; page++) {
            Map<String, Object> pageResult = callVatInvoiceOcr(fileBytes, fileName, page);
            if (!Boolean.TRUE.equals(pageResult.get("success"))) {
                lastError = String.valueOf(pageResult.get("message"));
                System.out.println("=== PDF 第" + page + "页识别失败: " + lastError + " ===");
                continue;
            }
            List<Map<String, Object>> pageRows = (List<Map<String, Object>>) pageResult.get("invoiceResults");
            if (pageRows == null || pageRows.isEmpty()) {
                System.out.println("=== PDF 第" + page + "页无商品行（可能是备注页） ===");
                continue;
            }
            int before = allRows.size();
            for (Map<String, Object> row : pageRows) {
                if (isMeaningfulCommodityRow(row)) {
                    allRows.add(row);
                }
            }
            System.out.println("=== PDF 第" + page + "页合并商品行 +" + (allRows.size() - before)
                    + "（本页原始 " + pageRows.size() + "）===");
        }

        Map<String, Object> merged = new HashMap<>();
        merged.put("invoiceResults", allRows);
        merged.put("count", allRows.size());
        merged.put("success", !allRows.isEmpty());
        merged.put("message", allRows.isEmpty()
                ? (lastError != null ? lastError : "多页 PDF 未识别到商品明细")
                : ("识别到 " + allRows.size() + " 条商品（共" + pageCount + "页）"));
        if (!allRows.isEmpty()) {
            Map<String, Object> first = allRows.get(0);
            merged.put("invoiceNo", first.get("invoiceNo"));
            merged.put("invoiceDate", first.get("invoiceDate"));
            merged.put("totalAmount", first.get("totalAmount"));
            merged.put("taxAmount", first.get("taxAmount"));
            merged.put("taxRate", first.get("taxRate"));
            merged.put("projectName", first.get("projectName"));
            merged.put("partNo", first.get("partNo"));
        }
        return merged;
    }

    private boolean isMeaningfulCommodityRow(Map<String, Object> row) {
        if (row == null) {
            return false;
        }
        fixMisplacedQuantityUnit(row);
        String name = row.get("projectName") == null ? "" : String.valueOf(row.get("projectName")).trim();
        double amount = toDouble(row.get("amount"));
        double tax = toDouble(row.get("taxAmount"));
        double total = toDouble(row.get("totalAmount"));
        if (amount <= 0 && total > 0) {
            amount = total;
        }
        // 金额全 0：备注页/空行误识别
        if (amount <= 0 && tax <= 0 && total <= 0) {
            return false;
        }
        if (name.isEmpty()) {
            return false;
        }
        String compact = name.replace(" ", "");
        if (compact.equals("合计") || compact.equals("小计") || compact.startsWith("价税合计")
                || compact.startsWith("项目名称") || compact.equals("备注") || compact.equals("费")
                || compact.contains("总数量") || compact.contains("总金额")
                || compact.contains("收款人") || compact.contains("复核人")
                || compact.contains("开户银行") || compact.contains("银行账号")) {
            return false;
        }
        return true;
    }

    /**
     * 百度偶发把数量填进「单位」、数量为 0（本张星图发票即如此）。
     */
    private void fixMisplacedQuantityUnit(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        double qty = toDouble(row.get("quantity"));
        String unit = row.get("unit") == null ? "" : String.valueOf(row.get("unit")).trim();
        if (qty <= 0 && unit.matches("\\d+(?:\\.\\d+)?")) {
            row.put("quantity", Double.parseDouble(unit));
            row.put("unit", "");
        }
    }

    private double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 回退方案：PDF 转图片后逐页用 增值税发票识别（vat_invoice）
     */
    private Map<String, Object> fallbackPdfToVatInvoice(byte[] fileBytes) throws Exception {
        List<byte[]> imagePages = pdfToImages(fileBytes);
        List<Map<String, Object>> allInvoiceResults = new ArrayList<>();
        Map<String, Object> mergedResult = new HashMap<>();

        for (int i = 0; i < imagePages.size(); i++) {
            byte[] pageImage = imagePages.get(i);
            Map<String, Object> pageResult = callVatInvoiceOcr(pageImage, "page_" + i + ".png");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pageInvoices = (List<Map<String, Object>>) pageResult.get("invoiceResults");
            if (pageInvoices != null) {
                allInvoiceResults.addAll(pageInvoices);
            }
        }

        mergedResult.put("invoiceResults", allInvoiceResults);
        mergedResult.put("count", allInvoiceResults.size());
        mergedResult.put("success", !allInvoiceResults.isEmpty());
        mergedResult.put("message", "PDF 识别到 " + allInvoiceResults.size() + " 条发票（回退路径）");
        if (!allInvoiceResults.isEmpty()) {
            Map<String, Object> first = allInvoiceResults.get(0);
            mergedResult.put("invoiceNo", first.get("invoiceNo"));
            mergedResult.put("invoiceDate", first.get("invoiceDate"));
            mergedResult.put("totalAmount", first.get("totalAmount"));
            mergedResult.put("taxAmount", first.get("taxAmount"));
            mergedResult.put("taxRate", first.get("taxRate"));
            mergedResult.put("projectName", first.get("projectName"));
            mergedResult.put("partNo", first.get("partNo"));
        }
        return mergedResult;
    }

    /**
     * PDF 转换为图片列表（每页一张 PNG）
     */
    private List<byte[]> pdfToImages(byte[] pdfBytes) throws IOException {
        List<byte[]> imagePages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                imagePages.add(baos.toByteArray());
            }
        }
        return imagePages;
    }

    private Map<String, Object> parseVatInvoiceResponse(String jsonResponse) {
        Map<String, Object> result = createEmptyResult("识别完成");
        try {
            JsonNode json = objectMapper.readTree(jsonResponse);
            result.put("rawResponse", jsonResponse);

            // DEBUG: 打印原始返回结构
            System.out.println("=== OCR VAT Response ===");
            System.out.println("words_result type: " + (json.has("words_result") ? json.get("words_result").getNodeType() : "MISSING"));
            System.out.println("words_result: " + json.get("words_result"));
            System.out.println("========================");

            if (json.has("error_code")) {
                String errorMsg = json.has("error_msg") ? json.get("error_msg").asText() : "百度 OCR 错误";
                result.put("message", errorMsg);
                result.put("success", false);
                return result;
            }

            JsonNode wordsResult = json.get("words_result");
            if (wordsResult == null) {
                result.put("message", "未识别到发票信息");
                result.put("success", false);
                return result;
            }

            // 判断是数组还是单个对象
            JsonNode vatResult;
            if (wordsResult.isArray()) {
                // 数组结构：取第一个元素的 result
                if (wordsResult.isEmpty()) {
                    result.put("message", "未识别到发票信息");
                    result.put("success", false);
                    return result;
                }
                JsonNode invoiceNode = wordsResult.get(0);
                vatResult = invoiceNode.has("result") ? invoiceNode.get("result") : invoiceNode;
            } else {
                // 单个对象结构：words_result 本身就是发票结果
                vatResult = wordsResult;
            }

            // 发票表头
            String invoiceNo = vatResult.has("InvoiceNum") ? vatResult.get("InvoiceNum").asText("") : "";
            String invoiceDate = vatResult.has("InvoiceDate") ? vatResult.get("InvoiceDate").asText("").replace("年", "-").replace("月", "-").replace("日", "") : "";
            String buyerName = vatResult.has("PurchaserName") ? vatResult.get("PurchaserName").asText("") : "";
            String sellerName = vatResult.has("SellerName") ? vatResult.get("SellerName").asText("") : "";

            // 按 row 分组：找出所有 row 值
            Set<String> rowKeys = new LinkedHashSet<>();
            JsonNode[] commodityFields = {
                vatResult.has("CommodityName") ? vatResult.get("CommodityName") : null,
                vatResult.has("CommodityType") ? vatResult.get("CommodityType") : null,
                vatResult.has("CommodityTaxRate") ? vatResult.get("CommodityTaxRate") : null,
                vatResult.has("CommodityAmount") ? vatResult.get("CommodityAmount") : null,
                vatResult.has("CommodityTax") ? vatResult.get("CommodityTax") : null,
            };
            for (JsonNode field : commodityFields) {
                if (field != null && field.isArray()) {
                    for (JsonNode item : field) {
                        if (item.has("row")) {
                            rowKeys.add(item.get("row").asText());
                        }
                    }
                }
            }

            // 按 row 聚合每行数据
            System.out.println("=== Row Keys Found: " + rowKeys + " (count=" + rowKeys.size() + ") ===");
            List<Map<String, Object>> invoiceResults = new ArrayList<>();
            for (String rowKey : rowKeys) {
                Map<String, Object> invoice = new HashMap<>();
                invoice.put("invoiceNo", invoiceNo);
                invoice.put("invoiceDate", invoiceDate);
                invoice.put("buyerName", buyerName);
                invoice.put("sellerName", sellerName);

                invoice.put("projectName", normalizeCommodityName(getRowWord(vatResult.has("CommodityName") ? vatResult.get("CommodityName") : null, rowKey)));
                invoice.put("partNo", getRowWord(vatResult.has("CommodityType") ? vatResult.get("CommodityType") : null, rowKey));
                invoice.put("quantity", parseDouble(getRowWord(vatResult.has("CommodityNum") ? vatResult.get("CommodityNum") : null, rowKey).replace(",", ""), 0.0));
                invoice.put("unit", getRowWord(vatResult.has("CommodityUnit") ? vatResult.get("CommodityUnit") : null, rowKey));
                invoice.put("taxRate", parseTaxRate(getRowWord(vatResult.has("CommodityTaxRate") ? vatResult.get("CommodityTaxRate") : null, rowKey)));
                double amount = parseDouble(getRowWord(vatResult.has("CommodityAmount") ? vatResult.get("CommodityAmount") : null, rowKey).replace(",", ""), 0.0);
                double taxAmount = parseDouble(getRowWord(vatResult.has("CommodityTax") ? vatResult.get("CommodityTax") : null, rowKey).replace(",", ""), 0.0);
                invoice.put("amount", amount);
                invoice.put("taxAmount", taxAmount);
                invoice.put("totalAmount", Math.round((amount + taxAmount) * 100.0) / 100.0);
                invoiceResults.add(invoice);
                System.out.println("Row " + rowKey + " -> projectName=" + invoice.get("projectName") + ", partNo=" + invoice.get("partNo") + ", amount=" + amount + ", taxAmount=" + taxAmount);
            }

            System.out.println("=== invoiceResults size: " + invoiceResults.size() + " ===");
            result.put("invoiceResults", invoiceResults);
            result.put("count", invoiceResults.size());

            if (!invoiceResults.isEmpty()) {
                Map<String, Object> first = invoiceResults.get(0);
                result.put("invoiceNo", first.get("invoiceNo"));
                result.put("invoiceDate", first.get("invoiceDate"));
                result.put("totalAmount", first.get("totalAmount"));
                result.put("taxAmount", first.get("taxAmount"));
                result.put("taxRate", first.get("taxRate"));
                result.put("projectName", first.get("projectName"));
                result.put("partNo", first.get("partNo"));
            }
            result.put("success", true);
            result.put("message", "识别到 " + invoiceResults.size() + " 条商品");
        } catch (Exception e) {
            result.put("message", "解析失败: " + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 电子发票 PDF 通常含文字层。OCR 对英文品名易把字母/空格认错（如 VDC→VIC、A/S 粘连），
     * 用 PDF 抽文本覆盖 projectName；规格型号有值时同步回填 partNo（零件号）。
     */
    @SuppressWarnings("unchecked")
    private void enrichProjectNamesFromPdf(Map<String, Object> result, byte[] pdfBytes) {
        try {
            String text = extractPdfText(pdfBytes);
            if (text == null || text.isBlank()) {
                return;
            }
            System.out.println("=== PDF Text Extract (first 800 chars) ===\n" + text.substring(0, Math.min(800, text.length())));
            List<Map<String, String>> pdfLines = parseCommodityLinesFromPdfText(text);
            if (pdfLines.isEmpty()) {
                System.out.println("=== PDF text: no commodity lines parsed ===");
                return;
            }
            System.out.println("=== PDF commodity lines: " + pdfLines.size() + " ===");

            List<Map<String, Object>> invoiceResults = (List<Map<String, Object>>) result.get("invoiceResults");
            if (invoiceResults == null || invoiceResults.isEmpty()) {
                return;
            }
            // 按金额对齐 PDF 明细；未命中的再按行序兜底
            alignAndEnrichFromPdfLines(invoiceResults, pdfLines);
            if (!invoiceResults.isEmpty()) {
                if (invoiceResults.get(0).get("projectName") != null) {
                    result.put("projectName", invoiceResults.get(0).get("projectName"));
                }
                if (invoiceResults.get(0).get("partNo") != null) {
                    result.put("partNo", invoiceResults.get(0).get("partNo"));
                }
            }
            result.put("projectNameSource", "pdf_text");
        } catch (Exception e) {
            System.out.println("=== PDF text enrich failed: " + e.getMessage() + " ===");
        }
    }

    /**
     * 用 PDF 文字层明细回填 OCR 行：优先金额对齐，其次行序。
     */
    private void alignAndEnrichFromPdfLines(
            List<Map<String, Object>> invoiceResults,
            List<Map<String, String>> pdfLines
    ) {
        boolean[] pdfUsed = new boolean[pdfLines.size()];
        for (Map<String, Object> inv : invoiceResults) {
            double invAmount = toDouble(inv.get("amount"));
            int matched = -1;
            if (invAmount > 0) {
                for (int p = 0; p < pdfLines.size(); p++) {
                    if (pdfUsed[p]) {
                        continue;
                    }
                    double pdfAmount = parseDouble(pdfLines.get(p).get("amount"), 0.0);
                    if (pdfAmount > 0 && Math.abs(pdfAmount - invAmount) < 0.02) {
                        matched = p;
                        break;
                    }
                }
            }
            if (matched >= 0) {
                pdfUsed[matched] = true;
                applyPdfLineToInvoice(inv, pdfLines.get(matched));
            }
        }
        // 行序兜底：尚未写入完整品名的 OCR 行
        int p = 0;
        for (Map<String, Object> inv : invoiceResults) {
            while (p < pdfLines.size() && pdfUsed[p]) {
                p++;
            }
            if (p >= pdfLines.size()) {
                break;
            }
            String curName = inv.get("projectName") == null ? "" : String.valueOf(inv.get("projectName"));
            String pdfName = pdfLines.get(p).get("projectName");
            boolean needName = curName.isBlank()
                    || (pdfName != null && pdfName.contains("费") && !curName.contains("费"))
                    || (pdfName != null && pdfName.startsWith("*") && !curName.startsWith("*"))
                    || (pdfName != null && !pdfName.isBlank() && pdfName.length() > curName.length() + 2);
            if (needName) {
                pdfUsed[p] = true;
                applyPdfLineToInvoice(inv, pdfLines.get(p));
                p++;
            }
        }
    }

    private void applyPdfLineToInvoice(Map<String, Object> inv, Map<String, String> pdfLine) {
        String pdfName = pdfLine.get("projectName");
        String pdfPartNo = pdfLine.get("partNo");
        if (pdfName != null && !pdfName.isBlank()) {
            inv.put("projectName", pdfName);
        }
        if (pdfPartNo != null && !pdfPartNo.isBlank()) {
            inv.put("partNo", pdfPartNo);
        }
        double pdfQty = parseDouble(pdfLine.get("quantity"), 0.0);
        if (pdfQty > 0) {
            inv.put("quantity", pdfQty);
            String unit = inv.get("unit") == null ? "" : String.valueOf(inv.get("unit")).trim();
            if (unit.matches("\\d+(?:\\.\\d+)?")) {
                inv.put("unit", "");
            }
        }
        double pdfAmount = parseDouble(pdfLine.get("amount"), 0.0);
        double pdfTax = parseDouble(pdfLine.get("taxAmount"), 0.0);
        if (pdfAmount > 0) {
            inv.put("amount", pdfAmount);
            if (pdfTax > 0) {
                inv.put("taxAmount", pdfTax);
            }
            inv.put("totalAmount", Math.round((pdfAmount + Math.max(pdfTax, 0)) * 100.0) / 100.0);
        }
    }

    /**
     * 最终纠偏：数量/单位错位、过滤垃圾行，并回写 count。
     */
    @SuppressWarnings("unchecked")
    private void finalizeCommodityRows(Map<String, Object> result) {
        Object raw = result.get("invoiceResults");
        if (!(raw instanceof List<?>)) {
            return;
        }
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw;
        List<Map<String, Object>> cleaned = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            fixMisplacedQuantityUnit(row);
            if (isMeaningfulCommodityRow(row)) {
                cleaned.add(row);
            }
        }
        result.put("invoiceResults", cleaned);
        result.put("count", cleaned.size());
        result.put("success", !cleaned.isEmpty());
        if (!cleaned.isEmpty()) {
            Map<String, Object> first = cleaned.get(0);
            result.put("invoiceNo", first.get("invoiceNo"));
            result.put("invoiceDate", first.get("invoiceDate"));
            result.put("totalAmount", first.get("totalAmount"));
            result.put("taxAmount", first.get("taxAmount"));
            result.put("taxRate", first.get("taxRate"));
            result.put("projectName", first.get("projectName"));
            result.put("partNo", first.get("partNo"));
            result.put("message", "识别到 " + cleaned.size() + " 条商品");
        }
    }

    /**
     * 品名 / 规格型号彻底分离 + 去掉误拼的数量单位。
     * 电子发票常见错误：品名写成「*税目*货物名称 + 规格型号」。
     */
    @SuppressWarnings("unchecked")
    private void sanitizeProjectNamesWithRowFields(Map<String, Object> result) {
        Object raw = result.get("invoiceResults");
        if (!(raw instanceof List<?>)) {
            return;
        }
        List<Map<String, Object>> invoiceResults = (List<Map<String, Object>>) raw;
        for (Map<String, Object> inv : invoiceResults) {
            if (inv == null) {
                continue;
            }
            String name = inv.get("projectName") == null ? "" : String.valueOf(inv.get("projectName"));
            String partNo = inv.get("partNo") == null ? "" : String.valueOf(inv.get("partNo")).trim();
            String unit = inv.get("unit") == null ? "" : String.valueOf(inv.get("unit")).trim();
            String qty = formatQtyToken(inv.get("quantity"));

            name = stripTrailingToken(name, qty);
            name = stripTrailingToken(name, unit);
            name = name.replaceFirst("\\s+\\d{4,}\\s+\\S+\\s+[\\d.]+\\s*$", "").trim();

            String[] split = splitProjectNameAndSpec(name, partNo);
            inv.put("projectName", normalizeCommodityName(split[0]));
            if (split[1] != null && !split[1].isBlank()) {
                inv.put("partNo", normalizePartNoDisplay(split[1]));
            }
        }
        if (!invoiceResults.isEmpty() && invoiceResults.get(0).get("projectName") != null) {
            result.put("projectName", invoiceResults.get(0).get("projectName"));
            if (invoiceResults.get(0).get("partNo") != null) {
                result.put("partNo", invoiceResults.get(0).get("partNo"));
            }
        }
    }

    /**
     * 将「*税收分类*货物名称」与「规格型号」拆开。
     * @return [projectName, partNo]；partNo 可能为空（表示沿用原 partNo）
     */
    static String[] splitProjectNameAndSpec(String projectName, String existingPartNo) {
        String name = normalizeCommodityName(projectName);
        String partNo = existingPartNo == null ? "" : existingPartNo.trim();
        if (name.isBlank()) {
            return new String[]{"", partNo};
        }

        // 0) 尾部纯数字零件号（如 …出口 26644565）
        Matcher trailPart = Pattern.compile("^(.*?)\\s+(\\d{5,})$").matcher(name);
        if (trailPart.matches()) {
            name = normalizeCommodityName(trailPart.group(1));
            partNo = preferPartNo(partNo, trailPart.group(2));
        }

        // 1) *税目*纯中文名 + 空格 + 字母开头规格（ZP22 / ZS EV / EH32 MCE）
        //    注意：勿把品名后缀缩写（车载TBOX / 车载 TBOX）误剥成规格
        Matcher withTax = Pattern.compile(
                "^(\\*[^*]+\\*[\\u4e00-\\u9fff]+)(\\s+[A-Za-z][\\s\\S]*)$"
        ).matcher(name);
        if (withTax.matches()) {
            String pure = normalizeCommodityName(withTax.group(1));
            String spec = normalizeCommodityName(withTax.group(2));
            if (!pure.isBlank() && !spec.isBlank() && shouldSplitAsSpec(spec)) {
                return new String[]{pure, preferPartNo(partNo, spec)};
            }
        }
        // 无空格：*税目*中文名ZP22... / 许可费ZP22...
        Matcher gluedTax = Pattern.compile(
                "^(\\*[^*]+\\*[\\u4e00-\\u9fff]+)([A-Za-z][\\s\\S]*)$"
        ).matcher(name);
        if (gluedTax.matches()) {
            String pure = normalizeCommodityName(gluedTax.group(1));
            String spec = normalizeCommodityName(gluedTax.group(2));
            if (!pure.isBlank() && !spec.isBlank() && shouldSplitAsSpec(spec)) {
                return new String[]{pure, preferPartNo(partNo, spec)};
            }
        }

        // 2) 已有零件号：从品名中剔除（忽略空格差异）
        if (!partNo.isBlank()) {
            name = stripCompactOccurrence(name, partNo);
        }

        // 3) 无税目标签时：从右侧剥连续「规格样」token
        String peeled = peelTrailingSpecTokens(name);
        if (!peeled.equals(name)) {
            String spec = name.substring(peeled.length()).trim();
            name = peeled;
            if (!spec.isBlank()) {
                partNo = preferPartNo(partNo, spec);
            }
        }

        return new String[]{normalizeCommodityName(name), partNo};
    }

    /** 已有零件号优先；否则用规格文本（去多余空格） */
    private static String preferPartNo(String existing, String specFromName) {
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        return normalizePartNoDisplay(specFromName);
    }

    /**
     * 是否应从品名中拆出为规格/零件号。
     * 单段纯字母缩写（TBOX、GPS）视为品名一部分；含数字、中文或多段型号才拆。
     */
    static boolean shouldSplitAsSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            return false;
        }
        String t = spec.trim();
        if (t.matches(".*\\d.*")) {
            return true;
        }
        if (t.matches(".*[\\u4e00-\\u9fff].*")) {
            return true;
        }
        // 多段（ZS EV / A/S）视为规格前缀
        if (t.contains(" ") || t.contains("/") || t.contains("-")) {
            return true;
        }
        // 单段纯拉丁（TBOX）留在品名
        return false;
    }

    static String normalizePartNoDisplay(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // 规格型号展示：压缩空格，保留字母数字中文
        return raw.trim().replaceAll("\\s+", "");
    }

    private static String compactAlnum(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 从品名中去掉与零件号等价的片段（忽略空格），优先去尾部。
     */
    static String stripCompactOccurrence(String name, String partNo) {
        if (name == null || name.isBlank() || partNo == null || partNo.isBlank()) {
            return name == null ? "" : name;
        }
        String compactPart = compactAlnum(partNo);
        if (compactPart.length() < 2) {
            return name;
        }
        String compactName = compactAlnum(name);
        if (!compactName.contains(compactPart)) {
            return name;
        }
        // 尾部匹配：按原串从右收缩
        if (compactName.endsWith(compactPart)) {
            int keepCompactLen = compactName.length() - compactPart.length();
            StringBuilder kept = new StringBuilder();
            int seen = 0;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isWhitespace(c)) {
                    if (seen > 0 && seen < keepCompactLen) {
                        kept.append(c);
                    }
                    continue;
                }
                if (seen < keepCompactLen) {
                    kept.append(c);
                    seen++;
                } else {
                    break;
                }
            }
            return normalizeCommodityName(kept.toString());
        }
        // 非尾部：去掉首次紧凑匹配对应的原文字段（粗粒度：按空格 token 删）
        String[] tokens = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        StringBuilder window = new StringBuilder();
        for (String tok : tokens) {
            String trial = window + compactAlnum(tok);
            if (trial.equals(compactPart) || trial.endsWith(compactPart)) {
                // drop this window
                window.setLength(0);
                continue;
            }
            if (compactPart.startsWith(trial) && trial.length() < compactPart.length()) {
                window.append(compactAlnum(tok));
                continue;
            }
            if (window.length() > 0) {
                // window 不是零件号前缀，吐出
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(window);
                window.setLength(0);
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(tok);
        }
        return normalizeCommodityName(out.toString());
    }

    /** 无税目时：剥掉尾部「型号样」token（字母开头且含数字） */
    static String peelTrailingSpecTokens(String name) {
        if (name == null || name.isBlank() || name.startsWith("*")) {
            return name == null ? "" : name;
        }
        String[] tokens = name.trim().split("\\s+");
        int end = tokens.length;
        while (end > 1) {
            String tok = tokens[end - 1];
            if (tok.matches("(?i)[A-Z]{1,8}\\d{1,6}[A-Z0-9._-]*")
                    || tok.matches("(?i).*(EU|UK|ISA|MY\\d+).*")
                    || tok.matches(".*图资.*") || tok.equals("离线") || tok.equals("离线图资")) {
                end--;
                continue;
            }
            break;
        }
        if (end == tokens.length) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    private String formatQtyToken(Object quantity) {
        if (quantity == null) {
            return "";
        }
        if (quantity instanceof Number) {
            double d = ((Number) quantity).doubleValue();
            if (Math.abs(d - Math.rint(d)) < 1e-9) {
                return String.valueOf((long) Math.rint(d));
            }
            return String.valueOf(quantity);
        }
        String s = String.valueOf(quantity).trim();
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    private String stripTrailingToken(String name, String token) {
        if (name == null || name.isBlank() || token == null || token.isBlank() || "null".equalsIgnoreCase(token)) {
            return name == null ? "" : name;
        }
        String t = name.trim();
        String tok = token.trim();
        if (t.endsWith(" " + tok)) {
            return t.substring(0, t.length() - tok.length() - 1).trim();
        }
        if (tok.matches("\\d+") && t.matches("(?s).*\\s+" + tok + "\\.0+\\s*$")) {
            return t.replaceFirst("\\s+" + tok + "\\.0+\\s*$", "").trim();
        }
        return t;
    }

    private String extractPdfText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * 从电子发票文字层解析明细行。
     * 各票列齐全度不同（规格型号/单位/数量/单价都可能缺），统一从行尾
     * 「金额 税率% 税额」锚定，再向左剥单价、数量、单位、规格型号，剩余为品名。
     */
    /**
     * 从电子发票文字层解析明细行。
     * 跨行结构（常见）：
     *   *税目*货物名称...
     *   费                  ← 品名续行（中文）
     *   ZP22 EU 离线图      ← 规格型号开始（字母型号）
     *   资
     *   13%金额 ...
     * 品名与规格分阶段收集，避免互相污染、避免「费」丢失。
     */
    private List<Map<String, String>> parseCommodityLinesFromPdfText(String text) {
        List<Map<String, String>> linesOut = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return linesOut;
        }
        String[] lines = text.replace("\r", "").split("\n");
        int i = indexOfCommodityHeader(lines, 0);
        if (i < 0) {
            return linesOut;
        }
        i++;

        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                i++;
                continue;
            }
            if (isCommoditySectionEnd(line) || looksLikePageFooter(line)) {
                int next = indexOfCommodityHeader(lines, i + 1);
                if (next >= 0) {
                    i = next + 1;
                    continue;
                }
                break;
            }

            // 跨页残片：上一页末行金额已解析，本页表头后先出现「费 年图资」等，挂回上一明细
            if (looksLikeFeeSpecFragment(line) && !linesOut.isEmpty()) {
                Map<String, String> prev = linesOut.get(linesOut.size() - 1);
                String prevName = prev.get("projectName");
                if (prevName != null && !prevName.endsWith("费")) {
                    StringBuilder name = new StringBuilder(prevName);
                    StringBuilder spec = new StringBuilder(
                            prev.get("partNo") == null ? "" : prev.get("partNo"));
                    if (applyFeeSpecContinuation(name, spec, line)) {
                        String[] split = splitProjectNameAndSpec(name.toString(), spec.toString());
                        prev.put("projectName", normalizeCommodityName(split[0]));
                        prev.put("partNo", normalizePartNoDisplay(
                                (split[1] != null && !split[1].isBlank()) ? split[1] : spec.toString()));
                        i++;
                        continue;
                    }
                }
            }

            // 优先：*税目* 跨行块
            if (line.startsWith("*")) {
                int consumed = parseStarTaxCategoryMultiline(lines, i, linesOut);
                if (consumed >= i) {
                    i = consumed + 1;
                    continue;
                }
            }

            ParsedCommodityRow row = parseCommodityRowFromRight(line);
            if (row == null) {
                row = parseGluedElectronicCommodityRow(line);
            }
            if (row != null) {
                Map<String, String> item = new HashMap<>();
                String[] split = splitProjectNameAndSpec(row.projectName, row.partNo);
                item.put("projectName", normalizeCommodityName(split[0]));
                item.put("partNo", normalizePartNoDisplay(
                        (split[1] != null && !split[1].isBlank()) ? split[1]
                                : (row.partNo == null ? "" : row.partNo)));
                putMoneyFields(item, row);
                linesOut.add(item);
                i++;
                continue;
            }
            i++;
        }
        return linesOut;
    }

    /**
     * 解析以 *税收分类* 开头的跨行明细，返回最后消费的行下标；失败返回 -1。
     * <p>
     * 布局 A（列错位拆行）：品名 / 费 / 规格 / 资 / 13%金额 分行<br>
     * 布局 B（PDFBox sortByPosition，星图常见）：首行已含品名+规格+金额，次行为「费 资」等残片
     */
    private int parseStarTaxCategoryMultiline(String[] lines, int start, List<Map<String, String>> linesOut) {
        String first = lines[start] == null ? "" : lines[start].trim();
        if (!first.startsWith("*")) {
            return -1;
        }

        // —— 布局 B：首行已带金额 ——
        if (first.contains("%")) {
            ParsedCommodityRow moneyOnFirst = parseCommodityRowFromRight(first);
            if (moneyOnFirst == null) {
                moneyOnFirst = parseGluedElectronicCommodityRow(first);
            }
            if (moneyOnFirst != null && moneyOnFirst.amount != null && moneyOnFirst.amount > 0) {
                StringBuilder name = new StringBuilder(
                        moneyOnFirst.projectName == null ? "" : moneyOnFirst.projectName);
                StringBuilder spec = new StringBuilder(
                        moneyOnFirst.partNo == null ? "" : moneyOnFirst.partNo);
                int last = start;
                for (int j = start + 1; j < lines.length && j - start <= 8; j++) {
                    String cont = lines[j] == null ? "" : lines[j].trim();
                    if (cont.isEmpty()) {
                        continue;
                    }
                    if (cont.startsWith("*") || isCommoditySectionEnd(cont) || looksLikePageFooter(cont)) {
                        break;
                    }
                    if (cont.contains("%")) {
                        break;
                    }
                    // 先吃「费/资」残片；否则吃品名续行（VDC售后加装… / VDC Installation）
                    if (applyFeeSpecContinuation(name, spec, cont)) {
                        last = j;
                    } else if (looksLikeNameContinuation(cont)) {
                        appendNameFragment(name, cont);
                        last = j;
                    } else {
                        break;
                    }
                }
                String[] split = splitProjectNameAndSpec(name.toString(), spec.toString());
                Map<String, String> item = new HashMap<>();
                item.put("projectName", normalizeCommodityName(split[0]));
                item.put("partNo", normalizePartNoDisplay(
                        (split[1] != null && !split[1].isBlank()) ? split[1] : spec.toString()));
                putMoneyFields(item, moneyOnFirst);
                linesOut.add(item);
                return last;
            }
        }

        // —— 布局 A：金额在后续行 ——
        StringBuilder name = new StringBuilder(first);
        StringBuilder spec = new StringBuilder();
        boolean inSpec = false;
        ParsedCommodityRow money = null;
        int last = start;

        for (int j = start + 1; j < lines.length && j - start <= 14; j++) {
            String cont = lines[j] == null ? "" : lines[j].trim();
            if (cont.isEmpty()) {
                continue;
            }
            if (cont.startsWith("*") || isCommoditySectionEnd(cont) || looksLikePageFooter(cont)) {
                break;
            }

            String moneyPart = cont;
            String specPrefix = "";
            if (cont.contains("%")) {
                Matcher mixed = Pattern.compile("^(.*?)(\\d+(?:\\.\\d+)?\\%.*)$").matcher(cont);
                if (mixed.matches()) {
                    specPrefix = mixed.group(1).trim();
                    moneyPart = mixed.group(2).trim();
                }
            }

            ParsedCommodityRow tryMoney = parseMoneyFieldsOnly(moneyPart);
            if (tryMoney == null && cont.contains("%")) {
                tryMoney = parseCommodityRowFromRight(cont);
                if (tryMoney == null) {
                    tryMoney = parseGluedElectronicCommodityRow(cont);
                }
                if (tryMoney != null) {
                    String pn = tryMoney.projectName == null ? "" : tryMoney.projectName;
                    if (!pn.startsWith("*")) {
                        if (!pn.isBlank()) {
                            appendFragment(spec, pn);
                        }
                        tryMoney = new ParsedCommodityRow(
                                name.toString(), "",
                                tryMoney.quantity, tryMoney.amount, tryMoney.taxAmount, tryMoney.taxRate);
                    }
                }
            }

            if (tryMoney != null && tryMoney.amount != null && tryMoney.amount > 0) {
                if (!specPrefix.isBlank()) {
                    inSpec = true;
                    appendFragment(spec, specPrefix);
                }
                money = tryMoney;
                last = j;
                break;
            }

            if (!inSpec) {
                if (looksLikeSpecLineStart(cont)) {
                    inSpec = true;
                    appendFragment(spec, cont);
                } else if (applyFeeSpecContinuation(name, spec, cont)) {
                    if (spec.length() > 0) {
                        inSpec = true;
                    }
                } else {
                    appendNameContinuationFragment(name, cont);
                }
            } else {
                appendFragment(spec, cont);
            }
            last = j;
        }

        if (money == null || money.amount == null || money.amount <= 0) {
            return -1;
        }
        String[] split = splitProjectNameAndSpec(name.toString(), spec.toString());
        Map<String, String> item = new HashMap<>();
        item.put("projectName", normalizeCommodityName(split[0]));
        String part = normalizePartNoDisplay(
                (split[1] != null && !split[1].isBlank()) ? split[1] : spec.toString());
        if (part.isBlank() && money.partNo != null) {
            part = normalizePartNoDisplay(money.partNo);
        }
        item.put("partNo", part);
        putMoneyFields(item, money);
        linesOut.add(item);
        return last;
    }

    /** 是否为品名/规格残片行（费… / 纯「资」等），用于跨页挂回 */
    private boolean looksLikeFeeSpecFragment(String line) {
        if (line == null || line.isBlank() || line.startsWith("*") || line.contains("%")) {
            return false;
        }
        String t = line.trim().replaceAll("\\s+", " ");
        return t.startsWith("费")
                || t.equals("资")
                || t.matches("^[\\u4e00-\\u9fff]{1,8}资$");
    }

    /** 品名续行：中英文描述，非页眉/合计/纯数字 */
    private boolean looksLikeNameContinuation(String line) {
        if (line == null || line.isBlank() || line.startsWith("*") || line.contains("%")) {
            return false;
        }
        String t = line.trim();
        if (isCommoditySectionEnd(t) || looksLikePageFooter(t) || looksLikeFeeSpecFragment(t)) {
            return false;
        }
        // 购销方/表头碎片
        if (t.matches("^[购买卖销方信息信名称统]+$") || t.contains("项目名称") || t.contains("纳税人识别号")) {
            return false;
        }
        if (t.matches("^[\\d¥,.\\s]+$")) {
            return false;
        }
        // 至少含中文或拉丁字母
        return t.matches(".*[\\u4e00-\\u9fffA-Za-z].*");
    }

    private void appendNameFragment(StringBuilder name, String cont) {
        String t = cont == null ? "" : cont.trim();
        if (t.isEmpty()) {
            return;
        }
        if (name.length() > 0) {
            char last = name.charAt(name.length() - 1);
            char first = t.charAt(0);
            // 中文接中文（列错位断字：车载主 + 机）不加空格；中英/英英之间加空格
            if (!(isCjkChar(last) && isCjkChar(first)) && !Character.isWhitespace(last)) {
                name.append(' ');
            }
        }
        name.append(t);
    }

    private static boolean isCjkChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * 消费「费 资」「费 离线图资」「费资 ISA 7年图资」「费 年图资」等残片行。
     * 费归品名，其后归规格。成功消费返回 true。
     */
    private boolean applyFeeSpecContinuation(StringBuilder name, StringBuilder spec, String cont) {
        String t = cont == null ? "" : cont.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return false;
        }
        if (t.startsWith("费")) {
            if (!name.toString().endsWith("费")) {
                name.append('费');
            }
            String rest = t.substring(1).trim();
            // 「费资 ISA…」：资为列错位，丢掉领头资，保留 ISA/年图资
            if (rest.startsWith("资")) {
                String afterZi = rest.substring(1).trim();
                if (afterZi.isEmpty()) {
                    appendFragment(spec, "资");
                } else if (afterZi.matches("^[A-Za-z].*") || afterZi.contains("年图") || afterZi.endsWith("图资")) {
                    appendFragment(spec, afterZi);
                } else {
                    appendFragment(spec, rest);
                }
            } else if (!rest.isEmpty()) {
                appendFragment(spec, rest);
            }
            return true;
        }
        if (t.equals("资") || t.matches("^[\\u4e00-\\u9fff]{1,8}资$")) {
            appendFragment(spec, t);
            return true;
        }
        return false;
    }

    private void putMoneyFields(Map<String, String> item, ParsedCommodityRow row) {
        if (row.quantity != null && row.quantity > 0) {
            item.put("quantity", String.valueOf(row.quantity));
        }
        if (row.amount != null && row.amount > 0) {
            item.put("amount", String.valueOf(row.amount));
        }
        if (row.taxAmount != null && row.taxAmount > 0) {
            item.put("taxAmount", String.valueOf(row.taxAmount));
        }
        if (row.taxRate != null && !row.taxRate.isBlank()) {
            item.put("taxRate", row.taxRate);
        }
    }

    /** 仅解析金额字段，占位品名，供跨行块使用 */
    private ParsedCommodityRow parseMoneyFieldsOnly(String moneyPart) {
        if (moneyPart == null || moneyPart.isBlank() || !moneyPart.contains("%")) {
            return null;
        }
        // 借一个占位品名通过 buildRowFromHead
        ParsedCommodityRow row = parseGluedElectronicCommodityRow("*占位*占位 " + moneyPart.trim());
        if (row == null) {
            row = parseGluedElectronicCommodityRow("*占位*占位" + moneyPart.trim());
        }
        if (row == null) {
            return null;
        }
        return new ParsedCommodityRow(
                "",
                "",
                row.quantity,
                row.amount,
                row.taxAmount,
                row.taxRate
        );
    }

    private boolean looksLikeSpecLineStart(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String t = line.trim();
        if (t.startsWith("*")) {
            return false;
        }
        // 型号行：以字母开头（ZP22 / EH32 / AS33P）
        return t.matches("^[A-Za-z][A-Za-z0-9].*")
                || t.matches("(?i)^[A-Z]{1,8}\\d{1,6}.*");
    }

    /** 品名续行：费 / 费资(只取费) / 纯中文短续行 */
    private void appendNameContinuationFragment(StringBuilder name, String cont) {
        String t = cont.trim();
        if (t.isEmpty()) {
            return;
        }
        // 「费资」：费属品名，资属规格尾字误提前，丢弃资（后面图资会带上）
        if (t.equals("费资") || t.equals("费图资")) {
            name.append('费');
            return;
        }
        if (t.startsWith("费") && t.length() <= 3 && t.matches("费[\\u4e00-\\u9fff]*")) {
            name.append('费');
            return;
        }
        // 不含拉丁字母的中文续行
        if (!t.matches(".*[A-Za-z].*")) {
            name.append(t);
            return;
        }
        // 含字母则不应走品名
        appendFragment(name, t);
    }

    private void appendFragment(StringBuilder sb, String cont) {
        if (cont == null || cont.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            char first = cont.charAt(0);
            if (isAsciiWordChar(last) && isAsciiWordChar(first)) {
                sb.append(' ');
            }
        }
        sb.append(cont.trim());
    }

    private int indexOfCommodityHeader(String[] lines, int from) {
        for (int i = Math.max(0, from); i < lines.length; i++) {
            if (lines[i] != null && lines[i].contains("项目名称")) {
                return i;
            }
        }
        return -1;
    }

    private boolean looksLikePageFooter(String line) {
        String t = line.replace(" ", "");
        return t.matches("共\\d+页.*") || t.matches("第\\d+页.*");
    }

    private boolean isCommoditySectionEnd(String line) {
        return (line.startsWith("合") && line.contains("计"))
                || line.startsWith("价税合计")
                || (line.startsWith("备") && line.contains("注"))
                || line.startsWith("开票人")
                || line.startsWith("销方开户银行")
                || line.startsWith("收款信息")
                || line.startsWith("订单号码")
                || line.startsWith("收款人");
    }

    private static final class ParsedCommodityRow {
        final String projectName;
        final String partNo;
        final Double quantity;
        final Double amount;
        final Double taxAmount;
        final String taxRate;

        ParsedCommodityRow(String projectName, String partNo) {
            this(projectName, partNo, null, null, null, null);
        }

        ParsedCommodityRow(String projectName, String partNo,
                           Double quantity, Double amount, Double taxAmount, String taxRate) {
            this.projectName = projectName;
            this.partNo = partNo;
            this.quantity = quantity;
            this.amount = amount;
            this.taxAmount = taxAmount;
            this.taxRate = taxRate;
        }
    }

    /**
     * 电子发票文字层粘连格式：
     * A) 13%金额 税额单价数量
     * B) 13%单价数量 金额 税额
     */
    private ParsedCommodityRow parseGluedElectronicCommodityRow(String line) {
        if (line == null || line.isBlank() || !line.contains("%")) {
            return null;
        }
        String raw = line.trim().replace(",", "");
        Pattern fmtB = Pattern.compile(
                "^(.*?)\\s*(\\d+(?:\\.\\d+)?)%(\\d+\\.\\d+)\\s+(\\d+\\.\\d{2})\\s+(\\d+\\.\\d{2})\\s*$"
        );
        Matcher mb = fmtB.matcher(raw);
        if (mb.matches()) {
            String head = mb.group(1).trim();
            double rate = Double.parseDouble(mb.group(2));
            String priceQtyGlued = mb.group(3);
            double amount = Double.parseDouble(mb.group(4));
            double tax = Double.parseDouble(mb.group(5));
            double[] pq = splitPriceAndQty(priceQtyGlued, amount);
            if (pq != null) {
                return buildRowFromHead(head, pq[1], amount, tax, rate);
            }
        }
        Pattern fmtA = Pattern.compile(
                "^(.*?)\\s*(\\d+(?:\\.\\d+)?)%(\\d+\\.\\d{2})\\s+(\\d+\\.\\d.+)$"
        );
        Matcher ma = fmtA.matcher(raw);
        if (ma.matches()) {
            String head = ma.group(1).trim();
            double rate = Double.parseDouble(ma.group(2));
            double amount = Double.parseDouble(ma.group(3));
            String glued = ma.group(4);
            double expectedTax = Math.round(amount * rate) / 100.0;
            String taxStr = String.format(Locale.US, "%.2f", expectedTax);
            if (glued.startsWith(taxStr)) {
                double[] pq = splitPriceAndQty(glued.substring(taxStr.length()), amount);
                if (pq != null) {
                    return buildRowFromHead(head, pq[1], amount, expectedTax, rate);
                }
            }
            for (int taxLen = 4; taxLen <= Math.min(14, glued.length() - 4); taxLen++) {
                String taxCandidate = glued.substring(0, taxLen);
                if (!taxCandidate.matches("\\d+\\.\\d{2}")) {
                    continue;
                }
                double tax = Double.parseDouble(taxCandidate);
                if (Math.abs(tax - expectedTax) > 0.05) {
                    continue;
                }
                double[] pq = splitPriceAndQty(glued.substring(taxLen), amount);
                if (pq != null) {
                    return buildRowFromHead(head, pq[1], amount, tax, rate);
                }
            }
        }
        return null;
    }

    private double[] splitPriceAndQty(String glued, double amount) {
        if (glued == null || glued.isBlank() || amount <= 0) {
            return null;
        }
        for (int qtyLen = 1; qtyLen <= Math.min(8, glued.length() - 3); qtyLen++) {
            String qtyStr = glued.substring(glued.length() - qtyLen);
            if (!qtyStr.matches("[1-9]\\d*")) {
                continue;
            }
            String priceStr = glued.substring(0, glued.length() - qtyLen);
            if (!priceStr.matches("\\d+\\.\\d+")) {
                continue;
            }
            double price = Double.parseDouble(priceStr);
            double qty = Double.parseDouble(qtyStr);
            double product = price * qty;
            if (Math.abs(product - amount) <= 0.05 || Math.abs(product - amount) / amount < 1e-4) {
                return new double[]{price, qty};
            }
        }
        return null;
    }

    private ParsedCommodityRow buildRowFromHead(String head, double qty, double amount, double tax, double rate) {
        String name = head == null ? "" : head.trim();
        String[] split = splitProjectNameAndSpec(name, "");
        name = split[0];
        String partNo = split[1] == null ? "" : split[1];
        if (name.isBlank()) {
            return null;
        }
        String rateStr = (Math.abs(rate - Math.rint(rate)) < 1e-9)
                ? ((long) Math.rint(rate)) + "%"
                : rate + "%";
        return new ParsedCommodityRow(name, partNo, qty, amount, tax, rateStr);
    }

    /**
     * 从右侧解析明细行：... [规格型号] [单位] [数量] [单价] 金额 税率% 税额
     */
    private ParsedCommodityRow parseCommodityRowFromRight(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        Pattern end = Pattern.compile(
                "^(.*?)\\s+([\\d,]+(?:\\.\\d+)?)\\s+(\\d+(?:\\.\\d+)?)%\\s+([\\d,]+(?:\\.\\d+)?)\\s*$"
        );
        Matcher m = end.matcher(line.trim());
        if (!m.matches()) {
            return null;
        }
        String head = m.group(1).trim();
        double amount = parseDouble(m.group(2).replace(",", ""), 0.0);
        String taxRate = m.group(3) + "%";
        double taxAmount = parseDouble(m.group(4).replace(",", ""), 0.0);
        String partNo = "";
        Double quantity = null;

        Matcher price = Pattern.compile("^(.*)\\s+([\\d,]+(?:\\.\\d+)?)\\s*$").matcher(head);
        if (price.matches()) {
            head = price.group(1).trim();
        }

        Matcher qty = Pattern.compile("^(.*)\\s+([\\d,]+(?:\\.\\d+)?)\\s*$").matcher(head);
        if (qty.matches()) {
            quantity = parseDouble(qty.group(2).replace(",", ""), 0.0);
            head = qty.group(1).trim();
        }

        Matcher unit = Pattern.compile("^(.*)\\s+(\\S+)\\s*$").matcher(head);
        if (unit.matches() && looksLikeUnit(unit.group(2))) {
            head = unit.group(1).trim();
        }

        if (head.isBlank()) {
            return null;
        }
        String[] split = splitProjectNameAndSpec(head, partNo);
        head = split[0];
        partNo = split[1] == null ? "" : split[1];
        if (head.isBlank()) {
            return null;
        }
        return new ParsedCommodityRow(head, partNo, quantity, amount > 0 ? amount : null,
                taxAmount > 0 ? taxAmount : null, taxRate);
    }

    private String stripTrailingAmountRateTax(String line) {
        ParsedCommodityRow row = parseCommodityRowFromRight(line);
        if (row != null) {
            return row.projectName;
        }
        return line.replaceFirst(
                "\\s+[\\d,]+(?:\\.\\d+)?\\s+\\d+(?:\\.\\d+)?%\\s+[\\d,]+(?:\\.\\d+)?\\s*$",
                "").trim();
    }

    /** 剥掉品名后残留的数量/单价数字 */
    private String peelTrailingQtyPrice(String head) {
        if (head == null || head.isBlank()) {
            return "";
        }
        String s = head.trim();
        Matcher price = Pattern.compile("^(.*)\\s+([\\d,]+\\.\\d+)\\s*$").matcher(s);
        if (price.matches()) {
            s = price.group(1).trim();
        }
        Matcher qty = Pattern.compile("^(.*)\\s+([\\d,]+(?:\\.\\d+)?)\\s*$").matcher(s);
        if (qty.matches()) {
            s = qty.group(1).trim();
        }
        return s;
    }

    private boolean looksLikeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return false;
        }
        String u = unit.trim();
        // 纯数字不是单位
        if (u.matches("[\\d,]+(?:\\.\\d+)?")) {
            return false;
        }
        return u.matches("(?i)(台|个|套|件|箱|批|次|项|只|辆|米|EA|PCS|PC|SET|KG|G|M|UNIT)");
    }

    private boolean looksLikePartNo(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String t = token.trim();
        if (looksLikeUnit(t)) {
            return false;
        }
        // 较长数字编号，如 26644565
        if (t.matches("\\d{4,}")) {
            return true;
        }
        // 字母数字零件号
        return t.matches("(?i)[A-Z][A-Z0-9._-]{2,}");
    }

    private boolean looksLikeTableNumericTail(String line) {
        return line.matches(".*\\d+(?:\\.\\d+)?%.*")
                || line.matches("^[\\d,.\\s%]+$")
                || line.matches(".*\\s+[\\d,]+(?:\\.\\d+)?\\s+[\\d,]+(?:\\.\\d+)?\\s+\\d+%\\s+[\\d,]+(?:\\.\\d+)?\\s*$");
    }

    /** 拼接跨行品名：英文/数字相邻时补空格，避免 A/S + VDC → A/SVDC */
    private void appendNameContinuation(StringBuilder name, String cont) {
        if (cont == null || cont.isEmpty()) {
            return;
        }
        if (name.length() > 0) {
            char last = name.charAt(name.length() - 1);
            char first = cont.charAt(0);
            if (isAsciiWordChar(last) && isAsciiWordChar(first)) {
                name.append(' ');
            }
        }
        name.append(cont);
    }

    private boolean isAsciiWordChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private String getRowWord(JsonNode array, String rowKey) {
        if (array == null || !array.isArray()) return "";
        for (JsonNode item : array) {
            if (rowKey.equals(item.has("row") ? item.get("row").asText() : "")) {
                return item.has("word") ? item.get("word").asText("") : "";
            }
        }
        return "";
    }

    /**
     * 清洗发票品名：保留税目（*税收分类简称*），仅做空白/不可见字符规范化。
     * 电子发票 CommodityName 常见格式：*税收分类简称*货物或应税劳务、服务名称
     */
    static String normalizeCommodityName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String name = raw.trim()
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        // 连续空白压成单空格（保留 *税目* 前缀）
        name = name.replaceAll("\\s+", " ").trim();
        // PDF/OCR 列错位常在汉字间插入空格（车载主 机 → 车载主机）
        name = name.replaceAll("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])", "");
        return name;
    }

    private String getJsonArrayText(JsonNode array, int index) {
        if (array == null || !array.isArray() || index >= array.size()) return "";
        JsonNode item = array.get(index);
        if (item == null) return "";
        if (item.has("word")) return item.get("word").asText("");
        return item.asText("");
    }

    private Double parseDouble(String str, Double defaultVal) {
        try { return Double.parseDouble(str); } catch (Exception e) { return defaultVal; }
    }

    private Map<String, Object> parseMultipleInvoiceResponse(String jsonResponse) {
        Map<String, Object> result = createEmptyResult("票据识别完成");
        try {
            JsonNode json = objectMapper.readTree(jsonResponse);
            result.put("rawResponse", jsonResponse);

            // DEBUG: 打印原始返回结构
            System.out.println("=== OCR PDF Response ===");
            System.out.println("words_result: " + json.get("words_result"));
            System.out.println("=======================");

            if (json.has("error_code")) {
                String errorMsg = json.has("error_msg") ? json.get("error_msg").asText() : "百度 OCR 错误";
                result.put("message", errorMsg);
                result.put("success", false);
                return result;
            }

            // PDF 返回结构: words_result 是数组，每个元素有 result 对象
            JsonNode wordsResultArray = json.get("words_result");
            if (wordsResultArray == null || !wordsResultArray.isArray() || wordsResultArray.isEmpty()) {
                result.put("message", "未识别到票据信息");
                result.put("success", false);
                return result;
            }

            // 解析所有发票
            List<Map<String, Object>> invoiceResults = new ArrayList<>();
            int count = wordsResultArray.size();
            for (int i = 0; i < count; i++) {
                JsonNode item = wordsResultArray.get(i);
                JsonNode wordsResult = item.has("result") ? item.get("result") : item;
                if (wordsResult == null) continue;

                // DEBUG: 打印每个发票的 result 结构
                System.out.println("=== PDF Invoice[" + i + "] result: " + wordsResult + " ===");

                // 按 row 分组：找出所有 row 值（参考增值税发票逻辑）
                Set<String> rowKeys = new LinkedHashSet<>();
                JsonNode[] commodityFields = {
                    wordsResult.has("CommodityName") ? wordsResult.get("CommodityName") : null,
                    wordsResult.has("CommodityType") ? wordsResult.get("CommodityType") : null,
                    wordsResult.has("CommodityTaxRate") ? wordsResult.get("CommodityTaxRate") : null,
                    wordsResult.has("CommodityAmount") ? wordsResult.get("CommodityAmount") : null,
                    wordsResult.has("CommodityTax") ? wordsResult.get("CommodityTax") : null,
                };
                for (JsonNode field : commodityFields) {
                    if (field != null && field.isArray()) {
                        for (JsonNode fieldItem : field) {
                            if (fieldItem.has("row")) {
                                rowKeys.add(fieldItem.get("row").asText());
                            }
                        }
                    }
                }
                System.out.println("PDF Invoice[" + i + "] rowKeys: " + rowKeys + " (count=" + rowKeys.size() + ")");

                // 如果有多个 row，按 row 拆分行
                if (!rowKeys.isEmpty()) {
                    for (String rowKey : rowKeys) {
                        Map<String, Object> invoice = new HashMap<>();
                        invoice.put("invoiceNo", extractWord(wordsResult.get("InvoiceNum")));
                        String dateStr = extractWord(wordsResult.get("InvoiceDate"));
                        invoice.put("invoiceDate", dateStr.isEmpty() ? "" : dateStr.replace("年", "-").replace("月", "-").replace("日", ""));
                        invoice.put("buyerName", extractWord(wordsResult.get("PurchaserName")));
                        invoice.put("sellerName", extractWord(wordsResult.get("SellerName")));

                        invoice.put("projectName", normalizeCommodityName(getRowWord(wordsResult.has("CommodityName") ? wordsResult.get("CommodityName") : null, rowKey)));
                        invoice.put("partNo", getRowWord(wordsResult.has("CommodityType") ? wordsResult.get("CommodityType") : null, rowKey));
                        invoice.put("quantity", parseDouble(getRowWord(wordsResult.has("CommodityNum") ? wordsResult.get("CommodityNum") : null, rowKey).replace(",", ""), 0.0));
                        invoice.put("unit", getRowWord(wordsResult.has("CommodityUnit") ? wordsResult.get("CommodityUnit") : null, rowKey));
                        double amount = parseDouble(getRowWord(wordsResult.has("CommodityAmount") ? wordsResult.get("CommodityAmount") : null, rowKey).replace(",", ""), 0.0);
                        double taxAmount = parseDouble(getRowWord(wordsResult.has("CommodityTax") ? wordsResult.get("CommodityTax") : null, rowKey).replace(",", ""), 0.0);
                        invoice.put("amount", amount);
                        invoice.put("taxAmount", taxAmount);
                        invoice.put("totalAmount", Math.round((amount + taxAmount) * 100.0) / 100.0);
                        invoice.put("taxRate", parseTaxRate(getRowWord(wordsResult.has("CommodityTaxRate") ? wordsResult.get("CommodityTaxRate") : null, rowKey)));

                        invoiceResults.add(invoice);
                        System.out.println("PDF Row " + rowKey + " -> projectName=" + invoice.get("projectName") + ", amount=" + amount);
                    }
                } else {
                    // 单行发票（原逻辑）
                    Map<String, Object> invoice = new HashMap<>();
                    invoice.put("invoiceNo", extractWord(wordsResult.get("InvoiceNum")));
                    String dateStr = extractWord(wordsResult.get("InvoiceDate"));
                    invoice.put("invoiceDate", dateStr.isEmpty() ? "" : dateStr.replace("年", "-").replace("月", "-").replace("日", ""));

                    try {
                        String totalAmount = extractWordOrDefault(wordsResult.get("AmountInFiguers"), "0");
                        invoice.put("totalAmount", Double.parseDouble(totalAmount.replace(",", "")));
                    } catch (Exception e) { invoice.put("totalAmount", 0.0); }

                    try {
                        String totalTax = extractWordOrDefault(wordsResult.get("TotalTax"), "0");
                        invoice.put("taxAmount", Double.parseDouble(totalTax.replace(",", "")));
                    } catch (Exception e) { invoice.put("taxAmount", 0.0); }

                    invoice.put("taxRate", parseTaxRate(extractWord(wordsResult.get("CommodityTaxRate"))));

                    invoice.put("projectName", normalizeCommodityName(extractWord(wordsResult.get("CommodityName"))));
                    invoice.put("partNo", extractWord(wordsResult.get("CommodityType")));
                    invoice.put("buyerName", extractWord(wordsResult.get("PurchaserName")));
                    invoice.put("sellerName", extractWord(wordsResult.get("SellerName")));
                    invoiceResults.add(invoice);
                }
            }

            result.put("invoiceResults", invoiceResults);
            result.put("count", invoiceResults.size());
            // 单条结果保持兼容
            if (!invoiceResults.isEmpty()) {
                Map<String, Object> first = invoiceResults.get(0);
                result.put("invoiceNo", first.get("invoiceNo"));
                result.put("invoiceDate", first.get("invoiceDate"));
                result.put("totalAmount", first.get("totalAmount"));
                result.put("taxAmount", first.get("taxAmount"));
                result.put("taxRate", first.get("taxRate"));
                result.put("projectName", first.get("projectName"));
                result.put("partNo", first.get("partNo"));
            }
            result.put("success", true);
            result.put("message", "识别到 " + invoiceResults.size() + " 条发票");
        } catch (Exception e) {
            result.put("message", "解析失败: " + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    private String doGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private String doPost(String urlStr, String formData) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(formData.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != 200) {
            throw new IOException("HTTP " + responseCode + ": " + response);
        }
        return response.toString();
    }

    private static final DateTimeFormatter EN_INVOICE_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final Pattern EN_ITEM_LINE = Pattern.compile(
            "^(\\d+)\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2,4})\\s+([\\d,]+\\.\\d{2})\\s*$");

    /**
     * 英文商业发票（Invoice Number / Line # Description Qty Unit Price Amount）。
     * 电子 PDF 直接用文字层，不调用百度增值税识别。
     */
    private Map<String, Object> tryParseEnglishCommercialInvoice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String norm = text.replace("\r", "");
        if (norm.contains("项目名称") || !norm.contains("Invoice Number") || !norm.contains("Invoice Date")) {
            return null;
        }
        Matcher no = Pattern.compile("Invoice Number\\s+([A-Za-z0-9\\-]+)").matcher(norm);
        if (!no.find()) {
            return null;
        }
        List<Map<String, Object>> lines = parseEnglishLineItems(norm);
        if (lines.isEmpty()) {
            return null;
        }

        String invoiceNo = no.group(1);
        String invoiceDate = "";
        Matcher dt = Pattern.compile("Invoice Date\\s+([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4})").matcher(norm);
        if (dt.find()) {
            invoiceDate = formatEnglishInvoiceDate(dt.group(1));
        }
        String seller = "";
        Matcher sellerM = Pattern.compile("(?m)^(.+?)\\s+Contract Number\\b").matcher(norm);
        if (sellerM.find()) {
            seller = sellerM.group(1).trim();
        }
        String buyer = extractBillToName(norm);
        String currency = currencyOf(norm);
        double headerTaxRate = extractEnglishTaxRate(norm);
        double[] totals = extractEnglishTotals(norm);

        for (Map<String, Object> line : lines) {
            line.put("invoiceNo", invoiceNo);
            line.put("invoiceDate", invoiceDate);
            line.put("buyerName", buyer);
            line.put("sellerName", seller);
            line.put("partNo", "");
            line.put("unit", "");
            line.put("currency", currency);
            line.put("taxRate", headerTaxRate);
            double amount = ((Number) line.get("amount")).doubleValue();
            double tax = Math.round(amount * headerTaxRate) / 100.0;
            line.put("taxAmount", tax);
            line.put("totalAmount", Math.round((amount + tax) * 100.0) / 100.0);
        }
        if (lines.size() == 1 && totals != null) {
            Map<String, Object> only = lines.get(0);
            only.put("amount", totals[0]);
            only.put("taxAmount", totals[1]);
            only.put("totalAmount", totals[2]);
        }

        Map<String, Object> result = createEmptyResult("英文发票识别到 " + lines.size() + " 条");
        result.put("invoiceResults", lines);
        result.put("count", lines.size());
        result.put("success", true);
        result.put("projectNameSource", "pdf_text_en");
        Map<String, Object> first = lines.get(0);
        result.put("invoiceNo", first.get("invoiceNo"));
        result.put("invoiceDate", first.get("invoiceDate"));
        result.put("buyerName", first.get("buyerName"));
        result.put("sellerName", first.get("sellerName"));
        result.put("projectName", first.get("projectName"));
        result.put("partNo", first.get("partNo"));
        result.put("amount", first.get("amount"));
        result.put("taxRate", first.get("taxRate"));
        result.put("taxAmount", first.get("taxAmount"));
        result.put("totalAmount", first.get("totalAmount"));
        result.put("currency", currency);
        System.out.println("=== English invoice parsed: no=" + invoiceNo
                + " name=" + first.get("projectName")
                + " qty=" + first.get("quantity")
                + " total=" + first.get("totalAmount") + " ===");
        return result;
    }

    private List<Map<String, Object>> parseEnglishLineItems(String text) {
        List<Map<String, Object>> lines = new ArrayList<>();
        String[] rows = text.split("\n");
        int i = 0;
        while (i < rows.length && !(rows[i].contains("Description") && rows[i].contains("Unit Price"))) {
            i++;
        }
        if (i >= rows.length) {
            return lines;
        }
        i++;
        Map<String, Object> current = null;
        while (i < rows.length) {
            String line = rows[i].trim();
            i++;
            if (line.isEmpty()) {
                continue;
            }
            if (isEnglishItemSectionEnd(line)) {
                break;
            }
            Matcher m = EN_ITEM_LINE.matcher(line);
            if (m.matches()) {
                current = new HashMap<>();
                current.put("projectName", m.group(2).trim());
                current.put("quantity", parseAmount(m.group(3)));
                current.put("amount", parseAmount(m.group(5)));
                current.put("taxRate", 0.0);
                current.put("taxAmount", 0.0);
                current.put("totalAmount", parseAmount(m.group(5)));
                lines.add(current);
                continue;
            }
            if (current != null && !line.matches(".*\\d+\\.\\d{2}.*")) {
                current.put("projectName", (current.get("projectName") + " " + line).replaceAll("\\s+", " ").trim());
            }
        }
        return lines;
    }

    private boolean isEnglishItemSectionEnd(String line) {
        return line.startsWith("Bank Details")
                || line.startsWith("Tax Details")
                || line.startsWith("SubTotal")
                || line.startsWith("Registered at");
    }

    private String extractBillToName(String text) {
        int idx = text.indexOf("Bill To:");
        if (idx < 0) {
            return "";
        }
        String[] rows = text.substring(idx).split("\n");
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < rows.length && i < 8; i++) {
            String line = rows[i].trim();
            if (line.isEmpty() || line.contains("@") || line.startsWith("For Questions")) {
                continue;
            }
            if (line.matches("(?i)^(Room|No\\.?\\s*\\d|Customer Contact|China|Germany).*")
                    || line.matches(".*\\d{5,}.*")) {
                break;
            }
            Matcher dup = Pattern.compile("^(.+?)\\s+\\1\\s*$").matcher(line);
            if (dup.matches()) {
                line = dup.group(1).trim();
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(line);
            if (line.matches("(?i).*(LTD|LIMITED|INC|GMBH|CO\\.,?\\s*LTD)\\.?")) {
                break;
            }
        }
        return name.toString().replaceAll("\\s+", " ").trim();
    }

    private double extractEnglishTaxRate(String text) {
        Matcher m = Pattern.compile("(?i)Zero Rated\\s*\\((\\d+(?:\\.\\d+)?)\\s*%\\)").matcher(text);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        Matcher m2 = Pattern.compile("(?m)^\\s*(\\d+(?:\\.\\d+)?)\\s*%\\s*$").matcher(text);
        if (m2.find()) {
            return Double.parseDouble(m2.group(1));
        }
        return 0;
    }

    /** @return [goods, tax, total] or null */
    private double[] extractEnglishTotals(String text) {
        Matcher m = Pattern.compile("([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([A-Z]{3})").matcher(text);
        if (!m.find()) {
            return null;
        }
        return new double[] { parseAmount(m.group(1)), parseAmount(m.group(2)), parseAmount(m.group(3)) };
    }

    private String currencyOf(String text) {
        Matcher m = Pattern.compile("([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([A-Z]{3})").matcher(text);
        return m.find() ? m.group(4) : "";
    }

    private String formatEnglishInvoiceDate(String raw) {
        try {
            return LocalDate.parse(raw.trim(), EN_INVOICE_DATE).toString();
        } catch (DateTimeParseException e) {
            return raw;
        }
    }

    /** 票面税率。空值返回 null，免税/*** /0% 返回 0，不再默认成 13。 */
    private Double parseTaxRate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.contains("免税") || t.contains("不征税") || t.contains("免征") || t.contains("*")) {
            return 0.0;
        }
        String s = t.replace("%", "").replace(",", "").replace(" ", "");
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private double parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        return Double.parseDouble(raw.replace(",", "").trim());
    }

    private Map<String, Object> createEmptyResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("invoiceNo", "");
        result.put("invoiceDate", "");
        result.put("buyerName", "");
        result.put("sellerName", "");
        result.put("projectName", "");
        result.put("partNo", "");
        result.put("serviceType", "");
        result.put("amount", 0.0);
        result.put("taxRate", 0.0);
        result.put("taxAmount", 0.0);
        result.put("totalAmount", 0.0);
        result.put("message", message);
        return result;
    }

    // 辅助方法：从 [{"word": "value"}] 结构中提取值
    private String extractWord(JsonNode node) {
        if (node != null && node.isArray() && node.size() > 0) {
            JsonNode first = node.get(0);
            if (first != null && first.has("word")) {
                return first.get("word").asText("");
            }
        }
        return "";
    }

    private String extractWordOrDefault(JsonNode node, String defaultValue) {
        if (node != null && node.isArray() && node.size() > 0) {
            JsonNode first = node.get(0);
            if (first != null && first.has("word")) {
                return first.get("word").asText(defaultValue);
            }
        }
        return defaultValue;
    }
}