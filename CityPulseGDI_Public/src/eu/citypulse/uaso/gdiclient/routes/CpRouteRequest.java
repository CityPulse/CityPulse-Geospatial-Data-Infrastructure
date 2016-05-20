package eu.citypulse.uaso.gdiclient.routes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.postgis.PGgeometry;

/**
 * @author danielk
 * One request of a  bunch of routes
 */
public class CpRouteRequest {

	
	/**  Distance metric is used */
	public static final String ROUTE_COST_METRIC_DISTANCE = "distance";
	/**  Time metric is used */
	public static final String ROUTE_COST_METRIC_TIME = "time";
	/**  Pollution metric is used */
	public static final String ROUTE_COST_METRIC_POLLUTION = "pollution";
	/**  Combined  metric is used */
	public static final String ROUTE_COST_METRIC_COMBINED = "combined";

	/**
	 * array of metrics
	 */
	public static final String[] ROUTE_COST_METRICS =new String[] {ROUTE_COST_METRIC_DISTANCE,ROUTE_COST_METRIC_TIME,ROUTE_COST_METRIC_POLLUTION, ROUTE_COST_METRIC_COMBINED};
	
	private String costMetric;
	private int nodeFrom, nodeTo;
	private ArrayList<CpRoute> routes;
	private PGgeometry fromGeom;
	private PGgeometry toGeom;
	private long requestId = -1;//has to be initialised

	/**
	 * @param nodeFrom
	 *            Node in the graph where the route originates from
	 * @param fromGeom
	 *            Geometry where the route originates from
	 * @param nodeTo
	 *            Node in the graph where the route ends
	 * @param toGeom
	 *            Geometry where the route ends
	 * @param costMetric
	 *            Cost metric that is used. one of:
	 *            CpRouteRequest.ROUTE_COST_METRIC_DISTANCE
	 *            CpRouteRequest.ROUTE_COST_METRIC_TIME
	 *            CpRouteRequest.ROUTE_COST_METRIC_POLLUTION
	 *            CpRouteRequest.ROUTE_COST_METRIC_COMBINED
	 */
	public CpRouteRequest(int nodeFrom, PGgeometry fromGeom, int nodeTo,
			PGgeometry toGeom, String costMetric) {
		super();
		this.costMetric = costMetric;
		this.nodeFrom = nodeFrom;
		this.nodeTo = nodeTo;
		this.fromGeom = fromGeom;
		this.toGeom = toGeom;

		routes = new ArrayList<CpRoute>();
	}

	/**
	 * @return Number of routes, created by this request
	 */
	public int getNumberOfRoutes() {
		return routes.size();
	}

	/**
	 * @return Array List of routes
	 */
	public ArrayList<CpRoute> getRoutes() {
		return routes;
	}

	/**
	 * Return specific route of this request
	 * @param index index of the route
	 * @return Route
	 */
	public CpRoute getRoute(int index) {
		return routes.get(index);
	}

	
	/**
	 * Get the "worst" route with the highest calculated cost
	 * @return Route
	 */
	public CpRoute getLastRoute() {
		return routes.get(routes.size());
	}
	
	/**
	 * Get the "best" route with the lowest calculated cost
	 * @return Route
	 */
	public CpRoute getFirstRoute() {
		return routes.get(0);
	}

	
	/**
	 * @param route that is added to the request
	 */
	public void addRoute(CpRoute route) {
		this.routes.add(route);
	}

	/**
	 * @return used cost metric
	 */
	public String getCostMetric() {
		return costMetric;
	}

	/**
	 * @return Originating node
	 */
	public int getNodeFrom() {
		return nodeFrom;
	}

	/**
	 * @return node where the route ends
	 */
	public int getNodeTo() {
		return nodeTo;
	}

	/**
	 * @return originating Geometry which was used for the search (on it's basis we found the nearest node)
	 */
	public PGgeometry getFromGeom() {
		return fromGeom;
	}

	/**
	 * @return target Geometry which was used for the search (on it's basis we found the nearest node)
	 */
	public PGgeometry getToGeom() {
		return toGeom;
	}

	/**
	 * @return Id of this request
	 */
	public long getRequestId() {
		return requestId;
	}

	/**
	 * @param requestId id of this request
	 */
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}
	
	
	/**
	 * @return Formula that was used to calculate the cost of this request
	 * @throws SQLException If the metric is unknown
	 */
	public String getCostMetricFormula() throws SQLException{
		return getCostMetricFormula(costMetric);
	}
	

	/**
	 * @param costMetric requested metric
	 * @return Type that was used to calculate the cost of this request
	 * @throws SQLException If the metric is unknown
	 */
	public static String getCostMetricFormula(String costMetric) throws SQLException{
		switch (costMetric) {
			case CpRouteRequest.ROUTE_COST_METRIC_DISTANCE:
				return "cityCostMulti * distanceMulti * length";
			case CpRouteRequest.ROUTE_COST_METRIC_TIME:
				return "cityCostMulti * timeMulti * maxspeed_forward * length";
			case CpRouteRequest.ROUTE_COST_METRIC_POLLUTION:
				return "cityCostMulti * pollutionMulti * length";
			case CpRouteRequest.ROUTE_COST_METRIC_COMBINED:
				return "cityCostMulti * distanceMulti * timeMulti * pollutionMulti * maxspeed_forward * length";
			default: 
			throw new SQLException("Unknown cost metric! use public metrics of CpRouteRequest! ");
		}
	}
	
	@Override
	public String toString() {
		String str= "CpRouteRequest [requestId = "+requestId+ ", selectedMetric=" + costMetric
				+ ", nodeFrom=" + nodeFrom + ", fromGeom=" + fromGeom
				+ ", nodeTo=" + nodeTo + ", toGeom=" + toGeom + ", routes= ";
		for (Iterator<CpRoute> iterator = routes.iterator(); iterator.hasNext();) {
			CpRoute cpRoute = (CpRoute) iterator.next();
			str += "\n Length:"+cpRoute.getLengthM() +" Cost: "+cpRoute.getCost() + cpRoute.toString() + " ";
		}
		str += "]";

		return str;
	}

}
