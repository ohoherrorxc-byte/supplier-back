package srm.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 从订单创建 ASN（send-deliver.md 模块）。
 */
public class CreateAsnFromOrdersRequest {

    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    @Valid
    private ContactInfo contactInfo;

    @NotEmpty(message = "lines 不能为空")
    @Valid
    private List<AsnOrderLine> lines;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<AsnOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<AsnOrderLine> lines) {
        this.lines = lines;
    }

    public static class ContactInfo {
        private String contactName;
        private String contactPhone;
        private String contactEmail;

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }
    }

    public static class AsnOrderLine {
        @NotNull(message = "orderDetailId 不能为空")
        private Long orderDetailId;

        @NotNull(message = "shippedQty 不能为空")
        @Positive(message = "shippedQty 必须大于0")
        private Integer shippedQty;

        private String confirmShipmentDate;

        private String warehouseInDate;

        private String warehouseInNo;

        private String shipDate;

        private String actualEtaDate;

        public Long getOrderDetailId() {
            return orderDetailId;
        }

        public void setOrderDetailId(Long orderDetailId) {
            this.orderDetailId = orderDetailId;
        }

        public Integer getShippedQty() {
            return shippedQty;
        }

        public void setShippedQty(Integer shippedQty) {
            this.shippedQty = shippedQty;
        }

        public String getConfirmShipmentDate() {
            return confirmShipmentDate;
        }

        public void setConfirmShipmentDate(String confirmShipmentDate) {
            this.confirmShipmentDate = confirmShipmentDate;
        }

        public String getWarehouseInDate() {
            return warehouseInDate;
        }

        public void setWarehouseInDate(String warehouseInDate) {
            this.warehouseInDate = warehouseInDate;
        }

        public String getWarehouseInNo() {
            return warehouseInNo;
        }

        public void setWarehouseInNo(String warehouseInNo) {
            this.warehouseInNo = warehouseInNo;
        }

        public String getShipDate() {
            return shipDate;
        }

        public void setShipDate(String shipDate) {
            this.shipDate = shipDate;
        }

        public String getActualEtaDate() {
            return actualEtaDate;
        }

        public void setActualEtaDate(String actualEtaDate) {
            this.actualEtaDate = actualEtaDate;
        }
    }
}
