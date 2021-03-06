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
package org.eigenbase.rel.metadata;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.sql.*;


/**
 * RelMdExplainVisibility supplies a default implementation of {@link
 * RelMetadataQuery#isVisibleInExplain} for the standard logical algebra.
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class RelMdExplainVisibility
    extends ReflectiveRelMetadataProvider
{
    //~ Constructors -----------------------------------------------------------

    public RelMdExplainVisibility()
    {
        // Tell superclass reflection about parameter types expected
        // for various metadata queries.

        // This corresponds to isVisibileInExplain(RelNode, SqlExplainLevel);
        // note that we don't specify the rel type because we always overload
        // on that.
        mapParameterTypes(
            "isVisibleInExplain",
            Collections.singletonList((Class) SqlExplainLevel.class));
    }

    //~ Methods ----------------------------------------------------------------

    // Catch-all rule when none of the others apply.
    public Boolean isVisibleInExplain(RelNode rel)
    {
        // no information available
        return null;
    }
}

// End RelMdExplainVisibility.java
