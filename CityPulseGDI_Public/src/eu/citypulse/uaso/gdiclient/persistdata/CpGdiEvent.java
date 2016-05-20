package eu.citypulse.uaso.gdiclient.persistdata;

import java.util.UUID;

import org.postgresql.geometric.PGpoint;

/**
 * @author Daniel Kuemper
 * Output class for found events in the gdi
 */
public class CpGdiEvent extends CpGdiPersistable {
	
	
	/**
	 * @param uuid uuid of the Event
	 * @param topic topic of the event
	 * @param pgp PGPoint Geometry
	 */
	public CpGdiEvent(UUID uuid, String topic, PGpoint pgp) {
		super();
		this.uuid = uuid;
		this.topic = topic;
		this.pgp = pgp;
	}

	private UUID uuid;
	private String topic;
	private PGpoint pgp;

	@Override
	public PGpoint getCenttralPoint() {
		return pgp;
	}
	
	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public String getPropertyDescriptionOrTopic() {
		return topic;
	}

	@Override
	public int getGdiPersistType() {
		return CpGdiPersistable.GDI_PERSIST_TYPE_EVENT;
	}

	
}
