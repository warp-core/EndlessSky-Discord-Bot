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
					data += sb.toString() + "\n~\n";
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
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				String imageName = GetImageName(output);
				String filepath = urlEncode(CONTENT_URL + "/images/" + imageName);
				String ending = GetImageEnding(filepath);
				if(ending.length() > 0){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue(x -> {
						if(output.contains("description"))
							OutputHelper(channel, output.substring(output.indexOf("description")).replaceFirst("description", ""));
						else
							OutputHelper(channel, "(no description)");
					});
				}
				else{
					// Expected image but could not find one.
					if(output.contains("description"))
						OutputHelper(channel, "I could not resolve the expected image for '" + imageName + "' from the input '" + request + "'. However, I did find this:\n\n" + output.substring(output.indexOf("description")).replaceFirst("description",""));
					else
						OutputHelper(channel, "There is no image or description associated with '" + request + "'.");
				}
			}
			else if(output.contains("description")){
					OutputHelper(channel, output.substring(output.indexOf("description")));
			}
			else if(output.length() > 0)
				OutputHelper(channel, "There is no image or description associated with '" + request + "'.");
			else
				OutputHelper(channel, "I could not find anything related to '" + request + "'.");
		}

	}

	@Command(aliases = {"-show"}, description = "Shows image and data of X.", usage = "-show X", privateMessages = true)
	public void onShowCommand(MessageChannel channel, String[] args){
		if(args.length >= 1){
			String returnMessage = "";
			String request = args[0];
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				String imageName = GetImageName(output);
				String filepath = urlEncode(CONTENT_URL + "/images/" + imageName);
				String ending = GetImageEnding(filepath);
				if(ending.length() > 0){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue(x -> {
						OutputHelper(channel, output);
					});
				}
				else{
					returnMessage = "I could not resolve the expected image for '" + imageName + "' from the input '" + request + "'. However, I did find this:";
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
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.contains("sprite") || output.contains("thumbnail")){
				String imageName = GetImageName(output);
				String filepath = urlEncode(CONTENT_URL + "/images/" + imageName);
				String ending = GetImageEnding(filepath);
				if(ending.length() > 0){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(filepath + ending);
					channel.sendMessage(eb.build()).queue();
				}
				else{
					// Could not resolve image ending from the detected output.
					returnMessage = "I could not resolve the expected image for '" + imageName + "' from the input '" + request + "'.";
				}
			}
			else{
				// No image in lookup.
				returnMessage = "There is no image associated with '" + request + "'.";
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
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.length() < 1){
				output = "I could not find any data associated with '" + request + "'.";
			}
			OutputHelper(channel, output);
		}
	}

	// Convert the requested lookup parameter into the relevant data
	// from the Endless Sky GitHub repository.
	// Returns nullstring if no data could be found.
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

		return "";
	}

	// Queries the loaded datafiles for special Endless Sky keywords.
	// If helper is 'true', will try both as-passed 'lookup', and with
	// enforced word capitalization.
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
			// Uppercase the first letter of words in the lookup string.
			lookup = CapitalizeWords(lookup);
			return checkLookup(lookup, false);
		}

		return "";
	}

	// Send the message 'output' to the desired channel, cutting into
	// multiple messages as needed.
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

	// Verify the passed URL resolves to an image file.
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

		while(!hasEnding && ++index < endings.length){
			hasEnding = isImage(url + endings[index] + ".png?raw=true");
		}
		if(hasEnding)
			return endings[index] + ".png?raw=true";

		return "";
	}

	public static String urlEncode(String url){
		return url.replace(" ", "%20");
	}

	// Check the string for a space character and if present, capitalize the
	// next letter. Returns the string with first letters of words capitalized.
	private static String CapitalizeWords(String input){
		int countWords = 1 + CountOf(input, ' ');
		char[] ic = input.toCharArray();
		ic[0] = Character.toUpperCase(ic[0]);
		if(countWords > 1){
			int index = input.indexOf(" ");
			for(int i = 0; i < countWords; ++i){
				++index;
				if(ic[index] == '(' || ic[index] == ')' || ic[index] == '"')
					++index;
				ic[index] = Character.toUpperCase(ic[index]);
				index = input.indexOf(" ", index);
			}
		}

		return new String(ic);
	}

	// Check for improperly-formatted ship variants, which require speccing as
	// "base model" "base model (variant)". If the request was not correctly
	// formatted, returns the request in the proper ship variant format.
	private String ParseVariants(String input){
		if((input.indexOf('(') > 0 || input.indexOf(')') > 0) && lookupData(input).length() < 1){
			String baseModel = GetBaseModelName(input);
			if(input.indexOf(baseModel) == input.lastIndexOf(baseModel)){
				input = "\"" + baseModel.replace("\"", "") + "\" \"" + input.replace("\"", "") + "\"";
			}
		}
		
		return input;
	}

	// Count the number of the given character in the given string.
	public static int CountOf(String input, char token){
		int count = 0;
		for(char c : input.toCharArray()){
			if(c == token)
				++count;
		}

		return count;
	}

	// Returns the bare image name without quotes, or a nullstring if no image.
	public static String GetImageName(String text){
		int start = 0;
		if(text.contains("thumbnail"))
			start = text.indexOf("thumbnail") + 10;
		else if(text.contains("sprite"))
			start = text.indexOf("sprite") + 7;
		else
			return "";

		int end = text.indexOf('\n', start);
		
		return text.substring(start, end).replace("\"", "");
	}

	// Called for ship variants in order to obtain the base model name. Returns
	// an unquoted ship, e.g. "Falcon (Plasma)" -> Falcon, or "Marauder Falcon
	// (Engines)" -> Marauder Falcon. Will not work for variants which do not use
	// the variant name convention of "Base Ship Name (varied text)"
	// Returns nullstring if no match.
	public static String GetBaseModelName(String text){
		int end = text.indexOf('(');
		if(end < 0)
			return "";

		return text.substring(0, end-1).replace("\"", "");
	}
}
