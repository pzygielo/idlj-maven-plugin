package org.codehaus.mojo.idlj;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * This is abstract class used to decrease the code needed to the creation of the compiler MOJO.
 *
 * @author Anders Hessellund Jensen <ahj@trifork.com>
 * @version $Id$
 */
public abstract class AbstractIDLJMojo extends AbstractMojo
{
    /**
     * A <code>List</code> of <code>Source</code> configurations to compile.
     *
     * @parameter
     */
    private List<Source> sources;

    /**
     * Activate more detailed debug messages.
     *
     * @parameter debug
     */
    private boolean debug;

    /**
     * Should the plugin fail the build if there's an error while generating sources from IDLs.
     *
     * @parameter expression="${failOnError}" default-value="true"
     */
    private boolean failOnError;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The granularity in milliseconds of the last modification date for testing whether a source needs recompilation.
     *
     * @parameter expression="${lastModGranularityMs}" default-value="0"
     */
    private int staleMillis;

    /**
     * The maven project helper class for adding resources.
     *
     * @component role="org.apache.maven.project.MavenProjectHelper"
     */
    private MavenProjectHelper projectHelper;

    /**
     * The directory to store the processed grammars. Used so that grammars are not constantly regenerated.
     *
     * @parameter default-value="${project.build.directory}/idlj-timestamp"
     */
    private File timestampDirectory;

    /**
     * The compiler to use. Current options are Suns idlj compiler and JacORB. Should be either "idlj" or "jacorb".
     *
     * @parameter default-value="idlj"
     */
    private String compiler;

    /**
     * The interface between this class and the rest of the world - unit tests replace the default implementation.
     */
    private DependenciesFacade dependencies;

    /**
     * The default implementation of the dependencies.
     */
    private static final DependenciesFacade DEPENDENCIES_FACADE = new DependenciesFacadeImpl();


    /**
     * Creates the abstract class using a production implementation of the dependencies.
     */
    protected AbstractIDLJMojo()
    {
        this( DEPENDENCIES_FACADE );
    }

    AbstractIDLJMojo( DependenciesFacade dependencies )
    {
        this.dependencies = dependencies;
    }

    /**
     * @return the source directory that contains the IDL files
     */
    protected abstract File getSourceDirectory();

    /**
     * @return the <code>File[]</code> of the directories to use as include directories for the compilation
     */
    protected abstract File[] getIncludeDirs();

    /**
     * @return the path of the directory that will contains the results of the compilation
     */
    protected abstract File getOutputDirectory();

    /**
     * Execute the goal of the MOJO that is: compiling the IDL files
     *
     * @throws MojoExecutionException if the compilation fails or the compiler crashes
     */
    public void execute() throws MojoExecutionException
    {
        prepareGeneratedSourceDirectory( getOutputDirectory() );
        createIfAbsent( timestampDirectory );

        if ( isSourceSpecified() )
        {
            translateSources( createTranslator(), sources );
        }
        else
        {
            translateInferredSource( createTranslator() );
        }
    }

    private void prepareGeneratedSourceDirectory( File directory ) throws MojoExecutionException
    {
        createIfAbsent( directory );
        failIfNotWriteable( directory );
        addCompileSourceRoot( directory );
    }

    private boolean isSourceSpecified()
    {
        return sources != null;
    }

    private void translateInferredSource( CompilerTranslator translator ) throws MojoExecutionException
    {
        processSource( new Source(), translator );
    }

    private void translateSources( CompilerTranslator translator, List<Source> sourceList )
            throws MojoExecutionException
    {
        for ( Source source : sourceList )
        {
            processSource( source, translator );
        }
    }

    private CompilerTranslator createTranslator() throws MojoExecutionException
    {
        CompilerTranslator translator = createAppropriateTranslatorType();

        translator.setDebug( debug );
        translator.setFailOnError( failOnError );
        translator.setLog( getLog() );
        return translator;
    }

    private CompilerTranslator createAppropriateTranslatorType() throws MojoExecutionException
    {
        if ( compiler == null )
        {
            return new IdljTranslator();
        }
        else if ( compiler.equals( "idlj" ) )
        {
            return new IdljTranslator();
        }
        else if ( compiler.equals( "jacorb" ) )
        {
            return new JacorbTranslator();
        }
        else
        {
            throw new MojoExecutionException( "Compiler not supported: " + compiler );
        }
    }

    private void failIfNotWriteable( File directory ) throws MojoExecutionException
    {
        if ( !dependencies.isWriteable( directory ) )
        {
            throw new MojoExecutionException( "Cannot write in : " + directory );
        }
    }

    private void createIfAbsent( File directory )
    {
        if ( !dependencies.exists( directory ) )
        {
            dependencies.createDirectory( directory );
        }
    }

    /**
     * Compile the IDL files located in the given source path.
     *
     * @param source     the <code>Source</code> that specify which file compile with arguments to use for the source
     * @param translator the <code>CompilerTranslator</code> that raprresents idl compiler backend that will be used
     * @throws MojoExecutionException if the compilation fails or the compiler crashes
     */
    private void processSource( Source source, CompilerTranslator translator )
            throws MojoExecutionException
    {
        Set<File> staleGrammars = computeStaleGrammars( source );
        reportProcessingNeeded( staleGrammars );

        for ( File idlFile : staleGrammars )
        {
            processIdlFile( source, translator, idlFile );
        }
    }

