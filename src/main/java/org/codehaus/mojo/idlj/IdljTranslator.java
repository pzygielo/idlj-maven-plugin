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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for translators that use the IDLJ parameters.
 */
abstract class IdljTranslator extends AbstractTranslator implements CompilerTranslator
{
    /**
     * Convert the provided filename from a Windows separator \\ to a unix/java separator /
     *
     * @param filename file name to fix separator
     * @return filename with all \\ replaced with /
     */
    private static String fixSeparator( String filename )
    {
        return StringUtils.replace( filename, '\\', '/' );
    }

    /**
     * Return the unique path to the resource.
     *
     * @param file a resource to locate
     * @return the computed path
     * @throws MojoExecutionException if the infrastructure detects a problem
     */
    private static String getCanonicalPath( File file )
            throws MojoExecutionException
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't canonicalize system path: " + file.getAbsolutePath(), e );
        }
    }

    /**
     * Taken from maven-eclipse-plugin
     *
     * @param fromdir                  not sure
     * @param todir                    what these are
     * @param replaceSlashesWithDashes true if we need to replace slashes with dashes to accomodate the OS
     * @return the relative path between fromdir to todir
     * @throws MojoExecutionException thrown if an error is detected by the mojo infrastructure
     */
    private static String toRelativeAndFixSeparator( File fromdir, File todir, boolean replaceSlashesWithDashes )
            throws MojoExecutionException
    {
        if ( !todir.isAbsolute() )
        {
            todir = new File( fromdir, todir.getPath() );
        }

        String basedirPath = getCanonicalPath( fromdir );
        String absolutePath = getCanonicalPath( todir );

        String relative;

        if ( absolutePath.equals( basedirPath ) )
        {
            relative = "."; //$NON-NLS-1$
        }
        else if ( absolutePath.startsWith( basedirPath ) )
        {
            // MECLIPSE-261
            // The canonical form of a windows root dir ends in a slash, whereas
            // the canonical form of any other file
            // does not.
            // The absolutePath is assumed to be: basedirPath + Separator +
            // fileToAdd
            // In the case of a windows root directory the Separator is missing
            // since it is contained within
            // basedirPath.
            int length = basedirPath.length() + 1;
            if ( basedirPath.endsWith( "\\" ) )
            {
                length--;
            }
            relative = absolutePath.substring( length );
        }
        else
        {
            relative = absolutePath;
        }

        relative = fixSeparator( relative );

        if ( replaceSlashesWithDashes )
        {
            relative = StringUtils.replace( relative, '/', '-' );
            relative = StringUtils.replace( relative, ':', '-' ); // remove ":"
            // for absolute
            // paths in
            // windows
        }

        return relative;
    }

    /**
     * This method it's used to invoke the compiler
     *
     * @param sourceDirectory the path to the sources
     * @param includeDirs     the <code>File[]</code> of directories where to find the includes
     * @param targetDirectory the path to the destination of the compilation
     * @param idlFile         the path to the file to compile
     * @param source          the source tag available in the configuration tree of the maven plugin
     * @throws MojoExecutionException the exception is thrown whenever the compilation fails or crashes
     */
    public void invokeCompiler( String sourceDirectory, File[] includeDirs, String targetDirectory, String idlFile,
                                Source source )
            throws MojoExecutionException
    {
        List<String> args = getArguments( sourceDirectory, includeDirs, targetDirectory, idlFile, source );

        invokeCompiler( args );
    }

    abstract void invokeCompiler( List<String> args ) throws MojoExecutionException;

    private List<String> getArguments( String sourceDirectory, File[] includeDirs, String targetDirectory,
                                       String idlFile, Source source ) throws MojoExecutionException
    {
        List<String> args = new ArrayList<>();
        args.add( "-i" );
        args.add( sourceDirectory );

        // add idl files from other directories as well
        if ( includeDirs != null && includeDirs.length > 0 )
        {
            for ( File includeDir : includeDirs )
            {
                args.add( "-i" );
                args.add( includeDir.toString() );
            }
        }

        args.add( "-td" );
        args.add( toRelativeAndFixSeparator( new File( System.getProperty( "user.dir" ) ), new File( targetDirectory ),
                false ) );

        if ( source.getPackagePrefix() != null )
        {
            throw new MojoExecutionException( "idlj compiler does not support packagePrefix" );
        }

        if ( source.getPackagePrefixes() != null )
        {
            for ( PackagePrefix prefix : source.getPackagePrefixes() )
            {
                args.add( "-pkgPrefix" );
                args.add( prefix.getType() );
                args.add( prefix.getPrefix() );
            }
        }

        if ( source.getPackageTranslations() != null )
        {
            for ( PackageTranslation translation : source.getPackageTranslations() )
            {
                args.add( "-pkgTranslate" );
                args.add( translation.getType() );
                args.add( translation.getReplacementPackage() );
            }
        }

        if ( source.getDefines() != null )
        {
            for ( Define define : source.getDefines() )
            {
                addSymbolDefinition( args, define );
            }
        }

        addEmitOption( args, source );

        if ( isOptionEnabled( source.compatible() ) )
        {
            args.add( "-oldImplBase" );
        }

        if ( source.getAdditionalArguments() != null )
        {
            for ( String arg : source.getAdditionalArguments() )
            {
                args.add( arg );
            }
        }

        args.add( idlFile );
        return args;
    }

    private void addSymbolDefinition( List<String> args, Define define ) throws MojoExecutionException
    {
        if ( define.getValue() != null )
        {
            throw new MojoExecutionException( "idlj compiler unable to define symbol values" );
        }
        args.add( "-d" );
        args.add( define.getSymbol() );
    }

    private void addEmitOption( List<String> args, Source source )
    {
        if ( isOptionEnabled( source.emitStubs() ) )
        {
            args.add( source.emitSkeletons() ? "-fallTIE" : "-fclient" );
        }
        else
        {
            args.add( isOptionEnabled( source.emitSkeletons() ) ? "-fserver" : "-fserverTIE" );
        }
    }

    private boolean isOptionEnabled( Boolean option )
    {
        return option != null && option;
    }

    /**
     * Invoke the specified compiler with a set of arguments
     *
     * @param compilerClass the <code>Class</code> that implements the compiler
     * @param args          a <code>List</code> that contains the arguments to use for the compiler
     * @throws MojoExecutionException if the compilation fail or the compiler crashes
     */
    void invokeCompiler( Class<?> compilerClass, List<String> args )
            throws MojoExecutionException
    {
        getLog().debug( "Current dir : " + System.getProperty( "user.dir" ) );

        if ( isDebug() )
        {
            args.add( 0, "-verbose" );
        }

        invokeCompilerInProcess( compilerClass, args );
    }

    @Override
    protected int runCompiler( Class<?> compilerClass, String... arguments )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method compilerMainMethod = compilerClass.getMethod( "main", String[].class );
        Object retVal = compilerMainMethod.invoke( compilerClass, new Object[]{arguments} );
        getLog().debug( "Completed with code " + retVal );
        return ( retVal != null ) && ( retVal instanceof Integer ) ? (Integer) retVal : 0;
    }
}
