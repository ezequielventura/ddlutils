package org.apache.ddlutils.io;

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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.beanutils.DynaBean;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

/**
 * Contains misc tests.
 * 
 * @version $Revision: $
 */
public class TestMisc extends RoundtripTestBase
{
    /**
     * Parameterized test case pattern.
     * 
     * @return The tests
     */
    public static Test suite() throws Exception
    {
        return getTests(TestMisc.class);
    }

    /**
     * Tests the backup and restore of a table with an identity column and a foreign key to
     * it when identity override is turned on.
     */
    public void testIdentityOverrideOn() throws Exception
    {
        if (!getPlatformInfo().isIdentityOverrideAllowed())
        {
            // TODO: for testing these platforms, we need deleteRows
            return;
        }
        final String modelXml = 
            "<?xml version='1.0' encoding='ISO-8859-1'?>\n"+
            "<database name='roundtriptest'>\n"+
            "  <table name='misc1'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true' autoIncrement='true'/>\n"+
            "    <column name='avalue' type='INTEGER' required='false'/>\n"+
            "  </table>\n"+
            "  <table name='misc2'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true'/>\n"+
            "    <column name='fk' type='INTEGER' required='false'/>\n"+
            "    <foreign-key name='test' foreignTable='misc1'>\n"+
            "      <reference local='fk' foreign='pk'/>\n"+
            "    </foreign-key>\n"+
            "  </table>\n"+
            "</database>";

        createDatabase(modelXml);

        getPlatform().setIdentityOverrideOn(true);

        insertRow("misc1", new Object[] { new Integer(10), new Integer(1) });
        insertRow("misc1", new Object[] { new Integer(12), new Integer(2) });
        insertRow("misc1", new Object[] { new Integer(13), new Integer(3) });
        insertRow("misc2", new Object[] { new Integer(1), new Integer(10) });
        insertRow("misc2", new Object[] { new Integer(2), new Integer(13) });

        StringWriter   stringWriter = new StringWriter();
        DatabaseDataIO dataIO       = new DatabaseDataIO();

        dataIO.writeDataToXML(getPlatform(), stringWriter, "UTF-8");

        String    dataAsXml = stringWriter.toString();
        SAXReader reader    = new SAXReader();
        Document  testDoc   = reader.read(new InputSource(new StringReader(dataAsXml)));

        List misc1Rows = testDoc.selectNodes("//misc1");
        List misc2Rows = testDoc.selectNodes("//misc2");

        if (misc1Rows.size() > 0)
        {
            assertEquals(3, misc1Rows.size());
            assertEquals("10", ((Element)misc1Rows.get(0)).attributeValue("pk"));
            assertEquals("1",  ((Element)misc1Rows.get(0)).attributeValue("avalue"));
            assertEquals("12", ((Element)misc1Rows.get(1)).attributeValue("pk"));
            assertEquals("2",  ((Element)misc1Rows.get(1)).attributeValue("avalue"));
            assertEquals("13", ((Element)misc1Rows.get(2)).attributeValue("pk"));
            assertEquals("3",  ((Element)misc1Rows.get(2)).attributeValue("avalue"));
            assertEquals(2, misc2Rows.size());
            assertEquals("1",  ((Element)misc2Rows.get(0)).attributeValue("pk"));
            assertEquals("10", ((Element)misc2Rows.get(0)).attributeValue("fk"));
            assertEquals("2",  ((Element)misc2Rows.get(1)).attributeValue("pk"));
            assertEquals("13", ((Element)misc2Rows.get(1)).attributeValue("fk"));
        }
        else
        {
            misc1Rows = testDoc.selectNodes("//MISC1");
            misc2Rows = testDoc.selectNodes("//MISC2");

            assertEquals(3, misc1Rows.size());
            assertEquals("10", ((Element)misc1Rows.get(0)).attributeValue("PK"));
            assertEquals("1",  ((Element)misc1Rows.get(0)).attributeValue("AVALUE"));
            assertEquals("12", ((Element)misc1Rows.get(1)).attributeValue("PK"));
            assertEquals("2",  ((Element)misc1Rows.get(1)).attributeValue("AVALUE"));
            assertEquals("13", ((Element)misc1Rows.get(2)).attributeValue("PK"));
            assertEquals("3",  ((Element)misc1Rows.get(2)).attributeValue("AVALUE"));
            assertEquals(2, misc2Rows.size());
            assertEquals("1",  ((Element)misc2Rows.get(0)).attributeValue("PK"));
            assertEquals("10", ((Element)misc2Rows.get(0)).attributeValue("FK"));
            assertEquals("2",  ((Element)misc2Rows.get(1)).attributeValue("PK"));
            assertEquals("13", ((Element)misc2Rows.get(1)).attributeValue("FK"));
        }

        dropDatabase();
        createDatabase(modelXml);

        StringReader stringReader = new StringReader(dataAsXml);

        dataIO.writeDataToDatabase(getPlatform(), new Reader[] { stringReader });

        List beans = getRows("misc1");

        assertEquals(new Integer(10), beans.get(0), "pk");
        assertEquals(new Integer(1),  beans.get(0), "avalue");
        assertEquals(new Integer(12), beans.get(1), "pk");
        assertEquals(new Integer(2),  beans.get(1), "avalue");
        assertEquals(new Integer(13), beans.get(2), "pk");
        assertEquals(new Integer(3),  beans.get(2), "avalue");

        beans = getRows("misc2");

        assertEquals(new Integer(1),  beans.get(0), "pk");
        assertEquals(new Integer(10), beans.get(0), "fk");
        assertEquals(new Integer(2),  beans.get(1), "pk");
        assertEquals(new Integer(13), beans.get(1), "fk");
    }

