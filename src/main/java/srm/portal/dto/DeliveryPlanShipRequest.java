package srm.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DeliveryPlanShipRequest {
    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    /**
     * 由 URL PathVariable 注入（/delivery-plan/{planId}/ship），不要从请求体校验/传入。
     */
    private Long planId;

    @NotNull(message = "actualQuantity 不能为空")
    @Positive(message = "actualQuantity 必须大于0")
    private Integer actualQuantity;

    @NotNull(message = "actualEtaDate 不能为空")
    private String actualEtaDate; // YYYY-MM-DD

    @NotNull(message = "trackingNo 不能为空")
    private String trackingNo;

    @NotNull(message = "logisticsCompany 不能为空")
    private String logisticsCompany;

    @NotNull(message = "shipDate 不能为空")
    private String shipDate; // YYYY-MM-DD

    // Getters and Setters
    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getActualEtaDate() {
        return actualEtaDate;
    }

    public void setActualEtaDate(String actualEtaDate) {
        this.actualEtaDate = actualEtaDate;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getShipDate() {
        return shipDate;
    }

    public void setShipDate(String shipDate) {
        this.shipDate = shipDate;
    }
}
