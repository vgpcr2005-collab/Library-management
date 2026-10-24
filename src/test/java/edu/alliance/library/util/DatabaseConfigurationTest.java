package edu.alliance.library.util;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseConfigurationTest {
    @Test
    void acceptsHostedMysqlUrlWithRequiredTlsAndExplicitDatabasePort() {
        assertDoesNotThrow(() -> Database.validateProductionUrl(
                "jdbc:mysql://mysql.example.net:3306/alliance_library?sslMode=VERIFY_IDENTITY&serverTimezone=UTC"));
    }

    @Test
    void rejectsProductionUrlsWithoutDatabasePortOrRequiredTls() {
        assertThrows(SQLException.class, () -> Database.validateProductionUrl(
                "jdbc:mysql://mysql.example.net/alliance_library?sslMode=REQUIRED"));
        assertThrows(SQLException.class, () -> Database.validateProductionUrl(
                "jdbc:mysql://mysql.example.net:3306/alliance_library?sslMode=PREFERRED"));
    }
}
