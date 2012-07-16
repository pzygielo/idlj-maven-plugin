package org.codehaus.mojo.idlj;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests access using the Sun IDL compiler
 */
public class IDLJTestCase extends IDLJTestBase {

    @Test
    public void whenCompilerNotSpecified_chooseSunCompiler() throws Exception {
        mojo.execute();
        assertEquals( "com.sun.tools.corba.se.idl.toJavaPortable.Compile", loaderFacade.getIdlCompilerClass() );
    }

    @Test
    public void whenVMNameContainsIBM_chooseIBMIDLCompiler() throws Exception {
        System.setProperty("java.vm.vendor", "pretend it is IBM");
        mojo.execute();
        assertEquals("com.ibm.idl.toJavaPortable.Compile", loaderFacade.getIdlCompilerClass());
    }

    @Test
    public void whenVMNameContainsApple_loadClassesJar() throws Exception {
        System.setProperty( "java.vm.vendor", "pretend it is Apple" );
        mojo.execute();
        assertTrue(getPrependedUrls().contains("Classes/classes.jar"));
    }

    @Test(expected = MojoExecutionException.class)
    public void whenErrorMessageGenerated_failMojoStep() throws Exception {
        setFailOnError();
        TestIdlCompiler.defineErrorMessage("oops");
        mojo.execute();
    }

    @Test
    public void whenNoOptionsAreSpecified_useCurrentDirectoryAsIncludePath() throws Exception {
        mojo.execute();
        assertArgumentsContains("-i", getCurrentDir() + "/src/main/idl");
    }

    @Test
    public void whenIncludePathIsSpecified_createIncludeArguments() throws Exception {
        defineIncludePaths("/src/main/idl-include");
        mojo.execute();
        assertArgumentsContains("-i", "/src/main/idl-include");
    }

    @Test(expected = MojoExecutionException.class)
    public void whenSinglePackagePrefixDefined_throwException() throws Exception {
        Source source = createSource();
        defineSinglePrefix(source, "aPrefix1");
        mojo.execute();
        assertArgumentsContains("-pkgPrefix", "aType1", "aPrefix1");
    }

    @Test
    public void whenPackagePrefixDefined_createPrefixArguments() throws Exception {
        Source source = createSource();
        createPrefix(source, "aType1", "aPrefix1");
        createPrefix(source, "aType2", "aPrefix2");
        mojo.execute();
        assertArgumentsContains("-pkgPrefix", "aType1", "aPrefix1");
        assertArgumentsContains("-pkgPrefix", "aType2", "aPrefix2");
    }

    @Test
    public void whenPackageTranslationDefined_createTranslationArguments() throws Exception {
        Source source = createSource();
        createTranslation(source, "aType1", "aPackage1");
        createTranslation(source, "aType2", "aPackage2");
        mojo.execute();
        assertArgumentsContains("-pkgTranslate", "aType1", "aPackage1");
        assertArgumentsContains("-pkgTranslate", "aType2", "aPackage2");
    }

    @Test(expected = MojoExecutionException.class)
    public void whenSymbolDefineWithValue_throwException() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        createDefine(source, "symbol1", "value1");
        mojo.execute();
    }

    @Test
    public void whenSymbolsDefined_createSymbolArguments() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        createDefine(source, "symbol1");
        createDefine(source, "symbol2");
        mojo.execute();
        assertArgumentsContains("-d", "symbol1");
        assertArgumentsContains("-d", "symbol2");
    }

    @Test
    public void whenNoOptionsAreSpecified_generateFallArguments() throws Exception {
        mojo.execute();
        assertArgumentsContains("-fallTIE");
    }

    @Test
    public void whenEmitStubsOnly_generateFClientArgument() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        setGenerateStubs(source, true);
        setGenerateSkeletons(source, false);
        mojo.execute();
        assertArgumentsContains("-fclient");
    }

    @Test
    public void whenEmitSkeletonsOnly_generateFServerArgument() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        setGenerateStubs(source, false);
        setGenerateSkeletons(source, true);
        mojo.execute();
        assertArgumentsContains("-fserver");
    }

    @Test
    public void whenEmitNeitherStubsNorSkeletons_generateFServerTieArgument() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        setGenerateStubs(source, false);
        setGenerateSkeletons(source, false);
        mojo.execute();
        assertArgumentsContains("-fserverTIE");
    }

    @Test
    public void whenAdditionalArgumentsSpecified_copyToCommand() throws NoSuchFieldException, IllegalAccessException, MojoExecutionException {
        Source source = createSource();
        defineAdditionalArguments(source, "-arg1", "arg2");
        mojo.execute();
        assertArgumentsContains("-arg1", "arg2");
    }

}
