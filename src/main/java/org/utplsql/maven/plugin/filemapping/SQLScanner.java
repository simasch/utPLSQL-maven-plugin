package org.utplsql.maven.plugin.filemapping;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Resource;

/**
 * Utility to scan all resources.
 * 
 * @author Alberto Hernández, Vinicius Avellar
 */
public interface SQLScanner {

    List<String> findSQLs(
            File baseDir,
            List<Resource> resourceList,
            SourceType sourceType);
}
