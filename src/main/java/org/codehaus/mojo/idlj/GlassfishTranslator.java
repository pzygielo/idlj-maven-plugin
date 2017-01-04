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

import java.util.List;

/**
 * This class implements the <code>CompilerTranslator</code> for the Glassfish idlj compiler
 *
 * @author Russell Gold <russell.gold@oracle.com>
 */
class GlassfishTranslator
        extends IdljTranslator
{
    private static final String GLASSFISH_IDLJ_COMPILER_NAME = "com.sun.tools.corba.ee.idl.toJavaPortable.Compile";

    @Override
    void invokeCompiler( List<String> args ) throws MojoExecutionException
    {
        invokeCompilerInProcess( getCompilerClass(), args );
    }

    private Class<?> getCompilerClass() throws MojoExecutionException
    {
        try
        {
            return getClassLoaderFacade().loadClass( GLASSFISH_IDLJ_COMPILER_NAME );
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( " IDL compiler not available", e );
        }
    }
}