    /**
     * Tests the backup and restore of a table with an identity column and a foreign key to
     * it when identity override is turned off.
     */
    public void testIdentityOverrideOff() throws Exception
    {
        if (!getPlatformInfo().isIdentityOverrideAllowed())
        {
            // TODO: for testing these platforms, we need deleteRows
            return;
        }

        final String modelXml = 
            "<?xml version='1.0' encoding='ISO-8859-1'?>\n"+
            "<database name='roundtriptest'>\n"+
            "  <table name='misc1'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true' autoIncrement='true'/>\n"+
            "    <column name='avalue' type='INTEGER' required='false'/>\n"+
            "  </table>\n"+
            "  <table name='misc2'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true'/>\n"+
            "    <column name='fk' type='INTEGER' required='false'/>\n"+
            "    <foreign-key name='test' foreignTable='misc1'>\n"+
            "      <reference local='fk' foreign='pk'/>\n"+
            "    </foreign-key>\n"+
            "  </table>\n"+
            "</database>";

        createDatabase(modelXml);

        getPlatform().setIdentityOverrideOn(true);

        insertRow("misc1", new Object[] { new Integer(10), new Integer(1) });
        insertRow("misc1", new Object[] { new Integer(12), new Integer(2) });
        insertRow("misc1", new Object[] { new Integer(13), new Integer(3) });
        insertRow("misc2", new Object[] { new Integer(1), new Integer(10) });
        insertRow("misc2", new Object[] { new Integer(2), new Integer(13) });

        StringWriter   stringWriter = new StringWriter();
        DatabaseDataIO dataIO       = new DatabaseDataIO();

        dataIO.writeDataToXML(getPlatform(), stringWriter, "UTF-8");

        String    dataAsXml = stringWriter.toString();
        SAXReader reader    = new SAXReader();
        Document  testDoc   = reader.read(new InputSource(new StringReader(dataAsXml)));

        List misc1Rows = testDoc.selectNodes("//misc1");
        List misc2Rows = testDoc.selectNodes("//misc2");

        if (misc1Rows.size() > 0)
        {
            assertEquals(3, misc1Rows.size());
            assertEquals("10", ((Element)misc1Rows.get(0)).attributeValue("pk"));
            assertEquals("1",  ((Element)misc1Rows.get(0)).attributeValue("avalue"));
            assertEquals("12", ((Element)misc1Rows.get(1)).attributeValue("pk"));
            assertEquals("2",  ((Element)misc1Rows.get(1)).attributeValue("avalue"));
            assertEquals("13", ((Element)misc1Rows.get(2)).attributeValue("pk"));
            assertEquals("3",  ((Element)misc1Rows.get(2)).attributeValue("avalue"));
            assertEquals(2, misc2Rows.size());
            assertEquals("1",  ((Element)misc2Rows.get(0)).attributeValue("pk"));
            assertEquals("10", ((Element)misc2Rows.get(0)).attributeValue("fk"));
            assertEquals("2",  ((Element)misc2Rows.get(1)).attributeValue("pk"));
            assertEquals("13", ((Element)misc2Rows.get(1)).attributeValue("fk"));
        }
        else
        {
            misc1Rows = testDoc.selectNodes("//MISC1");
            misc2Rows = testDoc.selectNodes("//MISC2");

            assertEquals(3, misc1Rows.size());
            assertEquals("10", ((Element)misc1Rows.get(0)).attributeValue("PK"));
            assertEquals("1",  ((Element)misc1Rows.get(0)).attributeValue("AVALUE"));
            assertEquals("12", ((Element)misc1Rows.get(1)).attributeValue("PK"));
            assertEquals("2",  ((Element)misc1Rows.get(1)).attributeValue("AVALUE"));
            assertEquals("13", ((Element)misc1Rows.get(2)).attributeValue("PK"));
            assertEquals("3",  ((Element)misc1Rows.get(2)).attributeValue("AVALUE"));
            assertEquals(2, misc2Rows.size());
            assertEquals("1",  ((Element)misc2Rows.get(0)).attributeValue("PK"));
            assertEquals("10", ((Element)misc2Rows.get(0)).attributeValue("FK"));
            assertEquals("2",  ((Element)misc2Rows.get(1)).attributeValue("PK"));
            assertEquals("13", ((Element)misc2Rows.get(1)).attributeValue("FK"));
        }

        dropDatabase();
        createDatabase(modelXml);

        getPlatform().setIdentityOverrideOn(false);

        StringReader stringReader = new StringReader(dataAsXml);

        dataIO.writeDataToDatabase(getPlatform(), new Reader[] { stringReader });

        List beans = getRows("misc1");

        assertEquals(new Integer(1), beans.get(0), "pk");
        assertEquals(new Integer(1), beans.get(0), "avalue");
        assertEquals(new Integer(2), beans.get(1), "pk");
        assertEquals(new Integer(2), beans.get(1), "avalue");
        assertEquals(new Integer(3), beans.get(2), "pk");
        assertEquals(new Integer(3), beans.get(2), "avalue");

        beans = getRows("misc2");

        assertEquals(new Integer(1), beans.get(0), "pk");
        assertEquals(new Integer(1), beans.get(0), "fk");
        assertEquals(new Integer(2), beans.get(1), "pk");
        assertEquals(new Integer(3), beans.get(1), "fk");
    }

