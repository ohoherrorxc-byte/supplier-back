package srm.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import srm.portal.dto.ShipmentDto;

import java.util.List;

@Mapper
public interface ShipmentMapper {
    List<ShipmentDto> searchShipments(
            @Param("asnNos") List<String> asnNos,
            @Param("supplierNos") List<String> supplierNos,
            @Param("systemOrderNos") List<String> systemOrderNos,
            @Param("sapOrderNos") List<String> sapOrderNos,
            @Param("shippedFlag") String shippedFlag,
            @Param("partsNos") List<String> partsNos,
            @Param("actualEtaDate") String actualEtaDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ShipmentDto> searchShipmentsBySupplier(
            @Param("asnNos") List<String> asnNos,
            @Param("supplierNos") List<String> supplierNos,
            @Param("systemOrderNos") List<String> systemOrderNos,
            @Param("sapOrderNos") List<String> sapOrderNos,
            @Param("shippedFlag") String shippedFlag,
            @Param("partsNos") List<String> partsNos,
            @Param("actualEtaDate") String actualEtaDate,
            @Param("supplierId") Long supplierId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countShipments(
            @Param("asnNos") List<String> asnNos,
            @Param("supplierNos") List<String> supplierNos,
            @Param("systemOrderNos") List<String> systemOrderNos,
            @Param("sapOrderNos") List<String> sapOrderNos,
            @Param("shippedFlag") String shippedFlag,
            @Param("partsNos") List<String> partsNos,
            @Param("actualEtaDate") String actualEtaDate
    );

    long countShipmentsBySupplier(
            @Param("asnNos") List<String> asnNos,
            @Param("supplierNos") List<String> supplierNos,
            @Param("systemOrderNos") List<String> systemOrderNos,
            @Param("sapOrderNos") List<String> sapOrderNos,
            @Param("shippedFlag") String shippedFlag,
            @Param("partsNos") List<String> partsNos,
            @Param("actualEtaDate") String actualEtaDate,
            @Param("supplierId") Long supplierId
    );
}
