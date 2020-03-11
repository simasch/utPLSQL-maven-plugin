package org.utplsql.maven.plugin.filemapping.impl;

import static java.lang.String.format;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.utplsql.maven.plugin.filemapping.SQLScanner;
import org.utplsql.maven.plugin.filemapping.SourceType;

@Named
@Singleton
public class DefaultSQLScanner implements SQLScanner {

    public List<String> findSQLs(
            final File baseDir,
            final List<Resource> resourceList,
            final SourceType sourceType) {

        return resourceList.stream()
                .flatMap(fileResource -> this.scanDirectory(fileResource, baseDir, sourceType).stream())
                .collect(Collectors.toList());
    }

    private List<String> scanDirectory(
            final Resource fileResource,
            final File baseDir,
            final SourceType sourceType) {

        if (Objects.isNull(fileResource.getDirectory())) {
            fileResource.setDirectory(sourceType.getDirectory());
        }

        if (fileResource.getIncludes().isEmpty()) {
            fileResource.getIncludes().add(sourceType.getPattern());
        }

        final DirectoryScanner directoryScanner = this.buildScanner(baseDir.getPath(), fileResource);
        directoryScanner.scan();

        return Arrays.asList(directoryScanner.getIncludedFiles())
                .stream()
                .map(sqlPath -> this.normalizePath(directoryScanner, baseDir, sqlPath))
                .collect(Collectors.toList());

    }

    private String normalizePath(
            final DirectoryScanner directoryScanner,
            final File baseDir,
            final String sqlPath) {

        return baseDir.toURI().relativize(new File(directoryScanner.getBasedir(), sqlPath).toURI()).getPath();
    }

    private DirectoryScanner buildScanner(final String baseDir, final Resource fileResource) {

        final File fileBaseDir = new File(baseDir, fileResource.getDirectory());

        if (!fileBaseDir.exists() || !fileBaseDir.isDirectory() || !fileBaseDir.canRead()) {
            throw new IllegalArgumentException(
                    format("Invalid <directory> %s in resource. Check your pom.xml", fileResource.getDirectory()));
        }

        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(fileBaseDir.getPath());
        directoryScanner.setIncludes(fileResource.getIncludes().toArray(new String[0]));
        directoryScanner.setExcludes(fileResource.getExcludes().toArray(new String[0]));
        return directoryScanner;
    }
}
