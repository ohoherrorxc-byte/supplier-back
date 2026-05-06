package srm.portal.dto;

import java.time.LocalDateTime;

public class ShipmentDto {
    private Long asnId;
    private String asnNo;
    private String asnStatus;
    private String logisticsCompany;
    private String trackingNo;
    private LocalDateTime etaDate;
    private LocalDateTime actualShipDate;
    private LocalDateTime actualArrivalDate;
    private String shipmentBatchNo;
    private Integer shippedQty;
    private LocalDateTime createTime;
    private Long orderId;
    private String systemOrderNo;
    private String sapOrderNo;
    private Long supplierId;
    private String supplierDuns;
    private String supplierName;
    private String shipToPlant;
    private String shipToAddress;
    private String partsNo;
    private String partsName;
    private LocalDateTime shipDate;
    private LocalDateTime actualEtaDate;
    private String warehouseInNo;
    private String storageName;

    // Getters and setters
    public Long getAsnId() {
        return asnId;
    }

    public void setAsnId(Long asnId) {
        this.asnId = asnId;
    }

    public String getAsnNo() {
        return asnNo;
    }

    public void setAsnNo(String asnNo) {
        this.asnNo = asnNo;
    }

    public String getAsnStatus() {
        return asnStatus;
    }

    public void setAsnStatus(String asnStatus) {
        this.asnStatus = asnStatus;
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

    public LocalDateTime getEtaDate() {
        return etaDate;
    }

    public void setEtaDate(LocalDateTime etaDate) {
        this.etaDate = etaDate;
    }

    public Integer getShippedQty() {
        return shippedQty;
    }

    public void setShippedQty(Integer shippedQty) {
        this.shippedQty = shippedQty;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getSystemOrderNo() {
        return systemOrderNo;
    }

    public void setSystemOrderNo(String systemOrderNo) {
        this.systemOrderNo = systemOrderNo;
    }

    public String getSapOrderNo() {
        return sapOrderNo;
    }

    public void setSapOrderNo(String sapOrderNo) {
        this.sapOrderNo = sapOrderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierDuns() {
        return supplierDuns;
    }

    public void setSupplierDuns(String supplierDuns) {
        this.supplierDuns = supplierDuns;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getShipToPlant() {
        return shipToPlant;
    }

    public void setShipToPlant(String shipToPlant) {
        this.shipToPlant = shipToPlant;
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

    public LocalDateTime getActualShipDate() {
        return actualShipDate;
    }

    public void setActualShipDate(LocalDateTime actualShipDate) {
        this.actualShipDate = actualShipDate;
    }

    public LocalDateTime getActualArrivalDate() {
        return actualArrivalDate;
    }

    public void setActualArrivalDate(LocalDateTime actualArrivalDate) {
        this.actualArrivalDate = actualArrivalDate;
    }

    public String getShipmentBatchNo() {
        return shipmentBatchNo;
    }

    public void setShipmentBatchNo(String shipmentBatchNo) {
        this.shipmentBatchNo = shipmentBatchNo;
    }

    public String getShipToAddress() {
        return shipToAddress;
    }

    public void setShipToAddress(String shipToAddress) {
        this.shipToAddress = shipToAddress;
    }

    public LocalDateTime getShipDate() {
        return shipDate;
    }

    public void setShipDate(LocalDateTime shipDate) {
        this.shipDate = shipDate;
    }

    public LocalDateTime getActualEtaDate() {
        return actualEtaDate;
    }

    public void setActualEtaDate(LocalDateTime actualEtaDate) {
        this.actualEtaDate = actualEtaDate;
    }

    public String getWarehouseInNo() {
        return warehouseInNo;
    }

    public void setWarehouseInNo(String warehouseInNo) {
        this.warehouseInNo = warehouseInNo;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }
}
