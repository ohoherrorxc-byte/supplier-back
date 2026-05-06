package srm.portal.repository;

import org.springframework.stereotype.Repository;
import srm.portal.dto.ShipmentDto;
import srm.portal.mapper.ShipmentMapper;

import java.util.List;

@Repository
public class ShipmentRepository {
    private final ShipmentMapper shipmentMapper;

    public ShipmentRepository(ShipmentMapper shipmentMapper) {
        this.shipmentMapper = shipmentMapper;
    }

    public List<ShipmentDto> search(
            List<String> asnNos,
            List<String> supplierNos,
            List<String> systemOrderNos,
            List<String> sapOrderNos,
            String shippedFlag,
            List<String> partsNos,
            String actualEtaDate,
            int limit,
            int offset
    ) {
        return shipmentMapper.searchShipments(
                asnNos,
                supplierNos,
                systemOrderNos,
                sapOrderNos,
                shippedFlag,
                partsNos,
                actualEtaDate,
                limit,
                offset
        );
    }

    public long count(
            List<String> asnNos,
            List<String> supplierNos,
            List<String> systemOrderNos,
            List<String> sapOrderNos,
            String shippedFlag,
            List<String> partsNos,
            String actualEtaDate
    ) {
        return shipmentMapper.countShipments(
                asnNos,
                supplierNos,
                systemOrderNos,
                sapOrderNos,
                shippedFlag,
                partsNos,
                actualEtaDate
        );
    }

    public List<ShipmentDto> searchBySupplier(
            List<String> asnNos,
            List<String> supplierNos,
            List<String> systemOrderNos,
            List<String> sapOrderNos,
            String shippedFlag,
            List<String> partsNos,
            String actualEtaDate,
            Long supplierId,
            int limit,
            int offset
    ) {
        return shipmentMapper.searchShipmentsBySupplier(
                asnNos,
                supplierNos,
                systemOrderNos,
                sapOrderNos,
                shippedFlag,
                partsNos,
                actualEtaDate,
                supplierId,
                limit,
                offset
        );
    }

    public long countBySupplier(
            List<String> asnNos,
            List<String> supplierNos,
            List<String> systemOrderNos,
            List<String> sapOrderNos,
            String shippedFlag,
            List<String> partsNos,
            String actualEtaDate,
            Long supplierId
    ) {
        return shipmentMapper.countShipmentsBySupplier(
                asnNos,
                supplierNos,
                systemOrderNos,
                sapOrderNos,
                shippedFlag,
                partsNos,
                actualEtaDate,
                supplierId
        );
    }
}
