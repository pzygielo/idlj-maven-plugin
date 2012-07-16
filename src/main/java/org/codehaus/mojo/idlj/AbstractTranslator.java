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
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Shared capabilities for translators.
 *
 * @author Arnaud Heritier <aheritier AT apache DOT org>
 * @version $Id: AbstractIDLJMojo.java 9189 2009-03-10 21:47:46Z aheritier $
 */
public abstract class AbstractTranslator
        implements CompilerTranslator
{

    /**
     * enable/disable debug messages
     */
    private boolean debug;

    /**
     * Set to true to fail the build if an error occur while compiling the IDL.
     */
    private boolean failOnError;

    /**
     * the <code>Log</code> that will used for the messages
     */
    private Log log;

    /* A facade to enable unit testing to control compiler access. */
    private static ClassLoaderFacade classLoaderFacade = new ClassLoaderFacadeImpl();

    /**
     * Determines if the compiler can fork a process to run. Not all compilers support this.
     */
    private static boolean fork = true;

    /**
     * @return the debug
     */
    public boolean isDebug()
    {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    /**
     * @return the log
     */
    public Log getLog()
    {
        return log;
    }

    /**
     * @param log the log to set
     */
    public void setLog( Log log )
    {
        this.log = log;
    }

    /**
     * @return the failOnError
     */
    public boolean isFailOnError()
    {
        return failOnError;
    }

    /**
     * @param failOnError the failOnError to set
     */
    public void setFailOnError( boolean failOnError )
    {
        this.failOnError = failOnError;
    }

    /**
     * Returns true if the translator is allowed to create a new forked process.
     * @return true if forking is permitted
     */
    protected static boolean isFork()
    {
        return fork;
    }

    /**
     * Specifies the implementation of the classloader facade to use
     * @param classLoaderFacade a wrapper for class loading.
     */
    static void setClassLoaderFacade( ClassLoaderFacade classLoaderFacade )
    {
        AbstractTranslator.classLoaderFacade = classLoaderFacade;
        AbstractTranslator.fork = false;
    }

    /**
     * Returns the object to use for classloading.
     * @return the appropriate loader facade
     */
    protected ClassLoaderFacade getClassLoaderFacade()
    {
        return classLoaderFacade;
    }

    /**
     * Invokes the configured compiler and throws an exception if anything goes wrong
     * @param compilerClass the class representing the compiler to invoke
     * @param args the arguments to pass to the compiler
     * @throws MojoExecutionException if any error occurs
     */
    protected void invokeCompilerInProcess( Class<?> compilerClass, List<String> args ) throws MojoExecutionException
    {
        String[] arguments = args.toArray( new String[args.size()] );

        getLog().debug( getCommandLine( compilerClass, arguments ) );

        // Local channels
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int exitCode = runCompilerAndRecordOutput( compilerClass, arguments, err, out );
        logOutputMessages( err, out );

        if ( isFailOnError() && isCompilationFailed( err, exitCode ) )
        {
            throw new MojoExecutionException( "IDL compilation failed" );
        }
    }

    private int runCompilerAndRecordOutput( Class<?> compilerClass, String[] arguments, ByteArrayOutputStream err,
                                            ByteArrayOutputStream out ) throws MojoExecutionException
    {
        // Backup std channels
        PrintStream stdErr = System.err;
        PrintStream stdOut = System.out;

        System.setErr( new PrintStream( err ) );
        System.setOut( new PrintStream( out ) );
        try
        {
            return runCompiler( compilerClass, arguments );
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoExecutionException( "Error: Compiler had no main method" );
        }
        catch ( InvocationTargetException e )
        {
            throw new MojoExecutionException( "IDL compilation failed", e.getTargetException() );
        }
        catch ( Throwable e )
        {
            throw new MojoExecutionException( "IDL compilation failed", e );
        }
        finally
        {
            // Restore std channels
            System.setErr( stdErr );
            System.setOut( stdOut );
        }
    }

    /**
     * Runs the IDL compiler
     * @param compilerClass the class which implements the compiler
     * @param arguments the arguments to pass to the compiler
     * @return the return status (a non-zero value indicates an error)
     * @throws NoSuchMethodException if the method which should run the compiler does not exist
     * @throws IllegalAccessException if no constructor is available
     * @throws InvocationTargetException if an error occurs while invoking the compiler
     */
    protected abstract int runCompiler( Class<?> compilerClass, String... arguments )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

    private boolean isCompilationFailed( ByteArrayOutputStream err, int exitCode )
    {
        return exitCode != 0 || isNotEmpty( err );
    }

    private void logOutputMessages( ByteArrayOutputStream err, ByteArrayOutputStream out )
    {
        if ( isNotEmpty( out ) )
        {
            getLog().info( out.toString() );
        }
        if ( isNotEmpty( err ) )
        {
            getLog().error( err.toString() );
        }
    }

    private boolean isNotEmpty( ByteArrayOutputStream outputStream )
    {
        return !"".equals( outputStream.toString() );
    }

    private String getCommandLine( Class<?> compilerClass, String[] arguments )
    {
        String command = compilerClass.getName();
        for ( String argument : arguments )
        {
            command += " " + argument;
        }
        return command;
    }

    /**
     * An interface for loading the proper IDL compiler class.
     */
    public interface ClassLoaderFacade
    {
        /**
         * Updates the active classloader to include the specified URLs before the original definitions.
         *
         * @param urls a list of URLs to include when searching for classes.
         */
        void prependUrls( URL... urls );

        /**
         * Loads the specified class using the appropriate classloader.
         *
         * @param idlCompilerClass the name of the class to use for compiling IDL files.
         * @throws ClassNotFoundException if the specified class doesn't exist
         * @return the actual compiler class to use
         */
        Class<?> loadClass( String idlCompilerClass ) throws ClassNotFoundException;
    }

    /**
     * The implementation of ClassLoaderFacade used at runtime.
     */
    static class ClassLoaderFacadeImpl implements ClassLoaderFacade
    {
        ClassLoader classLoader = getClass().getClassLoader();

        public void prependUrls( URL... urls )
        {
            classLoader = new URLClassLoader( urls, classLoader );
        }

        public Class<?> loadClass( String idlCompilerClass ) throws ClassNotFoundException
        {
            return classLoader.loadClass( idlCompilerClass );
        }

    }
}
