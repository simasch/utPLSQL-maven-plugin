package org.utplsql.maven.plugin.reporter;

import org.codehaus.plexus.util.StringUtils;
import org.utplsql.api.reporter.CoreReporters;

import lombok.Data;

/**
 * Represents a reporter parameter in the pom file. {@code
 * <reporter>
 *     <name>...</name>
 *     <fileOutput>...</fileOutput>
 *     <consoleOutput>...</consoleOutput>
 * </reporter>
 * }
 * 
 * @author Alberto Hernández
 */
@Data
public class ReporterParameter {

    private String name;

    private String fileOutput;

    private Boolean consoleOutput;

    public static ReporterParameter defaultReporter() {
        final ReporterParameter reporterParameter = new ReporterParameter();
        reporterParameter.setConsoleOutput(true);
        reporterParameter.setName(CoreReporters.UT_DOCUMENTATION_REPORTER.name());
        return reporterParameter;
    }

    /**
     * Returns whether the file output is enabled or not.
     * 
     * @return true if the file output is enabled, false otherwise
     */
    public boolean isFileOutput() {
        return StringUtils.isNotBlank(fileOutput);
    }

    /**
     * Returns whether the console output should be enabled or not.
     * 
     * @return true if console output is enable, false otherwise
     */
    public Boolean isConsoleOutput() {
        return Boolean.TRUE.equals(consoleOutput) || !this.isFileOutput();
    }
}
