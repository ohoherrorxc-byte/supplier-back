package srm.portal.dto;

import java.time.LocalDate;

public class DeliveryPlanInsertDto {
    private Long id;
    private String planNo;
    private Long orderId;
    private Long orderDetailId;
    private Long supplierId;
    private Integer planQuantity;
    private LocalDate planDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanNo() { return planNo; }
    public void setPlanNo(String planNo) { this.planNo = planNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOrderDetailId() { return orderDetailId; }
    public void setOrderDetailId(Long orderDetailId) { this.orderDetailId = orderDetailId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Integer getPlanQuantity() { return planQuantity; }
    public void setPlanQuantity(Integer planQuantity) { this.planQuantity = planQuantity; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
}