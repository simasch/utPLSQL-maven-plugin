package org.utplsql.maven.plugin.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class provides methods to retrieve the list of resources in the default
 * {@literal <source> and <test>} directories.
 * 
 * @author Alberto Hernández
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginDefault {

    /**
     * Source directory.
     */
    public static final String SOURCE_DIRECTORY = "src/main/plsql";

    /**
     * Test directory.
     */
    public static final String TEST_DIRECTORY = "src/test/plsql";

    /**
     * Default source file pattern.
     */
    public static final String SOURCE_FILE_PATTERN = "**/*.*";

    /**
     * Default test file pattern.
     */
    public static final String TEST_FILE_PATTERN = "**/*.pkg";
}
