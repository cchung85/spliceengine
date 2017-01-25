/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.splicemachine.db.iapi.types;

import org.apache.hadoop.hbase.util.*;
import org.apache.spark.sql.catalyst.expressions.UnsafeRow;
import org.apache.spark.sql.catalyst.expressions.codegen.BufferHolder;
import org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 *
 * Test Class for SQLTinyint
 *
 */
public class SQLRefTest extends SQLDataValueDescriptorTest {

        @Test
        public void serdeValueData() throws Exception {
                UnsafeRow row = new UnsafeRow(1);
                UnsafeRowWriter writer = new UnsafeRowWriter(new BufferHolder(row),1);
                SQLRef value = new SQLRef(new SQLRowId("1".getBytes()));
                SQLRef valueA = new SQLRef();
                writer.reset();
                value.write(writer, 0);
                valueA.read(row,0);
                Assert.assertEquals("SerdeIncorrect",(Object) new SQLRowId("1".getBytes()),(Object) valueA.getObject());
        }

        @Test
        public void serdeNullValueData() throws Exception {
                UnsafeRow row = new UnsafeRow(1);
                UnsafeRowWriter writer = new UnsafeRowWriter(new BufferHolder(row),1);
                SQLRef value = new SQLRef();
                SQLRef valueA = new SQLRef();
                value.write(writer, 0);
                Assert.assertTrue("SerdeIncorrect", row.isNullAt(0));
                value.read(row, 0);
                Assert.assertTrue("SerdeIncorrect", valueA.isNull());
        }
    
        @Test
        public void serdeKeyData() throws Exception {
                SQLRef value1 = new SQLRef(new SQLRowId("1".getBytes()));
                SQLRef value2 = new SQLRef(new SQLRowId("2".getBytes()));
                SQLRef value1a = new SQLRef();
                SQLRef value2a = new SQLRef();
                PositionedByteRange range1 = new SimplePositionedMutableByteRange(value1.encodedKeyLength());
                PositionedByteRange range2 = new SimplePositionedMutableByteRange(value2.encodedKeyLength());
                value1.encodeIntoKey(range1, Order.ASCENDING);
                value2.encodeIntoKey(range2, Order.ASCENDING);
                Assert.assertTrue("Positioning is Incorrect", Bytes.compareTo(range1.getBytes(), 0, 9, range2.getBytes(), 0, 9) < 0);
                range1.setPosition(0);
                range2.setPosition(0);
                value1a.decodeFromKey(range1);
                value2a.decodeFromKey(range2);
                Assert.assertEquals("1 incorrect",value1.getObject(),value1a.getObject());
                Assert.assertEquals("2 incorrect",value2.getObject(),value2a.getObject());
        }

        @Test
        public void testArray() throws Exception {
                UnsafeRow row = new UnsafeRow(1);
                UnsafeRowWriter writer = new UnsafeRowWriter(new BufferHolder(row),1);
                SQLArray value = new SQLArray();
                value.setType(new SQLRef(new SQLRowId()));
                value.setValue(new DataValueDescriptor[] {new SQLRef(new SQLRowId("1".getBytes())),new SQLRef(new SQLRowId("435".getBytes())),
                        new SQLRef(new SQLRowId("----".getBytes())), new SQLRef(new SQLRowId())});
                SQLArray valueA = new SQLArray();
                valueA.setType(new SQLRef(new SQLRowId()));
                writer.reset();
                value.write(writer,0);
                valueA.read(row,0);
                Assert.assertTrue("SerdeIncorrect", Arrays.equals(value.value,valueA.value));
        }
}
