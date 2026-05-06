package srm.portal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单确认与拆单（ASN.md 模块 A）。
 */
public class ConfirmAndSplitRequest {

    @NotNull(message = "operatorUserId 不能为空")
    private Long operatorUserId;

    @NotNull(message = "detailId 不能为空（对应 PO_Line / 采购明细行 id）")
    private Long detailId;

    /**
     * CONFIRM_ONLY：不拆单，生成 1:1 拆分行；SPLIT_AND_CONFIRM：按 splitLines 拆且数量守恒。
     */
    @NotBlank(message = "action 不能为空")
    private String action;

    @Valid
    private List<SplitLineDraft> splitLines;

    /** 是否同步将订单头置为已确认，默认 true */
    private Boolean confirmOrderHeader = Boolean.TRUE;

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public Long getDetailId() {
        return detailId;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<SplitLineDraft> getSplitLines() {
        return splitLines;
    }

    public void setSplitLines(List<SplitLineDraft> splitLines) {
        this.splitLines = splitLines;
    }

    public Boolean getConfirmOrderHeader() {
        return confirmOrderHeader;
    }

    public void setConfirmOrderHeader(Boolean confirmOrderHeader) {
        this.confirmOrderHeader = confirmOrderHeader;
    }

    public static class SplitLineDraft {
        @NotNull
        @Positive
        @JsonAlias("split_qty")
        private Integer splitQty;

        @JsonAlias("committed_date")
        private LocalDate committedDate;

        public Integer getSplitQty() {
            return splitQty;
        }

        public void setSplitQty(Integer splitQty) {
            this.splitQty = splitQty;
        }

        public LocalDate getCommittedDate() {
            return committedDate;
        }

        public void setCommittedDate(LocalDate committedDate) {
            this.committedDate = committedDate;
        }
    }
}
