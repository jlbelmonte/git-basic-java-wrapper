package utils;


public class GitConstants {
	public final static String TEMPLATE = "[BEGIN][CHUNK]rev:{rev}[CHUNK]node:{node}[CHUNK]author:{author}[CHUNK]date:{date|isodate}[CHUNK]message:{desc}[CHUNK]added:{file_adds}[CHUNK]removed:{file_dels}[CHUNK]files:{files}[CHUNK]EOC\\n";
	public final static String LOG_PATTERN ="\\[BEGIN\\]\\[CHUNK\\]rev:(\\w+)\\[CHUNK\\]node:(\\w+)\\[CHUNK\\]author:(.*?)\\[CHUNK\\]date:(.*?)\\[CHUNK\\]message:(.*?)\\[CHUNK\\]added:(.*?)\\[CHUNK\\]removed:(.*?)\\[CHUNK\\]files:(.*?)\\[CHUNK\\]EOC";
	public final static String LOG = "log";
	public final static String INCOMING = "incoming";
	public final static String CLONE = "clone";
	public static final String PULL = "pull";
}
