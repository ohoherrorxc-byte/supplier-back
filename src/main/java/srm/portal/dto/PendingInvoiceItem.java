package srm.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待开票项
 */
public class PendingInvoiceItem {

    private Long acceptDetailId;
    private String acceptApplyNo;
    private String partsNo;
    private String partsName;
    private String remark;
    private String contractNo;
    private String contractName;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String supplierNo;
    private String supplierName;
    private LocalDateTime acceptDate;
    private String unitName;
    private Integer acceptCategory;
    private Integer acceptStatus;

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
}