package srm.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待开票项
 */
public class PendingInvoiceItem {

    private Long acceptDetailId;
    private String acceptApplyNo;
    private String docTitle;
    private String partsNo;
    private String partsName;
    private String remark;
    private String contractNo;
    private String contractName;
    private BigDecimal quantity;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private String currency;
    private String supplierNo;
    private String supplierName;
    private LocalDateTime acceptDate;
    private String unitName;
    private Integer acceptCategory;
    private Integer acceptStatus;
    private String projectName;
    private String purchaseOrderType;
    /** 验收单人民币总金额（表头，非明细加总） */
    private BigDecimal acceptAmountRmb;
    /** 验收单原币总金额 */
    private BigDecimal acceptAmountOriginal;

    public PendingInvoiceItem() {
    }

    public Long getAcceptDetailId() {
        return acceptDetailId;
    }

    public void setAcceptDetailId(Long acceptDetailId) {
        this.acceptDetailId = acceptDetailId;
    }

    public String getAcceptApplyNo() {
        return acceptApplyNo;
    }

    public void setAcceptApplyNo(String acceptApplyNo) {
        this.acceptApplyNo = acceptApplyNo;
    }

    public String getDocTitle() {
        return docTitle != null ? docTitle.replace("&amp;", "&") : null;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getPartsNo() {
        return partsNo;
    }

    public void setPartsNo(String partsNo) {
        this.partsNo = partsNo;
    }

    public String getPartsName() {
        return partsName;
    }

    public void setPartsName(String partsName) {
        this.partsName = partsName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public LocalDateTime getAcceptDate() {
        return acceptDate;
    }

    public void setAcceptDate(LocalDateTime acceptDate) {
        this.acceptDate = acceptDate;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Integer getAcceptCategory() {
        return acceptCategory;
    }

    public void setAcceptCategory(Integer acceptCategory) {
        this.acceptCategory = acceptCategory;
    }

    public Integer getAcceptStatus() {
        return acceptStatus;
    }

    public void setAcceptStatus(Integer acceptStatus) {
        this.acceptStatus = acceptStatus;
    }

    public String getProjectName() {
        return projectName != null ? projectName.replace("&amp;", "&") : null;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPurchaseOrderType() {
        return purchaseOrderType;
    }

    public void setPurchaseOrderType(String purchaseOrderType) {
        this.purchaseOrderType = purchaseOrderType;
    }

    public BigDecimal getAcceptAmountRmb() {
        return acceptAmountRmb;
    }

    public void setAcceptAmountRmb(BigDecimal acceptAmountRmb) {
        this.acceptAmountRmb = acceptAmountRmb;
    }

    public BigDecimal getAcceptAmountOriginal() {
        return acceptAmountOriginal;
    }

    public void setAcceptAmountOriginal(BigDecimal acceptAmountOriginal) {
        this.acceptAmountOriginal = acceptAmountOriginal;
    }
}