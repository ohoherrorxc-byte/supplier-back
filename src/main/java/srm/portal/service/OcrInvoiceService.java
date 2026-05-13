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

/**
 * OCR 发票识别服务 - 使用百度智能云 OCR
 * 图片用增值税发票识别，PDF用通用识别
 */
@Service
public class OcrInvoiceService {

    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String VAT_INVOICE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice";
    private static final String MULTIPLE_INVOICE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/multiple_invoice";
    // private static final String MULTIPLE_INVOICE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic"; // 通用识别接口，支持PDF和图片
    // private static final String MULTIPLE_INVOICE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/receipt";

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
                // PDF 用通用识别
                if (generalApiKey == null || generalApiKey.isEmpty() || generalSecretKey == null || generalSecretKey.isEmpty()) {
                    Map<String, Object> result = createEmptyResult("通用 OCR 未配置 API Key");
                    result.put("success", false);
                    return result;
                }
                return callGeneralOcr(fileBytes, fileName);
            } else {
                // 图片用增值税发票识别
                if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                    Map<String, Object> result = createEmptyResult("增值税发票 OCR 未配置 API Key");
                    result.put("success", false);
                    return result;
                }
                return callVatInvoiceOcr(fileBytes, fileName);
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
        String token = getAccessToken(false);
        String requestUrl = VAT_INVOICE_URL + "?access_token=" + token;

        String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);
        String formData = "image=" + java.net.URLEncoder.encode(fileBase64, StandardCharsets.UTF_8);

        String response = doPost(requestUrl, formData);
        return parseVatInvoiceResponse(response);
    }

    private Map<String, Object> callGeneralOcr(byte[] fileBytes, String fileName) throws Exception {
        String token = getAccessToken(true);
        String requestUrl = MULTIPLE_INVOICE_URL + "?access_token=" + token;

        String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);

        // PDF 用 pdf_file 参数，图片用 image 参数
        String formData;
        if (fileName.toLowerCase().endsWith(".pdf")) {
            formData = "pdf_file=" + java.net.URLEncoder.encode(fileBase64, StandardCharsets.UTF_8);
        } else {
            formData = "image=" + java.net.URLEncoder.encode(fileBase64, StandardCharsets.UTF_8);
        }

        String response = doPost(requestUrl, formData);
        return parseMultipleInvoiceResponse(response);
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
            if (wordsResult == null || !wordsResult.isArray()) {
                result.put("message", "未识别到发票信息");
                result.put("success", false);
                return result;
            }

            // 取第一个发票（一般只有一张发票）
            JsonNode invoiceNode = wordsResult.get(0);
            JsonNode vatResult = invoiceNode.get("result");

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

                invoice.put("projectName", getRowWord(vatResult.has("CommodityName") ? vatResult.get("CommodityName") : null, rowKey));
                invoice.put("partNo", getRowWord(vatResult.has("CommodityType") ? vatResult.get("CommodityType") : null, rowKey));
                invoice.put("taxRate", parseDouble(getRowWord(vatResult.has("CommodityTaxRate") ? vatResult.get("CommodityTaxRate") : null, rowKey).replace("%", "").replace(" ", ""), 0.0));
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

    private String getRowWord(JsonNode array, String rowKey) {
        if (array == null || !array.isArray()) return "";
        for (JsonNode item : array) {
            if (rowKey.equals(item.has("row") ? item.get("row").asText() : "")) {
                return item.has("word") ? item.get("word").asText("") : "";
            }
        }
        return "";
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

                        invoice.put("projectName", getRowWord(wordsResult.has("CommodityName") ? wordsResult.get("CommodityName") : null, rowKey));
                        invoice.put("partNo", getRowWord(wordsResult.has("CommodityType") ? wordsResult.get("CommodityType") : null, rowKey));
                        double amount = parseDouble(getRowWord(wordsResult.has("CommodityAmount") ? wordsResult.get("CommodityAmount") : null, rowKey).replace(",", ""), 0.0);
                        double taxAmount = parseDouble(getRowWord(wordsResult.has("CommodityTax") ? wordsResult.get("CommodityTax") : null, rowKey).replace(",", ""), 0.0);
                        invoice.put("amount", amount);
                        invoice.put("taxAmount", taxAmount);
                        invoice.put("totalAmount", Math.round((amount + taxAmount) * 100.0) / 100.0);
                        try {
                            String taxRate = getRowWord(wordsResult.has("CommodityTaxRate") ? wordsResult.get("CommodityTaxRate") : null, rowKey).replace("%", "").replace(" ", "");
                            invoice.put("taxRate", parseDouble(taxRate, 0.0));
                        } catch (Exception e) { invoice.put("taxRate", 0.0); }

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

                    try {
                        String taxRate = extractWordOrDefault(wordsResult.get("CommodityTaxRate"), "0");
                        invoice.put("taxRate", Double.parseDouble(taxRate.replace("%", "").replace(" ", "")));
                    } catch (Exception e) { invoice.put("taxRate", 0.0); }

                    invoice.put("projectName", extractWord(wordsResult.get("CommodityName")));
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