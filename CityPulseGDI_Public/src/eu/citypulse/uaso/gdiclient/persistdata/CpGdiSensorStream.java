package eu.citypulse.uaso.gdiclient.persistdata;

import java.util.UUID;

import org.postgresql.geometric.PGpoint;


/**
 * @author Daniel Kuemper
 * Output class for found sensor streams in the GDI
 */
public class CpGdiSensorStream extends CpGdiPersistable {
	
	/**
	 * @param uuid uuid of sensor stream
	 * @param serviceCategory service category
	 * @param pgp PGPoint geometry
	 */
	public CpGdiSensorStream(UUID uuid, String serviceCategory, PGpoint pgp) {
		super();
		this.uuid = uuid;
		this.serviceCategory = serviceCategory;
		this.pgp = pgp;
	}

	private UUID uuid;
	private String serviceCategory;
	private PGpoint pgp;
	
	public PGpoint getCenttralPoint() {
		return pgp;
	}
	
	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public String getPropertyDescriptionOrTopic() {
		return serviceCategory;
	}

	@Override
	public int getGdiPersistType() {
		return CpGdiPersistable.GDI_PERSIST_TYPE_SENSOR_STREAM;
	}

	
}
