/*
// $Id$
// Fennel is a relational database kernel.
// Copyright (C) 2004-2005 John V. Sichi.
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

#include "fennel/common/CommonPreamble.h"
#include "fennel/exec/MockConsumerExecStream.h"
#include "fennel/exec/ExecStreamBufAccessor.h"
#include "fennel/exec/ExecStreamGraph.h"
#include "fennel/tuple/TuplePrinter.h"

FENNEL_BEGIN_CPPFILE("$Id$");

ExecStreamResult MockConsumerExecStream::execute(
    ExecStreamQuantum const &quantum)
{
    ExecStreamBufAccessor &inAccessor = *pInAccessor;
    switch (inAccessor.getState()) {
    case EXECBUF_EMPTY:
        inAccessor.requestProduction();
        return EXECRC_BUF_UNDERFLOW;
    case EXECBUF_UNDERFLOW:
        return EXECRC_BUF_UNDERFLOW;
    case EXECBUF_EOS:
        return EXECRC_EOS;
    case EXECBUF_NONEMPTY:
    case EXECBUF_OVERFLOW:
        break;
    default:
        permFail("Bad state " << inAccessor.getState());
    }
    assert(inAccessor.isConsumptionPossible());

    TuplePrinter tuplePrinter;

    // Read rows from the input buffer until we exceed the quantum or read all
    // of the rows. Convert each row to a string, and append to the rows
    // vector.
    for (uint iRow = 0; iRow < quantum.nTuplesMax; ++iRow) {
        if (!inAccessor.demandData()) {
            // Convert buf return code into stream return code.
            switch (inAccessor.getState()) {
            case EXECBUF_UNDERFLOW:
                return EXECRC_BUF_UNDERFLOW;
            case EXECBUF_EOS:
                return EXECRC_EOS;
            default:
                permAssert(false);
            }
        }
        inAccessor.unmarshalTuple(inputTuple);

        if (echoData) {
            tuplePrinter.print(*echoData, inAccessor.getTupleDesc(), inputTuple);
        }
        if (saveData) {
            std::ostringstream oss;
            tuplePrinter.print(oss, inAccessor.getTupleDesc(), inputTuple);
            const string &s = oss.str();
            rowStrings.push_back(s);
        }
        inAccessor.consumeTuple();        
    }
    return EXECRC_QUANTUM_EXPIRED;
}

void MockConsumerExecStream::prepare(
    MockConsumerExecStreamParams const &params)
{
    SingleInputExecStream::prepare(params);
    saveData = params.saveData;
    echoData = params.echoData;
}

void MockConsumerExecStream::open(bool restart)
{
    SingleInputExecStream::open(restart);
    rowStrings.clear();
    inputTuple.compute(pInAccessor->getTupleDesc());
}


FENNEL_END_CPPFILE("$Id$");

// End MockConsumerExecStream.cpp