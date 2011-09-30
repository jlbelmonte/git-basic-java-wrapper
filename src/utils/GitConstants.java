package utils;


public class GitConstants {
//	public final static String TEMPLATE = "[BEGIN][CHUNK]rev:{rev}[CHUNK]node:{node}[CHUNK]author:{author}[CHUNK]date:{date|isodate}[CHUNK]message:{desc}[CHUNK]added:{file_adds}[CHUNK]removed:{file_dels}[CHUNK]files:{files}[CHUNK]EOC\\n";
	public final static String TEMPLATE = "[CINIT]%n[BEGIN][CHUNK]rev:%H[CHUNK]tree:%T[CHUNK]author:%aN[CHUNK]email:%aE[CHUNK]message:%s[CHUNK]date:%ct[CHUNK]EOC";
	public final static String LOG_PATTERN ="\\[BEGIN\\]\\[CHUNK\\]rev:(.*?)\\[CHUNK\\]tree:(.*?)\\[CHUNK\\]author:(.*?)\\[CHUNK\\]email:(.*?)\\[CHUNK\\]message:(.*?)\\[CHUNK\\]date:(.*?)\\[CHUNK\\](.*?)EOC";
	public final static String LOG = "log";
	public final static String CLONE = "clone";
	public static final String PULL = "pull";
}
