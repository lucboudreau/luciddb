-- $Id$
-- Test effect on view's stored original text when a dependency is re-created.

create schema x;
create schema y;
create table x.t(i int not null primary key);
create table y.t(i int not null primary key);
insert into x.t values(100);
insert into y.t values(200);
set schema 'x';
create view y.q1 as select * from t;
create view y.q2 as select * from t union all select * from y.q1;
select * from y.q2;
set schema 'y';
create or replace view y.q1 as select * from t;
select * from y.q2;

!set outputformat csv
!set force true
CREATE OR REPLACE SCHEMA dtbug1173;
SET SCHEMA 'dtbug1173';
CREATE OR REPLACE VIEW v AS SELECT 1 AS i, 2 AS j FROM (VALUES (TRUE));
CREATE OR REPLACE VIEW v2 AS SELECT *, 999 AS k   FROM v;
CREATE OR REPLACE VIEW v2x AS SELECT k FROM v2;

-- text of view is stored unchanged
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V2';

SELECT * FROM v2;

-- Change definition of v, but keep same column names.
-- View is still valid, has same definition, but query returns different data.
CREATE OR REPLACE VIEW v AS SELECT 3 AS i, 2 AS j FROM (VALUES (TRUE));
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V2';
SELECT * FROM v2;

-- Add a column to underlying view V, and V2 is still valid, but does not gain column.
CREATE OR REPLACE VIEW v AS SELECT 1 AS j, 5 AS k, 2 AS i FROM (VALUES (FALSE));
SELECT * FROM v2;

-- Incompatible change to underlying view V. DDL fails, view is not changed, and
-- query returns same as previously.
CREATE OR REPLACE VIEW v AS SELECT 1 AS x, 5 AS k FROM (VALUES (FALSE));
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V';
SELECT * FROM v2;

-- Union view causes several references to V in same query.
CREATE OR REPLACE VIEW v6 AS
  SELECT * FROM v
  UNION ALL
  SELECT * FROM v AS t WHERE i < 3
  UNION ALL
  SELECT * FROM dtbug1173.v  WHERE v.i + j < 4;
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V6';
SELECT * FROM v6;

-- Change V in a way that will break multiple dependent views; expect an error,
-- and V's definition should be unchanged. (Behavior is different in SQLstream:
-- SQLstream accepts the change, and marks dependents as invalid.)
-- FIXME jvs 14-Mar-2010:  this test is currently disabled because
-- the error message is non-deterministic.
-- CREATE OR REPLACE VIEW v AS SELECT * FROM (VALUES (1)) AS t(j);
-- SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V';
-- 'originalDefinition'
-- 'SELECT 1 AS j, 5 AS k, 2 AS i FROM (VALUES (FALSE))'
-- SELECT * FROM v;
-- 'J','K','I'
-- '1','5','2'
-- SELECT * FROM v2;
-- 'I','J','K'
-- '2','1','999'
-- SELECT * FROM v6;
-- 'J','K','I'
-- '1','5','2'
-- '1','5','2'
-- '1','5','2'

-- Create view on top of two relations, then alter the relations so that the view
-- would now be invalid due to an ambiguous column.
CREATE VIEW va AS SELECT * FROM (VALUES (1, 2)) AS t(i, j);
CREATE VIEW vb AS SELECT * FROM (VALUES (2, 3)) AS t(x, y);
CREATE VIEW vc AS
  SELECT i, x FROM (
    SELECT * FROM va, vb WHERE j = x) AS z;
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'VC';
SELECT * FROM vc;

-- Alter VB so that both VA and VB have a column J.  This should be OK
-- since the canonical SQL early binding prevents ambiguity from creeping
-- in subsequently.
CREATE OR REPLACE VIEW vb AS SELECT * FROM (VALUES (2, 3, 4)) AS t(x, y, j);

-- But we cannot remove column J from VA, since that would break a binding.
CREATE OR REPLACE VIEW va AS SELECT * FROM (VALUES (2)) AS t(x);


-- repeat, this time with LucidDB personality, which eschews
-- the view text preservation since it causes spurious exceptions
-- in the log

alter session implementation set jar sys_boot.sys_boot.luciddb_plugin;

CREATE OR REPLACE SCHEMA ler_7331;
SET SCHEMA 'ler_7331';
CREATE OR REPLACE VIEW v AS SELECT 1 AS i FROM (VALUES (TRUE));
CREATE OR REPLACE VIEW v9 AS SELECT *, 999 AS k FROM v;
-- original text of V9 is expanded
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V9';
CREATE OR REPLACE VIEW v AS SELECT 2 AS i FROM (VALUES (TRUE));
-- original text of V9 is unchanged
SELECT "originalDefinition" FROM sys_fem."SQL2003"."LocalView" WHERE "name" = 'V9';
SELECT * FROM v9;

-- End replaceView.sql

