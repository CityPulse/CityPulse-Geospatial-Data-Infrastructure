package eu.citypulse.uaso.gdiclient.persistdata;

import java.util.UUID;

import org.postgresql.geometric.PGpoint;

/**
 * @author Daniel Kuemper
 * Output class for found event streams in the gdi
 */
public class CpGdiEventStream extends CpGdiPersistable {
	
	/**
	 * @param uuid uuid of event stream
	 * @param topic topic of the event stream
	 * @param pgp PGpoint Geometry
	 */
	public CpGdiEventStream(UUID uuid, String topic, PGpoint pgp ) {
		super();
		this.uuid = uuid;
		this.topic = topic;
		this.pgp = pgp;
	}

	private UUID uuid;
	private String topic;
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
		return topic;
	}

	@Override
	public int getGdiPersistType() {
		return CpGdiPersistable.GDI_PERSIST_TYPE_EVENT_STREAM;
	}

	
}
