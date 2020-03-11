package org.utplsql.maven.plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.utplsql.api.FileMapperOptions;
import org.utplsql.api.JavaApiVersionInfo;
import org.utplsql.api.TestRunner;
import org.utplsql.api.Version;
import org.utplsql.api.db.DatabaseInformation;
import org.utplsql.api.exception.SomeTestsFailedException;
import org.utplsql.api.reporter.Reporter;
import org.utplsql.maven.plugin.db.DatabaseConnection;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;
import org.utplsql.maven.plugin.filemapping.FileMapperOptionsFactory;
import org.utplsql.maven.plugin.filemapping.FileMappingParams;
import org.utplsql.maven.plugin.filemapping.SourceType;
import org.utplsql.maven.plugin.reporter.ReporterParameter;
import org.utplsql.maven.plugin.reporter.ReporterService;

/**
 * This class expose the {@link TestRunner} interface to Maven.
 * 
 * @author Alberto Hernández, Vinicius Avellar
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class UtPLSQLMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Parameter
    private String includeObject;

    @Parameter
    private String excludeObject;

    @Parameter(defaultValue = "false")
    private boolean skipCompatibilityCheck;

    @Parameter
    private List<ReporterParameter> reporters;

    @Parameter
    private List<String> paths = new ArrayList<>();

    @Parameter
    private FileMappingParams sourceFileMapping;

    @Parameter
    private FileMappingParams testFileMapping;

    @Parameter
    private Set<String> tags = new LinkedHashSet<>();

    @Parameter
    private boolean randomTestOrder;

    @Parameter
    private Integer randomTestOrderSeed;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String targetDir;

    @Parameter(defaultValue = "${maven.test.failure.ignore}")
    private boolean ignoreFailure;

    // Color in the console, bases on Maven logging configuration.
    private boolean colorConsole = MessageUtils.isColorEnabled();

    @Inject
    private DatabaseInformation databaseInformation;

    @Inject
    private ReporterService reporterService;

    @Inject
    private FileMapperOptionsFactory fileMapperOptionsFactory;

    /**
     * Executes the plugin.
     */
    @Override
    public void execute() throws MojoExecutionException {

        try (Connection databaseConnection = DatabaseConnection.getInstance()) {

            this.assignDefaults();
            this.executePlugin(databaseConnection);

        } catch (UtPLSQLMojoException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        } catch (SomeTestsFailedException e) {
            if (!this.ignoreFailure) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void assignDefaults() {

        if (Objects.isNull(this.reporters)) {
            this.reporters = Arrays.asList(ReporterParameter.defaultReporter());
        }

        if (Objects.isNull(this.sourceFileMapping)) {
            this.sourceFileMapping = FileMappingParams.forSourceType(
                    SourceType.SOURCE, this.mavenProject.getBasedir());
        }

        if (Objects.isNull(this.testFileMapping)) {
            this.testFileMapping = FileMappingParams.forSourceType(
                    SourceType.TEST, this.mavenProject.getBasedir());
        }
    }

    private void executePlugin(final Connection databaseConnection) throws SQLException {

        getLog().debug("Java Api Version = " + JavaApiVersionInfo.getVersion());

        final List<Pair<Reporter, ReporterParameter>> reporterPairList = this.reporters.stream()
                .map(reporterParameter -> this.reporterService.initReporter(databaseConnection, reporterParameter))
                .collect(Collectors.toList());

        final List<Reporter> utplsqlReporterList = reporterPairList.stream()
                .collect(Collectors.mapping(Pair::getLeft, Collectors.toList()));

        Version utlVersion = this.databaseInformation.getUtPlsqlFrameworkVersion(databaseConnection);
        getLog().info("utPLSQL Version = " + utlVersion);

        final FileMapperOptions sourceMappingOptions = this.fileMapperOptionsFactory.get(
                this.sourceFileMapping, SourceType.SOURCE, this.mavenProject);

        final FileMapperOptions testMappingOptions = this.fileMapperOptionsFactory.get(
                this.testFileMapping, SourceType.TEST, this.mavenProject);

        this.logParameters(sourceMappingOptions, testMappingOptions, utplsqlReporterList);

        final TestRunner testRunner = new TestRunner()
                .addPathList(this.paths)
                .addReporterList(utplsqlReporterList)
                .sourceMappingOptions(sourceMappingOptions)
                .testMappingOptions(testMappingOptions)
                .skipCompatibilityCheck(this.skipCompatibilityCheck)
                .colorConsole(this.colorConsole)
                .addTags(this.tags)
                .randomTestOrder(this.randomTestOrder)
                .randomTestOrderSeed(this.randomTestOrderSeed)
                .failOnErrors(!this.ignoreFailure);

        if (StringUtils.isNotBlank(this.excludeObject)) {
            testRunner.excludeObject(this.excludeObject);
        }
        if (StringUtils.isNotBlank(this.includeObject)) {
            testRunner.includeObject(this.includeObject);
        }

        testRunner.run(databaseConnection);

        reporterPairList.forEach(reporterPair -> this.reporterService.writeReporter(
                databaseConnection, this.targetDir, reporterPair));
    }

    private void logParameters(
            final FileMapperOptions sourceMappingOptions,
            final FileMapperOptions testMappingOptions,
            final List<Reporter> reporterList) {

        final Log log = getLog();
        log.info("Invoking TestRunner with: " + this.targetDir);

        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("Invoking TestRunner with: ");
        log.debug("reporters=");
        reporterList.forEach(reporter -> log.debug(reporter.getTypeName()));
        log.debug("sources=");
        sourceMappingOptions.getFilePaths().forEach(log::debug);
        log.debug("tests=");
        testMappingOptions.getFilePaths().forEach(log::debug);
    }
}