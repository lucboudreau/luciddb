/*
// $Id$
// Package org.eigenbase is a class library of database components.
// Copyright (C) 2002-2004 Disruptive Tech
// Copyright (C) 2003-2004 John V. Sichi
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
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
package org.eigenbase.rel;

import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptUtil;
import org.eigenbase.reltype.RelDataType;

/**
 * A relational expression which extract "collapses"
 * of multiply rows from one row
 *
 * @author Wael Chatila 
 * @since Dec 12, 2004
 * @version $Id$
 */
public class UncollectRel extends SingleRel {

    public UncollectRel(RelOptCluster cluster, RelNode child) {
        super(cluster, child);
    }

    // override Object (public, does not throw CloneNotSupportedException)
    public Object clone() {
        return new UncollectRel(cluster, RelOptUtil.clone(child));
    }

    protected RelDataType deriveRowType()
    {
        RelDataType ret = child.getRowType().getComponentType();
        assert(null!=ret);
        return ret;
    }
}