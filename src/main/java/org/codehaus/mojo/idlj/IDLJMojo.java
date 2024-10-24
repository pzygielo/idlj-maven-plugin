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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Process CORBA IDL files in IDLJ.
 *
 * @author Alan D. Cabrera <adc@apache.org>
 * @version $Id$
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class IDLJMojo extends AbstractIDLJMojo {
    /**
     * The source directory containing *.idl files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/idl")
    private File sourceDirectory;

    /**
     * The directory to output the generated sources to.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/idl")
    private File outputDirectory;

    /**
     * Constructs a standard IDL translation Mojo.
     */
    public IDLJMojo() {}

    IDLJMojo(DependenciesFacade dependenciesFacade) {
        super(dependenciesFacade);
    }

    /**
     * @return the source directory that contains the IDL files
     */
    protected File getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * @return the path of the directory that will contains the results of the compilation
     */
    protected File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Set the source directory.
     *
     * @param dir the path of directory that contains the IDL files
     */
    protected void setSourceDirectory(File dir) {
        this.sourceDirectory = dir;
    }

    /**
     * Add generated sources in compile source root
     *
     * @param directory a directory containing generated java files to be compiled.
     */
    protected void addCompileSourceRoot(File directory) {
        getProject().addCompileSourceRoot(directory.getPath());
    }
}
