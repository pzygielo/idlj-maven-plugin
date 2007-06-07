package org.codehaus.mojo.idlj;
/* *
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


import java.util.List;
import java.util.Set;


public class Source {

    /**
     * Active the generation of java source compatibile with jdk previus 1.4
     *
     * @parameter compatible
     */
    private Boolean compatible = Boolean.TRUE;
	
    /**
     * Whether the compiler should emit client stubs. Defaults to true.
     *
     * @parameter emitStubs;
     */
    private Boolean emitStubs = Boolean.TRUE;

    /**
     * Whether the compiler should emit server skeletons. Defaults to true.
     *
     * @parameter emitSkeletons;
     */
    private Boolean emitSkeletons = Boolean.TRUE;

    /**
     * Specifies a single, global packageprefix to use for all modules.
     *
     * @parameter packagePrefix;
     */
    private String packagePrefix;

    /**
     * Specifies which files to include in compilation.
     *
     * @parameter includes;
     */
    private Set includes;

    /**
     * Specifies which files to exclude from compilation.
     *
     * @parameter excludes;
     */
    private Set excludes;

    /**
     * The list of package prefixes for certain types.
     *
     * @parameter packagePrefixes;
     */
    private List packagePrefixes;

    /**
     * The list of preprocessor symbols to define.
     */
    private List defines;

    /**
     * The list of additional, compiler-specific arguments to use.
     */
    private List additionalArguments;


    public List getDefines() {
        return defines;
    }

    public Boolean emitStubs() {
        return emitStubs;
    }

    public Boolean emitSkeletons() {
        return emitSkeletons;
    }
    
    public Boolean compatible() {
	return compatible;
    }

    public Set getExcludes() {
        return excludes;
    }

    public Set getIncludes() {
        return includes;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public List getPackagePrefixes() {
        return packagePrefixes;
    }

    public List getAdditionalArguments() {
        return additionalArguments;
    }
}
