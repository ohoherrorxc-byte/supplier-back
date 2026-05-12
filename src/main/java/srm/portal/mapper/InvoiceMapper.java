package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import srm.portal.dto.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface InvoiceMapper {

    /**
     * 查询待开票清单 - 一般采购
     */
    List<PendingInvoiceItem> listPendingInvoiceGeneral(
            @Param("supplierNo") String supplierNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计待开票清单 - 一般采购
     */
    long countPendingInvoiceGeneral(@Param("supplierNo") String supplierNo);

    /**
     * 查询待开票清单 - BOM采购
     */
    List<PendingInvoiceItem> listPendingInvoiceBom(
            @Param("supplierNo") String supplierNo,
            @Param("partsNo") String partsNo,
            @Param("partsName") String partsName,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 统计待开票清单 - BOM采购
     */
    long countPendingInvoiceBom(@Param("supplierNo") String supplierNo);

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
                               @Param("acceptDetailIds") String acceptDetailIds);

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
    Integer getMaxSettlementSeq(@Param("dateStr") String dateStr);

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
}