    private void processIdlFile( Source source, CompilerTranslator translator, File idlFile )
            throws MojoExecutionException
    {
        try
        {
            translateIdlFile( idlFile, source, translator );
            copyToTimestampDirectory( idlFile );
        }
        catch ( IOException e )
        {
            getLog().warn( "Failed to copy IDL file to timestamp directory: " + e );
        }
    }

    private void translateIdlFile( File idlFile, Source source, CompilerTranslator translator )
            throws MojoExecutionException
    {
        getLog().debug( "Processing: " + idlFile.toString() );
        translator.invokeCompiler( getSourceDirectory().getAbsolutePath(),
                                   getIncludeDirs(),
                                   getOutputDirectory().getAbsolutePath(),
                                   idlFile.toString(), source );
    }

    private void copyToTimestampDirectory( File idlFile ) throws MojoExecutionException, IOException
    {
        URI relativeURI = getSourceDirectory().toURI().relativize( idlFile.toURI() );
        File timestampFile = new File( timestampDirectory.toURI().resolve( relativeURI ) );
        dependencies.copyFile( idlFile, timestampFile );
    }

    private void reportProcessingNeeded( Set<File> staleGrammars ) throws MojoExecutionException
    {
        if ( staleGrammars.size() > 0 )
        {
            getLog().info( "Processing " + staleGrammars.size() + " grammar files to " + getOutputDirectory() );
        }
        else
        {
            getLog().info( "Nothing to compile - all idl files are up to date" );
        }
    }

    /**
     * Determine which idl files need to be compiled.
     *
     * @param source the <code>Source</code> that represents which files to compile
     * @return a set of files that need to be compiled
     * @throws MojoExecutionException if the selection of the files to compile fails
     */
    private Set<File> computeStaleGrammars( Source source ) throws MojoExecutionException
    {
        File sourceDir = getSourceDirectory();
        getLog().debug( "sourceDir : " + sourceDir );

        return getStaleSources( createIdlScanner( source ), sourceDir );
    }

    private Set<File> getStaleSources( SourceInclusionScanner scanner, File sourceDir ) throws MojoExecutionException
    {
        try
        {
            return tryToGetStaleSources( scanner, sourceDir );
        }
        catch ( InclusionScanException e )
        {
            throw new MojoExecutionException( "Error scanning source root: \'" + sourceDir
                    + "\' for stale CORBA IDL files to reprocess.", e );
        }
    }

    private Set<File> tryToGetStaleSources( SourceInclusionScanner scanner,
                                            File sourceDir ) throws InclusionScanException
    {
        if ( isExistingDirectory( sourceDir ) )
        {
            return (Set<File>) scanner.getIncludedSources( sourceDir, timestampDirectory );
        }
        else
        {
            getLog().debug( "sourceDir isn't a directory" );
            return new HashSet<File>();
        }
    }

    private boolean isExistingDirectory( File sourceDir )
    {
        return dependencies.exists( sourceDir ) && dependencies.isDirectory( sourceDir );
    }

    private SourceInclusionScanner createIdlScanner( Source source )
    {
        Set<String> includes = getNonNullSet( "includes", source.getIncludes(), "**/*.idl" );
        Set<String> excludes = getNonNullSet( "excludes", source.getExcludes() );

        SourceInclusionScanner scanner = dependencies.createSourceInclusionScanner( staleMillis, includes, excludes );
        scanner.addSourceMapping( new SuffixMapping( ".idl", ".idl" ) );
        return scanner;
    }

    private Set<String> getNonNullSet( String comment, Set<String> set, String... defaultValues )
    {
        getLog().debug( comment + ": " + set );
        if ( set == null )
        {
            set = new HashSet<String>();
            Collections.addAll( set, defaultValues );
        }
        return set;
    }

    /**
     * Add generated sources in compile source root
     *
     * @param directory a directory containing generated java files to be compiled.
     */
    protected abstract void addCompileSourceRoot( File directory );

    /**
     * @return the current <code>MavenProject</code> instance
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @return the current <code>MavenProjectHelper</code> instance
     */
    protected MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    // ----------------------------------------------------------------------------------------------------
    // An interface for dependencies on the file system.
    // ----------------------------------------------------------------------------------------------------

    interface DependenciesFacade
    {
        SourceInclusionScanner createSourceInclusionScanner( int updatedWithinMsecs, Set<String> includes,
                                                             Set<String> excludes );

        void copyFile( File sourceFile, File targetFile ) throws IOException;

        boolean exists( File outputDirectory );

        void createDirectory( File directory );

        boolean isWriteable( File directory );

        boolean isDirectory( File file );
    }

    // ----------------------------------------------------------------------------------------------------
    // Standard file system dependencies.
    // ----------------------------------------------------------------------------------------------------

    static class DependenciesFacadeImpl implements DependenciesFacade
    {
        public void copyFile( File sourceFile, File targetFile ) throws IOException
        {
            FileUtils.copyFile( sourceFile, targetFile );
        }

        public SourceInclusionScanner createSourceInclusionScanner( int updatedWithinMsecs, Set<String> includes,
                                                                    Set<String> excludes )
        {
            return new StaleSourceScanner( updatedWithinMsecs, includes, excludes );
        }

        public boolean exists( File file )
        {
            return file.exists();
        }

        public void createDirectory( File directory )
        {
            directory.mkdirs();
        }

        public boolean isWriteable( File directory )
        {
            return directory.canWrite();
        }

        public boolean isDirectory( File file )
        {
            return file.isDirectory();
        }
    }

}
