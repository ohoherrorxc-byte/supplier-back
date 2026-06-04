package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplierMapper {

    @Select("SELECT * FROM ${tableName} WHERE ${pkColumn} = #{value}")
    @Results({
        @Result(column = "ID", property = "id", jdbcType = JdbcType.BIGINT),
        @Result(column = "IS_DELETED", property = "isDeleted", jdbcType = JdbcType.INTEGER)
    })
    Map<String, Object> findByPk(@Param("tableName") String tableName, @Param("pkColumn") String pkColumn, @Param("value") String value);

    @Select("SELECT * FROM ${tableName} WHERE ${columnName} = #{value}")
    Map<String, Object> findByColumn(@Param("tableName") String tableName, @Param("columnName") String columnName, @Param("value") String value);

    List<Map<String, Object>> listAllSuppliers(@Param("limit") int limit, @Param("offset") int offset);

    List<Map<String, Object>> searchSuppliersByName(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    Long findSupplierIdByName(@Param("supplierName") String supplierName);

    Map<String, Object> findSupplierPkByUserId(@Param("userId") String userId,
            @Param("userIdColumn") String userIdColumn,
            @Param("supplierIdColumn") String supplierIdColumn);

    Long findSupplierCompanyIdByUserId(@Param("userId") String userId,
            @Param("userIdColumn") String userIdColumn,
            @Param("supplierIdColumn") String supplierIdColumn);

    String findIsSupplierAdmin(@Param("userId") String userId);

    String findAccountByUserId(@Param("userId") String userId, @Param("pkColumn") String pkColumn);

    List<String> findRoleNamesByUserId(@Param("userId") String userId);

    String findSupplierClient(@Param("userId") String userId);

    Long countPendingOrders(@Param("supplierId") Long supplierId);

    Long countPendingOrdersAll();

    Long countActiveAsn(@Param("supplierId") Long supplierId);

    Long countActiveAsnAll();

    Long countTodayArriving(@Param("supplierId") Long supplierId);

    Long countTodayArrivingAll();

    Long countPendingForecast(@Param("supplierId") Long supplierId);

    Map<String, Object> findBladeUserByAccount(@Param("account") String account,
            @Param("tenantId") Long tenantId,
            @Param("accountColumn") String accountColumn);

    List<Map<String, Object>> findUserDetailsByUserId(@Param("userId") String userId,
            @Param("userIdColumn") String userIdColumn);

    int updatePassword(@Param("userId") Long userId,
            @Param("encryptedPassword") String encryptedPassword,
            @Param("passwordColumn") String passwordColumn,
            @Param("pkColumn") String pkColumn);

    List<Map<String, Object>> listOrdersBySupplier(@Param("supplierId") String supplierId,
            @Param("orderTable") String orderTable,
            @Param("supplierColumn") String supplierColumn,
            @Param("pkColumn") String pkColumn,
            @Param("limit") int limit, @Param("offset") int offset);

    String findOrderNoById(@Param("orderId") String orderId,
            @Param("orderTable") String orderTable,
            @Param("orderNoColumn") String orderNoColumn,
            @Param("pkColumn") String pkColumn);

    List<Map<String, Object>> listInvoicesByOrderDirect(@Param("orderId") String orderId,
            @Param("invoiceTable") String invoiceTable,
            @Param("orderIdColumn") String orderIdColumn,
            @Param("pkColumn") String pkColumn,
            @Param("limit") int limit, @Param("offset") int offset);

    List<Map<String, Object>> listInvoicesBySalesOrderNo(@Param("salesOrderNo") String salesOrderNo,
            @Param("invoiceTable") String invoiceTable,
            @Param("salesOrderNoColumn") String salesOrderNoColumn,
            @Param("pkColumn") String pkColumn,
            @Param("limit") int limit, @Param("offset") int offset);
}