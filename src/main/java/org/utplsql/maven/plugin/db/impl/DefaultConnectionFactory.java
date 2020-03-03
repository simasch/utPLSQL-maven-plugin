package org.utplsql.maven.plugin.db.impl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.utplsql.maven.plugin.db.ConnectionFactory;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;

import lombok.Data;
import oracle.jdbc.pool.OracleDataSource;

@Named
@Singleton
public class DefaultConnectionFactory implements ConnectionFactory {

    @Override
    public Connection getConnection() {

        final ConnectionCredentials connectionCredentials = this.getConnectionCredentials();
        if (StringUtils.isAnyEmpty(
                connectionCredentials.getDbUrl(),
                connectionCredentials.getDbUser(),
                connectionCredentials.getDbPass())) {
            throw new UtPLSQLMojoException("Missing connection credentials.");
        }

        try {
            final OracleDataSource dataSource = new OracleDataSource();
            dataSource.setURL(connectionCredentials.getDbUrl());
            dataSource.setUser(connectionCredentials.getDbUser());
            dataSource.setPassword(connectionCredentials.getDbPass());
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new UtPLSQLMojoException("Failed to create the database connection.", e);
        }
    }

    private ConnectionCredentials getConnectionCredentials() {

        final ConnectionCredentials connectionCredentials = new ConnectionCredentials();
        connectionCredentials.setDbUrl(System.getProperty("dbUrl"));
        connectionCredentials.setDbUser(System.getProperty("dbUser"));
        connectionCredentials.setDbPass(System.getProperty("dbPass"));
        return connectionCredentials;
    }

    @Data
    private static final class ConnectionCredentials {

        private String dbUrl;
        private String dbUser;
        private String dbPass;
    }
}
