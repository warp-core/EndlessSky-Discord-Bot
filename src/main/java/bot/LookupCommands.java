package bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

public class LookupCommands
implements CommandExecutor{

	private String data;
	public static final String HOST_RAW_URL = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master";
	public static final String CONTENT_URL = "https://github.com/endless-sky/endless-sky/raw/master";
	public static final String CONTENT_HDPI_URL = "https://github.com/endless-sky/endless-sky-high-dpi/raw/master";

	private ESBot bot;

	public LookupCommands(ESBot bot){
		this.bot = bot;
		data = readData();
		System.out.println("Lookups instantiated.");
	}

	// These datatypes are automatically checked for capitalization and
	// quotation errors during lookups.
	static final String[] dataTypes = {
		"ship",
		"outfit",
		"mission",
		"person",
		"planet",
		"system",
		"shipyard",
		"outfitter",
		"effect",
		"scene",
		"fleet",
		"event",
		"government",
		"phrase"
	};



	private String readData(){
		String data = "";
		
		LinkedList<URL> dataFiles = new LinkedList<>();
		try(BufferedReader br = new BufferedReader(Files.newBufferedReader(Paths.get("data", "dataFileNames.txt")))){
			String line = br.readLine();

			while (line != null){
				dataFiles.add(new URL(bot.DATA_URL + line + ".txt"));
				line = br.readLine();
			}
		}
		catch(IOException e){
			System.out.println("\nNo datafile found for file names.\nAll lookups will fail.\n");
			System.out.println(e.toString());
			return data;
		}
		try{
			StringBuilder sb = new StringBuilder();
			for(URL url : dataFiles){
				try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))){
					String line = br.readLine();

					while(line != null){
						sb.append(line);
						sb.append(System.lineSeparator());
						line = br.readLine();
					}
					sb.append("\n~\n");
				}
				catch(IOException e){
					// A file that is expected to exist might not (for example,
					// lookups are being reinitialized but dataFileNames is old
					// and the file was renamed on GitHub).
					System.out.println(e.toString());
				}
			}
			data = sb.toString();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return data;
	}



	@Command(aliases = {"-issue"}, description = "Link to Endless Sky issue #X. If no issue number is given, links the issues page.", usage = "-issue X", privateMessages = true)
	public void onIssueCommand(MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		final String base = "https://github.com/endless-sky/endless-sky/issues";
		StringBuilder output = new StringBuilder("");
		// Check each input for a numeric portion until a non-numeric arg is found.
		if(args.length > 0)
			for(String str : args){
				String number = Helper.GetNumeric(str);
				if(number.length() > 0)
					output.append(base + "/" + number + "\n");
				else
					break;
			}

		if(output.length() == 1)
			output.append(base);

		channel.sendMessage(output.toString().replaceAll(" ", "")).queue();
	}



	@Command(aliases = {"-pull"}, description = "Link to Endless Sky pull request (PR) #X. If no pull number is given, links the PR page.", usage = "-pull X", privateMessages = true)
	public void onPullCommand(MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		final String base = "https://github.com/endless-sky/endless-sky/pull";
		StringBuilder output = new StringBuilder("");
		// Check each input for a numeric portion until a non-numeric arg is found.
		if(args.length > 0)
			for(String str : args){
				String number = Helper.GetNumeric(str);
				if(number.length() > 0)
					output.append(base + "/" + number + "\n");
				else
					break;
			}

		if(output.length() == 1)
			output.append(base + "s");

		channel.sendMessage(output.toString().replaceAll(" ", "")).queue();
	}



	@Command(aliases = {"-commit"}, description = "Link to Endless Sky commit hash \"X\". Only the first 7 letters are necessary.\nLeave blank for the most recent commit.", usage = "-commit X", privateMessages = true)
	public void onCommitCommand(MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		final String base = "https://github.com/endless-sky/endless-sky/commit/";
		StringBuilder output = new StringBuilder("");
		// Check each input for a hexadecimal hash until a non-hash arg is found.
		if(args.length > 0)
			for(String str : args){
				String hash = Helper.GetHash(str);
				if(hash.length() > 6)
					output.append(base + hash + "\n");
				else
					break;
			}

		if(output.length() == 1)
			output.append(base);

		channel.sendMessage(output.toString().replaceAll(" ", "")).queue();
	}



	@Command(aliases = {"-lookup"}, description = "Shows the image and description of X.", usage = "-lookup X", privateMessages = true)
	public void onLookupCommand(Guild guild, MessageChannel channel, String[] args, User author){
		if (author.isBot()) return;
		String[] parsed = Helper.getWords(args);
		if(parsed.length > 0){
			String request = parsed[0];
			for(int i = 1; i < parsed.length; ++i)
				request += " " + parsed[i];
			String message = "";
			boolean printedImage = false;
			String variantParsedRequest = ParseVariants(request);
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(variantParsedRequest))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(Helper.GetBaseModelName(request)))))
				printedImage = true;
			else
				message = "There is no image associated with '" + request + "'";

			String output = lookupData(variantParsedRequest);
			if(!Helper.ShouldPrintThis(Helper.GetDataType(output)) && output.length() > 0){
				Helper.OutputHelper(channel, "Try '-showdata' for that information.");
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
				message = "There is no description of '" + variantParsedRequest + "'.";

			if(output.length() < 1)
				message = "I could not find anything associated with '" + variantParsedRequest + "'.";

			if(message.length() > 0)
				Helper.OutputHelper(channel, message);
		}
	}



	@Command(aliases = {"-show"}, description = "Shows both image and all data associated with X.", usage = "-show X", privateMessages = true)
	public void onShowCommand(Guild guild, MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		String[] parsed = Helper.getWords(args);
		if(parsed.length > 0){
			String request = parsed[0];
			for(int i = 1; i < parsed.length; ++i)
				request += " " + parsed[i];
			String message = "";
			boolean printedImage = false;
			String variantParsedRequest = ParseVariants(request);
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(variantParsedRequest))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(Helper.GetBaseModelName(request)))))
				printedImage = true;
			else
				message = "I could not find an image associated with '" + request + "'";

			String output = lookupData(variantParsedRequest);
			if(output.length() < 1){
				if(printedImage)
					message = "I could not find any data associated with '" + variantParsedRequest + "'.";
				else
					message += ", nor could I find any data.";
			}
			else if(!printedImage)
				message += ", but I did find this:\n\n";

			Helper.OutputHelper(channel, message + output);
		}
	}



	@Command(aliases = {"-showimage", "-showImage"}, description = "Shows image of X. Does not print data.", usage = "-showimage X", privateMessages = true)
	public void onShowimageCommand(Guild guild, MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		String[] parsed = Helper.getWords(args);
		if(parsed.length > 0){
			String request = parsed[0];
			for(int i = 1; i < parsed.length; ++i)
				request += " " + parsed[i];
			if(PrintImage(guild, channel, lookupData(request))
					|| PrintImage(guild, channel, lookupData(ParseVariants(request)))
					|| (IsShipVariantRequest(request)
								&& PrintImage(guild, channel, lookupData(Helper.GetBaseModelName(request))))){
				// This request was handled.
			}
			else
				Helper.OutputHelper(channel, "I could not find an image associated with '" + request + "'.");
		}
	}



	@Command(aliases = {"-showdata", "-showData"}, description = "Shows data of X. Does not print images.", usage = "-showdata X", privateMessages = true)
	public void onShowdataCommand(MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		String[] parsed = Helper.getWords(args);
		if(parsed.length > 0){
			String request = parsed[0];
			for(int i = 1; i < parsed.length; ++i)
				request += " " + parsed[i];
			request = ParseVariants(request);
			String output = lookupData(request);
			if(output.length() < 1){
				output = "I could not find any data associated with '" + request + "'.";
			}
			Helper.OutputHelper(channel, output);
		}
	}



	@Command(aliases = {"-quote"}, description = "Quote person X.", usage = "-quote X", privateMessages = true)
	public void onQuoteCommand(MessageChannel channel, String[] args, User author){
		if(author.isBot()) return;
		String[] parsed = Helper.getWords(args);
		if(parsed.length < 1)
			channel.sendMessage("A person! Give me a person!").queue();
		else{
			String request = parsed[0];
			for(int i = 1; i < parsed.length; ++i)
				request += " " + parsed[i];
			String quote = generateQuote(request);
			if(quote.length() > 0)
				channel.sendMessage("```\n``" + quote + "``\n\n" + "\t-- " + request + "```").queue();
			else
				channel.sendMessage("'" + request + "' hasn't said anything interesting.").queue();
		}
	}



	@Command(aliases = {"-swizzle"}, description = "Get information about a swizzle X (0-8). \nIf an image is attached, a swizzled version of that image will be returned, if X is not specified, swizzles 1-6 will be returned.", usage = "-swizzle X (Warning: Spoilers!)\n-swizzle X [attached image]\n-swizzle [attached image]", privateMessages = false)
	public void onSwizzleCommand(MessageChannel channel, Message msg, Guild guild, User author){
		if(author.isBot()) return;
		String swizzleStr;
		try {
			 swizzleStr = msg.getContentRaw().substring(msg.getContentRaw().indexOf(" ")).trim();
		}
		catch (StringIndexOutOfBoundsException e) {
			swizzleStr = null;
		}

		if(!msg.getAttachments().isEmpty()) {
			ImageSwizzler swizzler = new ImageSwizzler();
			try {
				InputStream image = swizzler.swizzle(msg.getAttachments().get(0).getInputStream(), swizzleStr);
				channel.sendFile(image, "swizzles.png").queue();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		// If no number is given, assign 9 to prevent a NumberFormatException.
		int swizzle = swizzleStr.length() == 0 ? 9 : new Integer(swizzleStr).intValue();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		if(swizzle >= 0 && swizzle <= 8){
			String[] vectors = {
					"{GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA} // red + yellow markings (republic)",
					"{GL_RED, GL_BLUE, GL_GREEN, GL_ALPHA} // red + magenta markings",
					"{GL_GREEN, GL_RED, GL_BLUE, GL_ALPHA} // green + yellow (freeholders)",
					"{GL_BLUE, GL_RED, GL_GREEN, GL_ALPHA} // green + cyan",
					"{GL_GREEN, GL_BLUE, GL_RED, GL_ALPHA} // blue + magenta (syndicate)",
					"{GL_BLUE, GL_GREEN, GL_RED, GL_ALPHA} // blue + cyan (merchant)",
					"{GL_GREEN, GL_BLUE, GL_BLUE, GL_ALPHA} // red and black (pirate)",
					"{GL_BLUE, GL_ZERO, GL_ZERO, GL_ALPHA} // red only (cloaked)",
					"{GL_ZERO, GL_ZERO, GL_ZERO, GL_ALPHA} // black only (outline)"
			};
			eb.setDescription("**Swizzle Vector:**\n```" + vectors[swizzle] + "```\n\n**Governments using this swizzle:**\n" + getGovernmentsBySwizzle(swizzle));
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/swizzles/" + swizzle + ".png");
		}
		else{
			eb.setDescription("This swizzle does not exist.");
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
		}
		channel.sendMessage(eb.build()).queue();
	}



	// Convert the requested lookup parameter into the relevant data
	// from the Endless Sky GitHub repository.
	// Returns nullstring if no data could be found.
	private String lookupData(String lookup){
		// Remove any leading or trailing spaces.
		lookup = lookup.trim();
		// The first word of the lookup may be a supported dataType.
		int countWords = 1 + Helper.CountOf(lookup, ' ');
		String category = "";
		if(countWords > 1){
			category = lookup.substring(0, lookup.indexOf(" ")).toLowerCase();
			boolean isCategory = false;
			// Is the first word a dataType?
			for(String str : dataTypes)
				if(str.contains(category)){
					isCategory = true;
					break;
				}
			
			if(isCategory)
				lookup = lookup.substring(lookup.indexOf(" ") + 1);
			else
				category = "";
		}
		lookup = checkLookup(category, lookup, true);
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
	// If the first word of a lookup was a supported category, it is not
	// subjected to capitalization and quoting.
	private String checkLookup(String dataType, String lookup, boolean helper){
		// The lookup may be exact:
		if(data.contains("\n" + lookup))
			return "\n" + lookup;

		// A supported dataType limiter may have been used.
		if(dataType.length() > 0){
			if(data.contains("\n" + dataType + " \"" + lookup + "\""))
				return "\n" + dataType + " \"" + lookup + "\"";
			else if(data.contains("\n" + dataType + " " + lookup))
				return "\n" + dataType + " " + lookup;
		}
		else{
			for(String str : dataTypes){
				if(data.contains("\n" + str + " \"" + lookup + "\""))
					return "\n" + str + " \"" + lookup + "\"";
				else if(data.contains("\n" + str + " " + lookup))
					return "\n" + str + " " + lookup;
			}
		}
		// The input may not have been capitalized correctly.
		if(helper){
			lookup = Helper.CapitalizeWords(lookup);
			return checkLookup(dataType, lookup, false);
		}

		return "";
	}



	// Check the string for image characteristics, and if found, print the image
	// to the specified channel & return true. Returns false for no image or no
	// valid image ending.
	private boolean PrintImage(Guild guild, MessageChannel channel, String input){
		if(Helper.HasImageToPrint(input)){
			String imageName = Helper.GetImageName(input);
			String filepath = Helper.urlEncode(CONTENT_HDPI_URL + "/images/" + imageName);
			String ending = GetImageEnding(filepath);
			if(ending.length() == 0) {
				filepath = Helper.urlEncode(CONTENT_URL + "/images/" + imageName);
				ending = GetImageEnding(filepath);
			}
			else if (ending.length() > 0){
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
		catch(Exception e){
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
		boolean hdpi = url.contains("/endless-sky-high-dpi/");
		int m = 0;
		int t = 0;
		boolean hasEnding = isImage(url + modes[m] + (hdpi ? "%402x" : "") + filetypes[t] + "?raw=true");

		while(!hasEnding && t < filetypes.length){
			m = t > 0 ? -1 : 0;
			while(!hasEnding && ++m < modes.length){
				hasEnding = isImage(url + modes[m] + (hdpi ? "%402x" : "") + filetypes[t] + "?raw=true");
			}
			if(!hasEnding)
				++t;
		}
		if(hasEnding)
			return modes[m] + (hdpi ? "%402x" : "") + filetypes[t] + "?raw=true";

		return "";
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
		String base = Helper.GetBaseModelName(request);
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
			String baseModel = Helper.GetBaseModelName(input);
			if(baseModel.length() > 0
					&& input.indexOf(baseModel) == input.lastIndexOf(baseModel))
				input = "\"" + baseModel.replace("\"", "") + "\" \"" + input.replace("\"", "") + "\"";
		}

		return input;
	}


	// Returns a String with one Government using the Swizzle swizzle in every line, starting with a newline
	public String getGovernmentsBySwizzle(int swizzle) {
		Scanner sc = new Scanner(data);
		String formerLine = "";
		ArrayList<String> results = new ArrayList<>();
		while(sc.hasNext()){
			String line = sc.nextLine();
			if(line.contains("swizzle " + swizzle) && formerLine.contains("government"))
				results.add(formerLine);
			formerLine = line;
		}
		StringBuilder sb = new StringBuilder("");
		for(String s : results)
			sb.append("\n\u2022 " + s.replace("government \"", "").replace("\"", "").trim());
		return sb.toString();
	}



	// Generate a quote from the named person, using their built-in phrases.
	// Phrases can be nested!! TODO: Move choice instantiation into readData.
	public String generateQuote(String person){
		String personData = lookupData(person);
		boolean hasPhrase = personData.indexOf("phrase\n") > -1 || personData.indexOf("phrase ") > -1;
		if(!hasPhrase)
			return "";
		int tabDepth = Helper.GetIndentLevel(personData, "phrase");
		LinkedList<String> strKeys = new LinkedList<String>();

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
}
