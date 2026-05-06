package srm.portal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckAsnTables {
    public static void main(String[] args) {
        String url = "jdbc:mysql://10.30.254.73:3306/license?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&tinyInt1isBit=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000";
        String username = "root";
        String password = "KDB@wxoZg&9v";
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            // 检查 tb_srm_asn 表是否存在
            String checkAsnTableSql = "SHOW TABLES LIKE 'tb_srm_asn';";
            ResultSet rs = stmt.executeQuery(checkAsnTableSql);
            if (rs.next()) {
                System.out.println("tb_srm_asn 表存在");
            } else {
                System.out.println("tb_srm_asn 表不存在");
            }
            
            // 检查 tb_srm_asn_order 表是否存在
            String checkAsnOrderTableSql = "SHOW TABLES LIKE 'tb_srm_asn_order';";
            rs = stmt.executeQuery(checkAsnOrderTableSql);
            if (rs.next()) {
                System.out.println("tb_srm_asn_order 表存在");
            } else {
                System.out.println("tb_srm_asn_order 表不存在");
            }
            
            // 检查 tb_srm_asn_timeline 表是否存在
            String checkAsnTimelineTableSql = "SHOW TABLES LIKE 'tb_srm_asn_timeline';";
            rs = stmt.executeQuery(checkAsnTimelineTableSql);
            if (rs.next()) {
                System.out.println("tb_srm_asn_timeline 表存在");
            } else {
                System.out.println("tb_srm_asn_timeline 表不存在");
            }
            
            // 检查 tb_srm_po_split_line 表是否存在
            String checkPoSplitLineTableSql = "SHOW TABLES LIKE 'tb_srm_po_split_line';";
            rs = stmt.executeQuery(checkPoSplitLineTableSql);
            if (rs.next()) {
                System.out.println("tb_srm_po_split_line 表存在");
            } else {
                System.out.println("tb_srm_po_split_line 表不存在");
            }
            
            // 检查 tb_srm_asn_line 表是否存在
            String checkAsnLineTableSql = "SHOW TABLES LIKE 'tb_srm_asn_line';";
            rs = stmt.executeQuery(checkAsnLineTableSql);
            if (rs.next()) {
                System.out.println("tb_srm_asn_line 表存在");
            } else {
                System.out.println("tb_srm_asn_line 表不存在");
            }
            
            // 检查 tb_srm_asn 表结构
            System.out.println("\ntb_srm_asn 表结构:");
            String descAsnTableSql = "DESCRIBE tb_srm_asn;";
            rs = stmt.executeQuery(descAsnTableSql);
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " " + rs.getString("Type") + " " + rs.getString("Null") + " " + rs.getString("Default") + " " + rs.getString("Extra"));
            }
            
            // 检查 tb_srm_po_split_line 表结构
            System.out.println("\ntb_srm_po_split_line 表结构:");
            String descPoSplitLineTableSql = "DESCRIBE tb_srm_po_split_line;";
            rs = stmt.executeQuery(descPoSplitLineTableSql);
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " " + rs.getString("Type") + " " + rs.getString("Null") + " " + rs.getString("Default") + " " + rs.getString("Extra"));
            }
            
            // 检查 tb_srm_asn_line 表结构
            System.out.println("\ntb_srm_asn_line 表结构:");
            String descAsnLineTableSql = "DESCRIBE tb_srm_asn_line;";
            rs = stmt.executeQuery(descAsnLineTableSql);
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " " + rs.getString("Type") + " " + rs.getString("Null") + " " + rs.getString("Default") + " " + rs.getString("Extra"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
