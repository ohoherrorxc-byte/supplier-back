package srm.portal.dto;

import java.util.List;

/**
 * 待开票清单响应
 */
public class PendingInvoiceResponse {
    private List<PendingInvoiceItem> items;
    private Long total;

    public List<PendingInvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<PendingInvoiceItem> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}