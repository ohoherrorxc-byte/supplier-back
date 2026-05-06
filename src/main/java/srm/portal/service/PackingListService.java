package srm.portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import srm.portal.dto.PackingListRequest;
import srm.portal.mapper.PackingListMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PackingListService {
    @Autowired
    private PackingListMapper packingListMapper;

    /**
     * 创建箱单（同时根据托数/箱数自动生成 Handling Unit）
     */
    @Transactional
    public Map<String, Object> createPackingList(PackingListRequest req) {
        // 参数校验
        if (req.getAsnId() == null) throw new IllegalStateException("ASN_ID_REQUIRED");
        if (req.getPackingMethod() == null) throw new IllegalStateException("PACKING_METHOD_REQUIRED");

        String method = req.getPackingMethod().toUpperCase();
        if (!method.equals("PALLET") && !method.equals("CARTON")) {
            throw new IllegalStateException("INVALID_PACKING_METHOD");
        }

        // 计算汇总值
        double totalGrossWeight = 0, totalNetWeight = 0, totalCbm = 0;
        int totalUnits = 0;
        int palletCountInt = 0;

        if ("PALLET".equals(method)) {
            int unitsPerPallet = req.getUnitsPerPallet() != null ? req.getUnitsPerPallet() : 0;
            double palletCount = req.getPalletCount() != null ? req.getPalletCount() : 0;
            totalUnits = unitsPerPallet * (int) Math.ceil(palletCount);
            totalGrossWeight = (req.getGrossWeightPer() != null ? req.getGrossWeightPer() : 0) * palletCount;
            totalNetWeight = (req.getNetWeightPer() != null ? req.getNetWeightPer() : 0) * palletCount;
            int len = req.getPalletLength() != null ? req.getPalletLength() : 0;
            int w = req.getPalletWidth() != null ? req.getPalletWidth() : 0;
            int h = req.getPalletHeight() != null ? req.getPalletHeight() : 0;
            if (len > 0 && w > 0 && h > 0) {
                totalCbm = (len * w * h / 1_000_000_000.0) * palletCount;
            }
            palletCountInt = (int) Math.ceil(palletCount);
        } else {
            // CARTON
            totalGrossWeight = req.getTotalGrossCarton() != null ? req.getTotalGrossCarton() : 0;
            totalNetWeight = req.getTotalNetCarton() != null ? req.getTotalNetCarton() : 0;
            totalCbm = req.getCartonCbm() != null ? req.getCartonCbm() : 0;
            totalUnits = req.getShippedQty() != null ? req.getShippedQty() : 0;
            palletCountInt = req.getCartonCount() != null ? req.getCartonCount() : 0;
        }

        // 数量守恒校验
        int shippedQty = req.getShippedQty() != null ? req.getShippedQty() : 0;
        if (shippedQty > 0 && totalUnits > 0 && totalUnits != shippedQty) {
            throw new IllegalStateException("箱单打包总数(" + totalUnits + "件)与本次计划发运数量(" + shippedQty + "件)不符，请检查包装明细！");
        }

        // 插入箱单主记录
        long packingListId = packingListMapper.insertPackingList(
                req.getAsnId(),
                req.getAsnLineId(),
                req.getPartsNo(),
                req.getPnNo(),
                req.getDescription(),
                method,
                shippedQty,
                req.getUnitsPerPallet(),
                req.getLayers(),
                req.getPalletCount(),
                totalUnits,
                req.getGrossWeightPer(),
                req.getNetWeightPer(),
                Math.round(totalGrossWeight * 100.0) / 100.0,
                Math.round(totalNetWeight * 100.0) / 100.0,
                req.getPalletLength(),
                req.getPalletWidth(),
                req.getPalletHeight(),
                Math.round(totalCbm * 100.0) / 100.0,
                req.getCartonCount(),
                req.getTotalGrossCarton(),
                req.getTotalNetCarton(),
                req.getCartonCbm(),
                req.getCartonSize(),
                req.getRemark()
        );

        // 自动生成 Handling Unit（托盘或纸箱单元）
        if ("PALLET".equals(method) && req.getPalletCount() != null && req.getPalletCount() > 0) {
            int count = (int) Math.ceil(req.getPalletCount());
            for (int i = 1; i <= count; i++) {
                String unitNo = generateUnitNo(req.getAsnId(), "PLT", i);
                double unitGross = req.getGrossWeightPer() != null ? req.getGrossWeightPer() : 0;
                double unitNet = req.getNetWeightPer() != null ? req.getNetWeightPer() : 0;
                double unitCbm = 0;
                int len = req.getPalletLength() != null ? req.getPalletLength() : 0;
                int w = req.getPalletWidth() != null ? req.getPalletWidth() : 0;
                int h = req.getPalletHeight() != null ? req.getPalletHeight() : 0;
                if (len > 0 && w > 0 && h > 0) {
                    unitCbm = len * w * h / 1_000_000_000.0;
                }
                int qtyPerUnit = req.getUnitsPerPallet() != null ? req.getUnitsPerPallet() : 0;
                packingListMapper.insertHandlingUnit(
                        packingListId, req.getAsnId(), "PALLET", unitNo, i,
                        qtyPerUnit, unitGross, unitNet, len, w, h,
                        Math.round(unitCbm * 10000.0) / 10000.0
                );
            }
        } else if ("CARTON".equals(method) && req.getCartonCount() != null && req.getCartonCount() > 0) {
            int count = req.getCartonCount();
            for (int i = 1; i <= count; i++) {
                String unitNo = generateUnitNo(req.getAsnId(), "CTN", i);
                packingListMapper.insertHandlingUnit(
                        packingListId, req.getAsnId(), "CARTON", unitNo, i,
                        shippedQty / count, // 平均分摊（可后续调整）
                        req.getTotalGrossCarton() != null ? req.getTotalGrossCarton() / count : null,
                        req.getTotalNetCarton() != null ? req.getTotalNetCarton() / count : null,
                        null, null, null, null
                );
            }
        }

        return getPackingListDetail(packingListId);
    }

    /**
     * 获取箱单详情（含 Handling Units）
     */
    public Map<String, Object> getPackingListDetail(long packingListId) {
        Map<String, Object> pl = packingListMapper.findPackingListById(packingListId);
        if (pl == null) throw new IllegalStateException("PACKING_LIST_NOT_FOUND");
        Map<String, Object> result = new LinkedHashMap<>(pl);
        List<Map<String, Object>> hus = packingListMapper.findHandlingUnitsByPackingListId(packingListId);
        result.put("handlingUnits", hus);
        return result;
    }

    /**
     * 查询某 ASN 的所有箱单
     */
    public List<Map<String, Object>> listByAsnId(long asnId) {
        List<Map<String, Object>> lists = packingListMapper.findByAsnId(asnId);
        for (Map<String, Object> pl : lists) {
            long plId = ((Number) pl.get("id")).longValue();
            List<Map<String, Object>> hus = packingListMapper.findHandlingUnitsByPackingListId(plId);
            pl.put("handlingUnits", hus);
            pl.put("unitCount", hus.size());
        }
        return lists;
    }

    /**
     * 删除箱单
     */
    @Transactional
    public void deletePackingList(long packingListId) {
        packingListMapper.deletePackingList(packingListId);
    }

    /**
     * 生成单元编号：PLT-YYYYMMDD-XXX 或 CTN-YYYYMMDD-XXX
     */
    private String generateUnitNo(Long asnId, String prefix, int seq) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }
}
