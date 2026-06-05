package srm.portal.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface GenericMapper {

    /**
     * 按主键查询记录
     */
    @Select("SELECT * FROM ${tableName} WHERE ${pkColumn} = #{value}")
    Map<String, Object> findByPk(
            @Param("tableName") String tableName,
            @Param("pkColumn") String pkColumn,
            @Param("value") String value
    );

    /**
     * 按列查询记录
     */
    @Select("SELECT * FROM ${tableName} WHERE ${columnName} = #{value}")
    Map<String, Object> findByColumn(
            @Param("tableName") String tableName,
            @Param("columnName") String columnName,
            @Param("value") String value
    );

    /**
     * 查询供应商信息
     */
    @Select("""
            SELECT * FROM tb_contract_supplier
            WHERE ID = #{supplierId}
            """)
    Map<String, Object> findSupplierById(@Param("supplierId") String supplierId);

    /**
     * 查询用户基本信息
     */
    @Select("""
            SELECT * FROM blade_user
            WHERE id = #{userId}
            """)
    Map<String, Object> findUserBaseById(@Param("userId") String userId);

    /**
     * 查询用户详情
     */
    @Select("""
            SELECT * FROM tb_user_detail
            WHERE USER_ID = #{userId}
            """)
    Map<String, Object> findUserDetailByUserId(@Param("userId") String userId);

    /**
     * 根据用户 ID 查询供应商公司 ID
     */
    @Select("""
            SELECT SUPPLIER_COMPANY_ID FROM tb_user_detail
            WHERE USER_ID = #{userId}
            """)
    Long findSupplierCompanyIdByUserId(@Param("userId") String userId);

    /**
     * 查询订单
     */
    @Select("""
            SELECT * FROM tb_purchase_parts_order
            WHERE id = #{orderId}
            """)
    Map<String, Object> findOrderById(@Param("orderId") String orderId);

    /**
     * 查询发票
     */
    @Select("""
            SELECT * FROM tb_invoice
            WHERE ID = #{invoiceId}
            """)
    Map<String, Object> findInvoiceById(@Param("invoiceId") String invoiceId);

    /**
     * 查询仪表板统计数据
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM tb_purchase_parts_order WHERE supplier_id = #{supplierId} AND supplier_order_status = 0 AND is_deleted = 0) as pending_orders,
              (SELECT COUNT(*) FROM tb_srm_asn WHERE supplier_id = #{supplierId} AND status IN ('CREATED', 'SHIPPED')) as active_asn,
              (SELECT COUNT(*) FROM tb_srm_asn WHERE supplier_id = #{supplierId} AND actual_eta_date = CURDATE() AND status = 'SHIPPED' AND IFNULL(is_deleted, 0) = 0) as today_arriving,
              (SELECT COUNT(*) FROM tb_delivery_plan WHERE supplier_id = #{supplierId} AND plan_date = CURDATE() AND IFNULL(is_deleted, 0) = 0) as today_plans
            """)
    Map<String, Object> getDashboardStats(@Param("supplierId") Long supplierId);

    /**
     * 查询订单列表
     */
    @Select("""
            SELECT * FROM tb_purchase_parts_order
            WHERE supplier_id = #{supplierId}
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listOrdersBySupplier(
            @Param("supplierId") String supplierId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 查询发票列表 - 直连订单
     */
    @Select("""
            SELECT * FROM tb_invoice
            WHERE PUCHASE_ORDER_ID = #{orderId}
            ORDER BY ID DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listInvoicesByOrderDirect(
            @Param("orderId") String orderId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 查询发票列表 - 通过销售订单号
     */
    @Select("""
            SELECT * FROM tb_invoice
            WHERE SALES_ORDER_NO = #{salesOrderNo}
            ORDER BY ID DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listInvoicesByOrderNo(
            @Param("salesOrderNo") String salesOrderNo,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 用户认证 - 查询 blade_user
     */
    @Select("""
            SELECT * FROM blade_user
            WHERE account = #{account} AND tenant_id = #{tenantId} AND IFNULL(is_deleted, 0) = 0
            """)
    Map<String, Object> findBladeUserByAccount(@Param("account") String account, @Param("tenantId") Long tenantId);

    /**
     * 用户认证 - 查询 tb_user_detail
     */
    @Select("""
            SELECT * FROM tb_user_detail
            WHERE USER_ID = #{userId} AND IFNULL(IS_DELETED, 0) = 0
            """)
    List<Map<String, Object>> findUserDetailsByUserId(@Param("userId") Long userId);

    /**
     * 判断用户是否为管理员（通过账号名）
     */
    @Select("""
            SELECT account FROM blade_user
            WHERE id = #{userId} AND IFNULL(is_deleted, 0) = 0
            """)
    String findAccountByUserId(@Param("userId") Long userId);

    /**
     * 判断用户是否为管理员（通过角色）
     */
    @Select("""
            SELECT r.role_name FROM blade_role ur
            INNER JOIN blade_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND IFNULL(ur.is_deleted, 0) = 0 AND IFNULL(r.is_deleted, 0) = 0
            """)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 判断用户是否为供应商客户
     */
    @Select("""
            SELECT SUPPLIER_CLIENT FROM tb_user_detail
            WHERE USER_ID = #{userId} AND IFNULL(IS_DELETED, 0) = 0
            """)
    String findSupplierClientByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT IS_SUPPLIER_ADMIN FROM tb_user_detail
            WHERE USER_ID = #{userId} AND IFNULL(IS_DELETED, 0) = 0
            """)
    String findSupplierAdminByUserId(@Param("userId") Long userId);

    /**
     * 判断用户是否只允许开票（only_open_invoice）
     */
    @Select("""
            SELECT only_open_invoice FROM blade_user
            WHERE id = #{userId} AND IFNULL(is_deleted, 0) = 0
            """)
    String findOnlyOpenInvoiceByUserId(@Param("userId") Long userId);

    /**
     * 修改密码
     */
    @Update("""
            UPDATE blade_user
            SET password = #{encryptedPassword}
            WHERE id = #{userId} AND IFNULL(is_deleted, 0) = 0
            """)
    int updatePassword(@Param("userId") Long userId, @Param("encryptedPassword") String encryptedPassword);

    /**
     * 根据供应商用户ID查询供应商
     */
    @Select("""
            SELECT s.* FROM tb_contract_supplier s
            INNER JOIN tb_user_detail ud ON s.ID = ud.SUPPLIER_COMPANY_ID
            WHERE ud.USER_ID = #{userId} AND IFNULL(ud.IS_DELETED, 0) = 0
            """)
    Map<String, Object> findSupplierByUserId(@Param("userId") Long userId);

    /**
     * 统计待确认订单
     */
    @Select("""
            SELECT COUNT(*) FROM tb_purchase_parts_order
            WHERE supplier_id = #{supplierId} AND supplier_order_status = 0 AND is_deleted = 0
            """)
    Long countPendingOrders(@Param("supplierId") Long supplierId);

    /**
     * 统计在途 ASN
     */
    @Select("""
            SELECT COUNT(*) FROM tb_srm_asn
            WHERE supplier_id = #{supplierId} AND status IN ('CREATED', 'SHIPPED') AND IFNULL(is_deleted, 0) = 0
            """)
    Long countActiveAsn(@Param("supplierId") Long supplierId);

    /**
     * 统计今日到货数量
     */
    @Select("""
            SELECT COALESCE(SUM(ship_qty), 0) FROM tb_srm_asn
            WHERE supplier_id = #{supplierId} AND actual_eta_date = CURDATE() AND status = 'SHIPPED' AND IFNULL(is_deleted, 0) = 0
            """)
    Long countTodayArriving(@Param("supplierId") Long supplierId);

    /**
     * 统计待确认预测数量
     */
    @Select("""
            SELECT COUNT(DISTINCT o.order_no)
            FROM tb_purchase_parts_order_detail d
            INNER JOIN tb_purchase_parts_order o ON d.order_id = o.id
            WHERE o.supplier_id = #{supplierId}
              AND IFNULL(d.is_deleted, 0) = 0
              AND IFNULL(o.is_deleted, 0) = 0
              AND o.require_attr = '预测'
              AND o.cover_predict_status = 2
              AND d.supplier_order_status = 0
              AND d.need_date >= DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 12 MONTH)
            """)
    Long countPendingForecast(@Param("supplierId") Long supplierId);
}
