/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2006-2009 LucidEra, Inc.
// Copyright (C) 2006-2009 The Eigenbase Project
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.lucidera.lcs;

import net.sf.farrago.fem.sql2003.*;
import net.sf.farrago.namespace.impl.*;

import org.eigenbase.rel.*;


/**
 * LcsColumnMetadata is an implementation of MedAbstractColumnMetadata for
 * LcsRowScanRel
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class LcsColumnMetadata
    extends MedAbstractColumnMetadata
{
    //~ Methods ----------------------------------------------------------------

    protected int mapColumnToField(
        RelNode rel,
        FemAbstractColumn keyCol)
    {
        return ((LcsRowScanRel) rel).getProjectedColumnOrdinal(
            keyCol.getOrdinal());
    }

    protected int mapFieldToColumnOrdinal(RelNode rel, int fieldNo)
    {
        FemAbstractColumn col = mapFieldToColumn(rel, fieldNo);
        if (col == null) {
            return -1;
        }
        return col.getOrdinal();
    }

    protected FemAbstractColumn mapFieldToColumn(RelNode rel, int fieldNo)
    {
        return ((LcsRowScanRel) rel).getColumnForFieldAccess(fieldNo);
    }
}

// End LcsColumnMetadata.java