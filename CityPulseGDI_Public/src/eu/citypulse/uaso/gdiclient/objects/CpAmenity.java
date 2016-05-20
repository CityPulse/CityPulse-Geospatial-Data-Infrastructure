package eu.citypulse.uaso.gdiclient.objects;

import org.postgis.PGgeometry;

import eu.citypulse.uaso.gdiclient.routes.CpRoute;

/**
 * Defining OSM Object
 * @author danielk
 *
 */
public class CpAmenity {
	private long osmId;
	private String name;
	private PGgeometry geom;
	private PGgeometry origin;
	private double directDistance;	
	private CpRoute route;

	public CpAmenity(long osmId, String name, PGgeometry geom, PGgeometry origin, double directDistance) {
		super();
		this.osmId = osmId;
		this.name = name;
		this.geom = geom;
		this.origin = origin;
		this.directDistance = directDistance;
	}
	public long getOsmId() {
		return osmId;
	}
	public String getName() {
		return name;
	}
	public PGgeometry getGeom() {
		return geom;
	}
	public double getDirectDistance() {
		return directDistance;
	}
	public CpRoute getRoute() {
		return route;
	}
	public void setRoute(CpRoute route) {
		this.route = route;
	}
	public PGgeometry getOrigin() {
		return origin;
	}
	@Override
	public String toString() {
		return "CpAmenity [osmId=" + osmId + ", name=" + name +
				//", geom=" + geom + ", origin=" + origin + 
				", directDistance=" + directDistance + ", routeLength="
				+ route.getLengthM() + "]";
	}
	
	
	
	
}
