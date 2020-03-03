package org.utplsql.maven.plugin.exception;

public final class UtPLSQLMojoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UtPLSQLMojoException(final String message) {
        super(message);
    }

    public UtPLSQLMojoException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
