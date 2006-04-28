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

import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Table;

/**
 * Represents the removal of a foreign key from a table. Note that for
 * simplicity and because it fits the model, this change actually implements
 * table change for the table that the foreign key originates.
 * 
 * @version $Revision: $
 */
public class RemoveForeignKeyChange extends TableChangeImplBase
{
    /** The foreign key. */
    private ForeignKey _foreignKey;

    /**
     * Creates a new change object.
     * 
     * @param table      The table to remove the foreign key from
     * @param foreignKey The foreign key
     */
    public RemoveForeignKeyChange(Table table, ForeignKey foreignKey)
    {
        super(table);
        _foreignKey = foreignKey;
    }

    /**
     * Returns the foreign key to be removed.
     *
     * @return The foreign key
     */
    public ForeignKey getForeignKey()
    {
        return _foreignKey;
    }
}
