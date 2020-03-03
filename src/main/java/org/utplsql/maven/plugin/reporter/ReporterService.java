package org.utplsql.maven.plugin.reporter;

import java.sql.Connection;

import org.apache.commons.lang3.tuple.Pair;
import org.utplsql.api.reporter.Reporter;

public interface ReporterService {

    Pair<Reporter, ReporterParameter> initReporter(
            Connection databaseConnection,
            ReporterParameter reporterParameter);

    /**
     * Writes the reporters to the output.
     * 
     * @param databaseConnection the database connection
     * @param targetDir          the output directory
     * @param reporterPair       details about the reporter
     */
    void writeReporter(
            final Connection databaseConnection,
            final String targetDir,
            final Pair<Reporter, ReporterParameter> reporterPair);
}
