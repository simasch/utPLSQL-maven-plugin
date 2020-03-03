package org.utplsql.maven.plugin.reporter.impl;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.utplsql.api.Version;
import org.utplsql.api.db.DatabaseInformation;
import org.utplsql.api.outputBuffer.OutputBuffer;
import org.utplsql.api.outputBuffer.OutputBufferProvider;
import org.utplsql.api.reporter.Reporter;
import org.utplsql.api.reporter.ReporterFactory;
import org.utplsql.maven.plugin.db.DatabaseInformationProvider;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;
import org.utplsql.maven.plugin.reporter.ReporterFactoryProvider;
import org.utplsql.maven.plugin.reporter.ReporterParameter;
import org.utplsql.maven.plugin.reporter.ReporterService;

@Named
@Singleton
public final class DefaultReporterService implements ReporterService {

    private static final Log LOG = new SystemStreamLog();

    private DatabaseInformation databaseInformation;

    private ReporterFactory reporterFactory;

    @Inject
    public DefaultReporterService(
            final DatabaseInformationProvider databaseInformationProvider,
            final ReporterFactoryProvider reporterFactoryProvider) {

        this.databaseInformation = databaseInformationProvider.get();
        this.reporterFactory = reporterFactoryProvider.get();
    }

    public Pair<Reporter, ReporterParameter> initReporter(
            final Connection databaseConnection,
            final ReporterParameter reporterParameter) {

        try {
            final Reporter utplsqlReporter = this.reporterFactory.createReporter(reporterParameter.getName());
            utplsqlReporter.init(databaseConnection);
            return Pair.of(utplsqlReporter, reporterParameter);
        } catch (SQLException e) {
            throw new UtPLSQLMojoException(format("Failed to initialize reporter %s.", reporterParameter.getName()), e);
        }
    }

    public void writeReporter(
            final Connection databaseConnection,
            final String targetDir,
            final Pair<Reporter, ReporterParameter> reporterPair) {

        final List<PrintStream> printStreams = new ArrayList<>();
        final Reporter utplsqlReporter = reporterPair.getLeft();
        final ReporterParameter reporterParameter = reporterPair.getRight();
        PrintStream filePrintStream = null;

        try {
            final Version frameworkVersion = this.databaseInformation.getUtPlsqlFrameworkVersion(databaseConnection);
            final OutputBuffer outputBuffer = OutputBufferProvider.getCompatibleOutputBuffer(
                    frameworkVersion, utplsqlReporter, databaseConnection);

            if (reporterParameter.isFileOutput()) {
                filePrintStream = this.getFilePrintStream(reporterParameter, targetDir);
                printStreams.add(filePrintStream);
            }

            if (Boolean.TRUE.equals(reporterParameter.isConsoleOutput())) {
                LOG.info(format("Writing report %s to Console.", reporterParameter.getName()));
                printStreams.add(System.out);
            }

            outputBuffer.printAvailable(databaseConnection, printStreams);
        } catch (Exception e) {
            throw new UtPLSQLMojoException("Unexpected error opening file ouput.", e);
        } finally {
            if (filePrintStream != null) {
                filePrintStream.close();
            }
        }

    }

    private PrintStream getFilePrintStream(final ReporterParameter reporterParameter, final String targetDir)
            throws FileNotFoundException {

        File outputFile = new File(reporterParameter.getFileOutput());
        if (!outputFile.isAbsolute()) {
            outputFile = new File(targetDir, reporterParameter.getFileOutput());
        }

        if (!outputFile.getParentFile().exists()) {
            LOG.debug(format("Creating directory for reporter file %s.", outputFile.getAbsolutePath()));
            outputFile.getParentFile().mkdirs();
        }

        final OutputStream outputStream = new FileOutputStream(outputFile);
        LOG.info(format("Writing report %s to %s.",
                reporterParameter.getName(), outputFile.getAbsolutePath()));

        return new PrintStream(outputStream);
    }
}
