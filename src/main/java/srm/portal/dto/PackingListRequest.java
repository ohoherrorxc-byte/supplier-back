package srm.portal.dto;

import java.util.List;

public class PackingListRequest {

    // ========== 托盘包装字段 ==========
    private Long asnId;
    private Long asnLineId;
    private String partsNo;
    private String pnNo;
    private String description;
    private String packingMethod; // PALLET / CARTON
    private Integer shippedQty;  // 本次发货总数量（用于校验）
    private Integer unitsPerPallet;
    private Integer layers;
    private Double palletCount;
    private Double grossWeightPer;
    private Double netWeightPer;
    private Integer palletLength;
    private Integer palletWidth;
    private Integer palletHeight;

    // ========== 纸箱包装字段 ==========
    private Integer cartonCount;
    private Double totalGrossCarton;
    private Double totalNetCarton;
    private Double cartonCbm;
    private String cartonSize;

    // ========== 公共字段 ==========
    private String remark;

    // ========== 多包装单元（自动生成 HU） ==========
    private List<PackingUnitItem> units;

    public Long getAsnId() { return asnId; }
    public void setAsnId(Long asnId) { this.asnId = asnId; }
    public Long getAsnLineId() { return asnLineId; }
    public void setAsnLineId(Long asnLineId) { this.asnLineId = asnLineId; }
    public String getPartsNo() { return partsNo; }
    public void setPartsNo(String partsNo) { this.partsNo = partsNo; }
    public String getPnNo() { return pnNo; }
    public void setPnNo(String pnNo) { this.pnNo = pnNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPackingMethod() { return packingMethod; }
    public void setPackingMethod(String packingMethod) { this.packingMethod = packingMethod; }
    public Integer getShippedQty() { return shippedQty; }
    public void setShippedQty(Integer shippedQty) { this.shippedQty = shippedQty; }
    public Integer getUnitsPerPallet() { return unitsPerPallet; }
    public void setUnitsPerPallet(Integer unitsPerPallet) { this.unitsPerPallet = unitsPerPallet; }
    public Integer getLayers() { return layers; }
    public void setLayers(Integer layers) { this.layers = layers; }
    public Double getPalletCount() { return palletCount; }
    public void setPalletCount(Double palletCount) { this.palletCount = palletCount; }
    public Double getGrossWeightPer() { return grossWeightPer; }
    public void setGrossWeightPer(Double grossWeightPer) { this.grossWeightPer = grossWeightPer; }
    public Double getNetWeightPer() { return netWeightPer; }
    public void setNetWeightPer(Double netWeightPer) { this.netWeightPer = netWeightPer; }
    public Integer getPalletLength() { return palletLength; }
    public void setPalletLength(Integer palletLength) { this.palletLength = palletLength; }
    public Integer getPalletWidth() { return palletWidth; }
    public void setPalletWidth(Integer palletWidth) { this.palletWidth = palletWidth; }
    public Integer getPalletHeight() { return palletHeight; }
    public void setPalletHeight(Integer palletHeight) { this.palletHeight = palletHeight; }
    public Integer getCartonCount() { return cartonCount; }
    public void setCartonCount(Integer cartonCount) { this.cartonCount = cartonCount; }
    public Double getTotalGrossCarton() { return totalGrossCarton; }
    public void setTotalGrossCarton(Double totalGrossCarton) { this.totalGrossCarton = totalGrossCarton; }
    public Double getTotalNetCarton() { return totalNetCarton; }
    public void setTotalNetCarton(Double totalNetCarton) { this.totalNetCarton = totalNetCarton; }
    public Double getCartonCbm() { return cartonCbm; }
    public void setCartonCbm(Double cartonCbm) { this.cartonCbm = cartonCbm; }
    public String getCartonSize() { return cartonSize; }
    public void setCartonSize(String cartonSize) { this.cartonSize = cartonSize; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<PackingUnitItem> getUnits() { return units; }
    public void setUnits(List<PackingUnitItem> units) { this.units = units; }

    public static class PackingUnitItem {
        private String unitType; // PALLET / CARTON
        private int quantity;
        private Double grossWeight;
        private Double netWeight;
        private Integer lengthMm;
        private Integer widthMm;
        private Integer heightMm;

        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public Double getGrossWeight() { return grossWeight; }
        public void setGrossWeight(Double grossWeight) { this.grossWeight = grossWeight; }
        public Double getNetWeight() { return netWeight; }
        public void setNetWeight(Double netWeight) { this.netWeight = netWeight; }
        public Integer getLengthMm() { return lengthMm; }
        public void setLengthMm(Integer lengthMm) { this.lengthMm = lengthMm; }
        public Integer getWidthMm() { return widthMm; }
        public void setWidthMm(Integer widthMm) { this.widthMm = widthMm; }
        public Integer getHeightMm() { return heightMm; }
        public void setHeightMm(Integer heightMm) { this.heightMm = heightMm; }
    }
}
