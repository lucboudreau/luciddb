/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2006-2006 Disruptive Tech
// Copyright (C) 2006-2006 The Eigenbase Project
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

#include "fennel/common/CommonPreamble.h"
#include "fennel/test/TestBase.h"
#include "fennel/common/TraceSource.h"

#include "fennel/tuple/TupleData.h"
#include "fennel/tuple/TupleDataWithBuffer.h"
#include "fennel/tuple/TuplePrinter.h"
#include "fennel/disruptivetech/calc/CalcCommon.h"
#include "fennel/common/FennelExcn.h"

#include <boost/test/floating_point_comparison.hpp>

#include <boost/test/test_tools.hpp>
#include <boost/scoped_array.hpp>
#include <string>
#include <limits>
#include <vector>

using namespace fennel;
using namespace std;


static const int64_t INT_TEST_MIN =  2;
static const int64_t INT_TEST_MAX =  105;

static const double DBL_TEST_MIN = 1.5;
static const double DBL_TEST_MAX = 874.5;

#define TEST_DATA_INDEX 0
#define SUM_INDEX 1
#define MIN_INDEX 2
#define MAX_INDEX 3

#define SAMPLE_SIZE 10


static int64_t intTestData[][SAMPLE_SIZE] =
{
    {12,33,52,14,10,63,5,INT_TEST_MIN,49,INT_TEST_MAX},  // test data values
    {12,45,97,111,121,184,189,191,240,345},              // running sum of test values
    {12,12,12,12,10,10,5,2,2,2},                         // running MIN values
    {12,33,52,52,52,63,63,63,63,105}                     // running MAX values
};


static double dblTestData[][SAMPLE_SIZE] =
{
    {63.5,63.1,92.9,DBL_TEST_MIN,6.3,38.5,23.1,DBL_TEST_MAX,44.7,498.0},
    {63.5,126.6,219.5,221.0,227.3,265.8,288.9,1163.4,1208.1,1706.1},
    {63.5,63.1,63.1,1.5,1.5,1.5,1.5,1.5,1.5,1.5},
    {63.5,63.5,92.9,92.9,92.9,92.9,92.9,874.5,874.5,874.5}
};

static vector<TupleData*>  testTuples;



class CalcExtMinMaxTest : virtual public TestBase, public TraceSource
{
    void checkWarnings(Calculator& calc, string expected);

    void testCalcExtMinMax();
    
    void initWindowedAggDataBlock(TupleDataWithBuffer* outTuple,
        StandardTypeDescriptorOrdinal dType);

    void printOutput(TupleData const & tup,
                     Calculator const & calc);
    
public:
    explicit CalcExtMinMaxTest()
        : TraceSource(shared_from_this(),"CalcExtMinMaxTest")
    {
        srand(time(NULL));
        CalcInit::instance();
        FENNEL_UNIT_TEST_CASE(CalcExtMinMaxTest, testCalcExtMinMax);

    }
     
    virtual ~CalcExtMinMaxTest()
    {
    }
};

// for nitty-gritty debugging. sadly, doesn't use BOOST_MESSAGE.
void
CalcExtMinMaxTest::printOutput(TupleData const & tup,
                             Calculator const & calc)
{
#if 0
    TuplePrinter tuplePrinter;
    tuplePrinter.print(cout, calc.getOutputRegisterDescriptor(), tup);
    cout << endl;
#endif
}


void 
CalcExtMinMaxTest::checkWarnings(Calculator& calc, string expected)
{
    try {
        calc.exec();
    } catch(...) {
        BOOST_FAIL("An exception was thrown while running program");
    }
    
    int i = calc.warnings().find(expected);

    if ( i < 0) {
        string msg ="Unexpected or no warning found\n";
        msg += "Expected: ";
        msg += expected;
        msg += "\nActual:  ";
        msg += calc.warnings();
	
        BOOST_FAIL(msg);
    }   
}


void
CalcExtMinMaxTest::initWindowedAggDataBlock(
    TupleDataWithBuffer* outTuple,
    StandardTypeDescriptorOrdinal dType)
{
    ostringstream pg("");

    // script to initialize the Windowed
    // agg functions.  Just pass in the Type
    // to be initialized.  It is supplied with
    // all subsequent calls
    pg << "O vc,4;" << endl;
    pg << "T;" << endl;
    pg << "CALL 'WINAGGINIT(O0);" << endl;

    // Allocate
    Calculator calc(0);
    calc.outputRegisterByReference(false);
    
    // Assemble the script
    try {
        calc.assemble(pg.str().c_str());
    }
    catch (FennelExcn& ex) {
        BOOST_FAIL("Assemble exception " << ex.getMessage()<< pg.str());
    }

    
    outTuple->computeAndAllocate(calc.getOutputRegisterDescriptor());
    
    TupleDataWithBuffer inTuple(calc.getInputRegisterDescriptor());

    calc.bind(&inTuple, outTuple);

    TupleDatum* pTD = &((*outTuple)[0]);

    *(reinterpret_cast<int32_t*>(const_cast<PBuffer>(pTD->pData))) =
        dType;
    
    calc.exec();
    printOutput(*outTuple, calc);
}

