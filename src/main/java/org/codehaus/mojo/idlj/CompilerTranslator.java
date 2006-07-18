package org.codehaus.mojo.idlj;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;


public interface CompilerTranslator {

    public void invokeCompiler(
            Log log,
            String sourceDirectory,
            String targetDirectory,
            String idlFile,
            Source source) throws MojoExecutionException;
}
