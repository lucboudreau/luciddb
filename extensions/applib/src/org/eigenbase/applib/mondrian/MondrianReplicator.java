/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/
package org.eigenbase.applib.mondrian;

import java.io.*;

import java.sql.*;

import java.util.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.cwm.core.*;
import net.sf.farrago.cwm.relational.*;
import net.sf.farrago.ddl.gen.*;
import net.sf.farrago.fem.med.*;
import net.sf.farrago.fem.sql2003.*;
import net.sf.farrago.runtime.*;
import net.sf.farrago.util.*;

import org.eigenbase.applib.resource.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.util.*;
import org.eigenbase.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;


/**
 * Implements an <a href="http://docs.eigenbase.org/LucidDbMondrianReplication">
 * automatic Mondrian replication procedure</a>.
 *
 * @author John Sichi
 * @version $Id$
 */
class MondrianReplicator
    implements ClosableAllocation
{
    //~ Enums ------------------------------------------------------------------

    /**
     * Purposes inferred for columns of interest.
     */
    private static enum ColumnType
    {
        INDEXED_ATTR, PRIMARY_KEY
    }

    //~ Instance fields --------------------------------------------------------

    private final File mondrianSchemaFile;

    private final File scriptFile;

    private final String foreignServerName;

    private final String foreignSchemaName;

    private final String localSchemaName;

    private final boolean copyData;

    private final Map<List<String>, ColumnType> columnMap;

    private final FarragoRepos repos;

    private Statement stmt;

    private FileWriter scriptFileWriter;

    private PrintWriter scriptWriter;

    private boolean success;

    //~ Constructors -----------------------------------------------------------

    MondrianReplicator(
        File mondrianSchemaFile,
        String foreignServerName,
        String foreignSchemaName,
        String localSchemaName,
        File scriptFile,
        boolean copyData)
        throws Exception
    {
        this.mondrianSchemaFile = mondrianSchemaFile;
        this.foreignServerName = foreignServerName;
        this.foreignSchemaName = foreignSchemaName;
        this.localSchemaName = localSchemaName;
        this.scriptFile = scriptFile;
        this.copyData = copyData;

        repos = FarragoUdrRuntime.getSession().getRepos();
        columnMap = new LinkedHashMap<List<String>, ColumnType>();
    }

    //~ Methods ----------------------------------------------------------------

    // override ClosableAllocation
    public void closeAllocation()
    {
        if (scriptFileWriter == null) {
            return;
        }
        try {
            if (!success) {
                scriptWriter.println();
                scriptWriter.print(
                    "-- Script generation incomplete due to errors!");
                scriptWriter.println();
            }
            Util.squelchWriter(scriptWriter);
        } finally {
            Util.squelchWriter(scriptFileWriter);
            scriptFileWriter = null;
        }
    }

    void execute()
        throws Exception
    {
        // TODO jvs 8-Jul-2007:  some validation would be nice for better
        // error message on missing file/server/schema/etc.

        Connection conn =
            DriverManager.getConnection("jdbc:default:connection");
        stmt = conn.createStatement();

        FileReader fileReader = new FileReader(mondrianSchemaFile);
        if (scriptFile != null) {
            scriptFileWriter = new FileWriter(scriptFile);
            scriptWriter = new PrintWriter(scriptFileWriter, true);
        } else {
            scriptWriter = new PrintWriter(new StringWriter());
        }
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        MondrianSchemaHandler handler = new MondrianSchemaHandler();
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
        xmlReader.parse(new InputSource(fileReader));
        for (String tableName : handler.getTableNames()) {
            replicateTable(tableName);
        }
        success = true;
    }

    private void processSql(
        String sql,
        boolean dml)
        throws Exception
    {
        scriptWriter.print(sql);
        scriptWriter.print(";");
        scriptWriter.println();
        if (!dml || copyData) {
            int rowCount;
            try {
                rowCount = stmt.executeUpdate(sql);
            } catch (Throwable ex) {
                throw ApplibResource.instance().SqlStatementExecutionFailed.ex(
                    sql,
                    ex);
            }
            if (dml) {
                scriptWriter.print(
                    "-- Initial load:  " + rowCount + " rows");
                scriptWriter.println();
            }
        }
        scriptWriter.println();
    }

    private void replicateTable(String tableName)
        throws Exception
    {
        SqlDialect dialect = SqlDialect.EIGENBASE;

        String linkTableName = tableName + "_link";
        String sql =
            "CREATE FOREIGN TABLE "
            + dialect.quoteIdentifier(localSchemaName)
            + "."
            + dialect.quoteIdentifier(linkTableName)
            + Util.lineSeparator
            + "SERVER "
            + dialect.quoteIdentifier(foreignServerName)
            + " OPTIONS(SCHEMA_NAME "
            + dialect.quoteStringLiteral(foreignSchemaName)
            + ", TABLE_NAME "
            + dialect.quoteStringLiteral(tableName)
            + ")";
        processSql(sql, false);

        String columnDefs;
        FarragoReposTxnContext txn = repos.newTxnContext(true);
        try {
            txn.beginReadTxn();
            columnDefs = generateColumnDefs(linkTableName, tableName);
        } finally {
            txn.commit();
        }

        sql =
            "CREATE TABLE "
            + dialect.quoteIdentifier(localSchemaName)
            + "."
            + dialect.quoteIdentifier(tableName)
            + columnDefs;
        processSql(sql, false);
        for (List<String> names : columnMap.keySet()) {
            if (!names.get(0).equals(tableName)) {
                continue;
            }
            ColumnType columnType = columnMap.get(names);
            if (columnType != ColumnType.INDEXED_ATTR) {
                continue;
            }
            String ddl =
                "CREATE INDEX "
                + dialect.quoteIdentifier(
                    names.get(0)
                    + "_"
                    + names.get(1)
                    + "_idx")
                + Util.lineSeparator
                + "ON "
                + dialect.quoteIdentifier(localSchemaName)
                + "."
                + dialect.quoteIdentifier(names.get(0))
                + "("
                + dialect.quoteIdentifier(names.get(1))
                + ")";
            processSql(ddl, false);
        }
        sql =
            "INSERT INTO "
            + dialect.quoteIdentifier(localSchemaName)
            + "."
            + dialect.quoteIdentifier(tableName)
            + " SELECT * FROM "
            + dialect.quoteIdentifier(localSchemaName)
            + "."
            + dialect.quoteIdentifier(linkTableName);
        processSql(sql, true);
        sql =
            "DROP TABLE "
            + dialect.quoteIdentifier(localSchemaName)
            + "."
            + dialect.quoteIdentifier(linkTableName);
        processSql(sql, false);
    }

    private String generateColumnDefs(
        String linkTableName,
        String tableName)
    {
        FemLocalSchema femSchema =
            FarragoCatalogUtil.getSchemaByName(
                repos.getSelfAsCatalog(),
                localSchemaName);
        FemForeignTable foreignTable =
            FarragoCatalogUtil.getModelElementByNameAndType(
                femSchema.getOwnedElement(),
                linkTableName,
                FemForeignTable.class);
        List<String> primaryKeyColumns = new ArrayList<String>();
        for (CwmFeature feature : foreignTable.getFeature()) {
            List<String> names = new ArrayList<String>();
            names.add(tableName);
            names.add(feature.getName());
            ColumnType columnType = columnMap.get(names);
            if (columnType == ColumnType.PRIMARY_KEY) {
                primaryKeyColumns.add(feature.getName());
            }
        }
        if (primaryKeyColumns.isEmpty()) {
            primaryKeyColumns = null;
        }
        FarragoDdlGenerator ddlGen =
            new FarragoDdlGenerator(SqlDialect.EIGENBASE, null);
        SqlBuilder sb = new SqlBuilder(SqlDialect.EIGENBASE);
        ddlGen.generateColumnsAndKeys(
            sb,
            Util.cast(foreignTable.getFeature(), CwmColumn.class),
            true,
            false,
            primaryKeyColumns);
        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * SAX parse handler for extracting information of interest to us from a
     * Mondrian schema file.
     */
    class MondrianSchemaHandler
        extends DefaultHandler
    {
        private Set<String> tableNames;
        private List<String> tableNameStack;
        private String currentTableName;
        private String pendingPrimaryKey;

        MondrianSchemaHandler()
        {
            tableNameStack = new ArrayList<String>();
            tableNames = new LinkedHashSet<String>();
        }

        // override DefaultHandler
        public void startElement(
            String uri,
            String name,
            String qName,
            Attributes attrs)
        {
            // TODO jvs 8-Jul-2007:  index Join keys; agg tables
            if (name.equals("Table")) {
                currentTableName = attrs.getValue("name");
                String tableSchemaName = attrs.getValue("schema");
                if (tableSchemaName != null) {
                    if (!tableSchemaName.equals(localSchemaName)) {
                        FarragoReposTxnContext txn = repos.newTxnContext(true);
                        try {
                            txn.beginReadTxn();
                            throw ApplibResource.instance()
                            .MondrianSchemaMismatch.ex(
                                repos.getLocalizedObjectName(tableSchemaName),
                                repos.getLocalizedObjectName(currentTableName),
                                repos.getLocalizedObjectName(localSchemaName));
                        } finally {
                            txn.commit();
                        }
                    }
                }
                tableNames.add(currentTableName);
                if (pendingPrimaryKey != null) {
                    addPendingPrimaryKey();
                }
            } else if (name.equals("Cube")) {
                pushTableName();
            } else if (name.equals("Hierarchy")) {
                assert (pendingPrimaryKey == null);
                pushTableName();
                pendingPrimaryKey = attrs.getValue("primaryKey");
                if (pendingPrimaryKey != null) {
                    currentTableName = attrs.getValue("primaryKeyTable");
                    if (currentTableName != null) {
                        addPendingPrimaryKey();
                    }
                }
            } else if (name.equals("Level")) {
                addColumn(
                    attrs.getValue("column"),
                    attrs.getValue("table"),
                    ColumnType.INDEXED_ATTR);
            } else if (name.equals("DimensionUsage")) {
                addColumn(
                    attrs.getValue("foreignKey"),
                    null,
                    ColumnType.INDEXED_ATTR);
            }
        }

        // override DefaultHandler
        public void endElement(
            String uri,
            String localName,
            String qName)
        {
            if (localName.equals("Hierarchy") || localName.equals("Cube")) {
                popTableName();
            }
        }

        private void pushTableName()
        {
            tableNameStack.add(currentTableName);
        }

        private void popTableName()
        {
            currentTableName = tableNameStack.remove(tableNameStack.size() - 1);
        }

        private void addPendingPrimaryKey()
        {
            addColumn(
                pendingPrimaryKey,
                null,
                ColumnType.PRIMARY_KEY);
            pendingPrimaryKey = null;
        }

        private void addColumn(
            String columnName,
            String tableName,
            ColumnType columnType)
        {
            if (columnName == null) {
                return;
            }
            List<String> names = new ArrayList<String>();
            if (tableName == null) {
                assert (currentTableName != null);
                names.add(currentTableName);
            } else {
                names.add(tableName);
            }
            names.add(columnName);
            columnMap.put(names, columnType);
        }

        Set<String> getTableNames()
        {
            return tableNames;
        }
    }
}

// End MondrianReplicator.java