    /**
     * Tests the backup and restore of a table with an identity column and a foreign key to
     * itself while identity override is off.
     */
    public void testSelfReferenceIdentityOverrideOff() throws Exception
    {
        final String modelXml = 
            "<?xml version='1.0' encoding='ISO-8859-1'?>\n"+
            "<database name='roundtriptest'>\n"+
            "  <table name='misc'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true' autoIncrement='true'/>\n"+
            "    <column name='fk' type='INTEGER' required='false'/>\n"+
            "    <foreign-key name='test' foreignTable='misc'>\n"+
            "      <reference local='fk' foreign='pk'/>\n"+
            "    </foreign-key>\n"+
            "  </table>\n"+
            "</database>";

        createDatabase(modelXml);

        getPlatform().setIdentityOverrideOn(false);

        insertRow("misc", new Object[] { new Integer(1), null });
        insertRow("misc", new Object[] { new Integer(2), new Integer(1) });
        insertRow("misc", new Object[] { new Integer(3), new Integer(2) });
        insertRow("misc", new Object[] { new Integer(4), new Integer(4) });

        StringWriter   stringWriter = new StringWriter();
        DatabaseDataIO dataIO       = new DatabaseDataIO();

        dataIO.writeDataToXML(getPlatform(), stringWriter, "UTF-8");

        String    dataAsXml = stringWriter.toString();
        SAXReader reader    = new SAXReader();
        Document  testDoc   = reader.read(new InputSource(new StringReader(dataAsXml)));

        List miscRows = testDoc.selectNodes("//misc");

        if (miscRows.size() > 0)
        {
            assertEquals(4, miscRows.size());
            assertEquals("1", ((Element)miscRows.get(0)).attributeValue("pk"));
            assertNull(((Element)miscRows.get(0)).attributeValue("fk"));
            assertEquals("2", ((Element)miscRows.get(1)).attributeValue("pk"));
            assertEquals("1", ((Element)miscRows.get(1)).attributeValue("fk"));
            assertEquals("3", ((Element)miscRows.get(2)).attributeValue("pk"));
            assertEquals("2", ((Element)miscRows.get(2)).attributeValue("fk"));
            assertEquals("4", ((Element)miscRows.get(3)).attributeValue("pk"));
            assertEquals("4", ((Element)miscRows.get(3)).attributeValue("fk"));
        }
        else
        {
            miscRows = testDoc.selectNodes("//MISC");

            assertEquals(4, miscRows.size());
            assertEquals("1", ((Element)miscRows.get(0)).attributeValue("PK"));
            assertNull(((Element)miscRows.get(0)).attributeValue("FK"));
            assertEquals("2", ((Element)miscRows.get(1)).attributeValue("PK"));
            assertEquals("1", ((Element)miscRows.get(1)).attributeValue("FK"));
            assertEquals("3", ((Element)miscRows.get(2)).attributeValue("PK"));
            assertEquals("2", ((Element)miscRows.get(2)).attributeValue("FK"));
            assertEquals("4", ((Element)miscRows.get(3)).attributeValue("PK"));
            assertEquals("4", ((Element)miscRows.get(3)).attributeValue("FK"));
        }

        dropDatabase();
        createDatabase(modelXml);

        StringReader stringReader = new StringReader(dataAsXml);

        dataIO.writeDataToDatabase(getPlatform(), new Reader[] { stringReader });

        List beans = getRows("misc");

        assertEquals(new Integer(1), beans.get(0), "pk");
        assertNull(((DynaBean)beans.get(0)).get("fk"));
        assertEquals(new Integer(2), beans.get(1), "pk");
        assertEquals(new Integer(1), beans.get(1), "fk");
        assertEquals(new Integer(3), beans.get(2), "pk");
        assertEquals(new Integer(2), beans.get(2), "fk");
        assertEquals(new Integer(4), beans.get(3), "pk");
        assertEquals(new Integer(4), beans.get(3), "fk");
    }

