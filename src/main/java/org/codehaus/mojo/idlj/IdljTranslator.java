package org.codehaus.mojo.idlj;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;


public class IdljTranslator implements CompilerTranslator {

    private final boolean debug;
    private final Log log;

    public IdljTranslator(boolean debug, Log log) {
        this.debug = debug;
        this.log = log;
    }

    public void invokeCompiler(Log log, String sourceDirectory, String targetDirectory, String idlFile, Source source) throws MojoExecutionException {
        List args = new ArrayList();
        args.add("-i");
        args.add(sourceDirectory);
        args.add("-td");
        args.add(targetDirectory);

        if (source.getPackagePrefix() != null) {
            throw new MojoExecutionException("idlj compiler does not support packagePrefix");
        }

        if (source.getPackagePrefixes() != null) {
            for (Iterator prefixes = source.getPackagePrefixes().iterator(); prefixes.hasNext();) {
                PackagePrefix prefix = (PackagePrefix) prefixes.next();
                args.add("-pkgPrefix");
                args.add(prefix.getType());
                args.add(prefix.getPrefix());
            }
        }

        if (source.getDefines() != null) {
            for (Iterator defs = source.getDefines().iterator(); defs.hasNext();) {
                Define define = (Define) defs.next();
                if (define.getValue() != null) {
                    throw new MojoExecutionException("idlj compiler unable to define symbol values");
                }
                args.add("-d");
                args.add(define.getSymbol());
            }
        }

        if (source.emitStubs().booleanValue()) {
            if (source.emitSkeletons().booleanValue()) {
                args.add("-fall");
            } else {
                args.add("-fclient");
            }
        } else {
            if (source.emitSkeletons().booleanValue()) {
                args.add("-fserver");
            } else {
                args.add("-fserverTIE");
            }
        }

        if (source.getAdditionalArguments() != null) {
            for (Iterator it = source.getAdditionalArguments().iterator(); it.hasNext();) {
                args.add(it.next());
            }
        }

        args.add(idlFile);

        Class compilerClass = getCompilerClass();
        invokeCompiler(compilerClass, args);
    }

    private Class getCompilerClass() throws MojoExecutionException {
        ClassLoader cl = this.getClass().getClassLoader();
        Class idljCompiler;
        try {
            idljCompiler = Class.forName("com.sun.tools.corba.se.idl.toJavaPortable.Compile");
        }
        catch (ClassNotFoundException e) {
            try {
                File javaHome = new File(System.getProperty("java.home"));
                File toolsJar = new File(javaHome, "../lib/tools.jar");
                URL toolsJarUrl = toolsJar.toURL();
                URLClassLoader urlLoader = new URLClassLoader(new URL[]{toolsJarUrl}, cl);
                idljCompiler = urlLoader.loadClass("com.sun.tools.corba.se.idl.toJavaPortable.Compile");
            }
            catch (Exception notUsed) {
                throw new MojoExecutionException(" Sun IDL compiler not available", e);
            }
        }
        return idljCompiler;
    }

    private void invokeCompiler(Class compilerClass, List args) throws MojoExecutionException {
        Method compilerMainMethod;
        String arguments[] = (String[]) args.toArray(new String[args.size()]);
        
        if (debug) {
            String command = "main";
            for (int i = 0; i < arguments.length; i++) {
                command += " " + arguments[i];
            }
            log.info(command);
        }

        try {
            compilerMainMethod = compilerClass.getMethod("main", new Class[]{String[].class});
        } catch (NoSuchMethodException e1) {
            throw new MojoExecutionException("Error: Compiler had no main method");
        }

        try {
            compilerMainMethod.invoke(compilerClass, new Object[]{arguments});
        }
        catch (InvocationTargetException e) {
            throw new MojoExecutionException("IDL compilation failed", e.getTargetException());
        }
        catch (Throwable e) {
            throw new MojoExecutionException("IDL compilation failed", e);
        }
    }


}
