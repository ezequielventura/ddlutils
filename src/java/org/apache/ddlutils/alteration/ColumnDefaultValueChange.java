package org.apache.ddlutils.alteration;

/*
 * Copyright 2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;

/**
 * Represents the change of the default value of a column.
 * 
 * @version $Revision: $
 */
public class ColumnDefaultValueChange
{
    /** The table of the column. */
    private Table _table;
    /** The column. */
    private Column _column;
    /** The new default value. */
    private String _newDefaultValue;

    /**
     * Creates a new change object.
     * 
     * @param table           The table of the column
     * @param column          The column
     * @param newDefaultValue The new default value
     */
    public ColumnDefaultValueChange(Table table, Column column, String newDefaultValue)
    {
        _table           = table;
        _column          = column;
        _newDefaultValue = newDefaultValue;
    }

    /**
     * Returns the column.
     *
     * @return The column
     */
    public Column getColumn()
    {
        return _column;
    }

    /**
     * Returns the new default value.
     *
     * @return The new default value
     */
    public String getNewDefaultValue()
    {
        return _newDefaultValue;
    }

    /**
     * Returns the table of the column.
     *
     * @return The table
     */
    public Table getTable()
    {
        return _table;
    }
}