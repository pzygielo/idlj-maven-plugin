/**
 *
 * Copyright 2005 (C) The original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.idlj;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.FileUtils;


public abstract class AbstractIDLJMojo extends AbstractMojo {
    /**
     * a list of idl sources to compile.
     *
     * @parameter
     */
    private List sources;
    /**
     * print out debug messages
     *
     * @parameter debug
     */
    private boolean debug;

    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * The granularity in milliseconds of the last modification
     * date for testing whether a source needs recompilation
     *
     * @parameter expression="${lastModGranularityMs}" default-value="0"
     */
    private int staleMillis;

    /**
     * the maven project helper class for adding resources
     *
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     */
    protected MavenProjectHelper projectHelper;

    /**
     * the directory to store the processed grammars
     *
     * @parameter expression="${basedir}/target"
     */
    private String timestampDirectory;

    /**
     * The compiler to use. Current options are Suns idlj compiler and JacORB.
     * Should be either "idlj" or "jacorb". Defaults to "idlj".
     *
     * @parameter expression="idlj"
     */
    private String compiler;

    protected abstract String getSourceDirectory();

    protected abstract String getOutputDirectory();

    public void execute() throws MojoExecutionException {
        if (!FileUtils.fileExists(getOutputDirectory())) {
            FileUtils.mkdir(getOutputDirectory());
        }

        CompilerTranslator translator;
        if (compiler == null) {
            translator = new IdljTranslator(debug, getLog());
        } else if (compiler.equals("idlj")) {
            translator = new IdljTranslator(debug, getLog());
        } else if (compiler.equals("jacorb")) {
            translator = new JacorbTranslator(debug, getLog());
        } else {
            throw new MojoExecutionException("Compiler not supported: " + compiler);
        }

        if (sources != null) {
            for (Iterator it = sources.iterator(); it.hasNext();) {
                Source source = (Source) it.next();
                processSource(source, translator);
            }
        }
        addCompileSourceRoot();
    }

    private void processSource(Source source, CompilerTranslator translator) throws MojoExecutionException {
        Set staleGrammars = computeStaleGrammars(source);
        for (Iterator it = staleGrammars.iterator(); it.hasNext();) {
            File idlFile = (File) it.next();
            getLog().info("Processing: " + idlFile.toString());
            translator.invokeCompiler(getLog(), getSourceDirectory(), getOutputDirectory(), idlFile.toString(), source);
            try {
                FileUtils.copyFileToDirectory(idlFile, new File(timestampDirectory));
            }
            catch (IOException e) {
                getLog().warn("Failed to copy IDL file to output directory");
            }
        }
    }

    private Set computeStaleGrammars(Source source) throws MojoExecutionException {
        Set includes = source.getIncludes();
        if (includes == null) {
            includes = new HashSet();
            includes.add("**/*.idl");
        }
        Set excludes = source.getExcludes();
        if (excludes == null) {
            excludes = new HashSet();
        }
        SourceInclusionScanner scanner = new StaleSourceScanner(staleMillis, includes, excludes);
        scanner.addSourceMapping(new SuffixMapping(".idl", ".idl"));

        Set staleSources = new HashSet();

        File outDir = new File(timestampDirectory);

        File sourceDir = new File(getSourceDirectory());

        try {
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                staleSources.addAll(scanner.getIncludedSources(sourceDir, outDir));
            }
        }
        catch (InclusionScanException e) {
            throw new MojoExecutionException("Error scanning source root: \'" + sourceDir + "\' for stale CORBA IDL files to reprocess.", e);
        }

        return staleSources;
    }

    protected abstract void addCompileSourceRoot();
}
