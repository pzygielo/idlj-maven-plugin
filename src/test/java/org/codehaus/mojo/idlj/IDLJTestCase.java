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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests access using the Built-in IDL compiler
 */
public class IDLJTestCase extends IdljCommonTests {

    private static final String ORACLE_JDK_IDL_CLASS = "com.sun.tools.corba.se.idl.toJavaPortable.Compile";
    private static final String IBM_JDK_IDL_CLASS = "com.ibm.idl.toJavaPortable.Compile";

    @Test
    public void whenDefaultCompilerSpecified_chooseOracleJdkCompiler() throws Exception {
        defineCompiler("idlj");

        mojo.execute();

        assertEquals(ORACLE_JDK_IDL_CLASS, getIdlCompilerClass());
    }

    @Test
    public void whenCompilerNotSpecified_chooseOracleJdkCompiler() throws Exception {
        mojo.execute();

        assertEquals(ORACLE_JDK_IDL_CLASS, getIdlCompilerClass());
    }

    @Test(expected = MojoExecutionException.class)
    public void whenUnknownCompilerSpecified_throwException() throws Exception {
        defineCompiler("unknown");

        mojo.execute();
    }

    @Test
    public void whenVMNameContainsIBM_chooseIBMIDLCompiler() throws Exception {
        System.setProperty("java.vm.vendor", "pretend it is IBM");
        mojo.execute();
        assertEquals(IBM_JDK_IDL_CLASS, getIdlCompilerClass());
    }

}
