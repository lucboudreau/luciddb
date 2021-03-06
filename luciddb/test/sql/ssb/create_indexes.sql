set schema 'ssb';

---------------------------
-- indices for foreign keys
---------------------------

-- LINEORDER: LO_CUSTKEY, LO_PARTKEY, LO_SUPPKEY, LO_ORDERDATE, LO_COMMITDATE
CREATE INDEX LO_CUSTKEY_IDX ON LINEORDER(LO_CUSTKEY);
CREATE INDEX LO_PARTKEY_IDX ON LINEORDER(LO_PARTKEY);
CREATE INDEX LO_SUPPKEY_IDX ON LINEORDER(LO_SUPPKEY);
CREATE INDEX LO_ORDERDATE_IDX ON LINEORDER(LO_ORDERDATE);
CREATE INDEX LO_COMMITDATE_IDX ON LINEORDER(LO_COMMITDATE);

----------------------------
-- indices for WHERE clauses
----------------------------

-- CUSTOMER: C_REGION, C_NATION, C_CITY
CREATE INDEX C_REGION_IDX ON CUSTOMER(C_REGION);
CREATE INDEX C_NAITON_IDX ON CUSTOMER(C_NATION);
CREATE INDEX C_CITY_IDX ON CUSTOMER(C_CITY);

-- DATES: D_YEAR, D_YEARMONTHNUM, D_WEEKNUMINYEAR, D_YEARMONTH
CREATE INDEX D_YEAR_IDX ON DATES(D_YEAR);
CREATE INDEX D_YEARMONTHNUM_IDX ON DATES(D_YEARMONTHNUM);
CREATE INDEX D_WEEKNUMINYEAR_IDX ON DATES(D_WEEKNUMINYEAR);
CREATE INDEX D_YEARMONTH_IDX ON DATES(D_YEARMONTH);

-- PART: P_CATEGORY, P_BRAND, P_MFGR
CREATE INDEX P_CATEGORY_IDX ON PART(P_BRAND);
CREATE INDEX P_BRAND_IDX ON PART(P_BRAND);
CREATE INDEX P_MFGR_IDX ON PART(P_BRAND);

-- SUPPLIER: S_REGION, S_NATION, S_CITY
CREATE INDEX S_REGION_IDX ON SUPPLIER(S_REGION);
CREATE INDEX S_NAITON_IDX ON SUPPLIER(S_NATION);
CREATE INDEX S_CITY_IDX ON SUPPLIER(S_CITY);

-- LINEORDER: LO_QUANTITY, LO_DISCOUNT, 
CREATE INDEX LO_QUANTITY_IDX ON LINEORDER(LO_QUANTITY);
CREATE INDEX LO_DISCOUNT_IDX ON LINEORDER(LO_DISCOUNT);

-----------------
-- analyze tables
-----------------

ANALYZE TABLE CUSTOMER ESTIMATE STATISTICS FOR ALL COLUMNS;
ANALYZE TABLE DATES ESTIMATE STATISTICS FOR ALL COLUMNS;
ANALYZE TABLE PART ESTIMATE STATISTICS FOR ALL COLUMNS;
ANALYZE TABLE SUPPLIER ESTIMATE STATISTICS FOR ALL COLUMNS;
ANALYZE TABLE LINEORDER ESTIMATE STATISTICS FOR ALL COLUMNS;
