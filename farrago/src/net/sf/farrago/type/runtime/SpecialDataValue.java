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
package net.sf.farrago.type.runtime;

/**
 * SpecialDataValue is an interface representing a runtime holder for a data
 * value. It may be used as an alternative to {@link DataValue}.
 *
 * <p>Since DataValue is typically used return Jdbc data, this class may be used
 * to return non-Jdbc data.
 *
 * @version $Id$
 */
public interface SpecialDataValue
{
    //~ Methods ----------------------------------------------------------------

    /**
     * @return an Object representation of this value's data, or null if this
     * value is null
     */
    Object getSpecialData();
}

// End SpecialDataValue.java
