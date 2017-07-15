package bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;

public class LookupCommands 
implements CommandExecutor{

	private String data;
	public static final String HOST_RAW_URL = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master";
	public static final String CONTENT_URL = "https://github.com/endless-sky/endless-sky/raw/master";

	public LookupCommands() {
		data = readData();
	}

	public String readData(){
		String data = "";
		try {
			LinkedList<URL> dataFiles = new LinkedList<>();
			try(BufferedReader br = new BufferedReader(new InputStreamReader(new URL(HOST_RAW_URL + "/data/dataFileNames.txt").openStream()))) {
				String line = br.readLine();

				while (line != null) {
					dataFiles.add(new URL("https://raw.githubusercontent.com/endless-sky/endless-sky/master/data/" + line + ".txt"));
					line = br.readLine();
				}
			}			
			for(URL url : dataFiles){
				try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();

					while (line != null) {
						sb.append(line);
						sb.append(System.lineSeparator());
						line = br.readLine();
					}
					data += sb.toString();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	@Command(aliases = {"-issue"}, description = "Provide link for \"X\" Endless Sky issue.", usage = "-issue X", privateMessages = true)
	public void onIssueCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/issues/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-pull"}, description = "Provide link for \"X\" Endless Sky pull request.", usage = "-pull X", privateMessages = true)
	public void onPullCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/pull/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-commit"}, description = "Provide link for \"X\" Endless Sky commit.", usage = "-commit X", privateMessages = true)
	public void onCommitCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/commit/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-lookup"}, description = "Shows image and description of X.", usage = "-lookup X", privateMessages = true)
	public void onLookupCommand(MessageChannel channel, String[] args){
		if(args.length >= 1){
			String request = args[0];
			for(int i = 1; i < args.length; i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				int start = 0, end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start) - 1;
				}
				else if(output.contains("sprite")){
					start = output.indexOf("sprite")+7;
					end = output.indexOf('\n', start) - 1;
				}
				String filepath = CONTENT_URL + "/images/" + output.substring(start, end).replace("\"", "");
				String ending = GetImageEnding(filepath);
				if(ending.length() > 0){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue(x -> {
						if(output.contains("description"))
							OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
					});
				}
				else{
					// Expected image but could not find one.
					if(output.contains("description"))
						OutputHelper(channel, output.substring(output.indexOf("description")));
					else
						OutputHelper(channel, "Did not find image or description associated with input '" + request + "'");
				}
			}
			else{
				if(output.contains("description"))
					OutputHelper(channel, output.substring(output.indexOf("description")));
			}
		}

	}

	@Command(aliases = {"-show"}, description = "Shows image and data of X.", usage = "-show X", privateMessages = true)
	public void onShowCommand(MessageChannel channel, String[] args){
		if(args.length >= 1){
			String returnMessage = "";
			String request = args[0];
			for(int i = 1; i < args.length; i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				int start = 0, end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start) - 1;
				}
				else if(output.contains("sprite")){
					start = output.indexOf("sprite") + 7;
					end = output.indexOf('\n', start) - 1;
				}
				String filepath = CONTENT_URL + "/images/" + output.substring(start, end).replace("\"", "");
				String ending = GetImageEnding(filepath);
				if(ending.length() > 0){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue(x -> {
						OutputHelper(channel, output);
					});
				}
				else{
					returnMessage = "Expected image, but could not find image, with input '" + request + "'";
					OutputHelper(channel, returnMessage + "\n\n" + output);
				}
			}
			else{
				OutputHelper(channel, output);
			}
		}
	}

	@Command(aliases = {"-showimage", "-showImage"}, description = "Shows image of X.", usage = "-showimage X", privateMessages = true)
	public void onShowimageCommand(MessageChannel channel, String[] args){
		String returnMessage = "";
		if(args.length >= 1){
			String request = args[0];
			for(int i = 1; i < args.length; i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				int start = 0, end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start) - 1;
				}
				else if(output.contains("sprite")){
					start = output.indexOf("sprite") + 7;
					end = output.indexOf('\n', start) - 1;
				}
				String filepath = CONTENT_URL + "/images/" + output.substring(start, end).replace("\"", "");
				String ending = GetImageEnding(filepath);
				if(ending.length() > 1){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue();
				}
				else{
					// Could not resolve image ending from the detected output.
					returnMessage = "Could not find image for '" + output.substring(start, end).replace("\"", "") + "' from input '" + request + "'";
				}
			}
			else{
				// No image in lookup.
				returnMessage = "Did not find an image for the input '" + request + "'";
			}
		}
		if(returnMessage.length() > 0){
			OutputHelper(channel, returnMessage);
		}
	}

	@Command(aliases = {"-showdata", "-showData"}, description = "Shows data of X.", usage = "-showdata X", privateMessages = true)
	public void onShowdataCommand(MessageChannel channel, String[] args){
		if(args.length >= 1){
			String request = args[0];
			for(int i = 1; i < args.length; i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.length() < 1){
				output = "Nothing found!";
			}
			OutputHelper(channel, output);
		}
	}
	
	public String lookupData(String lookup){
		lookup = checkLookup(lookup, true);
		if(lookup.length() > 0){
			int start = data.indexOf(lookup);
			int end = start + lookup.length();
			do{
				end = data.indexOf('\n', end + 1);
			}
			while(data.charAt(end + 1) == '\t' || data.charAt(end + 1) == '\n' || data.charAt(end + 1) == '#');
			return data.substring(start, end);
		}
		else
			return "Nothing found!";
	}
	
	public String checkLookup(String lookup, boolean helper){
		if(data.contains("\nship \"" + lookup + "\"")){
			return "\nship \"" + lookup + "\"";
		}
		else if(data.contains("\noutfit \"" + lookup + "\"")){
			return "\noutfit \"" + lookup + "\"";
		}
		else if(data.contains("\nmission \"" + lookup + "\"")){
			return "\nmission \"" + lookup + "\"";
		}
		else if(data.contains("\nsystem \"" + lookup + "\"")){
			return "\nsystem \"" + lookup + "\"";
		}
		else if(data.contains("\neffect \"" + lookup + "\"")){
			return "\neffect \"" + lookup + "\"";
		}
		else if(data.contains("\nship " + lookup)){
			return"\nship " + lookup;
		}
		else if(data.contains("\noutfit " + lookup)){
			return "\noutfit " + lookup;
		}
		else if(data.contains("\nmission " + lookup)){
			return "\nmission " + lookup;
		}
		else if(data.contains("\nsystem " + lookup)){
			return "\nsystem " + lookup;
		}
		else if(data.contains("\neffect " + lookup)){
			return "\neffect " + lookup;
		}
		else if(data.contains("\n"+lookup)){
			return "\n"+lookup;
		}
		else if(helper){
			lookup = Character.toUpperCase(lookup.charAt(0)) + lookup.toLowerCase().substring(1);
			return checkLookup(lookup, false);
		}
		else
			return "";
	}

	public void OutputHelper(MessageChannel channel, String output){
		if(output.length() < 1993){
			channel.sendMessage(":\n```" + output + "```").queue();
		}
		else{
			int cut = output.lastIndexOf('\n', 0 + 1992);
			String o = output.substring(0, cut);			
			channel.sendMessage(":\n```" + o + "```").queue(x -> {
				OutputHelper(channel, output.substring(cut + 1));
			});
		}
	}

	public boolean isImage(String url){
		try{
			URL u = new URL(url);
			return ImageIO.read(u) != null;
		}
		catch (Exception e){
			return false;
		}
	}
	
	// Iterate the possible image blending modes to determine which is the
	// appropriate file ending for the given file. Assumes all image files
	// are .png. Returns nullstring "" if no ending works, otherwise returns
	// the full ending (including the filetype).
	public String GetImageEnding(String url){
		String[] endings = {"", "-0", "+0", "~0", "=0"};
		int index = 0;
		boolean hasEnding = isImage(url + endings[index] + ".png?raw=true");
		
		while(!hasEnding && index < endings.length){
			hasEnding = isImage(url + endings[++index] + ".png?raw=true");
		}
		if(hasEnding)
			return endings[index] + ".png?raw=true";
		else
			return "";
	} 

}
