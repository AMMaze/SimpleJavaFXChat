package server;

import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:DBUsers.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname FROM main where login = '%s' and password = '%s'", login, pass);

        try {
            ResultSet rs;
            synchronized (AuthService.class) {
                rs = stmt.executeQuery(sql);
            }
            if (rs.next()) {
                return rs.getString(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String addNewUser(String login, String nickname, String pass) {
        String sql = String.format("SELECT login, nickname FROM main WHERE login = '%s' AND nickname = '%s'", login, nickname);
        try {
            synchronized (AuthService.class) {
                ResultSet rs = stmt.executeQuery(sql);
                if (!rs.next()) {
                    int id = Integer.parseInt(stmt.executeQuery("SELECT COUNT(*) FROM main").getString(1));
                    stmt.executeUpdate(String.format("INSERT INTO main VALUES (%d, '%s', '%s', '%s')", id + 1, login, pass, nickname));
                    return nickname;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
