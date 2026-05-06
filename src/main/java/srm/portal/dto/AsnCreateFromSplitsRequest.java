package srm.portal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * 按拆分行创建 ASN（ASN.md 模块 B）。
 */
public class AsnCreateFromSplitsRequest {

    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    @JsonAlias("carrier")
    private String logisticsCompany;
    @JsonAlias("tracking_no")
    private String trackingNo;
    @JsonAlias("eta_date")
    private LocalDate etaDate;

    @NotEmpty(message = "lines 不能为空")
    @Valid
    private List<AsnSplitShipLine> lines;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
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

    public List<AsnSplitShipLine> getLines() {
        return lines;
    }

    public void setLines(List<AsnSplitShipLine> lines) {
        this.lines = lines;
    }

    public static class AsnSplitShipLine {
        @NotNull
        @JsonAlias("split_line_id")
        private Long splitLineId;

        @NotNull
        @Positive
        @JsonAlias("shipped_qty")
        private Integer shippedQty;

        public Long getSplitLineId() {
            return splitLineId;
        }

        public void setSplitLineId(Long splitLineId) {
            this.splitLineId = splitLineId;
        }

        public Integer getShippedQty() {
            return shippedQty;
        }

        public void setShippedQty(Integer shippedQty) {
            this.shippedQty = shippedQty;
        }
    }
}
