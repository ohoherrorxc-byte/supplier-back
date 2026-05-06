package srm.portal.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    private final JdbcTemplate jdbcTemplate;

    public TestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/check-asn-tables")
    public ResponseEntity<Map<String, Object>> checkAsnTables() {
        try {
            // 检查 tb_srm_asn 表是否存在
            boolean asnTableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'license' AND table_name = 'tb_srm_asn'",
                    Integer.class) > 0;

            // 检查 tb_srm_asn_order 表是否存在
            boolean asnOrderTableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'license' AND table_name = 'tb_srm_asn_order'",
                    Integer.class) > 0;

            // 检查 tb_srm_asn_timeline 表是否存在
            boolean asnTimelineTableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'license' AND table_name = 'tb_srm_asn_timeline'",
                    Integer.class) > 0;

            return ResponseEntity.ok(Map.of(
                    "tb_srm_asn", asnTableExists,
                    "tb_srm_asn_order", asnOrderTableExists,
                    "tb_srm_asn_timeline", asnTimelineTableExists
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}