package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlacklistService {
    private static Connection connection;
    private static Statement stmt;

    public static void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getBlacklist(String nick) {
        String sql = String.format("SELECT bl_user FROM blacklist WHERE username = '%s'", nick);
        List<String> list = new ArrayList<>();
        try {
            ResultSet rs;
            synchronized (BlacklistService.class) {
                rs = stmt.executeQuery(sql);
            }

            while(rs.next()) {
                list.add(rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void addToBlacklist(String user, String bl) {
        String sql = String.format("INSERT INTO blacklist VALUES ('%s', '%s')", user, bl);
        try {
            synchronized (BlacklistService.class) {
                stmt.executeUpdate(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeFromBlacklist(String user, String bl) {
        String sql = String.format("DELETE FROM blacklist WHERE username = '%s' AND bl_user = '%s'", user, bl);
        try {
            synchronized (BlacklistService.class) {
                stmt.executeUpdate(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
