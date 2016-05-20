package eu.citypulse.uaso.gdiclient;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import eu.citypulse.uaso.gdiclient.persistdata.CpGdiPersistable;

/**
 * Test and Example calass for Geospatial Data Infrastructure(GDI) client of CityPulse 
 * @author Daniel Kuemper, University of Applied Sciences Osnabrueck (d.kuemper@hs-osnabrueck.de)
 * @version 0.1 draft
 *
 */
public class CpClientExample {

	/**
	 * Simple Class that shows functionality of the Requests
	 * @param args are not used
	 */
	public static void main(String[] args) { 

		CpGdiInterface cgi;
		try {
			cgi = new CpGdiInterface();
			
			cgi.simpleQuery();

				System.out.println("Inserted 1? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", 10.0, 56.0));
				System.out.println("Inserted 2? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", 10.0, 56.0, 4326));
				System.out.println("Inserted 3? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", "POINT(10.0 56.0)", 4326));
				System.out.println("Inserted 4? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", "POINT(10.0 56.0)", CpGdiInterface.EPSG_WGS84));		
				//Romania
				System.out.println("Inserted 5? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", 25.55691529433881, 45.587838521405224 , CpGdiInterface.EPSG_WGS84));			
				System.out.println("Inserted 6? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", "POINT(543596.363572 454379.287188)", CpGdiInterface.EPSG_STEREO70));
				UUID sensorUUID = UUID.randomUUID();
				System.out.println("Inserted 7? "+cgi.registerStream(sensorUUID, "900913", "testCategory",  543596.363572, 454379.287188, CpGdiInterface.EPSG_STEREO70));
				System.out.println("Deleted 7? "+cgi.removeStream(sensorUUID));
							

				System.out.println("Inserted ES1? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc",  10.0, 56.0));
				System.out.println("Inserted ES2? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc",10.0, 56.0, 4326));
				System.out.println("Inserted ES3? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc", "POINT(10.0 56.0)", 4326));
				System.out.println("Inserted ES4? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc", "POINT(10.0 56.0)", CpGdiInterface.EPSG_WGS84));		
				//Romania
				System.out.println("Inserted ES5? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc", 25.55691529433881, 45.587838521405224 , CpGdiInterface.EPSG_WGS84));			
				System.out.println("Inserted ES6? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc", "POINT(543596.363572 454379.287188)", CpGdiInterface.EPSG_STEREO70));
				UUID eventUUID = UUID.randomUUID();
				System.out.println("Inserted ES7? "+cgi.registerEventStream(eventUUID, "aaa/bbb/ccc", 543596.363572, 454379.287188, CpGdiInterface.EPSG_STEREO70));
				System.out.println("Deleted 7S? "+cgi.removeEventStream(eventUUID));
				System.out.println("Deleted 7S? "+cgi.removeEventStream(eventUUID)); //false since it's not available anymore
			

			UUID eventUUID1 = UUID.randomUUID();
			System.out.println("Inserted Event 1? "+cgi.registerEvent(eventUUID1, "aaa/bbb/ccc", new Timestamp(System.currentTimeMillis()),"POINT(10.0 56.0)", CpGdiInterface.EPSG_WGS84));		
			//Romania
			System.out.println("Inserted E2? "+cgi.registerEvent(UUID.randomUUID(), "aaa/bbb/ccc",  new Timestamp(System.currentTimeMillis()), 25.55691529433881, 45.587838521405224 , CpGdiInterface.EPSG_WGS84));			
			System.out.println("Inserted E3? "+cgi.registerEvent(UUID.randomUUID(), "aaa/bbb/ccc",  new Timestamp(System.currentTimeMillis()-3600000),"POINT(543596.363572 454379.287188)", CpGdiInterface.EPSG_STEREO70));
			UUID eventUUID2 = UUID.randomUUID();
			System.out.println("Inserted E4? "+cgi.registerEvent(eventUUID2, "aaa/bbb/ccc",new Timestamp(System.currentTimeMillis()-3600000), 543596.363572, 454379.287188, CpGdiInterface.EPSG_STEREO70));
			System.out.println("Deleted E4? "+cgi.deregisterEvent(eventUUID2));
			System.out.println("Deleted E4? "+cgi.deregisterEvent(eventUUID2)); //false since it's not available anymore
			System.out.println("Close Event 1? "+cgi.closeEvent(eventUUID1,  new Timestamp(System.currentTimeMillis())));		

			
			
	
//ROUTE 1			
			
