package srm.portal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AddSupplierOrderStatus {
    public static void main(String[] args) {
        String url = "jdbc:mysql://10.30.254.73:3306/license?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&tinyInt1isBit=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000";
        String username = "root";
        String password = "KDB@wxoZg&9v";
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            // 检查字段是否存在
            String checkColumnSql = "SHOW COLUMNS FROM tb_purchase_parts_order LIKE 'supplier_order_status'";
            ResultSet rs = stmt.executeQuery(checkColumnSql);
            
            if (!rs.next()) {
                // 添加 supplier_order_status 字段
                String addColumnSql = "ALTER TABLE `tb_purchase_parts_order` " +
                                    "ADD COLUMN `supplier_order_status` int(2) unsigned zerofill DEFAULT '00' COMMENT '供应商订单状态：0-待确认，20-已确认，25-部分发运，30-已发运，40-已结案' " +
                                    "AFTER `order_status`";
                stmt.executeUpdate(addColumnSql);
                System.out.println("添加字段成功");
            } else {
                System.out.println("字段已存在");
            }
            
            // 复制 order_status 的值到 supplier_order_status
            String updateSql = "UPDATE `tb_purchase_parts_order` " +
                            "SET `supplier_order_status` = `order_status` " +
                            "WHERE `order_status` != 0";
            int rows = stmt.executeUpdate(updateSql);
            System.out.println("更新数据成功，影响行数：" + rows);
            
            // 验证更新结果
            String verifySql = "SELECT id, order_no, order_status, supplier_order_status " +
                            "FROM `tb_purchase_parts_order` " +
                            "WHERE supplier_id = 25 " +
                            "LIMIT 10";
            rs = stmt.executeQuery(verifySql);
            System.out.println("验证结果：");
            System.out.println("id, order_no, order_status, supplier_order_status");
            while (rs.next()) {
                System.out.println(rs.getLong("id") + ", " + 
                                 rs.getString("order_no") + ", " + 
                                 rs.getInt("order_status") + ", " + 
                                 rs.getInt("supplier_order_status"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}