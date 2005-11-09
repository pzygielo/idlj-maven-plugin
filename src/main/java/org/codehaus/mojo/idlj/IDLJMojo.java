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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;


/**
 * A plugin for processing CORBA IDL files in IDLJ.
 *
 * @author maguro <alan.cabrea@gmail.com>
 * @version $Id$
 * @goal generate
 * @phase generate-sources
 * @description CORBA IDL compiler plugin
 */
public class IDLJMojo extends AbstractMojo
{
    /**
     * the Java JVM directory containing *.idl files
     *
     * @parameter expression="${java.home}/lib"
     */
    private String packagePrefix;

    /**
     * the Java JVM directory containing *.idl files
     *
     * @parameter expression="${java.home}/lib"
     */
    private String javaIdlDirectory;

    /**
     * the source directory containing *.idl files
     *
     * @parameter expression="${basedir}/src/main/idl"
     */
    private String sourceDirectory;

    /**
     * the directory to output the generated sources to
     *
     * @parameter expression="${project.build.directory}/generated-sources/idl"
     */
    private String outputDirectory;

    /**
     * the directory to store the processed grammars
     *
     * @parameter expression="${basedir}/target"
     */
    private String timestampDirectory;

    /**
     * The granularity in milliseconds of the last modification
     * date for testing whether a source needs recompilation
     *
     * @parameter expression="${lastModGranularityMs}" default-value="0"
     */
    private int staleMillis;

    /**
     * the list of prefixes to use for certain types
     *
     * @parameter packagePrefixes
     */
    private List packagePrefixes;

    /**
     * the maven project helper class for adding resources
     *
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException
    {
        checkForIDLCompiler();

        if ( !FileUtils.fileExists( outputDirectory ) )
        {
            FileUtils.mkdir( outputDirectory );
        }

        Set staleGrammars = computeStaleGrammars();

        if ( staleGrammars.isEmpty() )
        {
            getLog().info( "Nothing to process - all CORBA IDL files are up to date" );
            return;
        }

        for ( Iterator i = staleGrammars.iterator(); i.hasNext(); )
        {
            File idlFile = (File) i.next();

            ArrayList arguments = new ArrayList();

            arguments.add( "-i" );
            arguments.add( javaIdlDirectory.toString() );
            arguments.add( "-i" );
            arguments.add( sourceDirectory.toString() );
            arguments.add( "-td" );
            arguments.add( outputDirectory.toString() );

            for (Iterator prefixes = packagePrefixes.iterator(); prefixes.hasNext();) {
                PackagePrefix prefix = (PackagePrefix)prefixes.next();
                arguments.add( "-pkgPrefix" );
                arguments.add( prefix.getType() );
                arguments.add( prefix.getPrefix() );
            }

            arguments.add( idlFile.toString() );

            try
            {
                getLog().info( "Processing: " + idlFile.toString() );

                com.sun.tools.corba.se.idl.toJavaPortable.Compile.main( (String[]) arguments.toArray( new String[arguments.size()] ) );

                // make sure this is after the acutal processing,
                //otherwise it if fails the computeStaleGrammars will think it completed.
                FileUtils.copyFileToDirectory( idlFile, new File( timestampDirectory ) );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "IDLJ execution failed", e );
            }
            catch ( Throwable t )
            {
                throw new MojoExecutionException( "IDLJ execution failed", t );
            }
        }

        if ( project != null )
        {
            getLog().debug( "adding .dat resource" );
            projectHelper.addResource( project, outputDirectory, Collections.singletonList( "**/**.dat" ), new ArrayList() );
            project.addCompileSourceRoot( outputDirectory );
        }
    }

    private void checkForIDLCompiler() throws MojoExecutionException
    {
        try
        {
            Class.forName( "com.sun.tools.corba.se.idl.toJavaPortable.Compile" );
        }
        catch ( ClassNotFoundException doNothing )
        {
            throw new MojoExecutionException( "Sun IDL compiler not available" );
        }
    }

    private Set computeStaleGrammars() throws MojoExecutionException
    {
        SourceInclusionScanner scanner = new StaleSourceScanner( staleMillis );

        scanner.addSourceMapping( new SuffixMapping( ".idl", ".idl" ) );

        File outDir = new File( timestampDirectory );

        Set staleSources = new HashSet();

        File sourceDir = new File( sourceDirectory );

        try
        {
            staleSources.addAll( scanner.getIncludedSources( sourceDir, outDir ) );
        }
        catch ( InclusionScanException e )
        {
            throw new MojoExecutionException( "Error scanning source root: \'" + sourceDir + "\' for stale CORBA IDL files to reprocess.", e );
        }

        return staleSources;
    }
}
