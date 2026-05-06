package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PoSplitAsnMapper {

    // ============ 拆分行查询 ============

    Map<String, Object> findDetailRow(@Param("detailId") long detailId, @Param("orderId") long orderId);

    boolean existsPositiveShippedForDetail(@Param("detailId") long detailId);

    int deleteSplitsWithZeroShippedByDetail(@Param("detailId") long detailId);

    List<Map<String, Object>> listSplitLinesByOrder(@Param("orderId") long orderId);

    Map<String, Object> lockSplitLine(@Param("splitLineId") long splitLineId);

    List<Map<String, Object>> findSplitLineForRefresh(@Param("splitLineId") long splitLineId);

    // ============ 拆分行插入 ============

    int insertSplitLine(
            @Param("orderId") long orderId,
            @Param("detailId") long detailId,
            @Param("splitQty") int splitQty,
            @Param("committedDate") LocalDate committedDate,
            @Param("status") String status
    );

    // ============ 拆分行更新 ============

    int addSplitShippedQty(@Param("splitLineId") long splitLineId, @Param("delta") int delta);

    int addSplitReceivedQty(@Param("splitLineId") long splitLineId, @Param("delta") int delta);

    int updateSplitStatus(@Param("splitLineId") long splitLineId, @Param("status") String status);

    // ============ ASN 行查询 ============

    Map<String, Object> findAsnLineById(@Param("asnLineId") long asnLineId);

    Map<String, Object> lockAsnLine(@Param("asnLineId") long asnLineId);

    List<Map<String, Object>> listAsnLinesByAsnId(@Param("asnId") long asnId);

    Long findOrderIdBySplitLineId(@Param("splitLineId") long splitLineId);

    // ============ ASN 行插入 ============

    int insertAsnLine(
            @Param("asnId") long asnId,
            @Param("splitLineId") long splitLineId,
            @Param("shippedQty") int shippedQty
    );

    // ============ ASN 行更新 ============

    int addAsnLineReceived(@Param("asnLineId") long asnLineId, @Param("delta") int delta, @Param("receiptTime") LocalDateTime receiptTime);
}