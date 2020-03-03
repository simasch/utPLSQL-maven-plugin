package org.utplsql.maven.plugin.db;

import java.sql.Connection;

public interface ConnectionFactory {

    Connection getConnection();
}
