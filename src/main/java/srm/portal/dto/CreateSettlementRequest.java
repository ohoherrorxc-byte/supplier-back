package srm.portal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建结算单请求
 */
public class CreateSettlementRequest {
    private Long userId;
    private String supplierName;
    private BigDecimal invoiceAmount;
    private BigDecimal invoiceTaxRate;
    private BigDecimal invoiceTaxAmount;
    private BigDecimal invoiceTotalAmount;
    private String remark;
    private String createDept;
    private List<Long> acceptDetailIds;
    private List<SettlementInvoiceLineRequest> invoiceLines;
    private List<CompletionFileRequest> completionFiles;
    private List<CompletionFileRequest> orderFiles;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(BigDecimal invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public BigDecimal getInvoiceTaxRate() {
        return invoiceTaxRate;
    }

    public void setInvoiceTaxRate(BigDecimal invoiceTaxRate) {
        this.invoiceTaxRate = invoiceTaxRate;
    }

    public BigDecimal getInvoiceTaxAmount() {
        return invoiceTaxAmount;
    }

    public void setInvoiceTaxAmount(BigDecimal invoiceTaxAmount) {
        this.invoiceTaxAmount = invoiceTaxAmount;
    }

    public BigDecimal getInvoiceTotalAmount() {
        return invoiceTotalAmount;
    }

    public void setInvoiceTotalAmount(BigDecimal invoiceTotalAmount) {
        this.invoiceTotalAmount = invoiceTotalAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateDept() {
        return createDept;
    }

    public void setCreateDept(String createDept) {
        this.createDept = createDept;
    }

    public List<Long> getAcceptDetailIds() {
        return acceptDetailIds;
    }

    public void setAcceptDetailIds(List<Long> acceptDetailIds) {
        this.acceptDetailIds = acceptDetailIds;
    }

    public List<SettlementInvoiceLineRequest> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(List<SettlementInvoiceLineRequest> invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    public List<CompletionFileRequest> getCompletionFiles() {
        return completionFiles;
    }

    public void setCompletionFiles(List<CompletionFileRequest> completionFiles) {
        this.completionFiles = completionFiles;
    }

    public List<CompletionFileRequest> getOrderFiles() {
        return orderFiles;
    }

    public void setOrderFiles(List<CompletionFileRequest> orderFiles) {
        this.orderFiles = orderFiles;
    }
}