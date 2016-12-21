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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * This class implement the <code>CompilerTranslator</code> for the Sun idlj IDL compiler
 *
 * @author Anders Hessellund Jensen <ahj@trifork.com>
 * @version $Id$
 */
class BuiltInTranslator
        extends IdljTranslator {

    private static final String AIX_IDLJ_COMPILER_NAME = "com.ibm.idl.toJavaPortable.Compile";
    private static final String ORACLE_IDLJ_COMPILER_NAME = "com.sun.tools.corba.se.idl.toJavaPortable.Compile";

    /**
     * Default constructor
     */
    BuiltInTranslator()
    {
        super();
    }

    @Override
    void invokeCompiler(List<String> args ) throws MojoExecutionException
    {
        Class<?> compilerClass = getCompilerClass();
        invokeCompiler( compilerClass, args );
    }

    /**
     * @return the <code>Class</code> that implements the idlj compiler
     * @throws MojoExecutionException if the search for the class fails
     */
    static Class<?> getCompilerClass()
            throws MojoExecutionException
    {
        Class<?> idljCompiler;
        try
        {
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


    private static void addToolsJarToPath() throws MalformedURLException, ClassNotFoundException
    {
        File javaHome = new File( System.getProperty( "java.home" ) );
        File toolsJar = new File( javaHome, "../lib/tools.jar");
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


    /**
     * @return the name of the class that implements the compiler
     */
    private static String getIDLCompilerClassName()
    {
        return isAix() ? AIX_IDLJ_COMPILER_NAME : ORACLE_IDLJ_COMPILER_NAME;
    }


    private static boolean isAix()
    {
        return System.getProperty( "java.vm.vendor" ).contains( "IBM" );
    }
}