template <typename DTYPE>
void
WinAggAddTest(
    TupleDataWithBuffer* winAggTuple,
    DTYPE testData[][SAMPLE_SIZE],
    StandardTypeDescriptorOrdinal dType,
    void (*check)(TupleDataWithBuffer*,DTYPE[][SAMPLE_SIZE],int))
{
    ostringstream pg("");

    if (StandardTypeDescriptor::isExact(dType)) {
        pg << "O s8,s8,s8,s8,s8;" << endl;
        pg << "I s8,vb,4;" <<endl;
    } else if (StandardTypeDescriptor::isApprox(dType)) {
        pg << "O s8,d,d,d,d;" << endl;
        pg << "I d,vb,4;" <<endl;
    }
    pg << "T;" << endl;
    pg << "CALL 'WinAggAdd(O0,I0,I1);" << endl;
    pg << "CALL 'WinAggSum(O1,I1);" << endl;
    pg << "CALL 'WinAggAvg(O2,I1);" << endl;
    pg << "CALL 'WinAggMin(O3,I1);" << endl;
    pg << "CALL 'WinAggMax(O4,I1);" << endl;

    // Allocate
    Calculator calc(0);
    calc.outputRegisterByReference(false);
    
    // Assemble the script
    try {
        calc.assemble(pg.str().c_str());
    }
    catch (FennelExcn& ex) {
        BOOST_FAIL("Assemble exception " << ex.getMessage()<< pg.str());
    }
    
    TupleDataWithBuffer outTuple( calc.getOutputRegisterDescriptor());

    for (int i=0; i < 10; i++){
        TupleDataWithBuffer *inTuple = new TupleDataWithBuffer( calc.getInputRegisterDescriptor());
        testTuples.push_back( inTuple);

        calc.bind( inTuple, &outTuple);
        
        // copy the Agg data block pointer into the input tuple
        (*inTuple)[1] = (*winAggTuple)[0];
        
        TupleDatum* pTD = &((*inTuple)[0]);
        reinterpret_cast<DTYPE*>(const_cast<PBuffer>(pTD->pData)) =
            &testData[TEST_DATA_INDEX][i];
    
        calc.exec();

        (*check)(&outTuple,testData,i);
    }
    assert(10 == *(reinterpret_cast<int64_t*>(const_cast<uint8_t*>(outTuple[0].pData))));
}

void checkAddInt(
    TupleDataWithBuffer* outTuple,
    int64_t testData[][SAMPLE_SIZE],
    int index)
{
    BOOST_CHECK(index+1 == *(reinterpret_cast<const int64_t*>((*outTuple)[0].pData)));
    BOOST_CHECK(testData[SUM_INDEX][index] == *(reinterpret_cast<const int64_t*>((*outTuple)[1].pData)));
    BOOST_CHECK(testData[MIN_INDEX][index] == *(reinterpret_cast<const int64_t*>((*outTuple)[3].pData)));
    BOOST_CHECK(testData[MAX_INDEX][index] == *(reinterpret_cast<const int64_t*>((*outTuple)[4].pData)));
}

void checkDropInt(
    TupleDataWithBuffer* outTuple,
    int64_t testData[][SAMPLE_SIZE],
    int index)
{
    if (index > 0) {
        BOOST_CHECK(index == *(reinterpret_cast<const int64_t*>((*outTuple)[0].pData)));
        BOOST_CHECK(testData[SUM_INDEX][index-1] == *(reinterpret_cast<const int64_t*>((*outTuple)[1].pData)));
        BOOST_CHECK(testData[MIN_INDEX][index-1] == *(reinterpret_cast<const int64_t*>((*outTuple)[3].pData)));
        BOOST_CHECK(testData[MAX_INDEX][index-1] == *(reinterpret_cast<const int64_t*>((*outTuple)[4].pData)));
    }
}

