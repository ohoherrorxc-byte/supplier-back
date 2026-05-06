package srm.portal.repository;

import org.springframework.stereotype.Repository;
import srm.portal.mapper.PackingListMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PackingListRepository {
    private final PackingListMapper packingListMapper;

    public PackingListRepository(PackingListMapper packingListMapper) {
        this.packingListMapper = packingListMapper;
    }

    // ==================== 箱单主表 CRUD ====================

    public long insertPackingList(Long asnId, Long asnLineId, String partsNo, String pnNo, String description,
                                   String packingMethod, Integer shippedQty,
                                   Integer unitsPerPallet, Integer layers, Double palletCount,
                                   Integer totalUnits, Double grossWeightPer, Double netWeightPer,
                                   Double totalGrossWeight, Double totalNetWeight,
                                   Integer palletLength, Integer palletWidth, Integer palletHeight,
                                   Double totalCbm,
                                   Integer cartonCount, Double totalGrossCarton, Double totalNetCarton,
                                   Double cartonCbm, String cartonSize, String remark) {
        return packingListMapper.insertPackingList(
                asnId, asnLineId, partsNo, pnNo, description, packingMethod, shippedQty,
                unitsPerPallet, layers, palletCount, totalUnits, grossWeightPer, netWeightPer,
                totalGrossWeight, totalNetWeight, palletLength, palletWidth, palletHeight, totalCbm,
                cartonCount, totalGrossCarton, totalNetCarton, cartonCbm, cartonSize, remark
        );
    }

    public Optional<Map<String, Object>> findPackingListById(long id) {
        Map<String, Object> row = packingListMapper.findPackingListById(id);
        return Optional.ofNullable(row);
    }

    public List<Map<String, Object>> findByAsnId(long asnId) {
        return packingListMapper.findByAsnId(asnId);
    }

    public List<Map<String, Object>> findByAsnLineId(long asnLineId) {
        return packingListMapper.findByAsnLineId(asnLineId);
    }

    public int deletePackingList(long id) {
        // 同时删除关联的 Handling Unit
        packingListMapper.deleteHandlingUnitsByPackingListId(id);
        return packingListMapper.deletePackingList(id);
    }

    // ==================== 包装单元 CRUD ====================

    public long insertHandlingUnit(Long packingListId, Long asnId, String unitType, String unitNo,
                                   int unitSeq, int quantity, Double grossWeight, Double netWeight,
                                   Integer lengthMm, Integer widthMm, Integer heightMm, Double cbm) {
        return packingListMapper.insertHandlingUnit(
                packingListId, asnId, unitType, unitNo, unitSeq, quantity,
                grossWeight, netWeight, lengthMm, widthMm, heightMm, cbm
        );
    }

    public List<Map<String, Object>> findHandlingUnitsByPackingListId(long packingListId) {
        return packingListMapper.findHandlingUnitsByPackingListId(packingListId);
    }

    public List<Map<String, Object>> findHandlingUnitsByAsnId(long asnId) {
        return packingListMapper.findHandlingUnitsByAsnId(asnId);
    }

    public int countHandlingUnitsByPackingListId(long packingListId) {
        return packingListMapper.countHandlingUnitsByPackingListId(packingListId);
    }
}