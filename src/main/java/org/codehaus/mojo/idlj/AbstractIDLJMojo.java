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
import java.util.HashSet;
import java.util.ArrayList;
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

/**
 * This is abstarct class used to decrease the code needed
 * to the creation of the compiler MOJO.
 * 
 * @author Anders Hessellund Jensen <ahj@trifork.com>
 * @version $Id$
 */
public abstract class AbstractIDLJMojo extends AbstractMojo
{
    /**
     * A <code>List</code> of <code>Source</code> to compile.
     * 
     * @parameter
     */
    private List sources;

    /**
     * Activate more detailed debug messages.
     * 
     * @parameter debug
     */
    private boolean debug;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The granularity in milliseconds of the last modification date for testing
     * whether a source needs recompilation.
     * 
     * @parameter expression="${lastModGranularityMs}" default-value="0"
     */
    private int staleMillis;

    /**
     * The maven project helper class for adding resources.
     * 
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     */
    private MavenProjectHelper projectHelper;

    /**
     * The directory to store the processed grammars. Used so that grammars
     * are not constantly regenerated.
     * 
     * @parameter default-value="${basedir}/target"
     */
    private String timestampDirectory;

    /**
     * The compiler to use. Current options are Suns idlj compiler and JacORB.
     * Should be either "idlj" or "jacorb".
     * 
     * @parameter default-value="idlj"
     */
    private String compiler;

    /**
     * @return the source directory that conatins the IDL files
     */
    protected abstract String getSourceDirectory();

    /**
     * @return the <code>List</code> of the directories to use as 
     * include directories for the compilation
     */
    protected abstract List getIncludeDirs();

    /**
     * @return the path of the directory that will contains the results of the compilation
     */
    protected abstract String getOutputDirectory();


    /**
     * Execute the goal of the MOJO that is: compiling the IDL files
     * 
     * @throws MojoExecutionException if the compilation fails or the compiler crashes
     */    
    public void execute() throws MojoExecutionException
    {
        if ( !FileUtils.fileExists( getOutputDirectory() ) )
        {
            FileUtils.mkdir( getOutputDirectory() );
        }

        CompilerTranslator translator;
        if ( compiler == null )
        {
            translator = new IdljTranslator( debug, getLog() );
        }
        else if ( compiler.equals( "idlj" ) )
        {
            translator = new IdljTranslator( debug, getLog() );
        }
        else if ( compiler.equals( "jacorb" ) )
        {
            translator = new JacorbTranslator( debug, getLog() );
        }
        else
        {
            throw new MojoExecutionException( "Compiler not supported: " + compiler );
        }
        if ( sources == null )
        {
            // Intialize the sources in order to conform to documentation
            sources = new ArrayList();
            sources.add( new Source() );
        }
        if ( sources != null )
        {
            for ( Iterator it = sources.iterator(); it.hasNext(); )
            {
                Source source = (Source) it.next();
                processSource( source, translator );
            }
        }
        addCompileSourceRoot();
    }

    /**
     * Invoke the compiliation of a single IDL file
     * 
     * @param source the <code>Source</code> that specify which file compile with arguments to use for the source
     * @param translator the <code>CompilerTranslator</code> that raprresents idl compiler backend that will be used
     * @throws MojoExecutionException if the compilation fails or the compiler crashes
     */
    private void processSource( Source source, CompilerTranslator translator ) throws MojoExecutionException
    {
        Set staleGrammars = computeStaleGrammars( source );
        for ( Iterator it = staleGrammars.iterator(); it.hasNext(); )
        {
            File idlFile = (File) it.next();
            getLog().info( "Processing: " + idlFile.toString() );
            translator.invokeCompiler( getLog(), getSourceDirectory(), getIncludeDirs(), getOutputDirectory(), idlFile
                    .toString(), source );
            try
            {
                FileUtils.copyFileToDirectory( idlFile, new File( timestampDirectory ) );
            }
            catch ( IOException e )
            {
                getLog().warn( "Failed to copy IDL file to output directory" );
            }
        }
    }

    /**
     * 
     * @param source the <code>Source</code> that rapresent which file to compile
     * @return a set of file that need to be compiled
     * 
     * @throws MojoExecutionException if the selection of the file to compile fails
     */
    private Set computeStaleGrammars( Source source ) throws MojoExecutionException
    {
        Set includes = source.getIncludes();
        if ( includes == null )
        {
            includes = new HashSet();
            includes.add( "**/*.idl" );
        }
        Set excludes = source.getExcludes();
        if ( excludes == null )
        {
            excludes = new HashSet();
        }
        SourceInclusionScanner scanner = new StaleSourceScanner( staleMillis, includes, excludes );
        scanner.addSourceMapping( new SuffixMapping( ".idl", ".idl" ) );

        Set staleSources = new HashSet();

        File outDir = new File( timestampDirectory );

        File sourceDir = new File( getSourceDirectory() );

        try
        {
            if ( sourceDir.exists() && sourceDir.isDirectory() )
            {
                staleSources.addAll( scanner.getIncludedSources( sourceDir, outDir ) );
            }
        }
        catch ( InclusionScanException e )
        {
            throw new MojoExecutionException( "Error scanning source root: \'" + sourceDir
                    + "\' for stale CORBA IDL files to reprocess.", e );
        }

        return staleSources;
    }

    /**
     * //TODO ??? 
     */
    protected abstract void addCompileSourceRoot();

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
}
