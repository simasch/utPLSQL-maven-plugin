package org.utplsql.maven.plugin.filemapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Resource;
import org.utplsql.maven.plugin.common.CustomTypeMapping;

import lombok.Data;

@Data
public class FileMappingParams {

    private List<Resource> resources;

    private String owner;

    private String regexExpression;

    private Integer ownerSubexpression;

    private Integer nameSubexpression;

    private Integer typeSubexpression;

    private List<CustomTypeMapping> customTypeMappings;

    public static FileMappingParams forSourceType(final SourceType sourceType, final File baseDir) {

        final FileMappingParams fileMappingParams = new FileMappingParams();
        fileMappingParams.setResources(new ArrayList<>());

        final File defaultSourceDirectory = new File(baseDir, sourceType.getDirectory());
        if (defaultSourceDirectory.exists()) {

            final Resource defaultResource = new Resource();
            defaultResource.setDirectory(sourceType.getDirectory());
            defaultResource.setIncludes(Arrays.asList(sourceType.getPattern()));
            fileMappingParams.getResources().add(defaultResource);
        }

        return fileMappingParams;
    }
}
