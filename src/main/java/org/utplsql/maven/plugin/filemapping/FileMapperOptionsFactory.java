package org.utplsql.maven.plugin.filemapping;

import org.apache.maven.project.MavenProject;
import org.utplsql.api.FileMapperOptions;

public interface FileMapperOptionsFactory {

    FileMapperOptions get(
            FileMappingParams fileMappingParams,
            SourceType sourceType,
            MavenProject mavenProject);
}
