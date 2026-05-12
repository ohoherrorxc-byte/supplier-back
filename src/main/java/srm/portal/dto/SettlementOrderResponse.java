package srm.portal.dto;

import java.util.List;

/**
 * 结算单列表响应
 */
public class SettlementOrderResponse {
    private List<SettlementOrderItem> items;
    private Long total;

    public List<SettlementOrderItem> getItems() {
        return items;
    }

    public void setItems(List<SettlementOrderItem> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}