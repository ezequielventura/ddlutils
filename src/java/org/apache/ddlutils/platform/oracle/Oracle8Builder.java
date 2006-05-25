package org.apache.ddlutils.platform.oracle;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import java.io.IOException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ddlutils.DdlUtilsException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.alteration.AddColumnChange;
import org.apache.ddlutils.alteration.AddPrimaryKeyChange;
import org.apache.ddlutils.alteration.PrimaryKeyChange;
import org.apache.ddlutils.alteration.RemoveColumnChange;
import org.apache.ddlutils.alteration.RemovePrimaryKeyChange;
import org.apache.ddlutils.alteration.TableChange;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.model.TypeMap;
import org.apache.ddlutils.platform.SqlBuilder;
import org.apache.ddlutils.util.Jdbc3Utils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * The SQL Builder for Oracle.
 *
 * @author James Strachan
 * @author Thomas Dudziak
 * @version $Revision$
 */
public class Oracle8Builder extends SqlBuilder
{
	/** The regular expression pattern for ISO dates, i.e. 'YYYY-MM-DD'. */
	private Pattern _isoDatePattern;
	/** The regular expression pattern for ISO times, i.e. 'HH:MI:SS'. */
	private Pattern _isoTimePattern;
	/** The regular expression pattern for ISO timestamps, i.e. 'YYYY-MM-DD HH:MI:SS.fffffffff'. */
	private Pattern _isoTimestampPattern;

