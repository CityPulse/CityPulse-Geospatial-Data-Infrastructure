package eu.citypulse.uaso.gdiclient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import eu.citypulse.uaso.gdiclient.objects.CpAmenity;
import eu.citypulse.uaso.gdiclient.persistdata.CpGdiPersistable;
import eu.citypulse.uaso.gdiclient.routes.CpRouteRequest;

/**
 * Test and Example calass for Geospatial Data Infrastructure(GDI) client of CityPulse 
 * @author Daniel Kuemper, University of Applied Sciences Osnabrueck (d.kuemper@hs-osnabrueck.de)
 * @version 0.1 draft
 *
 */
public class CpClientExampleStockholm {

	/**
	 * Simple Class that shows functionality of the Requests
	 * @param args are not used
	 */
	public static void main(String[] args) { 

		CpGdiInterface cgi;
		try {
			cgi = new CpGdiInterface(GdiConfig.GDI_HOST,GdiConfig.GDI_PORT,GdiConfig.GDI_DBNAME,GdiConfig.GDI_USERNAME, GdiConfig.GDI_PASSWORD);
			System.out.println("Start");
			cgi.simpleQuery();//test
					
			
			System.out.println("Cost Multiplicator");
			cgi.updateCostMultiplicatorRadial(18.03, 59.31, 500.0, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 1.0);

			System.out.println("Looking for Route");
			CpRouteRequest cprr1 = cgi.getCityRoutes(18.003, 59.2732, 18.0525, 59.3687, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 1);
			System.out.println("cprr1:\n"+cprr1);
			
			System.out.println("Polluting Area");
			int count = cgi.updateCostMultiplicatorRadial(18.03, 59.31, 500.0, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 50.0);
			System.out.println("polluted "+ count +"edges");
			
			System.out.println("Looking for Route");
			CpRouteRequest cprr2 = cgi.getCityRoutes(18.003, 59.2732, 18.0525, 59.3687, CpRouteRequest.ROUTE_COST_METRIC_POLLUTION, 1);
			System.out.println("cprr2:\n"+cprr2);
			
			
			//Amenity search currently limited for stockholm to "speedup" looking for own PoIs i a dedicated table would be very quick
			System.out.println("Looking for Parking...");
			ArrayList<CpAmenity> ams=  
					cgi.getnNextLocationsByAmenity(18.0013, 59.2732, "parking", 3);
			System.out.println("Found "+ams.size());
			for (Iterator<CpAmenity> iterator = ams.iterator(); iterator.hasNext();) {
				CpAmenity cpAmenity = (CpAmenity) iterator.next();
				System.out.println(cpAmenity);
			}
			
			System.out.println("Looking for Parking...");
			long currentTime = System.currentTimeMillis();
			ArrayList<CpAmenity> amsHospital=  
					cgi.getnNextLocationsByAmenity(18.0013, 59.2732, "hospital", 3);
			System.out.println("Parking Select and route took "+(currentTime-System.currentTimeMillis())+"ms");
			System.out.println("Found "+amsHospital.size());
			for (Iterator<CpAmenity> iterator = amsHospital.iterator(); iterator.hasNext();) {
				CpAmenity cpAmenity = (CpAmenity) iterator.next();
				System.out.println(cpAmenity);
			}
			
			
			System.out.println("Inserted Sensor Stream 1? "+cgi.registerStream(UUID.randomUUID(), "OnPollutedRoute", "Temperature", 18.0315, 59.3202));
			System.out.println("Inserted  Sensor Stream 2? "+cgi.registerStream(UUID.randomUUID(), "OnUnpollutedRoute", "Temperature", 18.0693, 59.3256));
			
			cprr1.getFirstRoute().getGeom();
			System.out.println("Event_Streamsnon Route:");
			CpGdiPersistable[] streams =cgi.getEventStreamsForRoute(cprr1.getFirstRoute().getGeom(), 200); // in a 100 meter buffer  
			System.out.println(streams.length);
			
			streams =cgi.getEventStreamsForRoute(cprr2.getFirstRoute().getGeom(), 200); // in a 100 meter buffer  
			System.out.println(streams.length);


			
			
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
