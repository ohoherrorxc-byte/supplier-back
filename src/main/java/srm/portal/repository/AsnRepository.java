package srm.portal.repository;

import org.springframework.stereotype.Repository;
import srm.portal.mapper.AsnMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AsnRepository {
    private final AsnMapper asnMapper;

    public AsnRepository(AsnMapper asnMapper) {
        this.asnMapper = asnMapper;
    }

    public long insertHeader(Long supplierId, String logisticsCompany, String trackingNo, LocalDate etaDate, String status) {
        asnMapper.insertHeader(supplierId, logisticsCompany, trackingNo, etaDate, status);
        return 0; // MyBatis Mapper XML 不返回自增ID，需要通过其他方式获取
    }

    public void updateAsnNo(long asnId, String asnNo) {
        asnMapper.updateAsnNo(asnId, asnNo);
    }

    public void insertOrderLink(long asnId, long orderId) {
        asnMapper.insertOrderLink(asnId, orderId);
    }

    public void insertTimeline(long asnId, String eventCode, String description, LocalDateTime eventTime) {
        asnMapper.insertTimeline(asnId, eventCode, description, eventTime);
    }

    public List<Map<String, Object>> listAsnBySupplier(Long supplierId, int limit, int offset) {
        return asnMapper.listAsnBySupplier(supplierId, limit, offset);
    }

    public List<Map<String, Object>> listUpcomingAsnBySupplier(Long supplierId) {
        return asnMapper.listUpcomingAsnBySupplier(supplierId);
    }

    public long countAsnBySupplier(Long supplierId) {
        Long count = asnMapper.countAsnBySupplier(supplierId);
        return count != null ? count : 0L;
    }

    public Optional<Map<String, Object>> findHeaderById(long asnId) {
        Map<String, Object> row = asnMapper.findHeaderById(asnId);
        return Optional.ofNullable(row);
    }

    public Optional<Long> findSupplierIdByAsnId(long asnId) {
        Long supplierId = asnMapper.findSupplierIdByAsnId(asnId);
        return Optional.ofNullable(supplierId);
    }

    public List<Long> findOrderIdsByAsnId(long asnId) {
        return asnMapper.findOrderIdsByAsnId(asnId);
    }

    public List<Map<String, Object>> findAsnLinesByAsnId(long asnId) {
        return asnMapper.findAsnLinesByAsnId(asnId);
    }

    public List<Map<String, Object>> findTimelineByAsnId(long asnId) {
        List<Map<String, Object>> raw = asnMapper.findTimelineByAsnId(asnId);
        return raw.stream().map(this::normalizeTimelineRow).toList();
    }

    private Map<String, Object> normalizeTimelineRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>(row);
        Object t = m.get("time");
        if (t instanceof java.sql.Timestamp ts) {
            m.put("time", ts.toLocalDateTime());
        }
        return m;
    }

    public int updateAsnStatus(long asnId, String newStatus) {
        return asnMapper.updateAsnStatus(asnId, newStatus);
    }

    public long insertAsnLineNoAsnNo(Long supplierId, Long orderDetailId, int shipQty, String shipDate, String actualEtaDate, String warehouseInNo) {
        LocalDate sd = parseDate(shipDate);
        LocalDate eta = parseDate(actualEtaDate);
        return asnMapper.insertAsnLineNoAsnNo(supplierId, orderDetailId, shipQty, sd, eta, warehouseInNo);
    }

    public long insertAsnLine(Long supplierId, String asnNo, Long orderDetailId, int shipQty, String shipDate, String actualEtaDate, String warehouseInNo) {
        LocalDate sd = parseDate(shipDate);
        LocalDate eta = parseDate(actualEtaDate);
        return asnMapper.insertAsnLine(supplierId, asnNo, orderDetailId, shipQty, sd, eta, warehouseInNo);
    }

    public long insertAsnLineWithAsnId(Long supplierId, Long asnId, Long orderDetailId, int shipQty, String shipDate, String actualEtaDate, String warehouseInNo) {
        LocalDate sd = parseDate(shipDate);
        LocalDate eta = parseDate(actualEtaDate);
        return asnMapper.insertAsnLineWithAsnId(asnId, supplierId, orderDetailId, shipQty, sd, eta, warehouseInNo);
    }

    public void updateAsnSelfReference(long asnId) {
        asnMapper.updateAsnSelfReference(asnId);
    }

    public int updateAsnOrderDetailAndShipQty(long asnId, long orderDetailId, int shipQty, String shipDate, String actualEtaDate, String warehouseInNo) {
        return asnMapper.updateAsnOrderDetailAndShipQty(asnId, orderDetailId, shipQty, shipDate, actualEtaDate, warehouseInNo);
    }

    public int updateAsnShipInfo(long asnId, Integer shippedQty, String shipDate, String actualEtaDate, String warehouseInNo) {
        return asnMapper.updateAsnShipInfo(asnId, shippedQty, shipDate, actualEtaDate, warehouseInNo);
    }

    public int getMaxSeqBySupplierAndDate(Long supplierId, LocalDate date) {
        Integer maxSeq = asnMapper.getMaxSeqBySupplierAndDate(supplierId, date);
        return maxSeq != null ? maxSeq : 0;
    }

    public void cleanupPlaceholderAsn(Long supplierId, LocalDate shipDate) {
        asnMapper.cleanupPlaceholderAsn(supplierId, shipDate);
    }

    public int softDeleteAsn(long asnId) {
        return asnMapper.softDeleteAsn(asnId);
    }

    public String generateAsnNo(Long supplierId, LocalDate shipDate) {
        cleanupPlaceholderAsn(supplierId, shipDate);

        for (int retry = 0; retry < 10; retry++) {
            int maxSeq = getMaxSeqBySupplierAndDate(supplierId, shipDate);
            int nextSeq = maxSeq + 1;
            String asnNo = "ASN" + shipDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%04d", nextSeq);
            int updated = asnMapper.insertAsnPlaceholder(asnNo, supplierId, shipDate);
            if (updated > 0) {
                System.err.println("ASN编号生成成功: " + asnNo + ", retry=" + retry);
                return asnNo;
            }
            System.err.println("ASN编号被占用，重试: " + asnNo + ", retry=" + retry);
        }
        throw new IllegalStateException("ASN_NO_GENERATE_FAILED_AFTER_RETRY");
    }

    public long lockAsnNoAndGetId(String asnNo, Long supplierId) {
        Long id = asnMapper.lockAsnNoAndGetId(asnNo, supplierId);
        if (id == null) {
            throw new IllegalStateException("ASN_NO_NOT_FOUND");
        }
        return id;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        return LocalDate.parse(date);
    }
}