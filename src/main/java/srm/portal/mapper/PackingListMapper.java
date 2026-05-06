package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PackingListMapper {

    // ============ 箱单主表 CRUD ============

    int insertPackingList(
            @Param("asnId") Long asnId,
            @Param("asnLineId") Long asnLineId,
            @Param("partsNo") String partsNo,
            @Param("pnNo") String pnNo,
            @Param("description") String description,
            @Param("packingMethod") String packingMethod,
            @Param("shippedQty") Integer shippedQty,
            @Param("unitsPerPallet") Integer unitsPerPallet,
            @Param("layers") Integer layers,
            @Param("palletCount") Double palletCount,
            @Param("totalUnits") Integer totalUnits,
            @Param("grossWeightPer") Double grossWeightPer,
            @Param("netWeightPer") Double netWeightPer,
            @Param("totalGrossWeight") Double totalGrossWeight,
            @Param("totalNetWeight") Double totalNetWeight,
            @Param("palletLength") Integer palletLength,
            @Param("palletWidth") Integer palletWidth,
            @Param("palletHeight") Integer palletHeight,
            @Param("totalCbm") Double totalCbm,
            @Param("cartonCount") Integer cartonCount,
            @Param("totalGrossCarton") Double totalGrossCarton,
            @Param("totalNetCarton") Double totalNetCarton,
            @Param("cartonCbm") Double cartonCbm,
            @Param("cartonSize") String cartonSize,
            @Param("remark") String remark
    );

    Map<String, Object> findPackingListById(@Param("id") long id);

    List<Map<String, Object>> findByAsnId(@Param("asnId") long asnId);

    List<Map<String, Object>> findByAsnLineId(@Param("asnLineId") long asnLineId);

    int deletePackingList(@Param("id") long id);

    int deleteHandlingUnitsByPackingListId(@Param("packingListId") long packingListId);

    // ============ 包装单元 CRUD ============

    int insertHandlingUnit(
            @Param("packingListId") Long packingListId,
            @Param("asnId") Long asnId,
            @Param("unitType") String unitType,
            @Param("unitNo") String unitNo,
            @Param("unitSeq") Integer unitSeq,
            @Param("quantity") Integer quantity,
            @Param("grossWeight") Double grossWeight,
            @Param("netWeight") Double netWeight,
            @Param("lengthMm") Integer lengthMm,
            @Param("widthMm") Integer widthMm,
            @Param("heightMm") Integer heightMm,
            @Param("cbm") Double cbm
    );

    List<Map<String, Object>> findHandlingUnitsByPackingListId(@Param("packingListId") long packingListId);

    List<Map<String, Object>> findHandlingUnitsByAsnId(@Param("asnId") long asnId);

    int countHandlingUnitsByPackingListId(@Param("packingListId") long packingListId);
}