template <typename DTYPE>
void
WinAggDropTest(
    TupleDataWithBuffer* winAggTuple,
    DTYPE testData[][SAMPLE_SIZE],
    StandardTypeDescriptorOrdinal dType,
    void (*check)(TupleDataWithBuffer*,DTYPE[][SAMPLE_SIZE],int))
{
    ostringstream pg("");

    if (StandardTypeDescriptor::isExact(dType)) {
        pg << "O s8,s8,s8,s8,s8;" << endl;
        pg << "I s8,vb,4;" <<endl;
    } else if (StandardTypeDescriptor::isApprox(dType)) {
        pg << "O s8,d,d,d,d;" << endl;
        pg << "I d,vb,4;" <<endl;
    }
    pg << "T;" << endl;
    pg << "CALL 'WinAggDrop(O0,I0,I1);" << endl; // returns count()
    pg << "CALL 'WinAggSum(O1,I1);" << endl;
    pg << "CALL 'WinAggAvg(O2,I1);" << endl;
    pg << "CALL 'WinAggMin(O3,I1);" << endl;
    pg << "CALL 'WinAggMax(O4,I1);" << endl;

    // Allocate
    Calculator calc(0);
    calc.outputRegisterByReference(false);
    
    // Assemble the script
    try {
        calc.assemble(pg.str().c_str());
    }
    catch (FennelExcn& ex) {
        BOOST_FAIL("Assemble exception " << ex.getMessage()<< pg.str());
    }

    // Alloc tuples and buffer space
    TupleDataWithBuffer outTuple( calc.getOutputRegisterDescriptor());

    // Step backwards through the data table and remove each entry
    // from the window checking the function returns along the way.
    for (int i=SAMPLE_SIZE-1; i >=0 ; i--){
        TupleData* inTuple = testTuples[i];
        TupleDatum* pTD = &(*inTuple)[0];

        calc.bind( inTuple, &outTuple);
        
        // copy the Agg data block pointer into the input tuple
        (*inTuple)[1] = (*winAggTuple)[0];
    
        reinterpret_cast<DTYPE*>(const_cast<PBuffer>(pTD->pData)) =
            &testData[TEST_DATA_INDEX][i];
    
        calc.exec();

        (*check)(&outTuple, testData, i);
    }
    assert( 0 == *(reinterpret_cast<const int64_t*>(outTuple[0].pData)));
}

void checkAddDbl(
    TupleDataWithBuffer* outTuple,
    double testData[][SAMPLE_SIZE],
    int index)
{
    int64_t rowCount = *(reinterpret_cast<const int64_t*>((*outTuple)[0].pData));
    BOOST_CHECK(index+1 == rowCount);
    
    double tdSum = testData[SUM_INDEX][index];
    double calcSum = *(reinterpret_cast<const double*>((*outTuple)[1].pData));
    BOOST_CHECK_CLOSE(tdSum, calcSum, 0.1);
    
    double tdMin = testData[MIN_INDEX][index];
    double calcMin = *(reinterpret_cast<const double*>((*outTuple)[3].pData));
    BOOST_CHECK(tdMin == calcMin);
    
    double tdMax = testData[MAX_INDEX][index];
    double calcMax = *(reinterpret_cast<const double*>((*outTuple)[4].pData));
    BOOST_CHECK(tdMax == calcMax);
}

void checkDropDbl(
    TupleDataWithBuffer* outTuple,
    double testData[][SAMPLE_SIZE],
    int index)
{
    if (index > 0) {
        int64_t calcRowCount = *(reinterpret_cast<const int64_t*>((*outTuple)[0].pData));
        BOOST_CHECK(index == calcRowCount);

        double tdSum = testData[SUM_INDEX][index-1];
        double calcSum = *(reinterpret_cast<const double*>((*outTuple)[1].pData));
        BOOST_CHECK_CLOSE(tdSum, calcSum, 0.1);

        double tdMin = testData[MIN_INDEX][index-1];
        double calcMin = *(reinterpret_cast<const double*>((*outTuple)[3].pData));
        BOOST_CHECK_CLOSE(tdMin, calcMin, 0.1);

        double tdMax = testData[MAX_INDEX][index-1];
        double calcMax = *(reinterpret_cast<const double*>((*outTuple)[4].pData));
        BOOST_CHECK_CLOSE(tdMax, calcMax, 0.1);
    }
}

void
CalcExtMinMaxTest::testCalcExtMinMax()
{

    // Test windowing integer type
    TupleDataWithBuffer intAggTuple;
    initWindowedAggDataBlock(&intAggTuple, STANDARD_TYPE_INT_64);
    WinAggAddTest(&intAggTuple, intTestData, STANDARD_TYPE_INT_64, checkAddInt);
    WinAggDropTest(&intAggTuple, intTestData, STANDARD_TYPE_INT_64, checkDropInt);

    // Clear the vector that holds the TupleData for the simulated window.
    testTuples.clear();
    
    // windowing real type
    TupleDataWithBuffer dblAggTuple;
    initWindowedAggDataBlock(&dblAggTuple, STANDARD_TYPE_DOUBLE);
    WinAggAddTest(&dblAggTuple, dblTestData, STANDARD_TYPE_DOUBLE, checkAddDbl);
    WinAggDropTest(&dblAggTuple, dblTestData, STANDARD_TYPE_DOUBLE, checkDropDbl);
}


FENNEL_UNIT_TEST_SUITE(CalcExtMinMaxTest);
