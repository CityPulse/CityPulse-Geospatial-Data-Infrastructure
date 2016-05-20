package eu.citypulse.uaso.gdiclient.routes;

import java.util.Arrays;

import org.postgis.PGgeometry;

/**
 * @author Daniel Kuemper
 * Output class of a single route
 */
public class CpRoute {
	private PGgeometry geom;
	private int lengthM;
	private int timeS;
	private double cost;
	private Long[] edges;
	private Double[] cityCostMulti;
	private long routeId = -1;
	private Double[] edgeLengths;
	
	public Double[] getEdgeLengths() {
		return edgeLengths;
	}

	public CpRoute(PGgeometry geom, int lengthM, int timeS, double cost,
			Long[] edges, Double[] cityCostMulti, Double[] edgeLengths) {
		this(geom, lengthM, timeS, cost, edges, cityCostMulti);
		this.edgeLengths=edgeLengths;
	}

	/**Constructor 
	 * @param geom Geometry of the route as a linesting
	 * @param lengthM length of the route in meters
	 * @param timeS relative time (at max speed, should be taken e.g.  *0.5)
	 * @param cost relative cost
	 * @param edges array of edges of the route
	 * @param cityCostMulti multiplicators of the edges
	 */
	public CpRoute(PGgeometry geom, int lengthM, int timeS, double cost,
			Long[] edges, Double[] cityCostMulti) {
		super();
		this.geom = geom;
		this.lengthM = lengthM;
		this.timeS = timeS;
		this.cost = cost;
		this.edges = edges;
		this.cityCostMulti = cityCostMulti;
	}
	
	/**
	 * @return Geometry of the single route
	 */
	public PGgeometry getGeom() {
		return geom;
	}
	/**
	 * @return length of the route 
	 */
	public int getLengthM() {
		return lengthM;
	}
	/**
	 * @return relative time (at max speed, should be taken e.g.  *0.5)
	 */
	public int getTimeS() {
		return timeS;
	}
	/**
	 * @return relative cost
	 */
	public double getCost() {
		return cost;
	}
	/**
	 * @return Edges of the route
	 */
	public Long[] getEdges() {
		return edges;
	}
	/**
	 * @return cost multiplicators of the edges
	 */
	public Double[] getCityCostMulti() {
		return cityCostMulti;
	}

	
	/**
	 * @return Id of the route
	 */
	public long getRouteId() {
		return routeId;
	}

	/**
	 * @param routeId id of the route
	 */
	public void setRouteId(long routeId) {
		this.routeId = routeId;
	}
	
	@Override
	public String toString() {
		return "CpRoute [routeId = "+routeId+ " geom=" + geom + ", lengthM=" + lengthM + ", timeS="
				+ timeS + ", cost=" + cost + ", edges="
				+ Arrays.toString(edges) + ", cityCostMulti="
				+ Arrays.toString(cityCostMulti) + "]";
	}


	
	
}
