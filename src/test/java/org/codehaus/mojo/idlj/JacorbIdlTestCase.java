package org.codehaus.mojo.idlj;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JacorbIdlTestCase extends IDLJTestBase {

    @Before
    public void setUpJacorb() throws NoSuchFieldException, IllegalAccessException {
        defineCompiler("jacorb");
    }


    @Test
    public void whenSpecified_chooseJacorbCompiler() throws Exception {
        mojo.execute();
        assertEquals("org.jacorb.idl.parser", loaderFacade.getIdlCompilerClass());
    }

    @Test
    public void byDefault_allowSloppyNames() throws Exception {
        mojo.execute();
        assertArgumentsContains("-sloppy_names");
    }

    @Test
    public void whenNoOptionsAreSpecified_useCurrentDirectoryAsIncludePath() throws Exception {
        mojo.execute();
        assertArgumentsContains("-I" + getCurrentDir() + "/src/main/idl");
    }

    @Test
    public void whenIncludePathIsSpecified_createIncludeArguments() throws Exception {
        defineIncludePaths("/src/main/idl-include");
        mojo.execute();
        assertArgumentsContains("-I/src/main/idl-include");
    }

    @Test
    public void whenSinglePackagePrefixDefined_createPackageArgument() throws Exception {
        Source source = createSource();
        defineSinglePrefix(source, "aPrefix");
        mojo.execute();
        assertArgumentsContains("-i2jpackage", ":aPrefix");
    }

    @Test
    public void whenPackagePrefixDefined_createPrefixArguments() throws Exception {
        Source source = createSource();
        createPrefix(source, "aType1", "aPrefix1");
        createPrefix(source, "aType2", "aPrefix2");
        mojo.execute();
        assertArgumentsContains("-i2jpackage", "aType1:aPrefix1.aType1");
        assertArgumentsContains("-i2jpackage", "aType2:aPrefix2.aType2");
    }

    @Test
    public void whenSymbolsDefinedWithoutValues_createSymbolArguments() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        createDefine(source, "symbol1");
        createDefine(source, "symbol2");
        mojo.execute();
        assertArgumentsContains("-Dsymbol1");
        assertArgumentsContains("-Dsymbol2");
    }

    @Test
    public void whenSymbolsDefinedWithValues_createSymbolArguments() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        createDefine(source, "symbol1", "value1");
        createDefine(source, "symbol2", "value2");
        mojo.execute();
        assertArgumentsContains("-Dsymbol1=value1");
        assertArgumentsContains("-Dsymbol2=value2");
    }

    @Test
    public void whenEmitStubsOnly_generateNoSkelArgument() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        setGenerateStubs(source, true);
        setGenerateSkeletons(source, false);
        mojo.execute();
        assertArgumentsContains("-noskel");
    }

    @Test
    public void whenEmitSkeletonsOnly_generateNoStubArgument() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        setGenerateStubs(source, false);
        setGenerateSkeletons(source, true);
        mojo.execute();
        assertArgumentsContains("-nostub");
    }

    @Test
    public void whenAdditionalArgumentsSpecified_copyToCommand() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        defineAdditionalArguments(source, "-arg1", "arg2");
        mojo.execute();
        assertArgumentsContains("-arg1", "arg2");
    }
}
