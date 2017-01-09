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

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.SystemPropertySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests access using the Built-in IDL compiler
 */
public class IDLJTestCase extends IdljCommonTests {

    private static final String ORACLE_JDK_IDL_CLASS = "com.sun.tools.corba.se.idl.toJavaPortable.Compile";
    private static final String IBM_JDK_IDL_CLASS = "com.ibm.idl.toJavaPortable.Compile";
    private static final String GLASSFISH_IDL_CLASS = "com.sun.tools.corba.ee.idl.toJavaPortable.Compile";

    private List<Memento> mementos = new ArrayList<>();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mementos.add(SystemPropertySupport.preserve("java.vm.vendor"));
        mementos.add(SystemPropertySupport.preserve("java.version"));
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        for (Memento memento : mementos) memento.revert();
    }

    @Test
    public void whenJdkCompilerSpecified_chooseOracleJdkCompiler() throws Exception {
        defineCompiler("idlj");

        mojo.execute();

        assertEquals(ORACLE_JDK_IDL_CLASS, getIdlCompilerClass());
    }

    @Test
    public void whenGlassfishCompilerSpecified_chooseGlassfishCompiler() throws Exception {
        defineCompiler("glassfish");

        mojo.execute();

        assertEquals(GLASSFISH_IDL_CLASS, getIdlCompilerClass());
    }

    @Test
    public void whenCompilerNotSpecifiedAndNoModuleSystem_chooseOracleJdkCompiler() throws Exception {
        assumeTrue( isBuiltInOrbPresent() );

        mojo.execute();

        assertEquals(ORACLE_JDK_IDL_CLASS, getIdlCompilerClass());
    }

    private boolean isBuiltInOrbPresent()
    {
        try
        {
            Class.forName( "javax.rmi.PortableRemoteObject" );
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }

    @Test
    public void whenCompilerNotSpecifiedAndModuleSystemPresent_chooseGlassfishCompiler() throws Exception {
        System.setProperty("java.version", "9.0");

        mojo.execute();

        assertEquals(GLASSFISH_IDL_CLASS, getIdlCompilerClass());
    }

    @Test(expected = MojoExecutionException.class)
    public void whenUnknownCompilerSpecified_throwException() throws Exception {
        defineCompiler("unknown");

        mojo.execute();
    }

    @Test
    public void whenVMNameContainsIBM_chooseIBMIDLCompiler() throws Exception {
        assumeTrue( isBuiltInOrbPresent());

        System.setProperty("java.vm.vendor", "pretend it is IBM");
        mojo.execute();
        assertEquals(IBM_JDK_IDL_CLASS, getIdlCompilerClass());
    }

    @Test(expected = MojoExecutionException.class)
    public void whenCompilerNotFound_throwException() throws Exception {
        declareAllClassesNotFound();

        mojo.execute();
    }

    private void declareAllClassesNotFound()
    {
        setClassNotFoundFilter(new ClassNotFoundFilter() {
            @Override
            public boolean throwException(URL... prependedUrls) {
                return true;
            }
        });
    }

    @Test
    public void whenCompilerNotFound_tryAgainWithToolsJar() throws Exception {
        declareAllClassesNotFound();

        try {
            mojo.execute();
        } catch (MojoExecutionException ignored) {
        }

        assertTrue(isToolsJarSpecified());
    }

    private boolean containsToolsJar(URL[] prependedUrls) {
        for (URL url : prependedUrls)
            if (!url.getPath().contains("tools.jar")) return true;

        return true;
    }

    @Test
    public void whenCompilerInToolsJar_locateCompiler() throws Exception {
        setClassNotFoundFilter(new ClassNotFoundFilter() {
            @Override
            public boolean throwException(URL... prependedUrls) {
                return !containsToolsJar(prependedUrls);
            }
        });

        mojo.execute();
    }

    @Test
    public void whenBuiltInCompilerSelectedAndModuleSystemPresent_errorRecommendsGlassfish() throws Exception {
        defineCompiler("idlj");
        declareAllClassesNotFound();

        System.setProperty("java.version", "9.0");

        try {
            mojo.execute();
            fail("Did not report compiler class not found");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), Matchers.containsString("Use the glassfish compiler"));
        }
    }
}
