package org.codehaus.mojo.idlj;

public class PackageTranslation
{
    /**
     * The simple name of either a top-level module, or an IDL type defined outside of any module
     *
     * @parameter type
     */
    private String type;

    /**
     * The package name to use in place of the specified type
     *
     * @parameter replacementPackage
     */
    private String replacementPackage;

    /**
     * @return the name of a top-level module
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return the package name to replace the module name
     */
    public String getReplacementPackage()
    {
        return replacementPackage;
    }
}
