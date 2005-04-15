package org.apache.commons.sql.builder;

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 *
 * $Id$
 */
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.sql.io.DatabaseReader;
import org.apache.commons.sql.model.Database;

/**
 * Test harness for the SqlBuilder for various databases.
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision$
 */
public class TestBuilder extends TestCase
{
    private Database database;
    private String baseDir;
 
    /**
     * A unit test suite for JUnit
     */
    public static Test suite()
    {
        return new TestSuite(TestBuilder.class);
    }

    /**
     * Constructor for the TestBuilder object
     *
     * @param testName
     */
    public TestBuilder(String testName)
    {
        super(testName);
    }

    /**
     * The JUnit setup method
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        baseDir = System.getProperty("basedir", ".");
        String uri = baseDir + "/src/test-input/datamodel.xml";
        
        DatabaseReader reader = new DatabaseReader ();
        database = (Database) reader.parse(new FileInputStream(uri));
        assertTrue("Loaded a valid database", database != null);

    }

    /**
     * A unit test for JUnit
     */
    public void testBuilders()
        throws Exception
    {

        testBuilder( new AxionBuilder(), "axion.sql" );
        testBuilder( new HsqlDbBuilder(), "hsqldb.sql" );
        testBuilder( new MSSqlBuilder(), "mssql.sql" );        
        testBuilder( new MySqlBuilder(), "mysql.sql" );
        testBuilder( new OracleBuilder(), "oracle.sql" );
        testBuilder( new PostgreSqlBuilder(), "postgres.sql" );
        testBuilder( new SybaseBuilder(), "sybase.sql" );

    }
    
    /**
     * A unit test for JUnit
     */
    public void testBaseBuilder()
        throws Exception
    {
    
        SqlBuilder builder = new HsqlDbBuilder();
        StringWriter sw = new StringWriter();
        builder.setWriter(sw);
        builder.dropDatabase(database);       

        String drop = sw.toString();
        int bookIdx = drop.indexOf("drop table book");
        int authIdx = drop.indexOf("drop table author");

        assertTrue("dropDatabase Failed to create proper drop statement for " +
                    "book table. Here is the statment created:\n" + drop, 
                    bookIdx > 0);
        
        assertTrue("dropDatabase Failed to create proper drop statement for " +
                    "author table. Here is the statment created:\n" + drop,
                     authIdx > 0);

        Writer wr = builder.getWriter();
        assertTrue("Couldnt find writer", wr != null);  


    }

    protected void testBuilder(SqlBuilder builder, String fileName) throws Exception 
    {

        String name = baseDir + "/target/" + fileName;
        
        FileWriter writer = new FileWriter( name );
        builder.setWriter( writer );
        builder.createDatabase( database );
        writer.close();

    }
}

