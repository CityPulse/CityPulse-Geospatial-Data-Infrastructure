package eu.citypulse.uaso.gdiclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.postgis.PGgeometry;
import org.postgresql.geometric.PGpoint;

import eu.citypulse.uaso.gdiclient.objects.CpAmenity;
import eu.citypulse.uaso.gdiclient.persistdata.CpGdiEvent;
import eu.citypulse.uaso.gdiclient.persistdata.CpGdiEventStream;
import eu.citypulse.uaso.gdiclient.persistdata.CpGdiSensorStream;
import eu.citypulse.uaso.gdiclient.routes.CpRoute;
import eu.citypulse.uaso.gdiclient.routes.CpRouteRequest;

/**
 * Geospatial Data Infrastructure(GDI) client for CityPulse
 * http://www.ict-citypulse.eu/
 * 
 * @author Daniel Kuemper, University of Applied Sciences Osnabrueck
 *         (d.kuemper@hs-osnabrueck.de)
 * @version 0.1 draft
 *
 */
public class CpGdiInterface {
	/**
	 * EPSG code of the WGS 84 Coordinate Reference System used in GPS
	 */
	public static int EPSG_WGS84 = 4326;
	/**
	 * EPSG code of the Stereo70 Coordinate Reference System used in Brasov
	 */
	public static int EPSG_STEREO70 = 31700;

	/**
	 * EPSG code of metrci UTM 32N CRS that can be used in Germany and Denmark
	 * 
	 * public static int EPSG_UTM32N = 32631; /** EPSG code of metric UTM 33N
	 * CRS that can be used in East Germany
	 */
	public static int EPSG_UTM33N = 32633;

	/**
	 * EPSG code of metric UTM zone 36N CRS that can be used in East Germany
	 */
	public static int EPSG_UTM36N = 32636;

	/**
	 * Multiplicator that is used to alter the cost on the routes when searching
	 * for alternatives
	 */
	private static double CITYCOSTMULTIPLICATOR_MODIFIER = 1.1;

	private Connection conn;
	private PreparedStatement stmtInsertRoute;
	private PreparedStatement stmtInsertRequest;
	private PreparedStatement stmtgetRoute;
	private PreparedStatement stmtResetAll;

	private PreparedStatement stmtGetRouteEventStreams, stmtGetAllEventStreams;
	private PreparedStatement stmtGetRouteEvents;
	private PreparedStatement stmtGetRouteSensors;

	private PreparedStatement stmtPropGetEdges;

	private PreparedStatement stmtInsertEvent;
	private PreparedStatement stmtRemoveEvent;
	private PreparedStatement stmtCloseEvent;

	private PreparedStatement stmtInsertEventStream, stmtRemoveEventStream, stmtRemoveAllEventStream;// ,stmtGetAllStreams;

	private PreparedStatement stmtInsertSensorStream, stmtRemoveSensorStream, stmtRemoveAllSensorStream;

	private PreparedStatement stmtInsertPropagation;

	private PreparedStatement stmtSearchAmenity;// , stmtSearchName,
												// stmtSearchTag;
	private PreparedStatement stmtInsertDistanceRequest, stmtInsertDistance;

	/**
	 * Initialisation of the database connection and prepared statements with
	 * Configuration fom GdiConfig class
	 * 
	 * @throws ClassNotFoundException
	 *             If pg-driver is not available
	 * @throws SQLException
	 *             If it cant be connected...
	 */
	public CpGdiInterface() throws ClassNotFoundException, SQLException {
		this(GdiConfig.GDI_HOST, GdiConfig.GDI_PORT, GdiConfig.GDI_DBNAME, GdiConfig.GDI_USERNAME,
				GdiConfig.GDI_PASSWORD);
	}

