package wrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.UUID;

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
	private String command = "/usr/bin/hg";
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
	private Json callHG (String action) throws RepositoryNotFoundException{
			File dir = new File(path);

		CommandLine cl = new CommandLine(command);
		logger.debug("HGConnector msg: Starting to "+action+" over "+this.uri);
		if (GitConstants.LOG.equals(action) || "incoming".equals(action)){
			if (!dir.exists() || dir.list().length == 0){
				throw new RepositoryNotFoundException(path);
			}
			cl.addArgument(action);
			cl.addArgument("--rev");
			cl.addArgument(revFrom+":"+revTo);
			cl.addArgument("--template");
			cl.addArgument(GitConstants.TEMPLATE);
		} else if ("cloneRev".equals(action)) {
			if (!dir.exists()) {
				dir.mkdir();
			}
			cl.addArgument(GitConstants.CLONE);
			cl.addArgument("--rev");
			cl.addArgument(revFrom);
			cl.addArgument(uri);
			cl.addArgument(path);
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
				return callHG(GitConstants.CLONE);
			}else if(dir.list().length ==1 && dir.list()[0].equals(".hg")){
				//some times the clone corrupts so we just get a .hg dir but not working local repo
				//delete it and try a clean clone
				logger.info("deleting"+ dir.getAbsolutePath());
				GitUtilities.deleteDirectory(dir);
				return callHG(GitConstants.CLONE);
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
			file = File.createTempFile("HG-", ".log");
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
			logger.debug("HGConnector msg: Executed "+action+" over "+this.uri+ " exitStatus "+statusCode);
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			String stdErr = GitUtilities.piped2String(pipeIn);
			result = GitUtilities.parseData(br, stdErr, statusCode, action);
			logger.debug("HGConnector msg: result "+ result);
			
		}
		catch (IOException e) {
			logger.error("HG Exception: "+ uri, e);
		} finally {
			try {fOS.close();} catch (Exception e) {}
			try {fr.close();} catch (Exception e) {}
			try {br.close();} catch (Exception e) {}
			try {pipeOut.close();} catch (Exception e) {}
			try {pipeIn.close();} catch (Exception e) {}
			logger.debug("File size " + file.length());
			file.delete();
		}
		return result;
	}
	
	// public methods
	//clones
	
	public Json cloneRev(String revision) throws RepositoryNotFoundException{
		this.revFrom = revision;
		return callHG("cloneRev");
	}

	public Json cloneRepo() throws RepositoryNotFoundException{
		return callHG("clone");
	}
	
	// logs
	public Json log() throws RepositoryNotFoundException{
		return callHG("log");
	}
	public Json log(String revFrom, String revTo) throws RepositoryNotFoundException{
		this.revFrom = revFrom;
		this.revTo = revTo;
		return  callHG("log");		
	}
	
	public Json log(String revFrom) throws RepositoryNotFoundException{
		this.revFrom = revFrom;
		this.revTo="tip";
		return callHG("log");
	}
	
	public Json hgRemoteLog() throws RepositoryNotFoundException{
		return callHG("incoming");
	}
	
	//update
	public Json pull() throws RepositoryNotFoundException{
		return callHG("pull");
	}
}

