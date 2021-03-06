--
-- This set of tests will test equijoins, HashAgg and VI splicing on
-- the table with data in parabolic distribution
--
--SET semicolon=OFF
--create tablespace DIST_TS datafile 'dist_ts.dat' size 192000K
create schema dist;
set schema 'dist';

create function dist.diffdiv10(int1 integer, int2 integer)
returns integer
language sql
contains sql
return case when int1 < int2
then cast(ceil((int1-int2)/10) as integer)
else cast(floor((int1-int2)/10) as integer) end;

create function dist.eq1(int1 integer, int2 integer)
returns integer
language sql
contains sql
return (18-(int1-int2)*(int1-int2));

create function dist.eq2(int1 integer, int2 integer)
returns integer
language sql
contains sql
return (int1-int2)*(int1-int2);

create function dist.eq3(int1 integer, int2 integer)
returns integer
language sql
contains sql
return (18-((dist.diffdiv10(int1,int2))*(dist.diffdiv10(int1,int2))));

create function dist.eq4(int1 integer)
returns integer
language sql
contains sql
return (
case 
  when ((dist.diffdiv10(int1,250000)) > 46340) then 2147395600
  else ((dist.diffdiv10(int1,250000))*(dist.diffdiv10(int1,250000)))
end);

set schema 'ff_schema';

CREATE foreign table BENCH100_SOURCE (
C1 INTEGER,
C2 INTEGER,
C4 INTEGER,
C5 INTEGER,
C10 INTEGER,
C25 INTEGER,
C100 INTEGER,
C1K INTEGER,
C10K INTEGER,
C40K  INTEGER,
C100K INTEGER, 
C250K INTEGER,
C500K INTEGER)
--USING LINK ODBC_SQLSERVER DEFINED BY 
--'select kseq,k2,K4,K5,K10,K25,K100,K1K,K10K,K40K,K100K,k250k,k500k from BENCHMARK.dbo.bench100'
server ff_server
options (
SCHEMA_NAME 'BCP',
filename 'bench100'
);

set schema 's';

CREATE TABLE DISTRIBUTION_100 (
  K100 INTEGER,
  K100SQUARE INTEGER,
  KSEQ INTEGER,
  KSEQSQUARE INTEGER );
--TABLESPACE DIST_TS

CREATE INDEX DIST_100_K100SQUARE ON DISTRIBUTION_100(K100SQUARE);

CREATE INDEX DIST_100_KSEQSQUARE ON DISTRIBUTION_100(KSEQSQUARE);

INSERT INTO DISTRIBUTION_100 (K100, K100SQUARE, KSEQ, KSEQSQUARE)
       SELECT C100, dist.eq1(C100, 50),
              C1, dist.eq2(C1, 50)
       FROM ff_schema.BENCH100_SOURCE;

analyze table distribution_100 compute statistics for all columns;

CREATE FOREIGN TABLE BENCH10K_SOURCE (
C1 INTEGER,
C2 INTEGER,
C4 INTEGER,
C5 INTEGER,
C10 INTEGER,
C25 INTEGER,
C100 INTEGER,
C1K INTEGER,
C10K INTEGER,
C40K  INTEGER,
C100K INTEGER, 
C250K INTEGER,
C500K INTEGER) 
--USING LINK ODBC_SQLSERVER DEFINED BY 
--'select kseq,k2,K4,K5,K10,K25,K100,K1K,K10K,K40K,K100K,k250k,k500k from BENCHMARK.dbo.bench10K'
server ff_server
options (
SCHEMA_NAME 'BCP',
filename 'bench10K'
);

CREATE TABLE DISTRIBUTION_10K (
 K10K           INTEGER,
 K10KSQUARE     INTEGER,
 KSEQ           INTEGER,
 KSEQSQUARE     INTEGER );
--TABLESPACE DIST_TS

CREATE INDEX DIST_10K_K10KSQUARE ON DISTRIBUTION_10K(K10KSQUARE);

CREATE INDEX DIST_10K_KSEQSQUARE ON DISTRIBUTION_10K(KSEQSQUARE);

INSERT INTO DISTRIBUTION_10K (K10K, K10KSQUARE, KSEQ, KSEQSQUARE)
      SELECT C10K, dist.eq1(C10K, 5000),
             C1, dist.eq2(C1, 5000)
      FROM BENCH10K_SOURCE;

analyze table distribution_10k compute statistics for all columns;

CREATE FOREIGN TABLE BENCH100K_SOURCE (
C1 INTEGER,
C2 INTEGER,
C4 INTEGER,
C5 INTEGER,
C10 INTEGER,
C25 INTEGER,
C100 INTEGER,
C1K INTEGER,
C10K INTEGER,
C40K  INTEGER,
C100K INTEGER, 
C250K INTEGER,
C500K INTEGER) 
--USING LINK ODBC_SQLSERVER DEFINED BY 
--'select kseq,k2,K4,K5,K10,K25,K100,K1K,K10K,K40K,K100K,k250k,k500k from BENCHMARK.dbo.bench100K'
server ff_server
options (
SCHEMA_NAME 'BCP',
filename 'bench100K'
);

CREATE TABLE DISTRIBUTION_100K (
 K100K          INTEGER,
 K100KSQUARE    INTEGER,
 KSEQ           INTEGER,
 KSEQSQUARE     INTEGER );
-- TABLESPACE DIST_TS

CREATE INDEX DIST_100K_K100KSQUARE ON DISTRIBUTION_100K(K100KSQUARE);

CREATE INDEX DIST_100K_KSEQSQUARE ON DISTRIBUTION_100K(KSEQSQUARE);

INSERT INTO DISTRIBUTION_100K (K100K, K100KSQUARE, KSEQ, KSEQSQUARE)
      SELECT C100K, dist.eq1(C100K, 50000),
             C1, dist.eq2(C1, 50000)
      FROM BENCH100K_SOURCE;

analyze table distribution_100k compute statistics for all columns;

CREATE FOREIGN TABLE BENCH1M_SOURCE (
C1 INTEGER,
C2 INTEGER,
C4 INTEGER,
C5 INTEGER,
C10 INTEGER,
C25 INTEGER,
C100 INTEGER,
C1K INTEGER,
C10K INTEGER,
C40K  INTEGER,
C100K INTEGER, 
C250K INTEGER,
C500K INTEGER) 
--USING LINK ODBC_SQLSERVER DEFINED BY 
--'select kseq,k2,K4,K5,K10,K25,K100,K1K,K10K,K40K,K100K,k250k,k500k from BENCHMARK.dbo.bench1M'
server ff_server
options (
SCHEMA_NAME 'BCP',
filename 'bench1M'
);

CREATE TABLE DISTRIBUTION_1M (
 K500K          INTEGER,
 K500KSQUARE    INTEGER,
 KSEQ           INTEGER,
 KSEQSQUARE     INTEGER );
-- TABLESPACE DIST_TS

CREATE INDEX DIST_1M_K500KSQUARE ON DISTRIBUTION_1M(K500KSQUARE);

CREATE INDEX DIST_1M_KSEQSQUARE ON DISTRIBUTION_1M(KSEQSQUARE);

INSERT INTO DISTRIBUTION_1M (K500K, K500KSQUARE, KSEQ, KSEQSQUARE)
      SELECT C500K, dist.eq3(C500K, 250000),
             C1, dist.eq4(C1)
      FROM BENCH1M_SOURCE;

analyze table distribution_1m compute statistics for all columns;

-- drop eq functions here
drop routine dist.eq1;
drop routine dist.eq2;
drop routine dist.eq3;
drop routine dist.eq4;
drop routine dist.diffdiv10;
