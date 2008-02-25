package org.apache.ddlutils.platform.db2;

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

import java.io.IOException;
import java.sql.Types;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.alteration.ColumnDefinitionChange;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.model.TypeMap;
import org.apache.ddlutils.platform.SqlBuilder;

/**
 * The SQL Builder for DB2.
 * 
 * @version $Revision$
 */
public class Db2Builder extends SqlBuilder
{
    /**
     * Creates a new builder instance.
     * 
     * @param platform The plaftform this builder belongs to
     */
    public Db2Builder(Platform platform)
    {
        super(platform);
        addEscapedCharSequence("'", "''");
    }

    /**
     * {@inheritDoc}
     */
    protected String getNativeDefaultValue(Column column)
    {
        if ((column.getTypeCode() == Types.BIT) || (column.getTypeCode() == Types.BOOLEAN))
        {
            return getDefaultValueHelper().convert(column.getDefaultValue(), column.getTypeCode(), Types.SMALLINT);
        }
        else
        {
            return super.getNativeDefaultValue(column);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeColumnAutoIncrementStmt(Table table, Column column) throws IOException
    {
        print("GENERATED BY DEFAULT AS IDENTITY");
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectLastIdentityValues(Table table)
    {
        return "VALUES IDENTITY_VAL_LOCAL()";
    }

    /**
     * Generates the SQL to drop a column from a table.
     * 
     * @param table  The table where to drop the column from
     * @param column The column to drop
     */
    public void dropColumn(Table table, Column column) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(table));
        printIndent();
        print("DROP COLUMN ");
        printIdentifier(getColumnName(column));
        printEndOfStatement();
    }

    /**
     * Writes the SQL for dropping the primary key of the given table.
     * 
     * @param table The table
     */
    public void dropPrimaryKey(Table table) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(table));
        printIndent();
        print("DROP PRIMARY KEY");
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     */
    public void dropIndex(Table table, Index index) throws IOException
    {
        // Index names in DB2 are unique to a schema and hence Derby does not
        // use the ON <tablename> clause
        print("DROP INDEX ");
        printIdentifier(getIndexName(index));
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeCastExpression(Column sourceColumn, Column targetColumn) throws IOException
    {
        String sourceNativeType = getBareNativeType(sourceColumn);
        String targetNativeType = getBareNativeType(targetColumn);

        if (sourceNativeType.equals(targetNativeType) &&
            !ColumnDefinitionChange.isSizeChanged(getPlatformInfo(), sourceColumn, targetColumn))
        {
            printIdentifier(getColumnName(sourceColumn));
        }
        else
        {
            String type = getSqlType(targetColumn);

            // DB2 has the limitation that it cannot convert numeric values
            // to VARCHAR, though it can convert them to CHAR
            if (TypeMap.isNumericType(sourceColumn.getTypeCode()) &&
                "VARCHAR".equalsIgnoreCase(targetNativeType))
            {
                Object sizeSpec = targetColumn.getSize();
                
                if (sizeSpec == null)
                {
                    sizeSpec = getPlatformInfo().getDefaultSize(targetColumn.getTypeCode());
                }
                type = "CHAR(" +sizeSpec.toString() + ")";
            }

            print("CAST(");
            printIdentifier(getColumnName(sourceColumn));
            print(" AS ");
            print(type);
            print(")");
        }
    }
}
