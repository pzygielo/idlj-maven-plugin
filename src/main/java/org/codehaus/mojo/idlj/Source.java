package org.codehaus.mojo.idlj;

import java.util.List;
import java.util.Set;


public class Source {

    /**
     * Whether the compiler should emit client stubs. Defaults to true.
     *
     * @parameter emitStubs;
     */
    private Boolean emitStubs;

    /**
     * Whether the compiler should emit server skeletons. Defaults to true.
     *
     * @parameter emitSkeletons;
     */
    private Boolean emitSkeletons;

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
