package srm.portal.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import srm.portal.dto.ShipmentShipRequest;
import srm.portal.repository.AsnRepository;
import srm.portal.repository.GenericTableLookupRepository;
import srm.portal.repository.ShipmentRepository;
import srm.portal.dto.ShipmentDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentRepository shipmentRepository;
    private final AsnRepository asnRepository;
    private final GenericTableLookupRepository lookupRepo;

    public ShipmentController(
            ShipmentRepository shipmentRepository,
            AsnRepository asnRepository,
            GenericTableLookupRepository lookupRepo
    ) {
        this.shipmentRepository = shipmentRepository;
        this.asnRepository = asnRepository;
        this.lookupRepo = lookupRepo;
    }

    /**
     * 发运信息管理列表查询（deliver.md 左侧多维查询区）
     * 非admin用户只能看自己所属供应商的数据
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String asnNos,
            @RequestParam(required = false) String supplierDuns,
            @RequestParam(required = false) String systemOrderNos,
            @RequestParam(required = false) String sapOrderNos,
            @RequestParam(required = false) String shippedFlag,
            @RequestParam(required = false) String partsNos,
            @RequestParam(required = false) String actualEtaDate,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false, defaultValue = "false") Boolean isAdmin,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int l = lookupRepo.normalizeLimit(limit);
        int o = lookupRepo.normalizeOffset(offset);

        List<ShipmentDto> items;
        long total;

        if (isAdmin) {
            items = shipmentRepository.search(
                    splitMulti(asnNos),
                    splitMulti(supplierDuns),
                    splitMulti(systemOrderNos),
                    splitMulti(sapOrderNos),
                    shippedFlag,
                    splitMulti(partsNos),
                    actualEtaDate,
                    l,
                    o
            );
            total = shipmentRepository.count(
                    splitMulti(asnNos),
                    splitMulti(supplierDuns),
                    splitMulti(systemOrderNos),
                    splitMulti(sapOrderNos),
                    shippedFlag,
                    splitMulti(partsNos),
                    actualEtaDate
            );
        } else {
            if (operatorUserId == null) {
                return ResponseEntity.status(400).body(Map.of("message", "operatorUserId不能为空"));
            }
            Optional<Long> supplierIdOpt = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(operatorUserId));
            if (supplierIdOpt.isEmpty()) {
                return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
            }
            Long supplierId = supplierIdOpt.get();
            items = shipmentRepository.searchBySupplier(
                    splitMulti(asnNos),
                    splitMulti(supplierDuns),
                    splitMulti(systemOrderNos),
                    splitMulti(sapOrderNos),
                    shippedFlag,
                    splitMulti(partsNos),
                    actualEtaDate,
                    supplierId,
                    l,
                    o
            );
            total = shipmentRepository.countBySupplier(
                    splitMulti(asnNos),
                    splitMulti(supplierDuns),
                    splitMulti(systemOrderNos),
                    splitMulti(sapOrderNos),
                    shippedFlag,
                    splitMulti(partsNos),
                    actualEtaDate,
                    supplierId
            );
        }

        return ResponseEntity.ok(Map.of("items", items, "total", total));
    }

    /**
     * 供应商点击“发货”确认：将 ASN 状态置为 SHIPPED，并写入时间轴。
     * 弹窗字段需与 deliver.md 保持一致。
     */
    @PostMapping("/{asnId}/ship")
    public ResponseEntity<Map<String, Object>> ship(@PathVariable Long asnId, @Valid @RequestBody ShipmentShipRequest req) {
        // 供应商隔离：operatorUserId -> supplierId，必须等于 ASN.supplier_id
        Optional<Long> userSupplier = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(req.getOperatorUserId()));
        if (userSupplier.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "当前用户未绑定供应商"));
        }
        Optional<Long> asnSupplier = asnRepository.findSupplierIdByAsnId(asnId);
        if (asnSupplier.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "ASN 不存在"));
        }
        if (!userSupplier.get().equals(asnSupplier.get())) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作该 ASN"));
        }

        // 更新 ASN 头字段
        String shipDateStr = req.getShipDate() != null ? req.getShipDate().toString() : null;
        String actualEtaDateStr = req.getActualEtaDate() != null ? req.getActualEtaDate().toString() : null;
        asnRepository.updateAsnShipInfo(asnId, req.getShippedQty(), shipDateStr, actualEtaDateStr, req.getWarehouseInNo());
        asnRepository.updateAsnStatus(asnId, "SHIPPED");
        String desc = "已发运 | 数量=" + req.getShippedQty()
                + " | 实际发货时间=" + shipDateStr
                + " | 实际到货时间=" + actualEtaDateStr
                + " | 进仓单号=" + (req.getWarehouseInNo() == null ? "-" : req.getWarehouseInNo())
                + " | 送货工厂=" + req.getShipFromPlant()
                + " | 需求工厂=" + req.getShipToPlant()
                + " | SAP发运号=" + (req.getSapShipmentNo() == null ? "-" : req.getSapShipmentNo())
                + " | 承运商=" + req.getLogisticsCompany()
                + " | 运单号=" + req.getTrackingNo()
                + " | ETA=" + req.getEtaDate();
        asnRepository.insertTimeline(asnId, "ASN_SHIPPED", desc, LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
                "asnId", asnId,
                "status", "SHIPPED",
                "message", "发货已确认"
        ));
    }

    private List<String> splitMulti(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String normalized = raw.replace("\r\n", "\n").replace(",", "\n");
        return java.util.Arrays.stream(normalized.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }
}
