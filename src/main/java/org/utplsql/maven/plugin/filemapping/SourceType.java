package org.utplsql.maven.plugin.filemapping;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SourceType {

    SOURCE("src/main/plsql", "**/*.*"),
    TEST("src/main/plsql", "**/*.pkg");

    @Getter
    private String directory;

    @Getter
    private String pattern;
}