	/**
	 * Initialisation of the database connection and prepared statements.
	 * 
	 * @param hostname
	 *            Hostname of the database (localhost)
	 * @param port
	 *            Port of the database (depends on if you are using a tunnel or
	 *            connect directly standard port is 5432)
	 * @throws ClassNotFoundException
	 *             If pg-driver is not available
	 * @throws SQLException
	 *             If it cant be connected...
	 */
	public CpGdiInterface(String hostname, int port, String dbname, String username, String password)
			throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");
		String url = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbname;
		System.out.println(url);
		conn = DriverManager.getConnection(url, username, password);
		((org.postgresql.PGConnection) conn).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
		((org.postgresql.PGConnection) conn).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));
		stmtInsertRoute = conn.prepareStatement(
				"INSERT INTO cp_routes (request_id, route_id, cost, geom, edge_count) VALUES (?, DEFAULT, ?,?,?) RETURNING  route_id;");
		stmtInsertRequest = conn.prepareStatement(
				"INSERT INTO cp_route_requests (request_id, cost_metric, from_geom,from_id, to_geom,to_id) VALUES (DEFAULT,?,?,?,?,?) RETURNING request_id;");
		stmtgetRoute = conn.prepareStatement("SELECT  ST_LineMerge(st_union(st_simplify(the_geom,100))) as geometry, "
				+ "st_length(ST_LineMerge(st_union(the_geom)),true)::int as length_m, "
				+ "sum(st_length(the_geom,true) / (maxspeed_forward /3.6)*0.75)::int as time_s, "
				+ "sum(route.cost) as total_cost, " + "count(osm_id)::int as distinct_osm_ids, "
				+ "array_agg(edge_id) as edges, " + "array_agg(cityCostMulti) as city_cost_multi, "
				+ "array_agg(st_length(the_geom,true)) as edge_lengths "
				+ "FROM ways JOIN (SELECT seq, node AS node, edge AS edge_id, cost " + "FROM pgr_dijkstra(?, ? , ? "
				+ ", false)   ) AS route ON ways.gid = route.edge_id");
		stmtResetAll = conn
				.prepareStatement("UPDATE ways SET " + getColumnByMetric(CpRouteRequest.ROUTE_COST_METRIC_DISTANCE)
						+ " = 1.0, " + getColumnByMetric(CpRouteRequest.ROUTE_COST_METRIC_POLLUTION) + " = 1.0, "
						+ getColumnByMetric(CpRouteRequest.ROUTE_COST_METRIC_TIME) + " = 1.0;");

		stmtGetRouteEventStreams = conn.prepareStatement(
				"select event_uuid as event_uuid , event_topic as event_topic, st_centroid(geom)::Point  as centroid from cp_event_stream where  st_intersects(geom, ST_Transform(ST_BUFFER(ST_Transform(?,900913),?),4326))");
		stmtGetRouteEvents = conn.prepareStatement(
				"select event_uuid as event_uuid , event_topic as event_topic, st_centroid(geom)::Point as centroid from cp_events where event_time >=(?::timestamp - (? || ' seconds')::interval) AND close_time IS NULL AND st_intersects(geom, ST_Transform(ST_BUFFER(ST_Transform(?,900913),?),4326))");
		stmtGetRouteSensors = conn.prepareStatement(
				"select sensor_uuid as sensor_uuid , sercvice_category as sercvice_category, st_centroid(geom)::Point as centroid from cp_sensors where  st_intersects(geom, ST_Transform(ST_BUFFER(ST_Transform(?,900913),?),4326))");

		stmtInsertEvent = conn.prepareStatement(
				"INSERT INTO cp_events(event_uuid, event_topic,event_time, geom) VALUES(?,?,?,ST_Transform(?,4326))");
		stmtRemoveEvent = conn.prepareStatement("DELETE FROM cp_events WHERE event_uuid=?");
		stmtCloseEvent = conn.prepareStatement("UPDATE cp_events SET close_time= ? WHERE event_uuid= ?");

		stmtInsertEventStream = conn.prepareStatement(
				"INSERT INTO cp_event_stream(event_uuid, event_topic, geom) VALUES(? , ? ,ST_Transform(?,4326))");
		stmtRemoveEventStream = conn.prepareStatement("DELETE FROM cp_event_stream WHERE event_uuid=?");
		stmtRemoveAllEventStream = conn.prepareStatement("DELETE FROM cp_event_stream");
		stmtGetAllEventStreams = conn.prepareStatement(
				"select event_uuid as event_uuid , event_topic as event_topic, st_centroid(geom)::Point as centroid from cp_event_stream");

		stmtInsertSensorStream = conn.prepareStatement(
				"INSERT INTO cp_sensors(sensor_uuid, sensor_annotation_id, sercvice_category, geom) VALUES(? , ? , ? ,ST_Transform(?,4326))");
		stmtRemoveSensorStream = conn.prepareStatement("DELETE FROM cp_sensors WHERE sensor_uuid=?");
		stmtRemoveAllSensorStream = conn.prepareStatement("DELETE FROM cp_sensors");

		stmtPropGetEdges = conn
				.prepareStatement("select gid as gid, source as source, the_geom as geom  from ways where target = ?");

		stmtInsertPropagation = conn.prepareStatement(
				"INSERT INTO cp_propagation(request_id, edge_gid, source_node, depth, geom) VALUES(?, ? , ?, ?, ?)");

		// stmtSearchAmenity=conn.prepareStatement("select osm_id as id,
		// st_transform(way,4326) as way,name as name,
		// st_distance(st_transform(way,4326), ?::geography) as dist from
		// planet_osm_point where way && st_transform(ST_MakeEnvelope(17.7,
		// 59.0, 18.3, 59.7, 4326),4326) and amenity= ? order by dist limit
		// ?;");
		stmtSearchAmenity = conn.prepareStatement(
				"select osm_id as id, st_transform(way,4326) as way,name as name, st_distance(st_transform(way,4326), ?::geography) as dist from planet_osm_point where  way && st_transform(ST_MakeEnvelope(17.7, 59.0, 18.3, 59.7, 4326),4326) and amenity= ? order by dist limit ?;");

		stmtInsertDistanceRequest = conn.prepareStatement(
				"INSERT INTO cp_distance_requests (repetition_id, request_id, from_id, from_geom, amenity, count) VALUES (?,DEFAULT,?,?,?,?) RETURNING request_id;");
		stmtInsertDistance = conn.prepareStatement(
				"INSERT INTO cp_distances (request_id, osm_id, direct_distance, route_distance, euclidean_geom, route_geom, better_than_before, edge_count, edge_length_array, duration) VALUES (?,?,?,?,ST_MakeLine(?,?),?,?,?,?,?) RETURNING request_id;");

	}

	/**
	 * discard database connection
	 * 
	 * @throws SQLException
	 *             If connection can't be closed
	 */
	public void closeConnection() throws SQLException {
		conn.close();
		// statements could be closed here
	}

	public static PGgeometry getPointGeometry(double lon, double lat) throws SQLException {
		return new PGgeometry("SRID=4326;POINT(" + lon + " " + lat + ")");
	}

	public static PGgeometry getPointGeometry(double x, double y, int epsg) throws SQLException {
		return new PGgeometry("SRID=" + epsg + ";POINT(" + x + " " + y + ")");
	}

	public static PGgeometry getGeometry(String wktWithoutEpsg, int epsg) throws SQLException {
		return new PGgeometry("SRID=" + epsg + ";" + wktWithoutEpsg);
	}

	public static PGgeometry getLineGeometry(double fromX, double fromY, double toX, double toY, int epsg)
			throws SQLException {
		return new PGgeometry("SRID=" + epsg + ";LINESTRING(" + fromX + " " + fromY + "," + toX + " " + toY + ")");
	}

	/**
	 * just a test function
	 * 
	 * @return 1 if gdi is available
	 */
	public int simpleQuery() {
		Statement s;
		try {
			s = conn.createStatement();
			ResultSet r = s.executeQuery("select the_geom,osm_id from ways limit 1");
			while (r.next()) {
				PGgeometry geom = (PGgeometry) r.getObject(1);
				int id = r.getInt(2);
				System.out.println("Row " + id + ":");
				System.out.println(geom.toString());
			}
			s.close();
			return 1;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Register a new sensor/stream
	 * 
	 * @param uuid
	 *            UUID of sensor/stream
	 * @param sensorId
	 *            Internal id for debg purposes (eg. the number of the aarhus
	 *            traffic sensors, or parkinggarage name)
	 * @param serviceCategory
	 *            for extended searches like (return all parking garages)
	 * @param lon
	 *            longitude in wgs84
	 * @param lat
	 *            latitude in wgs84
	 * @return true if sensor was registeres
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean registerStream(UUID uuid, String sensorId, String serviceCategory, double lon, double lat)
			throws SQLException {
		return registerStream(uuid, sensorId, serviceCategory, getPointGeometry(lon, lat));
	}

	/**
	 * Register a new sensor/stream and convert coordinates to WGS84 in the
	 * database
	 * 
	 * @param uuid
	 *            UUID of sensor/stream
	 * @param sensorId
	 *            Internal id for debug purposes (eg. the number of the aarhus
	 *            traffic sensors, or parkinggarage name)
	 * @param serviceCategory
	 *            for extended searches like (return all parking garages)
	 * @param x
	 *            1st WKT-coordinate-Value of reference system
	 * @param y
	 *            2nd WKT-coordinate-Value of reference system
	 * @param epsg
	 *            EPSG of coordinate reference system (eg. 4326 for WGS84/GPS,
	 *            or 31700 for stereo70,
	 *            http://spatialreference.org/ref/epsg/wgs-84/)
	 * @return true if sensor was registeres
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean registerStream(UUID uuid, String sensorId, String serviceCategory, double x, double y, int epsg)
			throws SQLException {
		return registerStream(uuid, sensorId, serviceCategory, getPointGeometry(x, y, epsg));
	}

	/**
	 * Register a new sensor/stream and convert Geometry to WGS84 in the
	 * database
	 * 
	 * @param uuid
	 *            UUID of sensor/stream
	 * @param sensorId
	 *            Internal id for debug purposes (eg. the number of the aarhus
	 *            traffic sensors, or parkinggarage name)
	 * @param serviceCategory
	 *            for extended searches like (return all parking garages)
	 * @param wkt
	 *            WellKnownText geometry description without Coordinate
	 *            Reference System
	 * @param epsg
	 *            EPSG of coordinate reference system (eg. 4326 for WGS84/GPS,
	 *            or 31700 for stereo70,
	 *            http://spatialreference.org/ref/epsg/wgs-84/)
	 * @return true if sensor was registered
	 * @throws SQLException
	 *             If something went wrong
	 */

	public boolean registerStream(UUID uuid, String sensorId, String serviceCategory, String wkt, int epsg)
			throws SQLException {
		return registerStream(uuid, sensorId, serviceCategory, getGeometry(wkt, epsg));
	}

	private boolean registerStream(UUID uuid, String sensorId, String serviceCategory, PGgeometry geom)
			throws SQLException {
		System.out.println(uuid);
		stmtInsertSensorStream.setObject(1, uuid);
		stmtInsertSensorStream.setString(2, sensorId);
		stmtInsertSensorStream.setString(3, serviceCategory);
		stmtInsertSensorStream.setObject(4, geom);
		System.out.println(stmtInsertSensorStream);
		boolean ex = stmtInsertSensorStream.executeUpdate() > 0;
		return ex;
	}

	/**
	 * Deregister/Remove previously registered Sensor/stream (delete it from the
	 * database)
	 * 
	 * @param uuid
	 *            uuid of stream/sensor
	 * @return true if entry was deleted
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean removeStream(UUID uuid) throws SQLException {
		stmtRemoveSensorStream.setObject(1, uuid);
		boolean ex = stmtRemoveSensorStream.executeUpdate() > 0;
		return (ex);
	}

	public boolean removeAllStreams() throws SQLException {
		boolean ex = stmtRemoveAllSensorStream.executeUpdate() > 0;
		return (ex);
	}

	/**
	 * Register a new EventStream in the GDI
	 * 
	 * @param uuid
	 *            Event Stream UUID
	 * @param routingKey
	 *            Event routingKey
	 * @param lon
	 *            longitude in wgs84
	 * @param lat
	 *            latitude in wgs84
	 * @return true if added
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean registerEventStream(UUID uuid, String routingKey, double lon, double lat) throws SQLException {
		return registerEventStream(uuid, routingKey, getPointGeometry(lon, lat));
	}

	/**
	 * Register a new EventStream in the GDI
	 * 
	 * @param uuid
	 *            Event Stream UUID
	 * @param routingKey
	 *            Event routingKey
	 * @param x
	 *            1st WKT-coordinate-Value of reference system
	 * @param y
	 *            2nd WKT-coordinate-Value of reference system
	 * @param epsg
	 *            EPSG of coordinate reference system (eg. 4326 for WGS84/GPS,
	 *            or 31700 for stereo70,
	 *            http://spatialreference.org/ref/epsg/wgs-84/)
	 * @return true if EventStream was registered
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean registerEventStream(UUID uuid, String routingKey, double x, double y, int epsg) throws SQLException {
		return registerEventStream(uuid, routingKey, getPointGeometry(x, y, epsg));
	}

	/**
	 * Register a new EventStream in the GDI
	 * 
	 * @param uuid
	 *            Event Stream UUID
	 * @param routingKey
	 *            Event routingKey
	 * @param wkt
	 *            WellKnownText geometry description without Coordinate
	 *            Reference System
	 * @param epsg
	 *            EPSG of coordinate reference system (eg. 4326 for WGS84/GPS,
	 *            or 31700 for stereo70,
	 *            http://spatialreference.org/ref/epsg/wgs-84/)
	 * @return true if sensor was registered
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean registerEventStream(UUID uuid, String routingKey, String wkt, int epsg) throws SQLException {
		return registerEventStream(uuid, routingKey, getGeometry(wkt, epsg));
	}

	private boolean registerEventStream(UUID uuid, String routingKey, PGgeometry geom) throws SQLException {
		// System.out.println(uuid);
		stmtInsertEventStream.setObject(1, uuid);
		stmtInsertEventStream.setString(2, routingKey);
		stmtInsertEventStream.setObject(3, geom);
		// System.out.println(stmtInsertEventStream);
		boolean ex = stmtInsertEventStream.executeUpdate() > 0;
		return ex;
		// String sqlString;
		// System.out.println(epsg + " / " + CpGdiInterface.EPSG_WGS84);
		// if (epsg != CpGdiInterface.EPSG_WGS84) { // convert
		// sqlString = "INSERT INTO cp_event_stream(event_uuid, event_topic,
		// geom) VALUES('" + uuid + "','" + topic +
		// "',ST_Transform(ST_GeomFromText('" + wkt
		// + "'," + epsg + ")," + CpGdiInterface.EPSG_WGS84 + "));";
		// } else { // don't convert
		// sqlString = "INSERT INTO cp_event_stream(event_uuid, event_topic,
		// geom) VALUES('" + uuid + "','" + topic + "',ST_GeomFromText('" + wkt
		// + "',"
		// + epsg + "));";
		// }
		// System.out.println(sqlString);
		// Statement s = conn.createStatement();
		// boolean ex = s.execute(sqlString);
		// boolean ok = (!ex && (s.getUpdateCount() == 1));
		// s.close();
		// return ok;
	}

	/**
	 * Deregister previously registered EventStream (delete it from the
	 * database)
	 * 
	 * @param uuid
	 *            uuid of stream/sensor
	 * @return true if entry was deleted
	 * @throws SQLException
	 *             If something went wrong
	 */
	public boolean removeEventStream(UUID uuid) throws SQLException {
		stmtRemoveEventStream.setObject(1, uuid);
		boolean ex = stmtRemoveEventStream.executeUpdate() > 0;
		return (ex);
	}

	public boolean removeAllEventStream() throws SQLException {
		boolean ex = stmtRemoveAllEventStream.executeUpdate() > 0;
		return (ex);
	}

	/**
	 * Register a new Event in the GDI
	 * 
	 * @param uuid
	 *            uuid
	 * @param topic
	 *            topic
	 * @param event_time
	 *            time the event occured
	 * @param lon
	 *            longitude wgs84
	 * @param lat
	 *            latidute wgs84
	 * @return true if the event was registered in the gdi
	 * @throws SQLException
	 *             if something went wrong
	 */
	public boolean registerEvent(UUID uuid, String topic, Timestamp event_time, double lon, double lat)
			throws SQLException {
		return registerEvent(uuid, topic, event_time, getPointGeometry(lon, lat));
	}

	/**
	 * Register a new Event in the GDI
	 * 
	 * @param uuid
	 *            uuid
	 * @param topic
	 *            topic
	 * @param event_time
	 *            time the event occured
	 * @param x
	 *            x coordinate of the coordinate referene system defined by epsg
	 * @param y
	 *            y coordinate of the coordinate referene system defined by epsg
	 * @param epsg
	 *            EPSG code of the Coordinate reference system
	 * @return true if the event was registered in the gdi
	 * @throws SQLException
	 *             if something went wrong
	 */
	public boolean registerEvent(UUID uuid, String topic, Timestamp event_time, double x, double y, int epsg)
			throws SQLException {
		return registerEvent(uuid, topic, event_time, getPointGeometry(x, y, epsg));
	}

	/**
	 * Register a new Event in the GDI
	 * 
	 * @param uuid
	 *            uuid
	 * @param topic
	 *            topic
	 * @param event_time
	 *            time the event occured
	 * @param wkt
	 *            WellKnownText description of the geometry/location
	 * @param epsg
	 *            EPSG code of the Coordinate reference system
	 * @return true if the event was registered in the gdi
	 * @throws SQLException
	 *             if something went wrong
	 */
	public boolean registerEvent(UUID uuid, String topic, Timestamp event_time, String wkt, int epsg)
			throws SQLException {
		return registerEvent(uuid, topic, event_time, getGeometry(wkt, epsg));
	}

	/**
	 * Register a new Event in the GDI
	 * 
	 * @param uuid
	 *            uuid
	 * @param topic
	 *            topic
	 * @param event_time
	 *            time the event occured
	 * @param geom
	 *            Geometry
	 * @return true if the event was registered in the gdi
	 * @throws SQLException
	 *             if something went wrong
	 */
	public boolean registerEvent(UUID uuid, String topic, Timestamp event_time, PGgeometry geom) throws SQLException {
		stmtInsertEvent.setObject(1, uuid);
		stmtInsertEvent.setString(2, topic);
		stmtInsertEvent.setTimestamp(3, event_time);
		stmtInsertEvent.setObject(4, geom);
		// System.out.println(uuid);
		// System.out.println(stmtInsertEvent);
		boolean ex = stmtInsertEvent.executeUpdate() > 0;
		return (ex);
	}

	/**
	 * Deregister an event / Remove it from the GDI
	 * 
	 * @param uuid
	 *            UUID of the event
	 * @return true if removed
	 * @throws SQLException
	 *             e.g. if uid is not regeistered
	 */
	public boolean deregisterEvent(UUID uuid) throws SQLException {
		stmtRemoveEvent.setObject(1, uuid);
		boolean ex = stmtRemoveEvent.executeUpdate() > 0;
		return (ex);
	}

	/**
	 * Set a stop / end time of the validity of the event
	 * 
	 * @param uuid
	 *            UUID of the event
	 * @param closeTime
	 *            timestamp when t ended / was not valid anymore
	 * @return true if time could be set
	 * @throws SQLException
	 *             e.g. if uid is not regeistered
	 */
	public boolean closeEvent(UUID uuid, Timestamp closeTime) throws SQLException {
		stmtCloseEvent.setObject(1, closeTime);
		stmtCloseEvent.setObject(2, uuid);
		System.out.println(stmtCloseEvent);
		boolean ex = stmtCloseEvent.executeUpdate() > 0;
		return (ex);
	}

	/**
	 * Get the nearest node it in the graph for routing
	 * 
	 * @param lon
	 *            WGS 84 longitude
	 * @param lat
	 *            WGS 84 latitude
	 * @return NodeId of nearest Node iin the Graph, -1 if nothing is found
	 * @throws SQLException
	 *             If something went wrong
	 */
	public int getNearestNodeID(double lon, double lat) throws SQLException {
		String sql = ("select id from ways_vertices_pgr order by st_distance(the_geom, st_setsrid(ST_GeomFromText('Point("
				+ lon + " " + lat + ")'), 4326)) limit 1");
		// String sql = ("SELECT * FROM denmark_nodes ORDER BY geom <->
		// St_SetSRID(ST_Point("+lon+","+lat+"),4326) LIMIT 1;");

		Statement s = conn.createStatement();
		ResultSet r = s.executeQuery(sql);
		if (r.next() != false) {
			int nodeId = r.getInt(1);
			s.close();
			return nodeId;
		} else {
			return -1;
		}
	}

	public int getNearestNodeID(PGgeometry geom) throws SQLException {
		String sql = ("select id from ways_vertices_pgr order by st_distance(the_geom, st_setsrid(ST_GeomFromText('"
				+ geom + "'), 4326)) limit 1");
		// System.out.println(sql);
		Statement s = conn.createStatement();
		ResultSet r = s.executeQuery(sql);
		if (r.next() != false) {
			int nodeId = r.getInt(1);
			s.close();
			return nodeId;
		} else {
			return -1;
		}
	}

	/**
	 * Request a number of routes between two points
	 * 
	 * @param fromLong
	 *            origin longitude (WGS84)
	 * @param fromLat
	 *            origin latitude (WGS84)
	 * @param toLong
	 *            destination longitude (WGS84)
	 * @param toLat
	 *            destination latitude (WGS84)
	 * @param costMetric
	 *            cost metric that should be used, e.g.
	 *            CpRouteRequest.ROUTE_COST_METRIC_DISTANCE
	 * @param count
	 *            number of routes that should be returned
	 * @return Route Request including all routes
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpRouteRequest getCityRoutes(double fromLong, double fromLat, double toLong, double toLat, String costMetric,
			int count) throws SQLException {
		// xMin,yMin 9.86849,56.0123 : xMax,yMax 10.4182,56.3069
		// if ((Math.min(fromLong, toLong) < 9.86849) || (Math.max(fromLong,
		// toLong) > 10.4182) || (Math.min(fromLat, toLat) < 56.0123)
		// || (Math.max(fromLat, toLat) > 56.3069)) {
		// throw new SQLException(
		// "Routing Layer currently is only available for City of Aarhus and has
		// the following Bounds: xMin,yMin 9.86849,56.0123 : xMax,yMax
		// 10.4182,56.3069");
		// }
		int fromNodeId = getNearestNodeID(fromLong, fromLat);
		int toNodeId = getNearestNodeID(toLong, toLat);
		PGgeometry fromGeom = getPointGeometry(fromLong, fromLat);// new
																	// PGgeometry("SRID=4326;POINT("
																	// +
																	// fromLong
																	// + " " +
																	// fromLat +
																	// ")");
		PGgeometry toGeom = getPointGeometry(toLong, toLat);// new
															// PGgeometry("SRID=4326;POINT("
															// + toLong + " " +
															// toLat + ")");
		CpRouteRequest cprr = new CpRouteRequest(fromNodeId, fromGeom, toNodeId, toGeom, costMetric);
		cprr.setRequestId(insertRouteRequestInGdi(cprr));
		return getCityRoutes(cprr, count);
	}

	/**
	 * Get a CpRouteRequest for two locations and a metric
	 * 
	 * @param fromWkt
	 *            From PGGeometry in WKT
	 * @param toWkt
	 *            To PGGeometry in WKT
	 * @param costMetric
	 *            Metric that should be used to find optimal route
	 * @param count
	 *            number of routes
	 * @return CpRouteRequest
	 * @throws SQLException
	 *             If invalid positions or geometries are given
	 */
	public CpRouteRequest getCityRoutes(String fromWkt, String toWkt, String costMetric, int count)
			throws SQLException {
		// xMin,yMin 9.86849,56.0123 : xMax,yMax 10.4182,56.3069
		PGgeometry fromGeom = getGeometry(fromWkt, 4326);
		PGgeometry toGeom = getGeometry(toWkt, 4326);
		int fromNodeId = getNearestNodeID(fromGeom);
		int toNodeId = getNearestNodeID(toGeom);
		CpRouteRequest cprr = new CpRouteRequest(fromNodeId, fromGeom, toNodeId, toGeom, costMetric);
		cprr.setRequestId(insertRouteRequestInGdi(cprr));
		return getCityRoutes(cprr, count);
	}

	/**
	 * Get a number of city Routes for a route request
	 * 
	 * @param cprr
	 *            get city routes from a pre-generated request
	 * @param count
	 *            number of routes that should be returned
	 * @return updated CpRouteRequest object
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpRouteRequest getCityRoutes(CpRouteRequest cprr, int count) throws SQLException {
		for (int i = 0; i < count; i++) {
			CpRoute cpr = getCityRoute(cprr);
			cprr.addRoute(cpr);
			cpr.setRouteId(insertRouteInGdi(cprr.getRequestId(), cpr));
			if (count > 1)
				System.out.println("Udated CityCost Multiplicators: " + updateLastRoutesEdges(cpr));
		}
		if (count > 1)
			System.out.println("Reseted CityCost Multiplicators: " + resetCityCostMultiplicator());
		return cprr;
	}

	private int updateLastRoutesEdges(CpRoute cp) throws SQLException {
		String sql = "UPDATE ways as d SET gid=s.gid, citycostmulti=s.citycostmulti FROM (VALUES ";
		Long[] edges = cp.getEdges();
		Double[] costMulti = cp.getCityCostMulti();
		for (int i = 0; i < edges.length; i++) {
			if (i > 0)
				sql += ",";
			sql += "(" + edges[i] + ", " + costMulti[i] * CITYCOSTMULTIPLICATOR_MODIFIER + ")";
		}
		sql += ")AS s(gid, citycostmulti) WHERE s.gid=d.gid";
		System.out.println(sql);
		Statement s = conn.createStatement();
		int count = s.executeUpdate(sql);
		return count;
	}

	protected int resetCityCostMultiplicator() throws SQLException {
		String sql = "UPDATE ways SET cityCostMulti = 1.0";
		Statement s = conn.createStatement();
		int count = s.executeUpdate(sql);
		return count;
	}

	private CpRoute getCityRoute(CpRouteRequest cprr) throws SQLException, NullPointerException {
		stmtgetRoute.setString(1, "SELECT gid AS id, source::integer, target::integer, (" + cprr.getCostMetricFormula()
				+ ")::double precision AS cost FROM ways");
		stmtgetRoute.setInt(2, cprr.getNodeFrom());
		stmtgetRoute.setInt(3, cprr.getNodeTo());
		
		System.out.println(stmtgetRoute);
			long currentTime = System.currentTimeMillis();
		
		ResultSet r = stmtgetRoute.executeQuery();
			System.out.println("Route Select took "+(currentTime-System.currentTimeMillis())+"ms");

		r.next(); // we get max. 1 row
		PGgeometry geom = (PGgeometry) r.getObject("geometry");
		int lengthM = (int) r.getObject("length_m");
		int timeS = (int) r.getObject("time_s");
		double cost = (double) r.getObject("total_cost");
		Long[] edges = (Long[]) r.getArray("edges").getArray();
		Double[] edgeLengths = (Double[]) r.getArray("edge_lengths").getArray();
		Double[] cityCostMulti = (Double[]) r.getArray("city_cost_multi").getArray();
		return (new CpRoute(geom, lengthM, timeS, cost, edges, cityCostMulti, edgeLengths));
	};

	/**
	 * Get ilst of n next Amenities (like Parking, Atm, Hospital)
	 * 
	 * @param lon
	 *            Longitude (WGS84)
	 * @param lat
	 *            Latitude (WGS84)
	 * @param amenity
	 *            Amenity Type
	 * @param count
	 *            Number of returned Values
	 * @return ArrayList of CpAmenity objects
	 * @throws SQLException
	 *             If request is not successfull
	 * @throws NullPointerException
	 *             If no routes are available
	 */
	public ArrayList<CpAmenity> getnNextLocationsByAmenity(double lon, double lat, String amenity, int count) throws SQLException, NullPointerException {
		PGgeometry fromGeom = getPointGeometry(lon, lat);
		return getnNextLocationsByAmenity(fromGeom, amenity, count, -1);
	}
	/**
	 * Get ilst of n next Amenities (like Parking, Atm, Hospital)
	 * 
	 * @param lon
	 *            Longitude (WGS84)
	 * @param lat
	 *            Latitude (WGS84)
	 * @param amenity
	 *            Amenity Type
	 * @param count
	 *            Number of returned Values
	 * @param repetitionId
	 *            (Id for persistence Monitoring)
	 * @return ArrayList of CpAmenity objects
	 * @throws SQLException
	 *             If request is not successfull
	 * @throws NullPointerException
	 *             If no routes are available
	 */
	public ArrayList<CpAmenity> getnNextLocationsByAmenity(double lon, double lat, String amenity, int count,
			int repetitionId) throws SQLException, NullPointerException {
		PGgeometry fromGeom = getPointGeometry(lon, lat);
		return getnNextLocationsByAmenity(fromGeom, amenity, count, repetitionId);
	}

	/**
	 * 
	 * @param fromGeom
	 *            PGGeometry in WKT
	 * @param amenity
	 *            Amenity Type
	 * @param count
	 *            Number of returned Values
	 * @param repetitionId
	 *            (Id for persistence Monitoring)
	 * @return ArrayList of CpAmenity objects
	 * @throws SQLException
	 *             If request is not successfull
	 * @throws NullPointerException
	 *             If no routes are available
	 */
	public ArrayList<CpAmenity> getnNextLocationsByAmenity(PGgeometry fromGeom, String amenity, int count,
			int repetitionId) throws SQLException, NullPointerException {
		ArrayList<CpAmenity> amenities = new ArrayList<CpAmenity>();
		int originId = this.getNearestNodeID(fromGeom);
		long distReqId = -1;
		if (GdiConfig.GDI_INSERT_DEBUG_DATA)
			distReqId = insertDistanceRequest(repetitionId, originId, fromGeom, amenity, count);
		stmtSearchAmenity.setObject(1, fromGeom);
		stmtSearchAmenity.setString(2, amenity);
		stmtSearchAmenity.setInt(3, count);
		 //System.out.println(stmtSearchAmenity);
		 //long currentTime = System.currentTimeMillis();
		 ResultSet r = stmtSearchAmenity.executeQuery();
		//System.out.println("Amenity Select took "+(currentTime-System.currentTimeMillis())+"ms");
			
		
		double lastRouteLength = 0;
		while (r.next()) {
			CpAmenity amen;
			try {
				long osmId = r.getLong("id");
				PGgeometry geom = (PGgeometry) r.getObject("way");
				String name = r.getString("name");
				double directDistance = r.getDouble("dist");
				amen = new CpAmenity(osmId, name, geom, fromGeom, directDistance);
				int targetId = this.getNearestNodeID(geom);
				// System.out.println("from "+ originId + " to "+ targetId);
				long current = System.currentTimeMillis();
				CpRouteRequest cprr = new CpRouteRequest(originId, fromGeom, targetId, geom,
						CpRouteRequest.ROUTE_COST_METRIC_DISTANCE);
				CpRoute cpr = this.getCityRoute(cprr);
				long duration = System.currentTimeMillis() - current;
				amen.setRoute(cpr);
				amenities.add(amen);
				if (GdiConfig.GDI_INSERT_DEBUG_DATA) {
					double routeLength = cpr.getLengthM();
					boolean betterThanBefore = (routeLength < lastRouteLength);
					// for just for research tasks
					insertDistance(distReqId, targetId, directDistance, cpr.getLengthM(), fromGeom, geom, cpr.getGeom(),
							betterThanBefore, cpr.getEdges().length, cpr.getEdgeLengths(), duration);
					lastRouteLength = routeLength;
				}
			} catch (NullPointerException e) {
				System.err.println("No Route Today...");
				throw e;
			}
		}
		return amenities;
	}

	private void getsourceNodes(int requestId, ArrayList<Integer> targetNode, int cost,
			HashMap<Integer, Integer> edgesVisited) throws SQLException {
		for (int j = 0; j < targetNode.size(); j++) {
			stmtPropGetEdges.setInt(1, targetNode.get(j));
			System.out.println(stmtPropGetEdges);
			ResultSet r = stmtPropGetEdges.executeQuery();
			targetNode.remove(j);
			while (r.next()) {
				Integer gid = r.getInt("gid");
				Integer source = r.getInt("source");
				PGgeometry geom = (PGgeometry) r.getObject("geom");
				System.out.println(gid + " - " + source + " - " + cost);
				if (!edgesVisited.containsKey(gid)) {
					edgesVisited.put(gid, cost);
					targetNode.add(source);
					insertPropagation(requestId, gid, source, cost, geom);
				}
				if (cost == 0)
					break; // to point out direction for the first edge
			}
		}

	}

	private boolean insertPropagation(int requestId, int edgeGid, int sourceNode, double cost, PGgeometry geom)
			throws SQLException {
		stmtInsertPropagation.setInt(1, requestId);
		stmtInsertPropagation.setInt(2, edgeGid);
		stmtInsertPropagation.setInt(3, sourceNode);
		stmtInsertPropagation.setDouble(4, cost);
		stmtInsertPropagation.setObject(5, geom);
		System.out.println(stmtInsertPropagation);
		boolean ex = stmtInsertPropagation.executeUpdate() > 0;
		return ex;
	}

	/**
	 * a
	 * 
	 * @param requestId
	 * @param targetNode
	 */
	public void depthSearch(int requestId, int targetNode) {
		ArrayList<Integer> targetNodes = new ArrayList<Integer>();
		targetNodes.add(targetNode);
		HashMap<Integer, Integer> edgesVisited = new HashMap<Integer, Integer>();
		for (int cost = 0; cost < 10; cost++) {
			try {
				getsourceNodes(requestId, targetNodes, cost, edgesVisited);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	private long insertRouteRequestInGdi(CpRouteRequest cprr) throws SQLException {
		stmtInsertRequest.setString(1, cprr.getCostMetricFormula());
		stmtInsertRequest.setObject(2, cprr.getFromGeom());
		stmtInsertRequest.setLong(3, cprr.getNodeFrom());
		stmtInsertRequest.setObject(4, cprr.getToGeom());
		stmtInsertRequest.setLong(5, cprr.getNodeTo());
		ResultSet r = stmtInsertRequest.executeQuery();
		r.next();
		long requestId = r.getLong("request_id");
		// stmtInsertRequest.close();
		return (requestId);
	}

	private long insertRouteInGdi(long request_id, CpRoute cpr) throws SQLException {
		stmtInsertRoute.setLong(1, request_id);
		stmtInsertRoute.setDouble(2, cpr.getCost());
		stmtInsertRoute.setObject(3, cpr.getGeom());
		stmtInsertRoute.setInt(4, cpr.getEdges().length);

		ResultSet r = stmtInsertRoute.executeQuery();
		r.next();
		long routeId = r.getLong("route_id");
		// stmtInsertRoute.close();
		return (routeId);
	}

	// EXPERIMENT stmtInsertDistance = conn .prepareStatement("INSERT INTO
	// cp_distance_requests (request_id, from_id, from_geom, amenity, count)
	// VALUES (DEFAULT,?,?,?,?) RETURNING request_id;");

	private long insertDistanceRequest(int repetitionId, int fromId, PGgeometry fromGeom, String amenity, int count)
			throws SQLException {
		stmtInsertDistanceRequest.setInt(1, repetitionId);
		stmtInsertDistanceRequest.setInt(2, fromId);
		stmtInsertDistanceRequest.setObject(3, fromGeom);
		stmtInsertDistanceRequest.setString(4, amenity);
		stmtInsertDistanceRequest.setInt(5, count);
		ResultSet r = stmtInsertDistanceRequest.executeQuery();
		r.next();
		long requestId = r.getLong("request_id");
		// stmtInsertRequest.close();
		return (requestId);
	}

	// (request_id, osm_id, direct_distance, route_distance, euclidean_geom,
	// route_geom)
	private long insertDistance(long request_id, long osm_id, double direct_distance, double route_distance,
			PGgeometry fromPoint, PGgeometry toPoint, PGgeometry route_geom, boolean betterThanBefore, int edges,
			Double[] edgeLengthArray, double duration) throws SQLException {
		stmtInsertDistance.setLong(1, request_id);
		stmtInsertDistance.setLong(2, osm_id);
		stmtInsertDistance.setDouble(3, direct_distance);
		stmtInsertDistance.setDouble(4, route_distance);
		stmtInsertDistance.setObject(5, fromPoint);
		stmtInsertDistance.setObject(6, toPoint);
		stmtInsertDistance.setObject(7, route_geom);
		stmtInsertDistance.setBoolean(8, betterThanBefore);
		stmtInsertDistance.setInt(9, edges);
		stmtInsertDistance.setArray(10, conn.createArrayOf("numeric", edgeLengthArray));
		stmtInsertDistance.setDouble(11, duration);
		ResultSet r = stmtInsertDistance.executeQuery();
		r.next();
		long requestId = r.getLong("request_id");
		// stmtInsertRequest.close();
		return (requestId);
	}
	// EXPERIMENT

	/**
	 * Update one of the costMultiplicator Layers around a certain point, e.g.
	 * to set higher routing cost for pollution
	 * 
	 * @param lon
	 *            WGS 84 longitude
	 * @param lat
	 *            WGS 84 latitude
	 * @param radius_m
	 *            radius around this point where edges of the graph will get a
	 *            higher cost multiplicator
	 * @param costMetric
	 *            cost metric that should be used, e.g.
	 *            CpRouteRequest.ROUTE_COST_METRIC_DISTANCE
	 * @param costMultiplicatorValue
	 *            a value higher than 1.0 to set a higher cost. I.e. 2.0 will
	 *            double the cost.
	 * @return number of edges that were updated
	 * @throws SQLException
	 *             If something went wrong
	 */
	public int updateCostMultiplicatorRadial(double lon, double lat, double radius_m, String costMetric,
			double costMultiplicatorValue) throws SQLException {
		String metricColumnName = getColumnByMetric(costMetric);
		// no prepared statement since layer is not a value
		String sql = "UPDATE ways SET " + metricColumnName + " = " + costMultiplicatorValue
				+ " where the_geom && st_buffer(ST_GeogFromText('SRID=4326;POINT(" + lon + " " + lat + ")'), "
				+ radius_m + ")";
		System.out.println(sql);
		Statement s = conn.createStatement();
		int count = s.executeUpdate(sql);
		return count;
	}

	/**
	 * Update the cost multiplicator in an rectengular area specified by minimum
	 * and maximum bounds
	 * 
	 * @param minLon
	 *            WGS 84 longitude
	 * @param minLat
	 *            WGS 84 latitude
	 * @param maxLon
	 *            WGS 84 longitude
	 * @param maxLat
	 *            WGS 84 latitude
	 * @param costMetric
	 *            cost metric that should be used, e.g.
	 *            CpRouteRequest.ROUTE_COST_METRIC_DISTANCE
	 * @param costMultiplicatorValue
	 *            a value higher than 1.0 to set a higher cost. I.e. 2.0 will
	 *            double the cost.
	 * @return number of edges that were updated
	 * @throws SQLException
	 *             If something went wrong
	 */
	public int updateCostMultiplicatorArea(double minLon, double minLat, double maxLon, double maxLat,
			String costMetric, double costMultiplicatorValue) throws SQLException {
		String metricColumnName = getColumnByMetric(costMetric);
		// no prepared statement since layer is not a value
		String sql = "UPDATE ways SET " + metricColumnName + " = " + costMultiplicatorValue
				+ " where the_geom && ST_MakeEnvelope (" + minLon + " , " + minLat + " , " + maxLon + ", " + maxLat
				+ ", 4326)";
		Statement s = conn.createStatement();
		int count = s.executeUpdate(sql);
		return count;
	}

	/**
	 * 
	 * Reset cost multiplicators of a specific type
	 * 
	 * @param costMetric
	 *            costMetric cost metric that should be reset, e.g.
	 *            CpRouteRequest.ROUTE_COST_METRIC_DISTANCE
	 * @return number of edges that were updated
	 * @throws SQLException
	 *             If something went wrong
	 */
	public int resetCostMultiplicators(String costMetric) throws SQLException {
		String metricColumnName = getColumnByMetric(costMetric);
		// no prepared statement since layer is not a value
		String sql = "UPDATE ways SET " + metricColumnName + " = 1.0";
		Statement s = conn.createStatement();
		int count = s.executeUpdate(sql);
		return count;
	}

	/**
	 * Reset all cost multiplcators for all metrics to 1.0
	 * 
	 * @return number of edges that were updated
	 * @throws SQLException
	 *             If something went wrong
	 */
	public int resetCostMultiplicators() throws SQLException {
		int count = stmtResetAll.executeUpdate();
		return count;
	}

	/**
	 * @param costMetric
	 * @return columnname in the ways table
	 * @throws SQLException
	 *             If something went wrong
	 */
	private String getColumnByMetric(String costMetric) throws SQLException {
		String metricColumnName;
		switch (costMetric) {
		case CpRouteRequest.ROUTE_COST_METRIC_DISTANCE:
			metricColumnName = "distancemulti";
			break;
		case CpRouteRequest.ROUTE_COST_METRIC_TIME:
			metricColumnName = "timemulti";
			break;
		case CpRouteRequest.ROUTE_COST_METRIC_POLLUTION:
			metricColumnName = "pollutionmulti";
			break;
		default:
			throw new SQLException(
					"Unknown cost metric column to update Table! use public metrics of CpRouteRequest. But not the combined one! ");
		}
		return metricColumnName;
	}

	// public void getEventStreamsForRoute(CpRoute cpr, double buffer_m) throws
	// SQLException {
	// stmtGetRouteEventStreamsArray.setObject(1, cpr.getGeom());
	// stmtGetRouteEventStreamsArray.setDouble(2, buffer_m);
	// ResultSet r = stmtGetRouteEventStreamsArray.executeQuery();
	// if (r.next() != false) {
	// System.out.println(r.getArray("event_uuid"));
	// UUID[] edges = (UUID[]) r.getArray("event_uuid").getArray();
	// String[] cityCostMulti = (String[]) r.getArray("event_topic").getArray();
	// for (int i = 0; i < cityCostMulti.length; i++) {
	// System.out.println(edges[i] + " " + cityCostMulti[i]);
	// }
	// } else {
	// System.out.println("No Result");
	// }
	//
	// }
	//
	// public void getSensorsForRoute(CpRoute cpr, double buffer_m) throws
	// SQLException {
	// stmtGetRouteSensorsArray.setObject(1, cpr.getGeom());
	// stmtGetRouteSensorsArray.setDouble(2, buffer_m);
	// ResultSet r = stmtGetRouteSensorsArray.executeQuery();
	// if (r.next() != false) {
	// UUID[] edges = (UUID[]) r.getArray("sensor_uuid").getArray();
	// String[] cityCostMulti = (String[])
	// r.getArray("sercvice_category").getArray();
	// for (int i = 0; i < cityCostMulti.length; i++) {
	// System.out.println(edges[i] + " " + cityCostMulti[i]);
	// }
	// } else {
	// System.out.println("No Result");
	// }
	// }
	//
	// public void getEventsForRoute(CpRoute cpr, Timestamp fromTime, int
	// duration_s, double buffer_m) throws SQLException {
	// stmtGetRouteEventsArray.setObject(1, cpr.getGeom());
	// stmtGetRouteEventsArray.setTimestamp(2, fromTime);
	// stmtGetRouteEventsArray.setInt(3, duration_s);
	// stmtGetRouteEventsArray.setDouble(4, buffer_m);
	// System.out.println(stmtGetRouteEventsArray);
	// ResultSet r = stmtGetRouteSensorsArray.executeQuery();
	// if (r.next() != false) {
	// UUID[] edges = (UUID[]) r.getArray("sensor_uuid").getArray();
	// String[] cityCostMulti = (String[])
	// r.getArray("sercvice_category").getArray();
	// for (int i = 0; i < cityCostMulti.length; i++) {
	// System.out.println(edges[i] + " " + cityCostMulti[i]);
	// }
	// } else {
	// System.out.println("No Result");
	// }
	// }
	/**
	 * Get Event streams in a distance of a specific route
	 * 
	 * @param cpr
	 *            Route that should be checked
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiEventStream elements containing the nearby
	 *         EventStreams
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiEventStream[] getEventStreamsForRoute(CpRoute cpr, double buffer_m) throws SQLException {
		return getEventStreamsForRoute(cpr.getGeom(), buffer_m);
	}

	/**
	 * Get Event streams in a distance of a specific route
	 * 
	 * @param wgs84WktGeometry
	 *            WKT Text describing geometry in WGS84
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiEventStream elements containing the nearby
	 *         EventStreams
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiEventStream[] getEventStreamsForRoute(String wgs84WktGeometry, double buffer_m) throws SQLException {
		return (getEventStreamsForRoute(wgs84WktGeometry, 4326, buffer_m));
	}

	/**
	 * Get Event streams in a distance of a specific route
	 * 
	 * @param wktGeometry
	 *            WKT string to describe the route
	 * @param epsg
	 *            Epsg code for projection
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiEventStream elements containing the nearby
	 *         EventStreams
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiEventStream[] getEventStreamsForRoute(String wktGeometry, int epsg, double buffer_m)
			throws SQLException {
		String wktString = "SRID=" + epsg + ";" + wktGeometry;
		PGgeometry pgeo = new PGgeometry(wktString);
		return getEventStreamsForRoute(pgeo, buffer_m);
	}

	/**
	 * Get Event streams in a distance of a specific route
	 * 
	 * @param pgeo
	 *            PGGeometry object to describe the route
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiEventStream elements containing the nearby
	 *         EventStreams
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiEventStream[] getEventStreamsForRoute(PGgeometry pgeo, double buffer_m) throws SQLException {
		stmtGetRouteEventStreams.setObject(1, pgeo);
		stmtGetRouteEventStreams.setDouble(2, buffer_m);
		System.out.println(stmtGetRouteEventStreams);
		ResultSet r = stmtGetRouteEventStreams.executeQuery();
		System.out.println(stmtGetRouteEventStreams);

		ArrayList<CpGdiEventStream> list = new ArrayList<CpGdiEventStream>();
		while (r.next()) {
			UUID uuid = (UUID) r.getObject("event_uuid");
			String topic = r.getString("event_topic");
			PGpoint pgp = new PGpoint(r.getString("centroid"));
			list.add(new CpGdiEventStream(uuid, topic, pgp));
		}
		return (CpGdiEventStream[]) list.toArray(new CpGdiEventStream[list.size()]);
	}

	/**
	 * Return all Event Streams as Array
	 * 
	 * @return CpGdiEventStream[]
	 * @throws SQLException
	 */
	public CpGdiEventStream[] getAllEventStreams() throws SQLException {
		ResultSet r = stmtGetAllEventStreams.executeQuery();
		System.out.println(stmtGetAllEventStreams);

		ArrayList<CpGdiEventStream> list = new ArrayList<CpGdiEventStream>();
		while (r.next()) {
			UUID uuid = (UUID) r.getObject("event_uuid");
			String topic = r.getString("event_topic");
			PGpoint pgp = new PGpoint(r.getString("centroid"));
			list.add(new CpGdiEventStream(uuid, topic, pgp));
		}
		return (CpGdiEventStream[]) list.toArray(new CpGdiEventStream[list.size()]);
	}

	/**
	 * Get Sensor streams in a distance of a specific route
	 * 
	 * @param cpr
	 *            Route that should be checked
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiSensorStream elements containing the nearby
	 *         SensorStreams
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiSensorStream[] getSensorsForRoute(CpRoute cpr, double buffer_m) throws SQLException {
		stmtGetRouteSensors.setObject(1, cpr.getGeom());
		stmtGetRouteSensors.setDouble(2, buffer_m);
		ResultSet r = stmtGetRouteSensors.executeQuery();
		System.out.println(stmtGetRouteSensors);
		ArrayList<CpGdiSensorStream> list = new ArrayList<CpGdiSensorStream>();
		while (r.next()) {
			UUID uuid = (UUID) r.getObject("sensor_uuid");
			String serviceCategory = r.getString("sercvice_category");
			PGpoint pgp = new PGpoint(r.getString("centroid"));
			list.add(new CpGdiSensorStream(uuid, serviceCategory, pgp));
		}
		return (CpGdiSensorStream[]) list.toArray(new CpGdiSensorStream[list.size()]);
	}

	/**
	 * Get Events nearby the route which were created in duration_s before
	 * fromTime timestamp and have not been closed. So you can check e.g. for
	 * the interval of the last 500 seconds from the current time.
	 * 
	 * @param cpr
	 *            Route that should be checked
	 * @param fromTime
	 *            start duration we are looking at
	 * @param duration_s
	 *            duration of the interval
	 * @param buffer_m
	 *            the distance between events and routes
	 * @return Array of CpGdiEvent elements containing the nearby Events in the
	 *         specified time
	 * @throws SQLException
	 *             If something went wrong
	 */
	public CpGdiEvent[] getEventsForRoute(CpRoute cpr, Timestamp fromTime, int duration_s, double buffer_m)
			throws SQLException {
		stmtGetRouteEvents.setTimestamp(1, fromTime);
		stmtGetRouteEvents.setInt(2, duration_s);
		stmtGetRouteEvents.setObject(3, cpr.getGeom());
		stmtGetRouteEvents.setDouble(4, buffer_m);
		System.out.println(stmtGetRouteEvents);
		ResultSet r = stmtGetRouteEvents.executeQuery();
		ArrayList<CpGdiEvent> list = new ArrayList<CpGdiEvent>();
		while (r.next()) {
			UUID uuid = (UUID) r.getObject("event_uuid");
			String topic = r.getString("event_topic");
			PGpoint pgp = new PGpoint(r.getString("centroid"));
			list.add(new CpGdiEvent(uuid, topic, pgp));
		}
		return (CpGdiEvent[]) list.toArray(new CpGdiEvent[list.size()]);
	}

}
