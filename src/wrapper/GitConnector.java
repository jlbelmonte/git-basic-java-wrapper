package wrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

import siena.Json;
import utils.GitConstants;
import utils.GitUtilities;
import exceptions.RepositoryNotFoundException;


public class GitConnector {
	public static Logger logger = Logger.getLogger(GitConnector.class);	
	private String command = "/usr/bin/git";
	private String uri;
	private String path;
	private String revFrom = "";
	private String revTo = "";

	public GitConnector(String uri, String path, String command){
		this.command = command;
		this.uri = uri;
		this.path = path;
	}

	public GitConnector(String uri, String path){
		this.uri = uri;
		this.path = path;
	}
	
	// main call to system
	private Json callGit(String action) throws RepositoryNotFoundException{
			File dir = new File(path);

		CommandLine cl = new CommandLine(command);
		logger.debug("GitConnector msg: Starting to "+action+" over "+this.uri);
		if (GitConstants.LOG.equals(action)){
			if (!dir.exists() || dir.list().length == 0){
				throw new RepositoryNotFoundException(path);
			}
			cl.addArgument(action);
			if (!this.revFrom.isEmpty() && !this.revTo.isEmpty()) {
				cl.addArgument(revFrom+".."+revTo);
			} else if (!this.revFrom.isEmpty()) {
				cl.addArgument(revFrom+"..");
			}
			cl.addArgument("--format="+GitConstants.TEMPLATE);
			cl.addArgument("--raw");
			cl.addArgument("--name-status");
			cl.addArgument("--no-merges");
			cl.addArgument("--diff-filter=[AMD]");
			cl.addArgument("--date-order");
		} else if (GitConstants.CLONE.equals(action)) {
			if (!dir.exists()){ 
				logger.debug(dir.getAbsolutePath());
				logger.debug("do not exists");
				dir.mkdir();
			}
			cl.addArgument(GitConstants.CLONE);
			cl.addArgument(uri);
			cl.addArgument(path);
		} else if (GitConstants.PULL.equals(action)){
			if(!dir.exists() || dir.list().length == 0 ){
				return callGit(GitConstants.CLONE);
			}else if(dir.list().length ==1 && dir.list()[0].equals(".git")){
				//some times the clone corrupts so we just get a .hg dir but not working local repo
				//delete it and try a clean clone
				logger.info("deleting"+ dir.getAbsolutePath());
				GitUtilities.deleteDirectory(dir);
				return callGit(GitConstants.CLONE);
			}
			cl.addArgument(GitConstants.PULL);
			cl.addArgument("-u");
		}
		Json result = Json.map();
		
		File file = null;
		FileOutputStream fOS = null;
		FileReader fr = null;
		BufferedReader br = null;
		PipedOutputStream pipeOut = null;
		PipedInputStream pipeIn = null;
		
		try{
			file = File.createTempFile("Git-", ".log");
			fOS = new FileOutputStream(file);
			PumpStreamHandler streamHandler = new PumpStreamHandler();
			streamHandler = new PumpStreamHandler(fOS);
			Executor executor = new DefaultExecutor();
			executor.setStreamHandler(streamHandler);
			executor.setWorkingDirectory(dir);
			logger.debug(cl.toString());
			
			// redirecting System.err and prepare a pipeIn to read it
			pipeOut = new PipedOutputStream();
			pipeIn = new PipedInputStream(pipeOut);
			System.setErr (new PrintStream(pipeOut));
			
			/* 	DefaultExecutor only accepts one value as success value
				by default, we need to accept more and discriminate the 
				result afterwards. So Don't Panic
			*/
			int [] dontPanicValues = new int[256]; 
			for (int i = 0; i <= 255; i++) {
				dontPanicValues[i]=i;
			}
			executor.setExitValues(dontPanicValues);
			//execute command
			int statusCode = executor.execute(cl);
			logger.debug("GitConnector msg: Executed "+action+" over "+this.uri+ " exitStatus "+statusCode);

			fr = new FileReader(file);
			
			br = new BufferedReader(fr);
			
			String stdErr = GitUtilities.piped2String(pipeIn);
			
			result = GitUtilities.parseData(br, stdErr, statusCode, action);
			logger.debug("GitConnector msg: result "+ result);
			return result;
		} catch (IOException e) {
			logger.error("Git Exception: "+ uri, e);
			return Json.map().put("status", "NOK").put("action", action).put("error", e.getMessage());
		} finally {
			try {fOS.close();} catch (Exception e) {}
			try {fr.close();} catch (Exception e) {}
			try {br.close();} catch (Exception e) {}
			try {pipeOut.close();} catch (Exception e) {}
			try {pipeIn.close();} catch (Exception e) {}
			logger.debug("File size " + file.length());
			file.delete();
		}
		
	}
	
	// public methods
	//clones

	public Json cloneRepo() throws RepositoryNotFoundException{
		return callGit("clone");
	}
	
	// logs
	public Json log() throws RepositoryNotFoundException{
		return callGit("log");
	}
	public Json log(String revFrom, String revTo) throws RepositoryNotFoundException{
		this.revFrom = revFrom;
		this.revTo = revTo;
		return  callGit("log");		
	}
	
	public Json log(String revFrom) throws RepositoryNotFoundException{
		this.revFrom = revFrom;
		return callGit("log");
	}
	
	//update
	public Json pull() throws RepositoryNotFoundException{
		return callGit("pull");
	}
}

