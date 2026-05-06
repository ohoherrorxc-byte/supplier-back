package srm.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 仓库对 ASN 明细行收货（ASN.md 模块 C 到货回写）。
 */
public class ReceiveAsnLineRequest {

    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    @NotNull
    @Positive
    private Integer receivedQty;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public Integer getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Integer receivedQty) {
        this.receivedQty = receivedQty;
    }
}
