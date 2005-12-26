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

import java.util.ArrayList;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;


/**
 * A plugin for processing CORBA IDL files in IDLJ.
 *
 * @author Alan D. Cabrera <adc@apache.org>
 * @version $Id$
 * @goal generate
 * @phase generate-sources
 * @description CORBA IDL compiler plugin
 */
public class IDLJMojo extends AbstractIDLJMojo
{
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

    protected String getSourceDirectory()
    {
        return sourceDirectory;
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    protected void setSourceDirectory(String sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }
}
