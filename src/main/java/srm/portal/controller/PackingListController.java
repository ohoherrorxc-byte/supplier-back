package srm.portal.controller;

import org.springframework.web.bind.annotation.*;
import srm.portal.dto.PackingListRequest;
import srm.portal.service.PackingListService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration/packing")
public class PackingListController {

    private final PackingListService packingListService;

    public PackingListController(PackingListService packingListService) {
        this.packingListService = packingListService;
    }

    /**
     * 查询某 ASN 的所有箱单
     */
    @GetMapping("/asn/{asnId}")
    public List<Map<String, Object>> listByAsn(@PathVariable long asnId) {
        return packingListService.listByAsnId(asnId);
    }

    /**
     * 获取箱单详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getDetail(@PathVariable long id) {
        return packingListService.getPackingListDetail(id);
    }

    /**
     * 创建箱单
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody PackingListRequest req) {
        return packingListService.createPackingList(req);
    }

    /**
     * 删除箱单
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        packingListService.deletePackingList(id);
    }
}
