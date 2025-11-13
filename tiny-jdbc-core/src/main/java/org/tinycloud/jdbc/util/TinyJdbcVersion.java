package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * <p>
 * 获取版本号
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-16 11:11
 */
public class TinyJdbcVersion {
    private TinyJdbcVersion() {
    }

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    public static String getVersion() {
        return determineSpringBootVersion();
    }

    /**
     * 从jar包中获取版本号
     *
     * @return 版本号
     */
    private static String determineSpringBootVersion() {
        final Package pkg = TinyJdbcVersion.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        CodeSource codeSource = TinyJdbcVersion.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL codeSourceLocation = codeSource.getLocation();
        try {
            URLConnection connection = codeSourceLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                try (JarFile jarFile = ((JarURLConnection) connection).getJarFile()) {
                    return getImplementationVersion(jarFile);
                }
            } else {
                try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
                    return getImplementationVersion(jarFile);
                }
            }
        } catch (Exception e) {
            throw new TinyJdbcException("determineSpringBootVersion failed: ", e);
        }
    }

    private static String getImplementationVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }
}
