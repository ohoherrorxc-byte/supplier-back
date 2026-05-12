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

            if (wordsResult.has("InvoiceNum")) {
                result.put("invoiceNo", wordsResult.get("InvoiceNum").asText(""));
            }
            if (wordsResult.has("InvoiceDate")) {
                String dateStr = wordsResult.get("InvoiceDate").asText("");
                dateStr = dateStr.replace("年", "-").replace("月", "-").replace("日", "");
                result.put("invoiceDate", dateStr);
            }
            if (wordsResult.has("TotalAmount")) {
                try {
                    result.put("totalAmount", Double.parseDouble(wordsResult.get("TotalAmount").asText("0").replace(",", "")));
                } catch (Exception e) {
                    result.put("totalAmount", 0.0);
                }
            }
            if (wordsResult.has("TotalTax")) {
                try {
                    result.put("taxAmount", Double.parseDouble(wordsResult.get("TotalTax").asText("0").replace(",", "")));
                } catch (Exception e) {
                    result.put("taxAmount", 0.0);
                }
            }
            if (wordsResult.has("CommodityTaxRate")) {
                try {
                    String rate = wordsResult.get("CommodityTaxRate").get(0).get("word").asText("0").replace("%", "").replace(" ", "");
                    result.put("taxRate", Double.parseDouble(rate));
                } catch (Exception e) {
                    result.put("taxRate", 0.0);
                }
            }
            if (wordsResult.has("AmountInFiguers")) {
                try {
                    result.put("amount", Double.parseDouble(wordsResult.get("AmountInFiguers").asText("0").replace(",", "")));
                } catch (Exception e) {
                    result.put("amount", 0.0);
                }
            }
            if (wordsResult.has("PurchaserName")) {
                result.put("buyerName", wordsResult.get("PurchaserName").asText(""));
            }
            if (wordsResult.has("SellerName")) {
                result.put("sellerName", wordsResult.get("SellerName").asText(""));
            }
            if (wordsResult.has("ServiceType")) {
                result.put("serviceType", wordsResult.get("ServiceType").asText(""));
            }
            if (wordsResult.has("CommodityName")) {
                JsonNode commodityName = wordsResult.get("CommodityName");
                if (commodityName != null && commodityName.isArray() && commodityName.size() > 0) {
                    result.put("projectName", commodityName.get(0).get("word").asText(""));
                }
            }

            result.put("success", true);
        } catch (Exception e) {
            result.put("message", "解析失败: " + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    private Map<String, Object> parseMultipleInvoiceResponse(String jsonResponse) {
        Map<String, Object> result = createEmptyResult("票据识别完成");
        try {
            JsonNode json = objectMapper.readTree(jsonResponse);
            result.put("rawResponse", jsonResponse);

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

            // 取第一个
            JsonNode firstResult = wordsResultArray.get(0);
            JsonNode wordsResult = firstResult.get("result");
            if (wordsResult == null) {
                result.put("message", "票据数据解析失败");
                result.put("success", false);
                return result;
            }

            // 提取字段值（PDF返回结构是 [{"word": "value"}]）
            String invoiceNum = extractWord(wordsResult.get("InvoiceNum"));
            if (!invoiceNum.isEmpty()) {
                result.put("invoiceNo", invoiceNum);
            }

            String invoiceDate = extractWord(wordsResult.get("InvoiceDate"));
            if (!invoiceDate.isEmpty()) {
                invoiceDate = invoiceDate.replace("年", "-").replace("月", "-").replace("日", "");
                result.put("invoiceDate", invoiceDate);
            }

            try {
                // 总金额从 AmountInFiguers 提取（价税合计）
                String totalAmount = extractWordOrDefault(wordsResult.get("AmountInFiguers"), "0");
                result.put("totalAmount", Double.parseDouble(totalAmount.replace(",", "")));
            } catch (Exception e) {}

            try {
                String totalTax = extractWordOrDefault(wordsResult.get("TotalTax"), "0");
                result.put("taxAmount", Double.parseDouble(totalTax.replace(",", "")));
            } catch (Exception e) {}

            try {
                String taxRate = extractWordOrDefault(wordsResult.get("CommodityTaxRate"), "0");
                taxRate = taxRate.replace("%", "").replace(" ", "");
                result.put("taxRate", Double.parseDouble(taxRate));
            } catch (Exception e) {}

            try {
                String amount = extractWordOrDefault(wordsResult.get("AmountInFiguers"), "0");
                result.put("amount", Double.parseDouble(amount.replace(",", "")));
            } catch (Exception e) {}

            String purchaserName = extractWord(wordsResult.get("PurchaserName"));
            if (!purchaserName.isEmpty()) {
                result.put("buyerName", purchaserName);
            }

            String sellerName = extractWord(wordsResult.get("SellerName"));
            if (!sellerName.isEmpty()) {
                result.put("sellerName", sellerName);
            }

            // 项目名称（从商品名称提取）
            String projectName = extractWord(wordsResult.get("CommodityName"));
            if (!projectName.isEmpty()) {
                result.put("projectName", projectName);
            }

            // 零件号（从商品型号提取）
            String partNo = extractWord(wordsResult.get("CommodityType"));
            if (!partNo.isEmpty()) {
                result.put("partNo", partNo);
            }

            result.put("success", true);
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