package org.utplsql.maven.plugin.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;

import lombok.Getter;
import oracle.jdbc.pool.OracleDataSource;

@Named
@Singleton
public class DatabaseConnection {

    @Getter
    private static final Connection instance;

    static {
        instance = getConnection();
    }

    private static Connection getConnection() {

        try {
            final OracleDataSource dataSource = new OracleDataSource();
            dataSource.setURL(System.getProperty("dbUrl"));
            dataSource.setUser(System.getProperty("dbUser"));
            dataSource.setPassword(System.getProperty("dbPass"));

            if (StringUtils.isAnyEmpty(dataSource.getURL(), dataSource.getUser())) {
                throw new UtPLSQLMojoException("Missing connection credentials.");
            }

            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new UtPLSQLMojoException("Failed to create the database connection.", e);
        }
    }
}
