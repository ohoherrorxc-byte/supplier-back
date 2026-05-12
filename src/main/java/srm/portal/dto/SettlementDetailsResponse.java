package srm.portal.dto;

import java.util.List;

/**
 * 结算单明细响应
 */
public class SettlementDetailsResponse {
    private List<SettlementDetailItem> details;

    public List<SettlementDetailItem> getDetails() {
        return details;
    }

    public void setDetails(List<SettlementDetailItem> details) {
        this.details = details;
    }
}