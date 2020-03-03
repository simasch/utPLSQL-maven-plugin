package org.utplsql.maven.plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Resource;
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
import org.utplsql.api.KeyValuePair;
import org.utplsql.api.TestRunner;
import org.utplsql.api.Version;
import org.utplsql.api.db.DatabaseInformation;
import org.utplsql.api.exception.SomeTestsFailedException;
import org.utplsql.api.reporter.Reporter;
import org.utplsql.maven.plugin.common.CustomTypeMapping;
import org.utplsql.maven.plugin.common.PluginDefault;
import org.utplsql.maven.plugin.common.SQLScannerHelper;
import org.utplsql.maven.plugin.db.ConnectionFactory;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;
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
    private MavenProject project;

    @Parameter
    private String includeObject;

    @Parameter
    private String excludeObject;

    @Parameter(defaultValue = "false")
    private boolean skipCompatibilityCheck;

    @Parameter
    private List<ReporterParameter> reporters = new ArrayList<>();

    @Parameter
    private List<String> paths = new ArrayList<>();

    // Sources Configuration
    @Parameter
    private List<Resource> sources = new ArrayList<>();

    @Parameter
    private String sourcesOwner;

    @Parameter
    private String sourcesRegexExpression;

    @Parameter
    private Integer sourcesOwnerSubexpression;

    @Parameter
    private Integer sourcesNameSubexpression;

    @Parameter
    private Integer sourcesTypeSubexpression;

    @Parameter
    private List<CustomTypeMapping> sourcesCustomTypeMapping;

    // Tests Configuration
    @Parameter
    private List<Resource> tests = new ArrayList<>();

    @Parameter
    private String testsOwner;

    @Parameter
    private String testsRegexExpression;

    @Parameter
    private Integer testsOwnerSubexpression;

    @Parameter
    private Integer testsNameSubexpression;

    @Parameter
    private Integer testsTypeSubexpression;

    @Parameter
    private List<CustomTypeMapping> testsCustomTypeMapping;

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

    private ConnectionFactory connectionFactory;

    private DatabaseInformation databaseInformation;

    private ReporterService reporterService;

    @Inject
    public UtPLSQLMojo(
            final ConnectionFactory connectionFactory,
            final Provider<DatabaseInformation> databaseInformationProvider,
            final ReporterService reporterWriter) {

        this.connectionFactory = connectionFactory;
        this.databaseInformation = databaseInformationProvider.get();
        this.reporterService = reporterWriter;
    }

    /**
     * Executes the plugin.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            this.executePlugin();
        } catch (UtPLSQLMojoException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
    }

    private void executePlugin() {

        getLog().debug("Java Api Version = " + JavaApiVersionInfo.getVersion());
        final Connection databaseConnection = this.connectionFactory.getConnection();

        if (this.reporters.isEmpty()) {
            this.reporters.add(ReporterParameter.defaultReporter());
        }

        final List<Pair<Reporter, ReporterParameter>> reporterPairList = this.reporters.stream()
                .map(reporterParameter -> this.reporterService.initReporter(databaseConnection, reporterParameter))
                .collect(Collectors.toList());

        final List<Reporter> utplsqlReporterList = reporterPairList.stream()
                .collect(Collectors.mapping(Pair::getLeft, Collectors.toList()));

        try {
            Version utlVersion = this.databaseInformation.getUtPlsqlFrameworkVersion(databaseConnection);
            getLog().info("utPLSQL Version = " + utlVersion);

            final FileMapperOptions sourceMappingOptions = this.buildSourcesOptions();
            final FileMapperOptions testMappingOptions = this.buildTestsOptions();
            logParameters(sourceMappingOptions, testMappingOptions, utplsqlReporterList);

            TestRunner testRunner = new TestRunner()
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
        } catch (SomeTestsFailedException e) {
            if (!this.ignoreFailure) {
                throw new UtPLSQLMojoException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new UtPLSQLMojoException(e.getMessage(), e);
        } finally {
            try {
                if (null != databaseConnection) {
                    reporterPairList.forEach(reporterPair -> this.reporterService.writeReporter(
                            databaseConnection, this.targetDir, reporterPair));
                    databaseConnection.close();
                }
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        }
    }

    private FileMapperOptions buildSourcesOptions() {
        try {
            if (sources.isEmpty()) {
                File defaultSourceDirectory = new File(project.getBasedir(), PluginDefault.SOURCE_DIRECTORY);
                if (defaultSourceDirectory.exists()) {
                    sources.add(PluginDefault.buildDefaultSource());
                } else {
                    return new FileMapperOptions(new ArrayList<String>());
                }
            }

            List<String> scripts = SQLScannerHelper.findSQLs(project.getBasedir(), sources,
                    PluginDefault.SOURCE_DIRECTORY, PluginDefault.SOURCE_FILE_PATTERN);
            FileMapperOptions fileMapperOptions = new FileMapperOptions(scripts);

            if (StringUtils.isNotEmpty(sourcesOwner)) {
                fileMapperOptions.setObjectOwner(sourcesOwner);
            }

            if (StringUtils.isNotEmpty(sourcesRegexExpression)) {
                fileMapperOptions.setRegexPattern(sourcesRegexExpression);
            }

            if (sourcesOwnerSubexpression != null) {
                fileMapperOptions.setOwnerSubExpression(sourcesOwnerSubexpression);
            }

            if (sourcesNameSubexpression != null) {
                fileMapperOptions.setNameSubExpression(sourcesNameSubexpression);
            }

            if (sourcesTypeSubexpression != null) {
                fileMapperOptions.setTypeSubExpression(sourcesTypeSubexpression);
            }

            if (sourcesCustomTypeMapping != null && !sourcesCustomTypeMapping.isEmpty()) {
                fileMapperOptions.setTypeMappings(new ArrayList<KeyValuePair>());
                for (CustomTypeMapping typeMapping : sourcesCustomTypeMapping) {
                    fileMapperOptions.getTypeMappings()
                            .add(new KeyValuePair(typeMapping.getCustomMapping(), typeMapping.getType()));
                }
            }

            return fileMapperOptions;

        } catch (Exception e) {
            throw new UtPLSQLMojoException("Invalid <SOURCES> in your pom.xml", e);
        }

    }

    private FileMapperOptions buildTestsOptions() {
        try {
            if (tests.isEmpty()) {
                File defaultTestDirectory = new File(project.getBasedir(), PluginDefault.TEST_DIRECTORY);
                if (defaultTestDirectory.exists()) {
                    tests.add(PluginDefault.buildDefaultTest());
                } else {
                    return new FileMapperOptions(new ArrayList<String>());
                }
            }

            List<String> scripts = SQLScannerHelper.findSQLs(project.getBasedir(), tests, PluginDefault.TEST_DIRECTORY,
                    PluginDefault.TEST_FILE_PATTERN);
            FileMapperOptions fileMapperOptions = new FileMapperOptions(scripts);

            if (StringUtils.isNotEmpty(testsOwner)) {
                fileMapperOptions.setObjectOwner(testsOwner);
            }

            if (StringUtils.isNotEmpty(testsRegexExpression)) {
                fileMapperOptions.setRegexPattern(testsRegexExpression);
            }

            if (testsOwnerSubexpression != null) {
                fileMapperOptions.setOwnerSubExpression(testsOwnerSubexpression);
            }

            if (testsNameSubexpression != null) {
                fileMapperOptions.setNameSubExpression(testsNameSubexpression);
            }

            if (testsTypeSubexpression != null) {
                fileMapperOptions.setTypeSubExpression(testsTypeSubexpression);
            }

            if (testsCustomTypeMapping != null && !testsCustomTypeMapping.isEmpty()) {
                fileMapperOptions.setTypeMappings(new ArrayList<KeyValuePair>());
                for (CustomTypeMapping typeMapping : testsCustomTypeMapping) {
                    fileMapperOptions.getTypeMappings()
                            .add(new KeyValuePair(typeMapping.getCustomMapping(), typeMapping.getType()));
                }
            }

            return fileMapperOptions;

        } catch (Exception e) {
            throw new UtPLSQLMojoException("Invalid <TESTS> in your pom.xml: " + e.getMessage());
        }

    }

    private void logParameters(FileMapperOptions sourceMappingOptions, FileMapperOptions testMappingOptions,
            List<Reporter> reporterList) {
        Log log = getLog();
        log.info("Invoking TestRunner with: " + targetDir);

        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("Invoking TestRunner with: ");
        log.debug("reporters=");
        reporterList.forEach((Reporter r) -> log.debug(r.getTypeName()));
        log.debug("sources=");
        sourceMappingOptions.getFilePaths().forEach(log::debug);
        log.debug("tests=");
        testMappingOptions.getFilePaths().forEach(log::debug);
    }
}