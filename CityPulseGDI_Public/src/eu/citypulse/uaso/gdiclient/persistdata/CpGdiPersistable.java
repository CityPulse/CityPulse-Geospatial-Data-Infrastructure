package eu.citypulse.uaso.gdiclient.persistdata;

import java.util.UUID;

import org.postgresql.geometric.PGpoint;

/**
 * @author danielk
 * Abstract class the unifies Persisted objects in the GDI
 */
public abstract class CpGdiPersistable {
	
	/**
	 * When CpGdiPersistable is of Type Event
	 */
	public static final int GDI_PERSIST_TYPE_EVENT = 1;
	/**
	 * When CpGdiPersistable is of Type EventStream
	 */
	public static final int GDI_PERSIST_TYPE_EVENT_STREAM = 2;
	/**
	 * When CpGdiPersistable is of Type SensorStream
	 */
	public static final int GDI_PERSIST_TYPE_SENSOR_STREAM = 3;

	/**
	 * 
	 * @return UUID of persisted object 
	 */
	public abstract UUID getUuid();
	
	/**
	 * @return Topic or Service Category, depending on type of Object
	 */
	public abstract String getPropertyDescriptionOrTopic();
	
	/**
	 * @return one of GDI_PERSIST_TYPE_EVENT, GDI_PERSIST_TYPE_EVENT_STREAM, GDI_PERSIST_TYPE_SENSOR_STREAM
	 */
	public abstract int getGdiPersistType();

	public abstract PGpoint getCenttralPoint();
	
	@Override
	public String toString() {
		String type;
		switch (getGdiPersistType()) {
		case 1:
			type = "CpGdiEvent";
			break;
		case 2:
			type = "CpGdiEventStream";
			break;
		case 3:
			type = "CpGdiSensorStream";
			break;
		default:
			type = "UnknownType";
			break;
		}

		return type + " [getUuid()=" + getUuid() + ", getPropertyDescriptionOrTopic()=" + getPropertyDescriptionOrTopic() + "]";
	}

}
