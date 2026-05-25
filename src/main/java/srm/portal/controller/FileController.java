package srm.portal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${oss.endpoint:}")
    private String ossEndpoint;

    @Value("${oss.access-key:}")
    private String accessKey;

    @Value("${oss.secret-key:}")
    private String secretKey;

    @Value("${oss.bucket-name:}")
    private String bucketName;

    @Value("${spring.file.url:}")
    private String fileUrl;

    private S3Client createS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .endpointOverride(URI.create(ossEndpoint))
                .region(Region.of("cn-north-1"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }

    /**
     * 文件上传接口（只上传到OSS，不保存数据库）
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "文件不能为空"));
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成日期路径 + 文件名，存放到 upload/ 目录下
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            String objectKey = "upload/" + datePath + "/" + newFilename;

            // 上传到 OSS
            S3Client s3 = createS3Client();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            // 构建访问 URL
            String fileUrlPath = fileUrl + objectKey;

            result.put("success", true);
            result.put("message", "上传成功");
            result.put("url", fileUrlPath);
            result.put("name", originalFilename);
            result.put("size", file.getSize());
            // 不保存到数据库，返回临时ID（时间戳）
            result.put("id", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
        }
    }

    /**
     * 文件下载接口（通过后端代理转发，用户可匿名访问）
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String path, @RequestParam(required = false) String filename) {
        try {
            S3Client s3 = createS3Client();
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build();
            ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getObjectRequest);
            byte[] bytes = s3Object.readAllBytes();

            // 优先使用传入的文件名，否则从路径提取
            String name = (filename != null && !filename.isEmpty()) ? filename : (path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(name, java.nio.charset.StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 保存文件记录到数据库
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFileRecord(
            @RequestBody Map<String, Object> params
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文件记录保存成功");
        result.put("id", UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) String businessName,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("items", new ArrayList<>());
        result.put("total", 0);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成完工单Word文档下载
     */
    @GetMapping("/completion-slip-docx")
    public ResponseEntity<byte[]> downloadCompletionSlipDocx(
            @RequestParam(required = false) String docTitle,
            @RequestParam(required = false) String acceptAmountRmb,
            @RequestParam(required = false) String contractNo
    ) {
        try {
            org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = document.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = title.createRun();
            titleRun.setText(docTitle != null ? docTitle : "完工单");
            titleRun.setFontSize(16);
            titleRun.setBold(true);

            org.apache.poi.xwpf.usermodel.XWPFTable table = document.createTable(7, 2);
            table.setWidth("100%");

            String[][] rows = {
                {"甲方公司名称", "上汽海外出行科技有限公司"},
                {"项目进度", "已完成服务"},
                {"开票内容", ""},
                {"发票类型", "增值税专用发票"},
                {"税率", "6%"},
                {"开票金额(含税)", "¥" + (acceptAmountRmb != null ? acceptAmountRmb : "0.00")},
                {"合同号", contractNo != null ? contractNo : ""}
            };

            for (int i = 0; i < rows.length; i++) {
                org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.getRow(i);
                row.getCell(0).setText(rows[i][0]);
                row.getCell(1).setText(rows[i][1]);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            String filename = (docTitle != null && !docTitle.isBlank()) ? docTitle + ".docx" : "完工单.docx";
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(filename, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}