package org.apache.ddlutils.platform.mysql;

/*
 * Copyright 1999-2006 The Apache Software Foundation.
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

import java.sql.SQLException;
import java.util.Map;

import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.platform.DatabaseMetaDataWrapper;

/**
 * Reads a database model from a MySql 5 database.
 *
 * @author Martin van den Bemt
 * @version $Revision: $
 */
public class MySql50ModelReader extends MySqlModelReader
{
    /**
     * Creates a new model reader for MySql 5 databases.
     * 
     * @param platformInfo The platform specific settings
     */
    public MySql50ModelReader(PlatformInfo platformInfo)
    {
        super(platformInfo);
    }

    /**
     * {@inheritDoc}
     */
    protected Column readColumn(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        Column column = super.readColumn(metaData, values);

        // make sure the defaultvalue is null when an empty is returned.
        if ("".equals(column.getDefaultValue()))
        {
            column.setDefaultValue(null);
        }
        return column;
    }
    
}
