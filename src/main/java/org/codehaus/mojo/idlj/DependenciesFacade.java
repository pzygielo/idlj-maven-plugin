package org.codehaus.mojo.idlj;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;

interface DependenciesFacade {
    SourceInclusionScanner createSourceInclusionScanner(
            int updatedWithinMsecs, Set<String> includes, Set<String> excludes);

    void copyFile(File sourceFile, File targetFile) throws IOException;

    boolean exists(File outputDirectory);

    void createDirectory(File directory);

    boolean isWriteable(File directory);

    boolean isDirectory(File file);
}
