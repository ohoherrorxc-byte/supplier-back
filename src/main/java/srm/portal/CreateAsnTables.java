package srm.portal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateAsnTables {
    public static void main(String[] args) {
        String url = "jdbc:mysql://10.30.254.73:3306/license?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&tinyInt1isBit=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000";
        String username = "root";
        String password = "KDB@wxoZg&9v";
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            // 创建 tb_srm_asn 表
            String createAsnTableSql = "CREATE TABLE IF NOT EXISTS `tb_srm_asn` (" +
                                      "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                                      "`asn_no` varchar(64) DEFAULT NULL COMMENT '发货通知单号，写入后一般为 ASN{id}'," +
                                      "`supplier_id` bigint(20) NOT NULL COMMENT '供应商ID，对应 tb_contract_supplier.ID'," +
                                      "`logistics_company` varchar(255) DEFAULT NULL COMMENT '物流公司'," +
                                      "`tracking_no` varchar(255) DEFAULT NULL COMMENT '运单号'," +
                                      "`eta_date` date DEFAULT NULL COMMENT '预计到货日'," +
                                      "`status` varchar(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态'," +
                                      "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                                      "`order_detail_id` bigint(20) DEFAULT NULL COMMENT '关联的订单详情ID'," +
                                      "`ship_qty` int(11) NOT NULL COMMENT '发货数量'," +
                                      "`actual_quantity` int(11) DEFAULT NULL COMMENT '实际发货数量'," +
                                      "`actual_eta_date` date DEFAULT NULL COMMENT '实际到货时间'," +
                                      "`ship_date` date DEFAULT NULL COMMENT '实际发货日期'," +
                                      "`remark` text COMMENT '备注'," +
                                      "`warehouse_in_no` varchar(255) DEFAULT NULL COMMENT '进仓单号'," +
                                      "`confirm_ship_time` datetime DEFAULT NULL COMMENT '确认发货时间'," +
                                      "PRIMARY KEY (`id`)," +
                                      "UNIQUE KEY `uk_tb_srm_asn_no` (`asn_no`)," +
                                      "KEY `idx_tb_srm_asn_supplier` (`supplier_id`)" +
                                      ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SRM发货通知(ASN)主表';";
            stmt.executeUpdate(createAsnTableSql);
            System.out.println("创建 tb_srm_asn 表成功");
            
            // 创建 tb_srm_asn_order 表
            String createAsnOrderTableSql = "CREATE TABLE IF NOT EXISTS `tb_srm_asn_order` (" +
                                           "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                                           "`asn_id` bigint(20) NOT NULL COMMENT 'ASN主表id'," +
                                           "`order_id` bigint(20) NOT NULL COMMENT '采购订单 tb_purchase_parts_order.id'," +
                                           "PRIMARY KEY (`id`)," +
                                           "KEY `idx_tb_srm_asn_order_asn` (`asn_id`)," +
                                           "KEY `idx_tb_srm_asn_order_order` (`order_id`)" +
                                           ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ASN关联采购订单';";
            stmt.executeUpdate(createAsnOrderTableSql);
            System.out.println("创建 tb_srm_asn_order 表成功");
            
            // 创建 tb_srm_asn_timeline 表
            String createAsnTimelineTableSql = "CREATE TABLE IF NOT EXISTS `tb_srm_asn_timeline` (" +
                                             "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                                             "`asn_id` bigint(20) NOT NULL COMMENT 'ASN主表id'," +
                                             "`event_code` varchar(64) DEFAULT NULL COMMENT '事件编码'," +
                                             "`description` varchar(512) DEFAULT NULL COMMENT '说明'," +
                                             "`event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间'," +
                                             "PRIMARY KEY (`id`)," +
                                             "KEY `idx_tb_srm_asn_timeline_asn` (`asn_id`)" +
                                             ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ASN状态时间轴';";
            stmt.executeUpdate(createAsnTimelineTableSql);
            System.out.println("创建 tb_srm_asn_timeline 表成功");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}