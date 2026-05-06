package srm.portal.repository;

import org.springframework.stereotype.Repository;
import srm.portal.mapper.PoSplitAsnMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PoSplitAsnRepository {
    private final PoSplitAsnMapper poSplitAsnMapper;

    public PoSplitAsnRepository(PoSplitAsnMapper poSplitAsnMapper) {
        this.poSplitAsnMapper = poSplitAsnMapper;
    }

    public Optional<Map<String, Object>> findDetailRow(long detailId, long orderId) {
        Map<String, Object> row = poSplitAsnMapper.findDetailRow(detailId, orderId);
        return Optional.ofNullable(row);
    }

    public boolean existsPositiveShippedForDetail(long detailId) {
        return poSplitAsnMapper.existsPositiveShippedForDetail(detailId);
    }

    public int deleteSplitsWithZeroShippedByDetail(long detailId) {
        return poSplitAsnMapper.deleteSplitsWithZeroShippedByDetail(detailId);
    }

    public long insertSplitLine(long orderId, long detailId, int splitQty, LocalDate committedDate, String status) {
        return poSplitAsnMapper.insertSplitLine(orderId, detailId, splitQty, committedDate, status);
    }

    public List<Map<String, Object>> listSplitLinesByOrder(long orderId) {
        return poSplitAsnMapper.listSplitLinesByOrder(orderId);
    }

    public Optional<Map<String, Object>> lockSplitLine(long splitLineId) {
        Map<String, Object> row = poSplitAsnMapper.lockSplitLine(splitLineId);
        return Optional.ofNullable(row);
    }

    public void addSplitShippedQty(long splitLineId, int delta) {
        poSplitAsnMapper.addSplitShippedQty(splitLineId, delta);
    }

    public void addSplitReceivedQty(long splitLineId, int delta) {
        poSplitAsnMapper.addSplitReceivedQty(splitLineId, delta);
    }

    public void updateSplitStatus(long splitLineId, String status) {
        poSplitAsnMapper.updateSplitStatus(splitLineId, status);
    }

    public void refreshSplitRollupStatus(long splitLineId) {
        List<Map<String, Object>> rows = poSplitAsnMapper.findSplitLineForRefresh(splitLineId);
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> r = rows.get(0);
        int splitQty = toInt(r.get("split_qty"));
        int shipped = toInt(r.get("shipped_qty"));
        int received = toInt(r.get("received_qty"));
        String status;
        if (received >= splitQty) {
            status = "RECEIPT_COMPLETE";
        } else if (received > 0) {
            status = "PARTIAL_RECEIVED";
        } else if (shipped >= splitQty) {
            status = "SHIPPED_COMPLETE";
        } else if (shipped > 0) {
            status = "PARTIAL_SHIPPED";
        } else {
            status = "PLANNED";
        }
        updateSplitStatus(splitLineId, status);
    }

    public long insertAsnLine(long asnId, long splitLineId, int shippedQty) {
        return poSplitAsnMapper.insertAsnLine(asnId, splitLineId, shippedQty);
    }

    public Optional<Map<String, Object>> findAsnLineById(long asnLineId) {
        Map<String, Object> row = poSplitAsnMapper.findAsnLineById(asnLineId);
        return Optional.ofNullable(row);
    }

    public Optional<Map<String, Object>> lockAsnLine(long asnLineId) {
        Map<String, Object> row = poSplitAsnMapper.lockAsnLine(asnLineId);
        return Optional.ofNullable(row);
    }

    public void addAsnLineReceived(long asnLineId, int delta, LocalDateTime receiptTime) {
        poSplitAsnMapper.addAsnLineReceived(asnLineId, delta, receiptTime);
    }

    public List<Map<String, Object>> listAsnLinesByAsnId(long asnId) {
        return poSplitAsnMapper.listAsnLinesByAsnId(asnId);
    }

    public Optional<Long> findOrderIdBySplitLineId(long splitLineId) {
        Long oid = poSplitAsnMapper.findOrderIdBySplitLineId(splitLineId);
        return Optional.ofNullable(oid);
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString());
    }
}