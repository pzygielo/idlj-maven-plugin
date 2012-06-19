package org.codehaus.mojo.idlj;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.fail;

public class IDLJTestBase {
    protected static String[] args;
    private Properties savedProperties;
    protected TestClassloaderFacade loaderFacade = new TestClassloaderFacade();
    private TestScanner testScanner = new TestScanner();
    private TestDependenciesFacade testDependenciesFacade = new TestDependenciesFacade();
    private TestLog log = new TestLog();
    protected IDLJMojo mojo;

    @Before
    public void setUp() throws Exception {
        args = null;
        savedProperties = (Properties) System.getProperties().clone();
        AbstractTranslator.setClassLoaderFacade(loaderFacade);

        TestDependenciesFacade testDependenciesFacade1 = testDependenciesFacade;
        mojo = new IDLJMojo(testDependenciesFacade1);
        ignoreMavenProject();
        defineSourceDirectory("src/main/idl");
        defineOutputDirectory("target/main/generatedSources/idl");
        defineTimestampDirectory("target/main/timeStamps");
        mojo.setLog(log);
        testScanner.includedSources.add(new File("src/main/idl/dummy.idl"));
    }

    @After
    public void tearDown() {
        System.setProperties( savedProperties );
    }

    private void ignoreMavenProject() throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(mojo, "project", new MavenProject((Model) null));
    }

    private void defineTimestampDirectory(String path) throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue( mojo, "timestampDirectory", new File( path ) );
    }

    private void defineOutputDirectory(String path) throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue( mojo, "outputDirectory", new File( path ) );
    }

    private void defineSourceDirectory(String path) throws NoSuchFieldException, IllegalAccessException {
        mojo.setSourceDirectory(new File(path));
        testDependenciesFacade.readOnlyDirectories.add( new File( path ) );
    }

    protected void setPrivateFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Class theClass = obj.getClass();
        setPrivateFieldValue(obj, theClass, fieldName, value);
    }

    private void setPrivateFieldValue(Object obj, Class theClass, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        try {
            Field field = theClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            if (theClass.equals(Object.class))
                throw e;
            else
                setPrivateFieldValue(obj, theClass.getSuperclass(), fieldName, value);
        }
    }

    protected String getPrependedUrls() {
        StringBuilder sb = new StringBuilder( );
        for (URL url : loaderFacade.prependedURLs)
            sb.append( ':' ).append( url );
        return sb.toString();
    }

    protected final void defineSinglePrefix(Source source, String aPrefix) throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(source, "packagePrefix", aPrefix);
    }

    protected final void createPrefix(Source source, String aType, String aPrefix) throws NoSuchFieldException, IllegalAccessException {
        PackagePrefix prefix = createPrefix(source);
        setPrivateFieldValue(prefix, "type", aType);
        setPrivateFieldValue(prefix, "prefix", aPrefix);
    }

    private PackagePrefix createPrefix(Source source) throws NoSuchFieldException, IllegalAccessException {
        List<PackagePrefix> prefixes = getPrefixes(source);
        PackagePrefix prefix = new PackagePrefix();
        prefixes.add(prefix);
        return prefix;
    }

    private List<PackagePrefix> getPrefixes(Source source) throws NoSuchFieldException, IllegalAccessException {
        List<PackagePrefix> prefixes = (List<PackagePrefix>) getPrivateFieldValue(source, "packagePrefixes");
        if (prefixes == null)
            setPrivateFieldValue(source, "packagePrefixes", prefixes = new ArrayList<PackagePrefix>());
        return prefixes;
    }

    protected final void createTranslation(Source source, String aType, String aPackage) throws NoSuchFieldException,
                                                                                          IllegalAccessException {
        PackageTranslation translation = createTranslation( source );
        setPrivateFieldValue(translation, "type", aType);
        setPrivateFieldValue(translation, "replacementPackage", aPackage);
    }

    private PackageTranslation createTranslation(Source source) throws NoSuchFieldException, IllegalAccessException {
        List<PackageTranslation> translations = getTranslations( source );
        PackageTranslation translation = new PackageTranslation();
        translations.add(translation);
        return translation;
    }

    private List<PackageTranslation> getTranslations(Source source)
            throws NoSuchFieldException, IllegalAccessException {
        List<PackageTranslation> translations =
                (List<PackageTranslation>) getPrivateFieldValue(source, "packageTranslations");
        if (translations == null)
            setPrivateFieldValue(source, "packageTranslations", translations = new ArrayList<PackageTranslation>());
        return translations;
    }

    protected Object getPrivateFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return getPrivateFieldValue(obj, obj.getClass(), fieldName);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateFieldValue(Object obj, Class theClass, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        try {
            Field field = theClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException e) {
            if (theClass.equals(Object.class))
                throw e;
            else
                return (T) getPrivateFieldValue(obj, theClass.getSuperclass(), fieldName);
        }
    }

    protected final Source createSource() throws NoSuchFieldException, IllegalAccessException {
        Source source = new Source();
        getSources().add(source);
        return source;
    }

    private List<Source> getSources() throws NoSuchFieldException, IllegalAccessException {
        List<Source> sources = (List<Source>) getPrivateFieldValue(mojo, "sources");
        if (sources == null)
            setPrivateFieldValue(mojo, "sources", sources = new ArrayList<Source>());
        return sources;
    }

    protected final void createDefine(Source source, String aName, String aValue)
            throws NoSuchFieldException, IllegalAccessException {
        Define define = createDefine(source);
        setPrivateFieldValue(define, "symbol", aName);
        setPrivateFieldValue(define, "value", aValue);
    }

    protected Define createDefine(Source source) throws NoSuchFieldException, IllegalAccessException {
        List<Define> defines = getDefines(source);
        Define define = new Define();
        defines.add(define);
        return define;
    }

    private List<Define> getDefines(Source source) throws NoSuchFieldException, IllegalAccessException {
        List<Define> defines = (List<Define>) getPrivateFieldValue(source, "defines");
        if (defines == null)
            setPrivateFieldValue(source, "defines", defines = new ArrayList<Define>());
        return defines;
    }

    protected final void setGenerateStubs(Source source, boolean generateStubs)
            throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(source, "emitStubs", generateStubs);
    }

    protected final void setGenerateSkeletons(Source source, boolean generateSkeletons)
            throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(source, "emitSkeletons", generateSkeletons);
    }

    final protected void defineCompiler(String compiler) throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(mojo, "compiler", compiler);
    }

    protected final void createDefine(Source source, String aName) throws NoSuchFieldException, IllegalAccessException {
        Define define = createDefine(source);
        setPrivateFieldValue(define, "symbol", aName);
    }

    protected final String getCurrentDir() {
        return System.getProperty("user.dir").replace('\\','/');
    }

    protected final void assertArgumentsContains(String... expectedArgs) {
        if (!contains(args, expectedArgs))
            fail( toArgumentString( expectedArgs ) + " not found in " + toArgumentString(args));
    }

    private boolean contains(String[] container, String[] candidate) {
        for (int i = 0; i < container.length - candidate.length + 1; i++)
            if (isSubArrayAt(container, i, candidate)) return true;
        return false;
    }

    private boolean isSubArrayAt( String[] container, int start, String[] candidate ) {
        for (int j = 0; j < candidate.length; j++)
            if (!container[start+j].equals( candidate[j])) return false;
        return true;
    }

    private String toArgumentString(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args)
            sb.append( arg ).append( ' ' );
        return sb.toString().trim();
    }

    protected final void setFailOnError() throws NoSuchFieldException, IllegalAccessException {
        setPrivateFieldValue(mojo, "failOnError", true);
    }

    protected final void defineIncludePaths(String... paths) throws NoSuchFieldException, IllegalAccessException {
        File[] dirs = new File[ paths.length ];
        for (int i = 0; i < dirs.length; i++)
            dirs[i] = new File( paths[i] );
        setPrivateFieldValue(mojo, "includeDirs", dirs);
    }

    protected final void defineAdditionalArguments(Source source, String... additionalArguments)
            throws NoSuchFieldException, IllegalAccessException {
        List<String> arguments = Arrays.asList(additionalArguments);
        setPrivateFieldValue(source, "additionalArguments", arguments);
    }

    static class TestClassloaderFacade implements AbstractTranslator.ClassLoaderFacade {

        private List<URL> prependedURLs = new ArrayList<URL>();
        private List<String> loadedClasses = new ArrayList<String>();
        private String idlCompilerClass;

        public void prependUrls(URL... urls) {
            prependedURLs.addAll(Arrays.asList(urls));
        }

        public Class loadClass(String className) throws ClassNotFoundException {
            loadedClasses.add(className);
            idlCompilerClass = className;
            return TestIdlCompiler.class;
        }

        protected String getIdlCompilerClass() {
            return idlCompilerClass;
        }
    }

    static class TestIdlCompiler {
        private static String errorMessage;

        public static void main(String... args) {
            IDLJTestBase.args = new String[ args.length];
            for (int i = 0; i < args.length; i++)
                IDLJTestBase.args[i] = args[i].replace('\\','/');

            if ( errorMessage != null )
                System.err.println( errorMessage );
        }

        public static void compile(String... args) {
            main(args);
        }

        static void defineErrorMessage(String message) {
            errorMessage = message;
        }
    }

    static class TestScanner implements SourceInclusionScanner {

        private List<SourceMapping> mappings = new ArrayList<SourceMapping>();
        private Set<File> includedSources = new HashSet<File>();

        public void addSourceMapping(SourceMapping sourceMapping) {
            mappings.add(sourceMapping);
        }

        public Set getIncludedSources(File sourceDir, File targetDir) throws InclusionScanException {
            return includedSources;
        }
    }

    static class TestLog implements org.apache.maven.plugin.logging.Log {
        public boolean isDebugEnabled() {
            return false;
        }

        public void debug(CharSequence charSequence) {
        }

        public void debug(CharSequence charSequence, Throwable throwable) {
        }

        public void debug(Throwable throwable) {
        }

        public boolean isInfoEnabled() {
            return false;
        }

        public void info(CharSequence charSequence) {
        }

        public void info(CharSequence charSequence, Throwable throwable) {
        }

        public void info(Throwable throwable) {
        }

        public boolean isWarnEnabled() {
            return false;
        }

        public void warn(CharSequence charSequence) {
        }

        public void warn(CharSequence charSequence, Throwable throwable) {
        }

        public void warn(Throwable throwable) {
        }

        public boolean isErrorEnabled() {
            return false;
        }

        public void error(CharSequence charSequence) {
        }

        public void error(CharSequence charSequence, Throwable throwable) {
        }

        public void error(Throwable throwable) {
        }
    }

    class TestDependenciesFacade implements AbstractIDLJMojo.DependenciesFacade {
        List<File> sourceFiles = new ArrayList<File>();
        List<File> targetFiles = new ArrayList<File>();
        List<File> writeableDirectories = new ArrayList<File>();
        List<File> readOnlyDirectories = new ArrayList<File>();

        public SourceInclusionScanner createSourceInclusionScanner(int updatedWithinMsecs, Set includes, Set excludes) {
            return testScanner;
        }

        public void copyFile(File sourceFile, File targetFile) throws IOException {
            sourceFiles.add(sourceFile);
            targetFiles.add(targetFile);
        }

        public boolean exists(File directory) {
            return isDirectory(directory);
        }

        public void createDirectory(File directory) {
            writeableDirectories.add(directory);
        }

        public boolean isWriteable(File directory) {
            return writeableDirectories.contains(directory);
        }

        public boolean isDirectory(File file) {
            return writeableDirectories.contains(file) || readOnlyDirectories.contains(file);
        }
    }
}
