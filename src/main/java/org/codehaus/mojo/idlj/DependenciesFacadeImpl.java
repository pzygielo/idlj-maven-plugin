package org.codehaus.mojo.idlj;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.util.FileUtils;

class DependenciesFacadeImpl implements DependenciesFacade {
    public void copyFile(File sourceFile, File targetFile) throws IOException {
        FileUtils.copyFile(sourceFile, targetFile);
    }

    public SourceInclusionScanner createSourceInclusionScanner(
            int updatedWithinMsecs, Set<String> includes, Set<String> excludes) {
        return new StaleSourceScanner(updatedWithinMsecs, includes, excludes);
    }

    public boolean exists(File file) {
        return file.exists();
    }

    public void createDirectory(File directory) {
        directory.mkdirs();
    }

    public boolean isWriteable(File directory) {
        return directory.canWrite();
    }

    public boolean isDirectory(File file) {
        return file.isDirectory();
    }
}
