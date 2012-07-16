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

/**
 * Attributes for performing package translation.
 */
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
