/*
// $Id$
// Farrago is a relational database management system.
// Copyright (C) 2004-2004 John V. Sichi.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2.1
// of the License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sf.farrago.ddl;

import org.eigenbase.util.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.type.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.resource.*;
import net.sf.farrago.query.*;
import net.sf.farrago.session.*;
import net.sf.farrago.type.*;
import net.sf.farrago.util.*;
import net.sf.farrago.plugin.*;
import net.sf.farrago.namespace.*;
import net.sf.farrago.namespace.util.*;

import net.sf.farrago.cwm.core.*;
import net.sf.farrago.cwm.relational.*;
import net.sf.farrago.cwm.relational.enumerations.*;
import net.sf.farrago.fem.med.*;
import net.sf.farrago.fem.sql2003.*;

import java.io.*;
import java.nio.charset.*;
import java.sql.*;
import java.util.*;

/**
 * DdlRelationalHandler defines DDL handler methods for SQL/MED objects.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class DdlMedHandler extends DdlHandler
{
    public DdlMedHandler(DdlValidator validator)
    {
        super(validator);
    }

    // implement FarragoSessionDdlHandler
    public void validateDefinition(FemForeignTable foreignTable)
    {
        validateForeignColumnSetDefinition(foreignTable);
    }
    
    public void validateForeignColumnSetDefinition(
        FemBaseColumnSet columnSet)
    {
        FemDataServer dataServer = columnSet.getServer();
        FemDataWrapper dataWrapper = dataServer.getWrapper();

        if (!dataWrapper.isForeign()) {
            throw validator.res.newValidatorForeignTableButLocalWrapper(
                repos.getLocalizedObjectName(columnSet),
                repos.getLocalizedObjectName(dataWrapper));
        }
        
        validateBaseColumnSet(columnSet);

        FarragoMedColumnSet medColumnSet =
            validateMedColumnSet(columnSet);

        List columnList = columnSet.getFeature();
        if (columnList.isEmpty()) {
            // derive column information
            RelDataType rowType = medColumnSet.getRowType();
            RelDataTypeField [] fields = rowType.getFields();
            for (int i = 0; i < fields.length; ++i) {
                FemStoredColumn column = repos.newFemStoredColumn();
                columnList.add(column);
                convertFieldToCwmColumn(fields[i], column);
                validateAttribute(column);
            }
        }
    }
    
    // implement FarragoSessionDdlHandler
    public void validateDefinition(FemDataServer femServer)
    {
        // since servers are in the same namespace with CWM catalogs,
        // need a special name uniquness check here
        validator.validateUniqueNames(
            repos.getCatalog(FarragoRepos.SYSBOOT_CATALOG_NAME),
            repos.getRelationalPackage().getCwmCatalog().refAllOfType(),
            false);

        try {
            // validate that we can successfully initialize the server
            validator.getDataWrapperCache().loadServerFromCatalog(femServer);
        } catch (Throwable ex) {
            throw validator.res.newValidatorDefinitionInvalid(
                repos.getLocalizedObjectName(femServer),
                ex);
        }

        // REVIEW jvs 18-April-2004:  This uses default charset/collation
        // info from local catalog, but should really allow foreign
        // servers to override.
        repos.initializeCatalog(femServer);

        // REVIEW jvs 18-April-2004:  Query the plugin for these?
        if (femServer.getType() == null) {
            femServer.setType("UNKNOWN");
        }
        if (femServer.getVersion() == null) {
            femServer.setVersion("UNKNOWN");
        }

        validator.createDependency(
            femServer,
            Collections.singleton(femServer.getWrapper()),
            "WrapperAccessesServer");
    }
    
    // implement FarragoSessionDdlHandler
    public void validateDefinition(FemDataWrapper femWrapper)
    {
        FarragoMedDataWrapper wrapper;
        try {
            if (!femWrapper.getLibraryFile().startsWith(
                    FarragoDataWrapperCache.LIBRARY_CLASS_PREFIX))
            {
                // convert library filename to absolute path, if necessary
                String libraryFile = femWrapper.getLibraryFile();

                String expandedLibraryFile =
                    FarragoProperties.instance().expandProperties(libraryFile);

                // REVIEW: SZ: 7/20/2004: Maybe the library should
                // always be an absolute path?  (e.g. Always report an
                // error if the path given by the user is relative.)
                // If a user installs a thirdparty Data Wrapper we
                // probably don't want them using relative paths to
                // call out its location.
                if (libraryFile.equals(expandedLibraryFile)) {
                    // No properties were expanded, so make the path
                    // absolute if it isn't already absolute.
                    File file = new File(libraryFile);
                    femWrapper.setLibraryFile(file.getAbsolutePath());
                } else {
                    // Test that the expanded library file is an
                    // absolute path.  We don't set the absolute path
                    // because we want to keep the property in the
                    // library name.
                    File file = new File(expandedLibraryFile);
                    if (!file.isAbsolute()) {
                        throw new IOException(libraryFile
                            + " does not evaluate to an absolute path");
                    }
                }
            }

            // validate that we can successfully initialize the wrapper
            wrapper = validator.getDataWrapperCache().loadWrapperFromCatalog(
                femWrapper);
        } catch (Throwable ex) {
            throw validator.res.newValidatorDefinitionInvalid(
                repos.getLocalizedObjectName(femWrapper),
                ex);
        }

        if (femWrapper.isForeign()) {
            if (!wrapper.isForeign()) {
                throw validator.res.newValidatorForeignWrapperHasLocalImpl(
                    repos.getLocalizedObjectName(femWrapper));
            }
        } else {
            if (wrapper.isForeign()) {
                throw validator.res.newValidatorLocalWrapperHasForeignImpl(
                    repos.getLocalizedObjectName(femWrapper));
            }
        }
    }

    // implement FarragoSessionDdlHandler
    public void executeDrop(FemDataServer server)
    {
        validator.discardDataWrapper(server);
    }

    // implement FarragoSessionDdlHandler
    public void executeDrop(FemDataWrapper wrapper)
    {
        validator.discardDataWrapper(wrapper);
    }
    
    public FarragoMedColumnSet validateMedColumnSet(
        FemBaseColumnSet femColumnSet)
    {
        FarragoMedColumnSet medColumnSet;

        try {
            // validate that we can successfully initialize the table
            medColumnSet =
                validator.getDataWrapperCache().loadColumnSetFromCatalog(
                    femColumnSet,
                    validator.getTypeFactory());
        } catch (Throwable ex) {
            throw validator.res.newValidatorDataServerTableInvalid(
                repos.getLocalizedObjectName(femColumnSet),
                ex);
        }

        validator.createDependency(
            femColumnSet,
            Collections.singleton(femColumnSet.getServer()),
            "ServerProvidesColumnSet");

        return medColumnSet;
    }
}

// End DdlMedHandler.java