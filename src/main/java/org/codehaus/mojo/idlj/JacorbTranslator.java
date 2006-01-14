package org.codehaus.mojo.idlj;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;


public class JacorbTranslator implements CompilerTranslator {

    private final boolean debug;
    private final Log log;

    public JacorbTranslator(boolean debug, Log log) {
        this.debug = debug;
        this.log = log;
    }

    private void invokeCompiler(Class compilerClass, List args) throws MojoExecutionException {
        // It would be great to use some 3rd party library for this stuff
        boolean fork = true;
        if (!fork) {
            try {
                String arguments[] = (String[]) args.toArray(new String[args.size()]);

                if (debug) {
                    String command = "compile";
                    for (int i = 0; i < arguments.length; i++) {
                        command += " " + arguments[i];
                    }
                    log.info(command);
                }

                Method compileMethod = compilerClass.getMethod("compile", new Class[]{String[].class});
                compileMethod.invoke(compilerClass, arguments);
            } catch (InvocationTargetException e) {
                throw new MojoExecutionException("Compilation failed", e.getTargetException());
            } catch (Throwable t) {
                throw new MojoExecutionException("Compilation failed", t);
            }
        } else {

            // Forks a new java process.
            // Get path to java binary
            File javaHome = new File(System.getProperty("java.home"));
            File javaBin = new File(new File(javaHome, "bin"), "java");

            // Get current class path
            URLClassLoader cl = (URLClassLoader) this.getClass().getClassLoader();
            URL[] classPathUrls = cl.getURLs();

            // Construct list of arguments
            List binArgs = new ArrayList();

            // First argument is the java binary to run
            binArgs.add(javaBin.getPath());

            // Add the classpath to argument list
            binArgs.add("-classpath");
            String classPath = "" + new File(classPathUrls[0].getPath().replaceAll("%20", " "));
            for (int i = 1; i < classPathUrls.length; i++) {
                classPath += File.pathSeparator + new File(classPathUrls[i].getPath() .replaceAll("%20", " "));
            }
            classPath += "";
            binArgs.add(classPath);

            // Add class containing main method to arg list
            binArgs.add(compilerClass.getName());

            // Add java arguments
            for (Iterator it = args.iterator(); it.hasNext();) {
                Object o = it.next();
                binArgs.add(o.toString());
            }

            // Convert arg list to array
            String[] argArray = new String[binArgs.size()];
            for (int i = 0; i < argArray.length; i++) {
                argArray[i] = binArgs.get(i).toString();
            }

            if (debug) {
                String command = "";
                for (int i = 0; i < argArray.length; i++) {
                    command += " " + argArray[i];
                }
                log.info(command);
            }

            try {
                Process p = Runtime.getRuntime().exec(argArray);
                redirectStream(p.getErrorStream(), System.err, "");
                redirectStream(p.getInputStream(), System.out, "");

                p.waitFor();

                if (p.exitValue() != 0) {
                    throw new MojoExecutionException("IDL Compilation failure");
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error forking compiler", e);
            } catch (InterruptedException e) {
                throw new MojoExecutionException("Thread interrupted unexpectedly", e);
            }
        }
    }

    public void invokeCompiler(Log log, String sourceDirectory, String targetDirectory, String idlFile, Source source) throws MojoExecutionException {
        List args = new ArrayList();

        // TODO: This should be configurable
        args.add("-sloppy_names");

        args.add("-I" + sourceDirectory);
        args.add("-d");
        args.add(targetDirectory);

        if (source.emitSkeletons() != null &&
            !source.emitSkeletons().booleanValue()) {
            args.add("-noskel");
        }
        if (source.emitStubs() != null &&
            !source.emitStubs().booleanValue()) {
            args.add("-nostub");
        }

        if (source.getPackagePrefix() != null) {
            args.add("-i2jpackage");
            args.add(":" + source.getPackagePrefix());
        }

        if (source.getPackagePrefixes() != null) {
            for (Iterator prefixes = source.getPackagePrefixes().iterator(); prefixes.hasNext();) {
                PackagePrefix prefix = (PackagePrefix) prefixes.next();
                args.add("-i2jpackage");
                args.add(prefix.getType() + ":" + prefix.getPrefix() + "." + prefix.getType());
            }
        }

        if (source.getDefines() != null) {
            for (Iterator defs = source.getDefines().iterator(); defs.hasNext();) {
                Define define = (Define) defs.next();
                String arg = "-D" + define.getSymbol();
                if (define.getValue() != null) {
                    arg += "=" + define.getValue();
                }
                args.add(arg);
            }
        }

        if (source.getAdditionalArguments() != null) {
            for (Iterator it = source.getAdditionalArguments().iterator(); it.hasNext();) {
                args.add(it.next());
            }
        }

        args.add(idlFile);

        Class compilerClass;
        try {
            compilerClass = Class.forName("org.jacorb.idl.parser");
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("JacORB IDL compiler not found", e);
        }

        invokeCompiler(compilerClass, args);
    }

    public static void redirectStream(final InputStream in,
                                      final OutputStream out, final String streamName) {
        Thread stdoutTransferThread = new Thread() {
            public void run() {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(out),
                                                 true);
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {

                        pw.println(line);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    // throw new Error(e);
                }
            }
        };
        stdoutTransferThread.start();
    }
}
