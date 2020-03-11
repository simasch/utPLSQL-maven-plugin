package org.utplsql.maven.plugin.filemapping.impl;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.utplsql.api.FileMapperOptions;
import org.utplsql.api.KeyValuePair;
import org.utplsql.maven.plugin.common.CustomTypeMapping;
import org.utplsql.maven.plugin.exception.UtPLSQLMojoException;
import org.utplsql.maven.plugin.filemapping.FileMapperOptionsFactory;
import org.utplsql.maven.plugin.filemapping.FileMappingParams;
import org.utplsql.maven.plugin.filemapping.SQLScanner;
import org.utplsql.maven.plugin.filemapping.SourceType;

@Named
@Singleton
public class DefaultFileMapperOptionsFactory implements FileMapperOptionsFactory {

    @Inject
    private SQLScanner sqlScanner;

    public FileMapperOptions get(
            final FileMappingParams fileMappingParams,
            final SourceType sourceType,
            final MavenProject mavenProject) {

        final List<Resource> resourceList = fileMappingParams.getResources();
        if (resourceList.isEmpty()) {
            return new FileMapperOptions(new ArrayList<>());
        }

        try {
            final List<String> sqlScripts = this.sqlScanner.findSQLs(
                    mavenProject.getBasedir(), resourceList, sourceType);

            final FileMapperOptions fileMapperOptions = new FileMapperOptions(sqlScripts);
            fileMapperOptions.setObjectOwner(fileMappingParams.getOwner());
            fileMapperOptions.setRegexPattern(fileMappingParams.getRegexExpression());
            fileMapperOptions.setOwnerSubExpression(fileMappingParams.getOwnerSubexpression());
            fileMapperOptions.setNameSubExpression(fileMappingParams.getNameSubexpression());
            fileMapperOptions.setTypeSubExpression(fileMappingParams.getTypeSubexpression());
            fileMapperOptions.setTypeMappings(this.customTypeMapping(fileMappingParams.getCustomTypeMappings()));
            return fileMapperOptions;
        } catch (Exception e) {
            throw new UtPLSQLMojoException(format("Invalid <%s> in your pom.xml", sourceType), e);
        }
    }

    private List<KeyValuePair> customTypeMapping(final List<CustomTypeMapping> customTypeMappings) {
        return Optional.ofNullable(customTypeMappings)
                .orElse(Collections.emptyList())
                .stream()
                .map(typeMapping -> new KeyValuePair(typeMapping.getCustomMapping(), typeMapping.getType()))
                .collect(Collectors.toList());
    }
}
