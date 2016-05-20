package eu.citypulse.uaso.gdiclient;

public class GdiConfig {
	public static String GDI_DBNAME = "cp_sweden"; 
			
	public static String GDI_HOST = "localhost";
	public static int GDI_PORT = 5432; //5432 is the standard port, but we use it with a tunnel 
	public static String GDI_USERNAME="user";
	public static String GDI_PASSWORD="password";
	public static boolean GDI_INSERT_DEBUG_DATA = false; //DB writeback used for research experiements
}

