package exceptions;

public class RepositoryNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	String msg;
	String repoPath;
	public RepositoryNotFoundException() {}

	public RepositoryNotFoundException(String repoId) {
		this.msg = "The directory with the project in not in the file system or is just an empty dir, clone first";
		this.repoPath = repoId;
	}

	public String getMessage(){
		return this.msg;
	}
	public String getPath() {
		return repoPath;
	}
}
