-- MySQL dump 10.10
--
-- Host: localhost    Database: MEDJDBC_TEST
-- ------------------------------------------------------
-- Server version	5.0.22-standard

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `DATATYPES`
--

DROP TABLE IF EXISTS `DATATYPES`;
CREATE TABLE `DATATYPES` (
  `c_varchar` varchar(32) default NULL,
  `c_date` date default NULL,
  `c_time` time default NULL,
  `c_timestamp` datetime default NULL,
  `c_int` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `DATATYPES`
--


/*!40000 ALTER TABLE `DATATYPES` DISABLE KEYS */;
LOCK TABLES `DATATYPES` WRITE;
INSERT INTO `DATATYPES` VALUES ('varchar the first',NULL,NULL,'2005-10-12 03:00:00',1),('varchar the second',NULL,NULL,'2005-10-12 04:00:00',2),('varchar the third',NULL,'03:00:00','2005-10-12 03:00:00',3),('varchar the fourth','2005-10-12','04:00:00','2005-10-12 04:00:00',4),('varchar the fifth','2005-10-12',NULL,'2005-10-12 03:00:00',5);
UNLOCK TABLES;
/*!40000 ALTER TABLE `DATATYPES` ENABLE KEYS */;

--
-- Table structure for table `MAPPING_T1`
--

DROP TABLE IF EXISTS `MAPPING_T1`;
CREATE TABLE `MAPPING_T1` (
  `id` int(11) NOT NULL,
  `msg` varchar(16) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `MAPPING_T1`
--


/*!40000 ALTER TABLE `MAPPING_T1` DISABLE KEYS */;
LOCK TABLES `MAPPING_T1` WRITE;
INSERT INTO `MAPPING_T1` VALUES (1,'a'),(2,'b'),(3,'c');
UNLOCK TABLES;
/*!40000 ALTER TABLE `MAPPING_T1` ENABLE KEYS */;

--
-- Table structure for table `MAPPING_T2`
--

DROP TABLE IF EXISTS `MAPPING_T2`;
CREATE TABLE `MAPPING_T2` (
  `id` int(11) NOT NULL,
  `msg` varchar(16) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `MAPPING_T2`
--


/*!40000 ALTER TABLE `MAPPING_T2` DISABLE KEYS */;
LOCK TABLES `MAPPING_T2` WRITE;
INSERT INTO `MAPPING_T2` VALUES (1,'d'),(2,'e'),(3,'f');
UNLOCK TABLES;
/*!40000 ALTER TABLE `MAPPING_T2` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
