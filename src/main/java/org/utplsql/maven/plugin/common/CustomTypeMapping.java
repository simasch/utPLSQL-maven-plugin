package org.utplsql.maven.plugin.common;

import lombok.Data;

/**
 * Bean used by Maven to populate its model. {@code
 * <customTypeMapping>
 *     <type>...</type>
 *     <mapping>...</mapping>
 * </customTypeMapping>
 * }
 */
@Data
public final class CustomTypeMapping {

    /**
     * Object type.
     */
    private String type;

    /**
     * Custom mapping value.
     */
    private String customMapping;
}
