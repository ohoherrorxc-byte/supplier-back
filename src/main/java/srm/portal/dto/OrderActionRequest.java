package srm.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderActionRequest {
    @NotNull
    private Long operatorUserId;

    @Size(max = 255)
    private String reason;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

