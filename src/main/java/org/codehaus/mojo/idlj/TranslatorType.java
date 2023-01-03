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

import java.util.function.Supplier;

/**
 * A selector for the types of IDL translators supported
 */
enum TranslatorType
{
    AUTO("auto", () -> isJavaModuleSystemPresent() ?  new GlassfishTranslator() : new BuiltInTranslator()),
    BUILT_IN("idlj", BuiltInTranslator::new),
    GLASSFISH("glassfish", GlassfishTranslator::new),
    JACORB("jacorb", JacorbTranslator::new);

    private final String selector;

    private final Supplier<CompilerTranslator> compilerTranslatorSupplier;

    TranslatorType(String selector, Supplier<CompilerTranslator> compilerTranslatorSupplier) {
        assert selector != null;
        assert compilerTranslatorSupplier != null;
        this.selector = selector;
        this.compilerTranslatorSupplier = compilerTranslatorSupplier;
    }

    String getSelector() {
        return selector;
    }

    private static boolean isJavaModuleSystemPresent()
    {
        return !System.getProperty( "java.version" ).startsWith( "1." );
    }

    static CompilerTranslator selectTranslator( String compiler ) throws MojoExecutionException
    {
        for ( TranslatorType type : TranslatorType.values() )
        {
            if ( type.select( compiler ) )
            {
                return type.createTranslator();
            }
        }

        throw new MojoExecutionException( "Compiler not supported: " + compiler );
    }

    final boolean select( String compilerSetting ) {
        return selector.equals(compilerSetting);
    }

    final CompilerTranslator createTranslator() {
        return compilerTranslatorSupplier.get();
    }
}
