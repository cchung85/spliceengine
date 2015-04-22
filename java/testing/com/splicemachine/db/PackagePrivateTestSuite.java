/*

   Derby - Class com.splicemachine.db.PackagePrivateTestSuite

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package com.splicemachine.db;

import com.splicemachine.db.impl.jdbc._Suite;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.splicemachine.dbTesting.junit.BaseTestCase;

public class PackagePrivateTestSuite
    extends BaseTestCase {

    /**
     * Use the {@link #suite} method instead.
     */
    private PackagePrivateTestSuite(String name) {
        super(name);
    }

    public static Test suite() throws Exception {

        TestSuite suite = new TestSuite("Package-private tests");

        suite.addTest(_Suite.suite());
        suite.addTest(com.splicemachine.db.client.am._Suite.suite());

        return suite;
    }

}