    /**
     * Tests the backup and restore of a table with an identity column and a foreign key to
     * itself while identity override is off.
     */
    public void testSelfReferenceIdentityOverrideOn() throws Exception
    {
        if (!getPlatformInfo().isIdentityOverrideAllowed())
        {
            // TODO: for testing these platforms, we need deleteRows
            return;
        }

        final String modelXml = 
            "<?xml version='1.0' encoding='ISO-8859-1'?>\n"+
            "<database name='roundtriptest'>\n"+
            "  <table name='misc'>\n"+
            "    <column name='pk' type='INTEGER' primaryKey='true' required='true' autoIncrement='true'/>\n"+
            "    <column name='fk' type='INTEGER' required='false'/>\n"+
            "    <foreign-key name='test' foreignTable='misc'>\n"+
            "      <reference local='fk' foreign='pk'/>\n"+
            "    </foreign-key>\n"+
            "  </table>\n"+
            "</database>";

        createDatabase(modelXml);

        getPlatform().setIdentityOverrideOn(true);

        insertRow("misc", new Object[] { new Integer(10), null });
        insertRow("misc", new Object[] { new Integer(11), new Integer(10) });
        insertRow("misc", new Object[] { new Integer(12), new Integer(11) });
        insertRow("misc", new Object[] { new Integer(13), new Integer(13) });

        StringWriter   stringWriter = new StringWriter();
        DatabaseDataIO dataIO       = new DatabaseDataIO();

        dataIO.writeDataToXML(getPlatform(), stringWriter, "UTF-8");

        String    dataAsXml = stringWriter.toString();
        SAXReader reader    = new SAXReader();
        Document  testDoc   = reader.read(new InputSource(new StringReader(dataAsXml)));

        List miscRows = testDoc.selectNodes("//misc");

        if (miscRows.size() > 0)
        {
            assertEquals(4, miscRows.size());
            assertEquals("10", ((Element)miscRows.get(0)).attributeValue("pk"));
            assertNull(((Element)miscRows.get(0)).attributeValue("fk"));
            assertEquals("11", ((Element)miscRows.get(1)).attributeValue("pk"));
            assertEquals("10", ((Element)miscRows.get(1)).attributeValue("fk"));
            assertEquals("12", ((Element)miscRows.get(2)).attributeValue("pk"));
            assertEquals("11", ((Element)miscRows.get(2)).attributeValue("fk"));
            assertEquals("13", ((Element)miscRows.get(3)).attributeValue("pk"));
            assertEquals("13", ((Element)miscRows.get(3)).attributeValue("fk"));
        }
        else
        {
            miscRows = testDoc.selectNodes("//MISC");

            assertEquals(4, miscRows.size());
            assertEquals("10", ((Element)miscRows.get(0)).attributeValue("PK"));
            assertNull(((Element)miscRows.get(0)).attributeValue("FK"));
            assertEquals("11", ((Element)miscRows.get(1)).attributeValue("PK"));
            assertEquals("10", ((Element)miscRows.get(1)).attributeValue("FK"));
            assertEquals("12", ((Element)miscRows.get(2)).attributeValue("PK"));
            assertEquals("11", ((Element)miscRows.get(2)).attributeValue("FK"));
            assertEquals("13", ((Element)miscRows.get(3)).attributeValue("PK"));
            assertEquals("13", ((Element)miscRows.get(3)).attributeValue("FK"));
        }

        dropDatabase();
        createDatabase(modelXml);

        StringReader stringReader = new StringReader(dataAsXml);

        dataIO.writeDataToDatabase(getPlatform(), new Reader[] { stringReader });

        List beans = getRows("misc");

        assertEquals(new Integer(10), beans.get(0), "pk");
        assertNull(((DynaBean)beans.get(0)).get("fk"));
        assertEquals(new Integer(11), beans.get(1), "pk");
        assertEquals(new Integer(10), beans.get(1), "fk");
        assertEquals(new Integer(12), beans.get(2), "pk");
        assertEquals(new Integer(11), beans.get(2), "fk");
        assertEquals(new Integer(13), beans.get(3), "pk");
        assertEquals(new Integer(13), beans.get(3), "fk");
    }
}
