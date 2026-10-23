package edu.alliance.library.dao;

import edu.alliance.library.util.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SettingsDao {
    public Map<String, String> all() throws SQLException {
        Map<String, String> values = new HashMap<>();
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT setting_key,setting_value FROM library_settings");
             ResultSet r = p.executeQuery()) {
            while (r.next()) values.put(r.getString(1), r.getString(2));
        }
        return values;
    }

    public String get(Connection c, String key, String fallback) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT setting_value FROM library_settings WHERE setting_key=?")) {
            p.setString(1, key);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : fallback; }
        }
    }

    public Map<String, String> allInConnection(Connection c) throws SQLException {
        Map<String, String> values = new HashMap<>();
        try (PreparedStatement p = c.prepareStatement("SELECT setting_key,setting_value FROM library_settings");
             ResultSet r = p.executeQuery()) {
            while (r.next()) values.put(r.getString(1), r.getString(2));
        }
        return values;
    }

    public void update(String loanDays, String finePerDay, String borrowingLimit) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (String[] entry : new String[][]{{"loan_days", loanDays}, {"fine_per_day", finePerDay}, {"borrowing_limit", borrowingLimit}}) {
                    try (PreparedStatement update = c.prepareStatement(
                            "UPDATE library_settings SET setting_value=? WHERE setting_key=?")) {
                        update.setString(1, entry[1]);
                        update.setString(2, entry[0]);
                        if (update.executeUpdate() == 0) {
                            try (PreparedStatement insert = c.prepareStatement(
                                    "INSERT INTO library_settings(setting_key,setting_value) VALUES(?,?)")) {
                                insert.setString(1, entry[0]);
                                insert.setString(2, entry[1]);
                                insert.executeUpdate();
                            }
                        }
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
