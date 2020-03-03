package org.utplsql.maven.plugin.reporter;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.utplsql.api.reporter.ReporterFactory;

@Named
@Singleton
public final class ReporterFactoryProvider implements Provider<ReporterFactory> {

    @Override
    public ReporterFactory get() {
        return ReporterFactory.createEmpty();
    }
}