	/**
     * Creates a new builder instance.
     * 
     * @param platform The plaftform this builder belongs to
     */
    public Oracle8Builder(Platform platform)
    {
        super(platform);
        addEscapedCharSequence("'", "''");

        PatternCompiler compiler = new Perl5Compiler();

    	try
    	{
            _isoDatePattern      = compiler.compile("\\d{4}\\-\\d{2}\\-\\d{2}");
            _isoTimePattern      = compiler.compile("\\d{2}:\\d{2}:\\d{2}");
            _isoTimestampPattern = compiler.compile("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}[\\.\\d{1,8}]?");
        }
    	catch (MalformedPatternException ex)
        {
        	throw new DdlUtilsException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dropTable(Table table) throws IOException
    {
        print("DROP TABLE ");
        printIdentifier(getTableName(table));
        print(" CASCADE CONSTRAINTS");
        printEndOfStatement();

        Column[] columns = table.getAutoIncrementColumns();

        for (int idx = 0; idx < columns.length; idx++)
        {
            print("DROP TRIGGER ");
            printIdentifier(getConstraintName("trg", table, columns[idx].getName(), null));
            printEndOfStatement();
            print("DROP SEQUENCE ");
            printIdentifier(getConstraintName("seq", table, columns[idx].getName(), null));
            printEndOfStatement();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dropExternalForeignKeys(Table table) throws IOException
    {
        // no need to as we drop the table with CASCASE CONSTRAINTS
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternalIndexDropStmt(Table table, Index index) throws IOException
    {
        // Index names in Oracle are unique to a schema and hence Oracle does not
        // use the ON <tablename> clause
        print("DROP INDEX ");
        printIdentifier(getIndexName(index));
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     */
    public void createTable(Database database, Table table, Map parameters) throws IOException
    {
        // lets create any sequences
        Column[] columns = table.getAutoIncrementColumns();

        for (int idx = 0; idx < columns.length; idx++)
        {
            print("CREATE SEQUENCE ");
            printIdentifier(getConstraintName("seq", table, columns[idx].getName(), null));
            printEndOfStatement();
        }

        super.createTable(database, table, parameters);

        for (int idx = 0; idx < columns.length; idx++)
        {
            String columnName  = getColumnName(columns[idx]);
            String triggerName = getConstraintName("trg", table, columns[idx].getName(), null);

            // note that the BEGIN ... SELECT ... END; is all in one line and does
            // not contain a semicolon except for the END-one
            // this way, the tokenizer will not split the statement before the END
            print("CREATE OR REPLACE TRIGGER ");
            printIdentifier(triggerName);
            print(" BEFORE INSERT ON ");
            printIdentifier(getTableName(table));
            print(" FOR EACH ROW WHEN (new.");
            printIdentifier(columnName);
            println(" IS NULL)");
            print("BEGIN SELECT ");
            printIdentifier(getConstraintName("seq", table, columns[idx].getName(), null));
            print(".nextval INTO :new.");
            printIdentifier(columnName);
            print(" FROM dual");
            print(getPlatformInfo().getSqlCommandDelimiter());
            print(" END");
            // It is important that there is a semicolon at the end of the statement (or more
            // precisely, at the end of the PL/SQL block), and thus we put two semicolons here
            // because the tokenizer will remove the one at the end
            print(getPlatformInfo().getSqlCommandDelimiter());
            printEndOfStatement();
        }
    }

	/**
     * {@inheritDoc}
     */
    protected void printDefaultValue(Object defaultValue, int typeCode) throws IOException
    {
        if (defaultValue != null)
        {
            String  defaultValueStr = defaultValue.toString();
            boolean shouldUseQuotes = !TypeMap.isNumericType(typeCode) && !defaultValueStr.startsWith("TO_DATE(");
    
            if (shouldUseQuotes)
            {
                // characters are only escaped when within a string literal 
                print(getPlatformInfo().getValueQuoteToken());
                print(escapeStringValue(defaultValueStr));
                print(getPlatformInfo().getValueQuoteToken());
            }
            else
            {
                print(defaultValueStr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected String getNativeDefaultValue(Column column)
    {
        if ((column.getTypeCode() == Types.BIT) ||
            (Jdbc3Utils.supportsJava14JdbcTypes() && (column.getTypeCode() == Jdbc3Utils.determineBooleanTypeCode())))
        {
            return getDefaultValueHelper().convert(column.getDefaultValue(), column.getTypeCode(), Types.SMALLINT).toString();
        }
    	// Oracle does not accept ISO formats, so we have to convert an ISO spec if we find one
    	// But these are the only formats that we make sure work, every other format has to be database-dependent
    	// and thus the user has to ensure that it is correct
        else if (column.getTypeCode() == Types.DATE)
        {
            if (new Perl5Matcher().matches(column.getDefaultValue(), _isoDatePattern))
            {
            	return "TO_DATE('"+column.getDefaultValue()+"', 'YYYY-MM-DD')";
            }
        }
        else if (column.getTypeCode() == Types.TIME)
        {
            if (new Perl5Matcher().matches(column.getDefaultValue(), _isoTimePattern))
            {
            	return "TO_DATE('"+column.getDefaultValue()+"', 'HH24:MI:SS')";
            }
        }
        else if (column.getTypeCode() == Types.TIMESTAMP)
        {
            if (new Perl5Matcher().matches(column.getDefaultValue(), _isoTimestampPattern))
            {
            	return "TO_DATE('"+column.getDefaultValue()+"', 'YYYY-MM-DD HH24:MI:SS')";
            }
        }
        return super.getNativeDefaultValue(column);
    }

    /**
     * {@inheritDoc}
     */
    protected void writeColumnAutoIncrementStmt(Table table, Column column) throws IOException
    {
        // we're using sequences instead
    }

    /**
     * {@inheritDoc}
     */
    protected void createTemporaryTable(Database database, Table table, Map parameters) throws IOException
    {
        // we don't want the auto-increment triggers/sequences for the temporary table
        super.createTable(database, table, parameters);
    }

    /**
     * {@inheritDoc}
     */
    protected void dropTemporaryTable(Database database, Table table) throws IOException
    {
        // likewise, we don't need to drop a sequence or trigger
        super.dropTable(table);
    }

    /**
     * {@inheritDoc}
     */
    protected void processTableStructureChanges(Database currentModel,
                                                Database desiredModel,
                                                Table    sourceTable,
                                                Table    targetTable,
                                                Map      parameters,
                                                List     changes) throws IOException
    {
        // First we drop primary keys as necessary
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof RemovePrimaryKeyChange)
            {
                processChange(currentModel, desiredModel, (RemovePrimaryKeyChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
            else if (change instanceof PrimaryKeyChange)
            {
                PrimaryKeyChange       pkChange       = (PrimaryKeyChange)change;
                RemovePrimaryKeyChange removePkChange = new RemovePrimaryKeyChange(pkChange.getChangedTable(),
                                                                                   pkChange.getOldPrimaryKeyColumns());

                processChange(currentModel, desiredModel, removePkChange);
                removePkChange.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
            }
        }

        // Next we add/remove columns
        // While Oracle has an ALTER TABLE MODIFY statement, it is somewhat limited
        // esp. if there is data in the table, so we don't use it
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof AddColumnChange)
            {
                AddColumnChange addColumnChange = (AddColumnChange)change;

                // Oracle can only add not insert columns
                if (addColumnChange.isAtEnd())
                {
                    processChange(currentModel, desiredModel, addColumnChange);
                    change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                    changeIt.remove();
                }
            }
            else if (change instanceof RemoveColumnChange)
            {
                processChange(currentModel, desiredModel, (RemoveColumnChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
        }
        // Finally we add primary keys
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof AddPrimaryKeyChange)
            {
                processChange(currentModel, desiredModel, (AddPrimaryKeyChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
            else if (change instanceof PrimaryKeyChange)
            {
                PrimaryKeyChange    pkChange    = (PrimaryKeyChange)change;
                AddPrimaryKeyChange addPkChange = new AddPrimaryKeyChange(pkChange.getChangedTable(),
                                                                          pkChange.getNewPrimaryKeyColumns());

                processChange(currentModel, desiredModel, addPkChange);
                addPkChange.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
        }
    }

    /**
     * Processes the addition of a column to a table.
     * 
     * @param currentModel The current database schema
     * @param desiredModel The desired database schema
     * @param change       The change object
     */
    protected void processChange(Database        currentModel,
                                 Database        desiredModel,
                                 AddColumnChange change) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()));
        printIndent();
        print("ADD ");
        writeColumn(change.getChangedTable(), change.getNewColumn());
        printEndOfStatement();
    }

    /**
     * Processes the removal of a column from a table.
     * 
     * @param currentModel The current database schema
     * @param desiredModel The desired database schema
     * @param change       The change object
     */
    protected void processChange(Database           currentModel,
                                 Database           desiredModel,
                                 RemoveColumnChange change) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()));
        printIndent();
        print("DROP COLUMN ");
        printIdentifier(getColumnName(change.getColumn()));
        printEndOfStatement();
    }

    /**
     * Processes the removal of a primary key from a table.
     * 
     * @param currentModel The current database schema
     * @param desiredModel The desired database schema
     * @param change       The change object
     */
    protected void processChange(Database               currentModel,
                                 Database               desiredModel,
                                 RemovePrimaryKeyChange change) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()));
        printIndent();
        print("DROP PRIMARY KEY");
        printEndOfStatement();
    }
}
