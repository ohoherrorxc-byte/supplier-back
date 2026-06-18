package srm.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单项（列表展示用）
 */
public class SettlementOrderItem {
    private Long id;
    private String settlementNo;
    private String supplierNo;
    private String supplierName;
    private String invoiceNo;
    private String invoiceDate;
    private BigDecimal invoiceTotalAmount;
    private Integer status;
    private LocalDateTime createTime;
    private String acceptApplyNo;
    private String bomPurchaseOrderNo;
    private String contractNo;
    private String currency;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public String getSupplierNo() {
        return supplierNo;
    }

    public void setSupplierNo(String supplierNo) {
        this.supplierNo = supplierNo;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getInvoiceTotalAmount() {
        return invoiceTotalAmount;
    }

    public void setInvoiceTotalAmount(BigDecimal invoiceTotalAmount) {
        this.invoiceTotalAmount = invoiceTotalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getAcceptApplyNo() {
        return acceptApplyNo;
    }

    public void setAcceptApplyNo(String acceptApplyNo) {
        this.acceptApplyNo = acceptApplyNo;
    }

    public String getBomPurchaseOrderNo() {
        return bomPurchaseOrderNo;
    }

    public void setBomPurchaseOrderNo(String bomPurchaseOrderNo) {
        this.bomPurchaseOrderNo = bomPurchaseOrderNo;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}