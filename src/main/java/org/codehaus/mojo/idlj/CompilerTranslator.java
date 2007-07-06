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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * This is the interface to implement in order to add a new compiler
 * backend to this plugin
 * 
 * @author Anders Hessellund Jensen <ahj@trifork.com>
 * @version $Id$
 */
public interface CompilerTranslator
{

    /**
     * This method it's used to invoke the compiler
     * 
     * @param log the<code>Log</code> to use for log messages
     * @param sourceDirectory the path to the sources
     * @param includeDirs the <code>List</code> of directories where to find the includes
     * @param targetDirectory the path to the destination of the compilation
     * @param idlFile the path to the file to compile
     * @param source //TODO ???
     * @throws MojoExecutionException the exeception is thrown whenever the compilation fails or crashes
     */
    void invokeCompiler( Log log, String sourceDirectory, List includeDirs, String targetDirectory, String idlFile,
            Source source ) throws MojoExecutionException;
}
