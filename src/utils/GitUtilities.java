package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import siena.Json;

public class GitUtilities {
	public static Logger logger = Logger.getLogger(GitUtilities.class);	

	public static String getString(BufferedReader bR) {
		String s = null;
		String out = "";
		try {
			while ((s = bR.readLine()) != null) {
				out += s;
			}
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String piped2String(PipedInputStream pis){
		final PipedOutputStream pos = new PipedOutputStream();
		final BufferedInputStream bis = new BufferedInputStream(pis);
		final BufferedOutputStream bos = new BufferedOutputStream(pos);
		final StringBuffer sb = new StringBuffer();
		int p = 0;
		byte[] b = new byte[1024];
		try {
			while (!(bis.available()<1)){
				p = bis.read(b, 0, 1024);
				bos.write(b, 0 , p);
				sb.append(new String(b, 0 , p));
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			ioe.getMessage();
		}
		String content = sb.toString();
		return content;
	}
	
	public static Json parseData(BufferedReader stdOutput,  String stdErr, int statusCode, String action){
		// status code NOK
		if (statusCode != 0)
			return Json.map().put("status", "NOK").put("action", action).put("error", stdErr);

		// Log or incoming need parsing
		if (GitConstants.LOG.equals(action)){
			Pattern commitPattern = Pattern.compile(GitConstants.LOG_PATTERN);
			String s = null;
			Json logList = Json.list();
			String tmp="";
			try{
				//Deal with multiline comments and commitlogs
				while( ( s = stdOutput.readLine()) != null){
					s = s.trim();
					if(s.startsWith("[BEGIN]") && !s.endsWith("EOC")){
						tmp = s.replace("\n", " ");
						continue;
					}else if(!s.startsWith("[BEGIN]") && !s.endsWith("EOC") && tmp.startsWith("[BEGIN]")){
						tmp += s.replace("\n", " ");
						continue;
					}else if (!s.startsWith("[BEGIN]") && s.endsWith("EOC") && tmp.startsWith("[BEGIN]")){
						tmp += s.replace("\n", " ");
						s = tmp;
					}
					tmp="";
					Matcher logM = commitPattern.matcher(s);
					if(logM.find()){
						Json rev = processRevisionLog(logM);
						logList.add(rev);
					}
				}
			} catch (Exception e) {
				return(Json.map().put("status", "NOK").put("action", action).put("error", "exception").put("message", e.getMessage()));
			}
			return Json.map().put("status", "OK").put("commits", logList);
		}
		//everything else is OK
		return Json.map().put("status", "OK" );
	}

	private static Json processRevisionLog(Matcher rev) {
		String revision = rev.group(1);
		String node = rev.group(2);
		String author = rev.group(3);
		String date = rev.group(4);
		String message = rev.group(5);	
		String added = rev.group(6);
		String removed = rev.group(7);
		String files = rev.group(8);
		Json revLog = Json.map()
		.put("revision", revision)
		.put("node", node)
		.put("author", author)
		.put("date", date)
		.put("message", message);

		List <String >addedList = Arrays.asList(added.split("\\s+"));
		List <String >removedList = Arrays.asList(removed.split("\\s+"));
		List <String> modifiedList = new ArrayList<String>();
		List <String >fileList = Arrays.asList(files.split("\\s+"));
		for (String string : fileList) {
			if(!addedList.contains(string) || !removedList.contains(string)){
				modifiedList.add(string);
			}
		}
		revLog.put("added", addedList)
		.put("removed", removedList)
		.put("modified", modifiedList);
		return revLog;
	}
	
		static public boolean deleteDirectory(File path) {
			if( path.exists() ) {
				File[] files = path.listFiles();
				for(int i=0; i<files.length; i++) {
					if(files[i].isDirectory()) {
						deleteDirectory(files[i]);
					}
					else {
						files[i].delete();
					}
				}
			}
			return( path.delete() );
		}
}
