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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implement the <code>CompilerTranslator</code> for the Sun idlj IDL compiler
 *
 * @author Anders Hessellund Jensen <ahj@trifork.com>
 * @version $Id$
 */
public class IdljTranslator
        extends AbstractTranslator
        implements CompilerTranslator
{

    private static final String AIX_IDLJ_COMPILER_NAME = "com.ibm.idl.toJavaPortable.Compile";
    private static final String ORACLE_IDLJ_COMPILER_NAME = "com.sun.tools.corba.se.idl.toJavaPortable.Compile";

    /**
     * Default constructor
     */
    public IdljTranslator()
    {
        super();
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
        List<String> args = new ArrayList<String>();
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
                if ( define.getValue() != null )
                {
                    throw new MojoExecutionException( "idlj compiler unable to define symbol values" );
                }
                args.add( "-d" );
                args.add( define.getSymbol() );
            }
        }

        if ( isOptionEnabled( source.emitStubs() ) )
        {
            if ( source.emitSkeletons() )
            {
                args.add( "-fallTIE" );
            }
            else
            {
                args.add( "-fclient" );
            }
        }
        else
        {
            if ( isOptionEnabled( source.emitSkeletons() ) )
            {
                args.add( "-fserver" );
            }
            else
            {
                args.add( "-fserverTIE" );
            }
        }

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

        Class<?> compilerClass = getCompilerClass();
        invokeCompiler( compilerClass, args );
    }

    private boolean isOptionEnabled( Boolean option )
    {
        return option != null && option;
    }

    /**
     * @return the <code>Class</code> that implements the idlj compiler
     * @throws MojoExecutionException if the search for the class fails
     */
    private Class<?> getCompilerClass()
            throws MojoExecutionException
    {
        Class<?> idljCompiler;
        try
        {
            if ( isMacOSX() )
            {
                addToolsJarToPath();
            }
            idljCompiler = getClassLoaderFacade().loadClass( getIDLCompilerClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            try
            {
                addToolsJarToPath();
                idljCompiler = getClassLoaderFacade().loadClass( getIDLCompilerClassName() );
            }
            catch ( Exception notUsed )
            {
                throw new MojoExecutionException( " IDL compiler not available", e );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( " IDL compiler not available", e );
        }
        return idljCompiler;
    }


    private void addToolsJarToPath() throws MalformedURLException, ClassNotFoundException
    {
        File javaHome = new File( System.getProperty( "java.home" ) );
        File toolsJar = new File( javaHome, getToolsJarPath() );
        URL toolsJarUrl = toolsJar.toURI().toURL();
        getClassLoaderFacade().prependUrls( toolsJarUrl );

        // Unfortunately the idlj compiler reads messages using the system class path.
        // Therefore this really nasty hack is required.
        System.setProperty( "java.class.path", System.getProperty( "java.class.path" )
                + System.getProperty( "path.separator" ) + toolsJar.getAbsolutePath() );
        if ( System.getProperty( "java.vm.name" ).contains( "HotSpot" ) )
        {
            getClassLoaderFacade().loadClass( "com.sun.tools.corba.se.idl.som.cff.FileLocator" );
        }
    }


    private String getToolsJarPath()
    {
        return isMacOSX()
                ? "../Classes/classes.jar"
                : "../lib/tools.jar";
    }


    private boolean isMacOSX()
    {
        return System.getProperty( "java.vm.vendor" ).contains( "Apple" );
    }


    private static boolean isAix()
    {
        return System.getProperty( "java.vm.vendor" ).contains( "IBM" );
    }

    /**
     * @return the name of the class that implements the compiler
     */
    private static String getIDLCompilerClassName()
    {
        return isAix() ? AIX_IDLJ_COMPILER_NAME : ORACLE_IDLJ_COMPILER_NAME;
    }

    /**
     * Invoke the specified compiler with a set of arguments
     *
     * @param compilerClass the <code>Class</code> that implements the compiler
     * @param args          a <code>List</code> that contains the arguments to use for the compiler
     * @throws MojoExecutionException if the compilation fail or the compiler crashes
     */
    private void invokeCompiler( Class<?> compilerClass, List<String> args )
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


    /**
     * Convert the provided filename from a Windows separator \\ to a unix/java separator /
     *
     * @param filename file name to fix separator
     * @return filename with all \\ replaced with /
     */
    public static String fixSeparator( String filename )
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
    public static String getCanonicalPath( File file )
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
    public static String toRelativeAndFixSeparator( File fromdir, File todir, boolean replaceSlashesWithDashes )
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

}
