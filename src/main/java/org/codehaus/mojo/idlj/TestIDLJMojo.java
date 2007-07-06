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

import java.util.List;

/**
 * A plugin for processing CORBA IDL files in IDLJ.
 * 
 * @author maguro <adc@apache.org>
 * @version $Id$
 * @goal generate-test
 * @phase generate-test-sources
 * @description CORBA IDL compiler plugin
 */
public class TestIDLJMojo extends AbstractIDLJMojo
{

    /**
     * the source directory containing *.idl files
     * 
     * @parameter expression="${basedir}/src/test/idl"
     */
    private String sourceDirectory;

    /**
     * the include directories containing additional *.idl files required for
     * compilation
     * 
     * @parameter expression="${basedir}/src/test/idl"
     */
    private List includeDirs;

    /**
     * the directory to output the generated sources to
     * 
     * @parameter expression="${project.build.directory}/generated-test-sources/idl"
     */
    private String outputDirectory;

    /**
     * @return the directory that contains the source
     */
    protected String getSourceDirectory()
    {
        return sourceDirectory;
    }

    /**
     * @return the directory that will contain the generated code
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * //TODO
     */
    protected void addCompileSourceRoot()
    {
        getProject().addTestCompileSourceRoot( getOutputDirectory() );
    }

    /**
     * @return a <code>List</code> of directory to use as <i>include</i>
     */
    protected List getIncludeDirs()
    {
        return includeDirs;
    }
}
