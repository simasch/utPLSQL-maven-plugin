package org.utplsql.maven.plugin.db;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.utplsql.api.db.DatabaseInformation;
import org.utplsql.api.db.DefaultDatabaseInformation;

@Named
@Singleton
public class DatabaseInformationProvider implements Provider<DatabaseInformation> {

    @Override
    public DatabaseInformation get() {
        return new DefaultDatabaseInformation();
    }
}
