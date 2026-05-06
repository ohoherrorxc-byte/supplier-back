package srm.portal.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckUserPassword {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java CheckUserPassword <userId>");
            return;
        }

        String userId = args[0];
        String url = "jdbc:mysql://localhost:3306/srm_portal?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT password FROM blade_user WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        System.out.println("User ID: " + userId);
                        System.out.println("Password in DB: " + dbPassword);
                        System.out.println("Password length: " + (dbPassword != null ? dbPassword.length() : 0));
                    } else {
                        System.out.println("User not found: " + userId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}