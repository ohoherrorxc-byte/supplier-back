package srm.portal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 发货确认弹窗字段（与 deliver.md 列/字段保持一致口径）
 */
public class ShipmentShipRequest {
    @NotNull
    private Long operatorUserId;

    /** 本次发运数量 */
    @NotNull
    private Integer shippedQty;

    /** 送货工厂 */
    private String shipFromPlant;

    /** 需求工厂 */
    private String shipToPlant;

    /** SAP 发运号（可由 SAP 同步后回填，这里先允许手工录入/为空） */
    private String sapShipmentNo;

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号 */
    private String trackingNo;

    /** 预计到货日期（ETA） */
    private LocalDate etaDate;

    /** 实际发货时间 */
    private LocalDate shipDate;

    /** 实际到货时间 */
    private LocalDate actualEtaDate;

    /** 进仓单号 */
    private String warehouseInNo;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public Integer getShippedQty() {
        return shippedQty;
    }

    public void setShippedQty(Integer shippedQty) {
        this.shippedQty = shippedQty;
    }

    public String getShipFromPlant() {
        return shipFromPlant;
    }

    public void setShipFromPlant(String shipFromPlant) {
        this.shipFromPlant = shipFromPlant;
    }

    public String getShipToPlant() {
        return shipToPlant;
    }

    public void setShipToPlant(String shipToPlant) {
        this.shipToPlant = shipToPlant;
    }

    public String getSapShipmentNo() {
        return sapShipmentNo;
    }

    public void setSapShipmentNo(String sapShipmentNo) {
        this.sapShipmentNo = sapShipmentNo;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public LocalDate getEtaDate() {
        return etaDate;
    }

    public void setEtaDate(LocalDate etaDate) {
        this.etaDate = etaDate;
    }

    public LocalDate getShipDate() {
        return shipDate;
    }

    public void setShipDate(LocalDate shipDate) {
        this.shipDate = shipDate;
    }

    public LocalDate getActualEtaDate() {
        return actualEtaDate;
    }

    public void setActualEtaDate(LocalDate actualEtaDate) {
        this.actualEtaDate = actualEtaDate;
    }

    public String getWarehouseInNo() {
        return warehouseInNo;
    }

    public void setWarehouseInNo(String warehouseInNo) {
        this.warehouseInNo = warehouseInNo;
    }
}