//		CpRouteRequest cprr2 = cgi.getCityRoutes(10.1,56.1, 10.2,56.2, CpRouteRequest.ROUTE_COST_METRIC_DISTANCE, 3);
//			System.out.println("cprr2:\n"+cprr2);
//		
//			System.out.println(cgi.updateCostMultiplicatorRadial(10.1, 56.1, 300.0, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 5.0));
//			System.out.println(cgi.updateCostMultiplicatorRadial(10.2, 56.1, 333.3, CpRouteRequest.ROUTE_COST_METRIC_DISTANCE, 5.0));
//			System.out.println(cgi.updateCostMultiplicatorRadial(10.2, 56.2, 350.0, CpRouteRequest.ROUTE_COST_METRIC_TIME, 5.0));
//			System.out.println(cgi.updateCostMultiplicatorArea(9.9, 56.17,9.95, 56.07, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 5.0));
//			try {
//				System.out.println(cgi.updateCostMultiplicatorArea(9.9, 56.07,9.95, 56.17, CpRouteRequest.ROUTE_COST_METRIC_COMBINED, 5.0));
//			} catch (Exception e) {
//				System.err.println("Unknown metric!" +e.getLocalizedMessage());
//			}
//			
//			//reset one cost multiplicator layer
//			System.out.println(cgi.resetCostMultiplicators(CpRouteRequest.ROUTE_COST_METRIC_POLLUTION));
//
//			//reet all cost multiplicator layers
//			System.out.println(cgi.resetCostMultiplicators(CpRouteRequest.ROUTE_COST_METRIC_POLLUTION));
//
//			//let's place sth. near route 2:
//			System.out.println("Inserted current Event  on route? "+cgi.registerEvent(UUID.randomUUID(), "aaa/bbb/ccc",  new Timestamp(System.currentTimeMillis()), 10.0980917,  56.1054429));			
//			System.out.println("Inserted old Event (500 Seconds old) on route? "+cgi.registerEvent(UUID.randomUUID(), "aaa/bbb/ccc",  new Timestamp(System.currentTimeMillis()-500000), 10.0980917,  56.1054429));			
//			System.out.println("Inserted EventStream on route? "+cgi.registerEventStream(UUID.randomUUID(), "aaa/bbb/ccc", 10.0980917, 56.1054429));
//			System.out.println("Inserted sensorStream on route? "+cgi.registerStream(UUID.randomUUID(), "900913", "testCategory", 10.0980917, 56.1054429));
//
//			
//			
//			CpGdiPersistable[] events;
//			System.out.println("Event_Streamsnon Route:");
//			events=cgi.getEventStreamsForRoute(cprr2.getRoute(1), 500);
//			System.out.println(events.length);
//			if (events.length>0) System.out.println(events[0]);
//			
//			System.out.println("Sensor_Streams on Route:");
//			events=cgi.getSensorsForRoute(cprr2.getRoute(1), 500);
//			System.out.println(events.length);
//			if (events.length>0) System.out.println(events[0]);
//			
//			System.out.println("Events on Route in the last 5 minutes (300 seconds):");
//			events = cgi.getEventsForRoute(cprr2.getRoute(1), new Timestamp(System.currentTimeMillis()),300, 500.0);
//			System.out.println(events.length);
//			if (events.length>0) System.out.println(events[0]);
//
//			CpGdiEventStream cpgdies[] = cgi.getAllEventStreams();
//			for (int i = 0; i < cpgdies.length; i++) {
//				System.out.println(cpgdies[i]);
//				System.out.println(cpgdies[i].getCenttralPoint());
//			}
			//cgi.removeAllEventStream();

			String wktString = "LINESTRING(10.0983069 56.1016176,10.0974108 56.1039646,10.0988826 56.1049463,10.0980917 56.1054429,10.0997819 56.1056684,10.1032693 56.1062137,10.1041769 56.1063269,10.1048555 56.1064159,10.1093932 56.1071376,10.1098647 56.1072418,10.1103715 56.1073367,10.1112226 56.1074866,10.1155347 56.1081985,10.1157651 56.1082465,10.118797 56.1091077,10.1253912 56.1118011,10.133224 56.1150346,10.1397989 56.1177777,10.1431314 56.1191427,10.1440514 56.1195271,10.1463709 56.1204883,10.1465701 56.1205609,10.1479688 56.1211497,10.1519353 56.1230165,10.1520733 56.1230857,10.1521111 56.1231036,10.1553386 56.124607,10.1565188 56.1251343,10.1566161 56.1251776,10.1576391 56.1256331,10.1603172 56.126789,10.1613796 56.1276947,10.1624104 56.128561,10.1625506 56.1286457,10.1631923 56.1290345,10.1639399 56.1294614,10.1651501 56.1301991,10.1662482 56.1308892,10.1671713 56.1314736,10.1674363 56.1316468,10.1679911 56.1320309,10.1691602 56.132809,10.1725122 56.1347897,10.1738004 56.1354846,10.174909 56.136083,10.1756633 56.1364896,10.1767854 56.1370989,10.177115 56.1372709,10.1776999 56.1375637,10.1784316 56.1379301,10.1798373 56.1385858,10.1812589 56.1392475,10.1816083 56.1394212,10.1818736 56.1395561,10.1823019 56.1398057,10.1829491 56.140193,10.1854795 56.1416678,10.1862335 56.1421237,10.1866438 56.1423897,10.1870785 56.1426346,10.1871193 56.1426593,10.1880845 56.1431902,10.1892596 56.1438366,10.1894761 56.1439424,10.1904061 56.1444541,10.1920869 56.1453129,10.1929834 56.1457495,10.1934146 56.1460553,10.1938108 56.1462669,10.1944686 56.1466616,10.1950546 56.1470536,10.1957314 56.1475456,10.1965862 56.148167,10.1974235 56.1488124,10.197586 56.1489402,10.1984249 56.1496566,10.1988545 56.1500464,10.1994866 56.1505821,10.200022 56.1510493,10.2008241 56.1517634,10.201734 56.1525879,10.2018471 56.1526912,10.2021379 56.1529535,10.2024922 56.1533055,10.2011487 56.153678,10.1996984 56.1546385,10.1994494 56.155202,10.1994363 56.1552738,10.1993912 56.1554661,10.1993796 56.1555322,10.1992367 56.1564543,10.1992243 56.1564985,10.1991943 56.1565989,10.199114 56.1568001,10.1990247 56.1569804,10.1989627 56.1570981,10.1983681 56.1578704,10.198201 56.1581023,10.198156 56.1581648,10.1980573 56.158273,10.1980427 56.1582891,10.1969356 56.1590773,10.1965112 56.1593805,10.1963622 56.159502,10.1962744 56.1596121,10.196235 56.1596805,10.1962068 56.1598421,10.196241 56.1600369,10.1963749 56.1606671,10.1963889 56.1607437,10.1965088 56.1613613,10.1968688 56.1631244,10.1968743 56.1631874,10.1969725 56.1636322,10.1971208 56.1642218,10.1971745 56.164495,10.1971917 56.1645672,10.1972025 56.1646252,10.1972214 56.1646885,10.1973578 56.1653771,10.1975984 56.1665483,10.1976181 56.166673,10.1977201 56.1671395,10.1978855 56.1679299,10.1979123 56.1680441,10.1981355 56.1690609,10.1982286 56.1694494,10.1983588 56.1699713,10.1984916 56.1705209,10.1985372 56.1707096,10.1987549 56.1712316,10.1993527 56.1719437,10.1997568 56.1723068,10.1996992 56.1723339,10.1995613 56.1726295,10.1995675 56.17265,10.199766 56.1732618,10.1997915 56.173335,10.2000089 56.1740576,10.2002122 56.1746959,10.200237 56.1747529,10.2002655 56.1748186,10.2005443 56.1754552,10.1998289 56.1766362,10.1998387 56.1767297,10.1998734 56.1769232,10.199895 56.1769869,10.1997941 56.1774701,10.1998466 56.1777438,10.1998623 56.1779084,10.1997759 56.1783021,10.1999791 56.1783053,10.2000757 56.178314,10.2002789 56.1783337,10.1999986 56.1787412,10.1995137 56.1794384,10.199165 56.1799575,10.1991146 56.18003,10.1987543 56.1805498,10.1985695 56.1808195,10.1984058 56.1810583,10.1983596 56.1811283,10.1983878 56.1812222,10.198047 56.1818518,10.1979061 56.1820762,10.1976991 56.182412,10.1976518 56.1824911,10.1975332 56.1826517,10.1974863 56.1827237,10.1972438 56.1830437,10.1948179 56.1866613,10.1947202 56.186769,10.1949616 56.1868609,10.1955042 56.1870673,10.1959822 56.1872806,10.1967398 56.1875773,10.1966822 56.1876212,10.1991082 56.1886005,10.1990891 56.1886075,10.1990482 56.1887308,10.1990542 56.189061,10.1988914 56.1893147,10.1986131 56.1901952,10.1985332 56.1904214,10.1983149 56.1910395,10.1980506 56.1914396,10.1981957 56.1914854,10.1961178 56.1940248,10.196032 56.1941846,10.1958182 56.1945722,10.1958044 56.1945984,10.1955573 56.1950459,10.1955396 56.1951072,10.1958146 56.1951893,10.1959815 56.1952366,10.1971277 56.1955613,10.1985649 56.1960694,10.1999766 56.1967607,10.2006072 56.1971477,10.2008294 56.197289,10.2005759 56.1973684,10.2005662 56.1974275,10.2009688 56.1976726,10.2016435 56.1984272,10.2017872 56.1985519,10.2018438 56.1986011,10.1998439 56.1992954,10.1998967 56.1993416,10.1999166 56.1993593,10.2000545 56.1994822)";
			System.out.println(wktString);
			CpGdiPersistable[] events;
			System.out.println("Event_Streamsnon Route:");
			events=cgi.getEventStreamsForRoute(wktString, 500);
			System.out.println(events.length);
			if (events.length>0) System.out.println(events[0]);
			
			events=cgi.getEventStreamsForRoute(wktString, 4326,  500);
			System.out.println(events.length);
			if (events.length>0) System.out.println(events[0]);
			
			

			
			
			cgi.closeConnection();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		} 

}
