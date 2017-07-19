package bot;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;


public class VersionInfo {
	private String hash;
	private String msg;
	private String branch;

	// Constructor
	VersionInfo(){
		loadVersionInfo();
	}


	// Read data from the files in the ./data directory.
	private void loadVersionInfo(){
		try{
			Path path = Paths.get("./data/branch_name.txt");
			BufferedReader br = Files.newBufferedReader(path);
			this.branch = br.readLine();
			br.close();
		}
		catch(IOException x){
			this.branch = "Unknown";
		}
		try{
			Path path = Paths.get("./data/" + branch + "_version.txt");
			BufferedReader br = Files.newBufferedReader(path);
			this.hash = br.readLine().substring(7, 14);
			this.msg = br.readLine();
			br.close();
		}
		catch(IOException x){
			x.printStackTrace();
			this.hash = "unknown";
			this.msg = "unknown";
		}
	}



	public boolean hasCommitInfo(){
		return !this.hash.equals("unknown");
	}


	public String getCommitHash(){
		return this.hash;
	}



	public String getCommitMessage(){
		return this.msg;
	}



	public String getBranch(){
		return this.branch;
	}
}
