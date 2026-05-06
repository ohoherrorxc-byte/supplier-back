package srm.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DeliveryPlanCreateRequest {
    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    @NotNull(message = "orderDetailId 不能为空")
    private String orderDetailId;

    @NotNull(message = "planQuantity 不能为空")
    @Positive(message = "planQuantity 必须大于0")
    private Integer planQuantity;

    @NotNull(message = "planDate 不能为空")
    private String planDate; // YYYY-MM-DD

    private String remark;

    // Getters and Setters
    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(String orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public Integer getPlanQuantity() {
        return planQuantity;
    }

    public void setPlanQuantity(Integer planQuantity) {
        this.planQuantity = planQuantity;
    }

    public String getPlanDate() {
        return planDate;
    }

    public void setPlanDate(String planDate) {
        this.planDate = planDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
