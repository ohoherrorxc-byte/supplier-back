package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import srm.portal.dto.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface InvoiceMapper {

    /**
     * 查询待开票清单 - 一般采购（按验收单号分页）
     */
    List<String> listPendingInvoiceGeneralByPage(
            @Param("supplierNo") String supplierNo,
            @Param("acceptApplyNo") String acceptApplyNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("isAdmin") boolean isAdmin,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 根据验收单号列表查询待开票明细 - 一般采购
     */
    List<PendingInvoiceItem> listPendingInvoiceGeneralByAcceptApplyNos(
            @Param("supplierNo") String supplierNo,
            @Param("acceptApplyNo") String acceptApplyNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("isAdmin") boolean isAdmin,
            @Param("acceptApplyNos") List<String> acceptApplyNos
    );

    /**
     * 根据验收单号列表查询待开票明细 - 一般采购（简化版）
     */
    List<PendingInvoiceItem> listPendingInvoiceGeneralByAcceptApplyNosSimple(
            @Param("supplierNo") String supplierNo,
            @Param("isAdmin") boolean isAdmin,
            @Param("acceptApplyNos") List<String> acceptApplyNos
    );

    /**
     * 统计待开票清单 - 一般采购（按验收单号去重）
     */
    long countPendingInvoiceGeneral(@Param("supplierNo") String supplierNo, @Param("isAdmin") boolean isAdmin);

    /**
     * 获取待确认订单号列表（分页）
     */
    List<String> listPendingOrderApplyNos(
            @Param("supplierNo") String supplierNo,
            @Param("status") Integer status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计待确认订单数量
     */
    long countPendingOrders(@Param("supplierNo") String supplierNo, @Param("status") Integer status);

    /**
     * 获取待开票清单 - 一般采购（验收单号列表，用于消息展示）
     */
    List<String> listPendingInvoiceGeneralApplyNos(
            @Param("supplierNo") String supplierNo,
            @Param("isAdmin") boolean isAdmin,
            @Param("limit") int limit
    );

    /**
     * 获取待开票清单 - BOM采购（验收单号列表，用于消息展示）
     */
    List<String> listPendingInvoiceBomApplyNos(
            @Param("supplierNo") String supplierNo,
            @Param("isAdmin") boolean isAdmin,
            @Param("limit") int limit
    );

    /**
     * 查询待开票清单 - BOM采购（一次查询，分页+同一验收单不被拆散）
     */
    List<PendingInvoiceItem> listPendingInvoiceBomByPage(
            @Param("supplierNo") String supplierNo,
            @Param("acceptApplyNo") String acceptApplyNo,
            @Param("orderNo") String orderNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("isAdmin") boolean isAdmin,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 根据验收单号列表查询待开票明细 - BOM采购
     */
    List<PendingInvoiceItem> listPendingInvoiceBomByAcceptApplyNos(
            @Param("supplierNo") String supplierNo,
            @Param("acceptApplyNo") String acceptApplyNo,
            @Param("orderNo") String orderNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("isAdmin") boolean isAdmin,
            @Param("acceptApplyNos") List<String> acceptApplyNos
    );

    /**
     * 根据验收单号列表查询待开票明细 - BOM采购（简单版，仅用 acceptApplyNos 过滤）
     */
    List<PendingInvoiceItem> listPendingInvoiceBomByAcceptApplyNosSimple(
            @Param("supplierNo") String supplierNo,
            @Param("isAdmin") boolean isAdmin,
            @Param("acceptApplyNos") List<String> acceptApplyNos
    );

    /**
     * 统计待开票清单 - BOM采购（按验收单号去重）
     */
    long countPendingInvoiceBom(@Param("supplierNo") String supplierNo, @Param("isAdmin") boolean isAdmin);

    /**
     * 统计待开票清单明细行数 - BOM采购
     */
    long countPendingInvoiceBomDetails(@Param("supplierNo") String supplierNo, @Param("isAdmin") boolean isAdmin);

    /**
     * 创建结算单
     */
    void insertSettlementOrder(@Param("settlementNo") String settlementNo,
                               @Param("supplierNo") String supplierNo,
                               @Param("supplierName") String supplierName,
                               @Param("invoiceAmount") java.math.BigDecimal invoiceAmount,
                               @Param("invoiceTaxRate") java.math.BigDecimal invoiceTaxRate,
                               @Param("invoiceTaxAmount") java.math.BigDecimal invoiceTaxAmount,
                               @Param("invoiceTotalAmount") java.math.BigDecimal invoiceTotalAmount,
                               @Param("remark") String remark,
                               @Param("createUser") Long createUser,
                               @Param("createDept") String createDept,
                               @Param("acceptDetailIds") String acceptDetailIds,
                               @Param("acceptApplyNo") String acceptApplyNo);

    /**
     * 获取刚插入的结算单ID
     */
    Long getLastInsertId();

    /**
     * 查询结算单列表
     */
    List<SettlementOrderItem> listSettlementOrders(
            @Param("supplierNo") String supplierNo,
            @Param("settlementNo") String settlementNo,
            @Param("status") Integer status,
            @Param("acceptApplyNo") String acceptApplyNo,
            @Param("bomOrderNo") String bomOrderNo,
            @Param("contractNo") String contractNo,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计结算单数量
     */
    long countSettlementOrders(@Param("supplierNo") String supplierNo);

    /**
     * 查询供应商编号（根据供应商ID）
     */
    String getSupplierNoBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 生成结算单号（获取当前最大序号）
     */
    Integer getMaxSettlementSeq(@Param("supplierNo") String supplierNo, @Param("dateStr") String dateStr);

    /**
     * 查询供应商公司ID（根据用户ID）
     */
    Long findSupplierCompanyIdByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否为管理员
     */
    Boolean isAdminUser(@Param("userId") Long userId);

    /**
     * 创建结算单发票明细
     */
    void insertSettlementInvoiceLine(SettlementInvoiceLine line);

    /**
     * 根据结算单ID获取结算明细（通过accept_detail_ids反查验收明细）
     */
    List<SettlementDetailItem> getSettlementDetails(@Param("settlementId") Long settlementId);

    /**
     * 根据结算单ID获取发票明细
     */
    List<SettlementInvoiceLine> getSettlementInvoiceLines(@Param("settlementId") Long settlementId);

    /**
     * 更新结算单状态
     */
    void updateSettlementStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除结算单（软删除）
     */
    void deleteSettlementOrder(@Param("id") Long id);

    /**
     * 检查是否有草稿结算单包含指定验收明细
     */
    List<Map<String, Object>> findDraftSettlements(@Param("acceptDetailIds") List<Long> acceptDetailIds);

    /**
     * 获取结算单的完工单附件
     */
    List<Map<String, Object>> getCompletionFiles(@Param("settlementId") Long settlementId);

    /**
     * 获取结算单的订单附件
     */
    List<Map<String, Object>> getOrderFiles(@Param("settlementId") Long settlementId);

    /**
     * 获取结算单备注
     */
    String getSettlementRemark(@Param("settlementId") Long settlementId);

    /**
     * 插入完工单附件记录
     */
    void insertCompletionFile(@Param("id") Long id, @Param("businessId") Long businessId, @Param("businessName") String businessName,
                              @Param("url") String url, @Param("name") String name, @Param("createUser") Long createUser);
}