package bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;

public class LookupCommands
implements CommandExecutor{

	private String data;
	public static final String HOST_RAW_URL = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master";
	public static final String CONTENT_URL = "https://github.com/endless-sky/endless-sky/raw/master";

	private ESBot bot;

	public LookupCommands(ESBot bot){
		this.bot = bot;
		data = readData();
	}



	private String readData(){
		String data = "";
		try{
			LinkedList<URL> dataFiles = new LinkedList<>();
			try(BufferedReader br = new BufferedReader(new InputStreamReader(new URL(HOST_RAW_URL + "/data/dataFileNames.txt").openStream()))){
				String line = br.readLine();

				while (line != null){
					dataFiles.add(new URL("https://raw.githubusercontent.com/endless-sky/endless-sky/master/data/" + line + ".txt"));
					line = br.readLine();
				}
			}
			for(URL url : dataFiles){
				try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))){
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();

					while(line != null){
						sb.append(line);
						sb.append(System.lineSeparator());
						line = br.readLine();
					}
					data += sb.toString() + "\n~\n";
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return data;
	}



	@Command(aliases = {"-issue"}, description = "Link to Endless Sky issue #X. If no issue number is given, links the issues page.", usage = "-issue X", privateMessages = true)
	public void onIssueCommand(MessageChannel channel, String[] args){
		StringBuilder path = new StringBuilder("https://github.com/endless-sky/endless-sky/issues");
		if(args.length > 1){
			path.append("/");
			for(String str : args)
				path.append(str);
		}
		else if(args.length > 0)
			path.append("/" + args[0]);

		channel.sendMessage(path.toString().replace(" ", "")).queue();
	}



	@Command(aliases = {"-pull"}, description = "Link to Endless Sky pull request (PR) #X. If no pull number is given, links the PR page.", usage = "-pull X", privateMessages = true)
	public void onPullCommand(MessageChannel channel, String[] args){
		StringBuilder request = new StringBuilder("/");
		if(args.length > 1)
			for(String str : args)
				request.append(str);
		else if(args.length > 0)
			request.append(args[0]);
		else
			request = new StringBuilder("s");

		String path = "https://github.com/endless-sky/endless-sky/pull" + request.toString().replace(" ", "");

		channel.sendMessage(path).queue();
	}



	@Command(aliases = {"-commit"}, description = "Link to Endless Sky commit hash \"X\". Only the first 7 letters are necessary.\nLeave blank for the most recent commit.", usage = "-commit X", privateMessages = true)
	public void onCommitCommand(MessageChannel channel, String[] args){
		StringBuilder request = new StringBuilder("");
		if(args.length > 1)
			for(String str : args)
				request.append(str);
		else if(args.length > 0)
			request.append(args[0]);

		if(request.length() > 6){
			String path = "https://github.com/endless-sky/endless-sky/commit/" + request.toString().replace(" ","");
			channel.sendMessage(path).queue();
		}
		else if(request.length() > 0){
			channel.sendMessage("At least 7 characters are required.").queue();
		}
		else
			channel.sendMessage("https://github.com/endless-sky/endless-sky/commit/").queue();
	}



	@Command(aliases = {"-lookup"}, description = "Shows the image and description of X.", usage = "-lookup X", privateMessages = true)
	public void onLookupCommand(Guild guild, MessageChannel channel, String[] args){
		if(args.length > 0){
			String request = args[0];
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			String message = "";
			boolean printedImage = false;
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(ParseVariants(request)))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(GetBaseModelName(request))))){
				printedImage = true;
			}
			else
				message = "There is no image associated with '" + request + "'";

			request = ParseVariants(request);
			String output = lookupData(request);
			if(!ShouldPrintThis(GetDataType(output))){
				OutputHelper(channel, "Try '-showdata' for that information.");
				return;
			}
			if(output.contains("\tdescription")){
				if(!printedImage)
					message += ", but I did find this:\n\n";

				message += output.substring(output.indexOf("\tdescription")).replaceAll("\tdescription", "");
			}
			else if(!printedImage)
				message += ", nor any description.";
			else if(printedImage)
				message = "There is no description of '" + request + "'.";

			if(output.length() < 1)
				message = "I could not find anything associated with '" + request + "'.";

			if(message.length() > 0)
				OutputHelper(channel, message);
		}
	}



	@Command(aliases = {"-show"}, description = "Shows both image and all data associated with X.", usage = "-show X", privateMessages = true)
	public void onShowCommand(Guild guild, MessageChannel channel, String[] args){
		if(args.length > 0){
			String request = args[0];
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			String message = "";
			boolean printedImage = false;
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(ParseVariants(request)))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(GetBaseModelName(request))))){
				printedImage = true;
			}
			else
				message = "I could not find an image associated with '" + request + "'";

			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.length() < 1){
				if(printedImage)
					message = "I could not find any data associated with '" + request + "'.";
				else
					message += ", nor could I find any data.";
			}
			else if(!printedImage)
				message += ", but I did find this:\n\n";

			OutputHelper(channel, message + output);
		}
	}



	@Command(aliases = {"-showimage", "-showImage"}, description = "Shows image of X. Does not print data.", usage = "-showimage X", privateMessages = true)
	public void onShowimageCommand(Guild guild, MessageChannel channel, String[] args){
		if(args.length > 0){
			String request = args[0];
			for(int i = 1; i < args.length; ++i){
				request += " " + args[i];
			}
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(ParseVariants(request)))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(GetBaseModelName(request))))){
				// This request was handled.
			}
			else
				OutputHelper(channel, "I could not find an image associated with '" + request + "'.");
		}
	}



	@Command(aliases = {"-showdata", "-showData"}, description = "Shows data of X. Does not print images.", usage = "-showdata X", privateMessages = true)
	public void onShowdataCommand(MessageChannel channel, String[] args){
		if(args.length > 0){
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



	@Command(aliases = {"-quote"}, description = "Quote person X.", usage = "-quote X", privateMessages = true)
	public void onQuoteCommand(MessageChannel channel, String[] args){
		if(args.length < 1)
			channel.sendMessage("A person! Give me a person!").queue();
		else{
			String request = args[0];
			for(int i = 1; i < args.length; ++i)
				request += " " + args[i];
			String quote = generateQuote(request);
			if(quote.length() > 0)
				channel.sendMessage("```\n``" + quote + "``\n\n" + "\t-- " + request + "```").queue();
			else
				channel.sendMessage("'" + request + "' hasn't said anything interesting.").queue();
		}
	}



	// Convert the requested lookup parameter into the relevant data
	// from the Endless Sky GitHub repository.
	// Returns nullstring if no data could be found.
	private String lookupData(String lookup){
		lookup = checkLookup(lookup, true);
		if(lookup.length() > 0){
			int start = data.indexOf(lookup);
			int end = start + lookup.length();
			do{
				end = data.indexOf('\n', end + 1);
			}
			// A line that starts with a tab, newline, or comment is considered to be
			// a part of this current lookup. Advance to the next newline.
			while(data.charAt(end + 1) == '\t' || data.charAt(end + 1) == '\n' || data.charAt(end + 1) == '#');
			return data.substring(start, end);
		}

		return "";
	}



	// Queries the loaded datafiles for special Endless Sky keywords.
	// If helper is 'true', will try both as-passed 'lookup', and with
	// enforced word capitalization.
	private String checkLookup(String lookup, boolean helper){
		final String[] dataTypes = {
			"ship",
			"outfit",
			"mission",
			"person",
			"planet",
			"system",
			"effect",
			"scene",
			"fleet",
			"event",
			"government",
			"phrase"
		};
		// The lookup may be exact:
		if(data.contains("\n" + lookup)){
			return "\n" + lookup;
		}
		// The lookup may be only the value, and not the keyword:
		for(String str : dataTypes){
			if(data.contains("\n" + str + " \"" + lookup + "\"")){
				return "\n" + str + " \"" + lookup + "\"";
			}
			else if(data.contains("\n" + str + " " + lookup)){
				return "\n" + str + " " + lookup;
			}
		}
		// Or perhaps not capitalized correctly.
		if(helper){
			lookup = CapitalizeWords(lookup);
			return checkLookup(lookup, false);
		}

		return "";
	}



	// Send the message 'output' to the desired channel, cutting into
	// multiple messages as needed.
	private void OutputHelper(MessageChannel channel, String output){
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



	// Check the string for image characteristics, and if found, print the image
	// to the specified channel & return true. Returns false for no image or no
	// valid image ending.
	private boolean PrintImage(Guild guild, MessageChannel channel, String input){
		if(HasImageToPrint(input)){
			String imageName = GetImageName(input);
			String filepath = urlEncode(CONTENT_URL + "/images/" + imageName);
			String ending = GetImageEnding(filepath);
			if(ending.length() > 0){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setImage(filepath + ending);
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				channel.sendMessage(eb.build()).queue();
				return true;
			}
		}

		return false;
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
	// are .png or .jpg (landscapes). Returns nullstring "" if no ending works, otherwise returns
	// the full ending (including the filetype).
	public String GetImageEnding(String url){
		String[] modes = {"", "-0", "+0", "~0", "=0", "-00", "+00", "~00", "=00"};
		String[] filetypes = {".png", ".jpg"};
		int m = 0;
		int t = 0;
		boolean hasEnding = isImage(url + modes[m] + filetypes[t] + "?raw=true");

		while(!hasEnding && t < filetypes.length){
			m = t > 0 ? -1 : 0;
			while(!hasEnding && ++m < modes.length){
				hasEnding = isImage(url + modes[m] + filetypes[t] + "?raw=true");
			}
			if(!hasEnding)
				++t;
		}
		if(hasEnding)
			return modes[m] + filetypes[t] + "?raw=true";

		return "";
	}



	public static String urlEncode(String url){
		return url.replace(" ", "%20");
	}



	// Check the string for image indicators. Returns false if there is no image.
	public static boolean HasImageToPrint(String input){
		return input.contains("\tsprite ") || input.contains("\tthumbnail ")
				|| input.contains("\tlandscape ") || input.contains("\tscene ");
	}



	// Check the string for a space or dash character and if present,
	// capitalize the next letter. Returns the string with captialized words.
	private static String CapitalizeWords(String input){
		int countWords = 1 + CountOf(input, ' ') + CountOf(input, '-');
		char[] ic = input.toCharArray();
		ic[0] = Character.toUpperCase(ic[0]);
		boolean hasDash = input.indexOf("-") > -1;
		boolean hasSpace = input.indexOf(" ") > -1;
		if(--countWords > 0){
			int index = input.indexOf(" ");
			if(hasDash)
				index = Math.min(index, input.indexOf("-"));
			for(int i = 0; i < countWords; ++i){
				++index;
				if(ic[index] == '(' || ic[index] == ')' || ic[index] == '"' || ic[index] == '-')
					++index;
				ic[index] = Character.toUpperCase(ic[index]);
				// Find next word break.
				int s = input.indexOf(" ", index);
				int d = input.indexOf("-", index);
				hasDash &= d > -1;
				hasSpace &= s > -1;
				if(hasDash && hasSpace)
					index = (s < d) ? s : d;
				else if(hasDash)
					index = d;
				else if(hasSpace)
					index = s;
				else
					break;
			}
		}

		return new String(ic);
	}



	// Check if the request is for a ship variant.
	private boolean IsShipVariantRequest(String request){
		// All Endless Sky ship variants have the name in parentheses by convention.
		if(request.indexOf('(') < 0 || request.indexOf(')') < 0)
			return false;

		// A request may return valid data while having parentheses if it is either
		// not a ship variant, or is already a ship variant with proper formatting.
		// e.g. '"base name" "base name (variant)"', 'Thruster (Stellar Class)', or
		// '"Thruster (Stellar Class)"'
		String base = GetBaseModelName(request);
		if(lookupData(request).length() > 0)
			return request.indexOf(base) != request.lastIndexOf(base);

		// The request is not something that matches existing data keywords (yet).
		request = ParseVariants(request);
		if(lookupData(request).length() > 0)
			return true;

		// This request doesn't return data at all.
		return false;
	}



	// Check for improperly-formatted ship variants, which require speccing as
	// "base model" "base model (variant)". If the request was not correctly
	// formatted, returns the request in the proper ship variant format.
	private String ParseVariants(String input){
		if((input.indexOf('(') > 0 || input.indexOf(')') > 0) && lookupData(input).length() < 1){
			String baseModel = GetBaseModelName(input);
			if(baseModel.length() > 0
					&& input.indexOf(baseModel) == input.lastIndexOf(baseModel)){
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
		if(text.contains("\tthumbnail"))
			start = text.indexOf("thumbnail") + 10;
		else if(text.contains("\tsprite"))
			start = text.indexOf("sprite") + 7;
		else if(text.contains("\tlandscape"))
			start = text.indexOf("landscape") + 10;
		else if(text.contains("\tscene"))
			start = text.indexOf("scene") + 6;
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



	// Returns the bit that comes before the searched request string.
	// e.g. "mission", "ship", "fleet", "outfit"
	public static String GetDataType(String output){
		if(output.length() < 1 || output.indexOf(" ") < 1)
			return "";
		return output.substring(1, output.indexOf(" "));
	}



	// Things that don't generally have images or descriptions probably shouldn't
	// get printed from -lookup.
	private static boolean ShouldPrintThis(String lookupType){
		if(lookupType.length() < 1)
			return false;

		switch(lookupType.toLowerCase()){
			case "mission":
				return false;
			case "event":
				return false;
			case "fleet":
				return false;
			default:
				return true;
		}
	}



	// Generate a quote from the named person, using their built-in phrases.
	// Phrases can be nested!! TODO: Move choice instantiation into readData.
	public String generateQuote(String person){
		String personData = lookupData(person);
		boolean hasPhrase = personData.indexOf("phrase\n") > -1 || personData.indexOf("phrase ") > -1;
		if(!hasPhrase)
			return "";
		int tabDepth = GetIndentLevel(personData, "phrase");
		ArrayList<String> strKeys = new ArrayList<String>();

		// Outermost 'phrase'
		StringBuilder key = new StringBuilder();
		for(int i = 0; i < tabDepth; ++i)
			key.append("\t");
		key.append("phrase");
		if(++tabDepth == 1)
			key.append(" ");
		else
			key.append("\n");
		strKeys.add(key.toString());

		// 'word' demarcator.
		key = new StringBuilder();
		for(int i = 0; i < tabDepth; ++i)
			key.append("\t");
		key.append("word");
		key.append("\n");
		strKeys.add(key.toString());

		// Nested phrase reference.
		++tabDepth;
		key = new StringBuilder();
		for(int i = 0; i < tabDepth; ++i)
			key.append("\t");
		key.append("phrase");
		key.append("\n");
		strKeys.add(key.toString());

		// iterate personData using the strKeys with indexOf
		// and then passing the substring to MakeChoices

		return Integer.toString(tabDepth);
	}



	// Call this for each word in a phrase, giving it the bits
	// between 'word', 'phrase', or the next data entry.
	private static String[] MakeChoices(String wordlist){
		final int MAXPOS = 10000000;
		int start = wordlist.indexOf("\tword\n");
		ArrayList<String> choices = new ArrayList<String>();
		boolean reachedEnd = false;
		while(!reachedEnd){
			int startQuote = wordlist.indexOf("\t\"", start);
			int startTick = wordlist.indexOf("\t`", start);
			int startWord = Integer.min(startQuote > -1 ? startQuote : MAXPOS, startTick > -1 ? startTick : MAXPOS);
			if(startWord == MAXPOS){
				reachedEnd = true;
				break;
			}
			boolean findTick = startQuote == -1 || (startTick > -1 && startTick < startQuote);
			int endWord = wordlist.indexOf(findTick ? "`\n" : "\"\n", findTick ? ++startTick : ++startQuote);
			choices.add(wordlist.substring(findTick ? startTick : startQuote, ++endWord));
			start = endWord;
		}
		if(!choices.isEmpty())
			return choices.toArray(new String[choices.size()]);
		else
			return new String[0];
	}



	// Return the number of tabs that preceed the specified indented word in the
	// datastring. Phrases are generally 0 or 1 level, while word demarcations
	// are generally 1 or 2 indent levels.
	private static int GetIndentLevel(String data, String word){
		int level = -1;
		int start = data.indexOf(word);
		if(start < 0)
			return level;

		int lineStart = data.lastIndexOf("\n", start);
		// If the data string starts with this word, return 0, even if there is no
		// newline preceeding it.
		if(lineStart < 0 || start == 0)
			return 0;

		String line = data.substring(lineStart, start + word.length() + 1);
		return CountOf(line,'\t');
	}
}
