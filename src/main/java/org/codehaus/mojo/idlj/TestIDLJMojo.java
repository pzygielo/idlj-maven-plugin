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
 * Process CORBA IDL test files in IDLJ.
 *
 * @author maguro <adc@apache.org>
 * @version $Id$
 */
@Mojo(name = "generate-test", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class TestIDLJMojo extends AbstractIDLJMojo {
    /**
     * The source directory containing *.idl files.
     */
    @Parameter(defaultValue = "${basedir}/src/test/idl")
    private File sourceDirectory;

    /**
     * The directory to output the generated sources to.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/idl")
    private File outputDirectory;

    /**
     * @return the directory that contains the source
     */
    protected File getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * @return the directory that will contain the generated code
     */
    protected File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Adds the generated source path to the test source directories list so that maven can find the new sources to
     * compile tests.
     * @param directory the directory from which compilation should occur
     */
    protected void addCompileSourceRoot(File directory) {
        getProject().addTestCompileSourceRoot(directory.getAbsolutePath());
    }
}
