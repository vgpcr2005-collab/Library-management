package edu.alliance.library.dao;

import edu.alliance.library.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LibraryOperatorDao {
    public long operatorId() throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM users WHERE email=? AND role='ADMIN' AND password_hash IS NULL")) {
            statement.setString(1, "local-library@localhost");
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The library system operator account is missing.");
                }
                return result.getLong("id");
            }
        }
    }
}
