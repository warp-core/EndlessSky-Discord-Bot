package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.MessageChannel;

public class Helper {

	public static final String ROLE_PLAYBANNED = "Anti-DJ";
	public static final String ROLE_GULAG = "Bad Boy/Girl";

	// Count the number of the given character in the given string.
	public static int CountOf(String input, char token){
		int count = 0;
		for(char c : input.toCharArray()){
			if(c == token)
				++count;
		}

		return count;
	}



	// Return the numeric portion of the passed argument
	public static String GetNumeric(String arg){
		arg = arg.replaceAll("\"", "").replaceAll("\'", "").trim();
		Pattern regex = Pattern.compile("\\d{1,}");
		Matcher result = regex.matcher(arg);
		String numberPart = result.find() ? result.group() : "";
		return numberPart;
	}



	// Return the hexadecimal portion of the passed argument.
	public static String GetHash(String arg){
		arg = arg.replaceAll("\"", "").replaceAll("\'", "").trim();
		Pattern regex = Pattern.compile("[\\dA-Fa-f]+");
		Matcher result = regex.matcher(arg);
		String hash = result.find() ? result.group() : "";
		return hash.length() > 6 ? hash : "";
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

		return text.substring(0, end-1).replaceAll("\"", "").trim();
	}



	// Returns the bit that comes before the searched request string.
	// e.g. "mission", "ship", "fleet", "outfit"
	public static String GetDataType(String output){
		if(output.length() < 1 || output.indexOf(" ") < 1)
			return "";
		int start = (output.indexOf('\n') == 0 || output.indexOf('\t') == 0) ? 1 : 0;
		return output.substring(start, output.indexOf(" "));
	}



	// Things that don't generally have images or descriptions probably shouldn't
	// get printed from -lookup.
	public static boolean ShouldPrintThis(String lookupType){
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



	// Return the number of tabs that preceed the specified indented word in the
	// datastring. Phrases are generally 0 or 1 level, while word demarcations
	// are generally 1 or 2 indent levels.
	public static int GetIndentLevel(String data, String word){
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



	// Call this for each word in a phrase, giving it the bits
	// between 'word', 'phrase', or the next data entry.
	public static String[] MakeChoices(String wordlist){
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



	// Check the string for image indicators. Returns false if there is no image.
	public static boolean HasImageToPrint(String input){
		return input.contains("\tsprite ") || input.contains("\tthumbnail ")
				|| input.contains("\tlandscape ") || input.contains("\tscene ");
	}



	public static String urlEncode(String url){
		return url.replace(" ", "%20");
	}



	// Check the string for a space or dash character and if present,
	// capitalize the next letter. Returns the string with captialized words.
	public static String CapitalizeWords(String input){
		int countWords = 1 + CountOf(input, ' ') + CountOf(input, '-');
		char[] ic = input.toCharArray();
		ic[ic[0] == '"' ? 1 : 0] = Character.toUpperCase(ic[ic[0] == '"' ? 1 : 0]);
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



/**
	 * If the desired role does not exist, creates it.
	 * @param  Guild  guild         The server to make the role for.
	 * @param  String role          The role title to create.
	 * @return        true / false, depending on if there was an exception.
	 */
	public static boolean EnsureRole(Guild guild, String role){
		try{
			if(guild.getRolesByName(role, true).size() == 0){
				guild.getController().createRole().queue( r -> {
					r.getManager().setName(role).queue();
				});
			}
			return true;
		}
		catch(Exception e){
			System.out.println(e);
		}
		return false;
	}



	/**
	 * Parse the input string to determine if it is a valid integer in the
	 * range of min and max.
	 * @param  String input         The input string
	 * @param  int    min           Minimum acceptable value
	 * @param  int    max           Maximum acceptable value
	 * @return        true / false depending on if number and in range.
	 */
	public static boolean IsIntegerInRange(String input, int min, int max){
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		try{
			int amount = Integer.valueOf(input);
			if(amount >= low && amount <= high)
				return true;
		}
		catch (NumberFormatException e){
			return false;
		}
		return false;
	}



	/**
	 * Returns a random "access denied" message, for humorously dismissing
	 * Discord chatters who overstep.
	 * @return "Access Denied" string.
	 */
	public static String GetRandomDeniedMessage(){
		final String[] messageList = {
			"You can't order me around.",
			"I don't listen to you.",
			"You're not my boss.",
			"Try harder.",
			"You think you're a hotshot pirate?",
			"Your attempt at using 'Pug Magic' has failed.",
			"You're no Admiral Danforth.",
			"As if.",
			"That prison on Clink is looking rather empty...",
			"Oh yeah?",
			"Nice try.",
			"I may be old, but I'm not dumb.",
			"I'll pretend you didn't say that.",
			"Not today.",
			"Oh, to be young again...",
			"*yawn*",
			"I have the power. You don't.",
			"Go play in a hyperspace lane.",
			"How about I put *you* in the airlock?",
			"Access Denied."
		};
		Random rGen = new Random();
		int choice = rGen.nextInt(messageList.length);
		return messageList[choice];
	}



	/**
	 * Utility function that will concatenate a list of strings into valid
	 * Discord messages.
	 * @param TextChannel  channel The desired output channel
	 * @param List<String> output  The list of strings to write.
	 * @param String       header  A string that should prefix every chunk.
	 * @param String       footer  A string that should end every chunk.
	 */
	public static void writeChunks(TextChannel channel, List<String> output, String header, String footer){
		if(output.isEmpty())
			return;

		StringBuilder chunk = new StringBuilder(header);
		int chunkSize = chunk.length() + footer.length();
		final int sizeLimit = 1990;
		if(chunkSize > sizeLimit){
			System.out.println("Cannot ever print: header + footer too large.");
			return;
		}
		for(String str : output){
			if(chunkSize + str.length() <= sizeLimit)
				chunk.append(str);
			else{
				channel.sendMessage(chunk.append(footer).toString()).queue();
				chunk = new StringBuilder(header);
			}
			chunkSize = chunk.length() + footer.length();
		}
		// Write the final chunk.
		if(chunk.length() > header.length())
			channel.sendMessage(chunk.append(footer).toString()).queue();
	}



	// Send the message 'output' to the desired channel, cutting into
	// multiple messages as needed.
	public static void OutputHelper(MessageChannel channel, String output){
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



	/**
	 * Checks if the given member has special rights in the linked channel
	 * (and corresponding guild). If so, they are not bannable.
	 * This function ensures that bots do not attempt to ban / gulag other
	 * bots or moderators as a part of batch operations.
	 * @param  TextChannel channel       The channel to inspect for mod rights.
	 * @param  Member      member        The server member.
	 * @return             true / false, as expected.
	 */
	public static boolean isBannable(TextChannel channel, Member member){
		if(member.getUser().isBot())
			return false;

		if(CanModerate(channel, member))
			return false;

		return true;
	}



	/**
	 * Indicates if the given member has the rights to manage messages, the channel, or otherwise administrate the server.
	 * @param  TextChannel channel       The channel to moderate.
	 * @param  Member      member        The member who is/is not a moderator.
	 * @return             true / false if the member is a mod of that channel.
	 */
	public static boolean CanModerate(TextChannel channel, Member member){
		if(member.isOwner())
			return true;

		List<Permission> p = member.getPermissions(channel);
		if(p.contains(Permission.ADMINISTRATOR)
				|| p.contains(Permission.MESSAGE_MANAGE)
				|| p.contains(Permission.MANAGE_CHANNEL))
			return true;

		return false;
	}



	/**
	 * Indicates if the given member is a mod who is able to alter roles.
	 * @param  TextChannel channel       The channel to moderate.
	 * @param  Member      mod           The member who is/is not a moderator.
	 * @return             true / false as expected.
	 */
	public static boolean CanModAndRoleChange(TextChannel channel, Member mod){
		if(mod.isOwner() || (CanModerate(channel, mod)
				&& mod.getPermissions().contains(Permission.MANAGE_ROLES)))
			return true;

		return false;
	}



	/**
	 * Determine if the input string is a valid channel that is different
	 * from the origin channel, and in which the bot can post.
	 * @param  TextChannel origin    The channel from which the command came.
	 * @param  TextChannel input     The passed "channel" to test.
	 * @return             true / false depending if a channel and also different.
	 */
	public static boolean IsDiffAndWritable(TextChannel origin, TextChannel dest){
		// Compare the channels' string IDs.
		if(origin.getId().equals(dest.getId()))
			return false;

		// Ensure the bot is allowed to read & write in the destination.
		if(!dest.canTalk())
			return false;

		return true;
	}



	/**
	 * Gets the thumbnail of a track/video to be used in an embed.
	 * @param  String      TrackUrl    The URL of the track in question.
	 * @return             The URL of the thumbnail. Defaults to /thumbnails/info.png ,if no fitting thumbnail is found.
	 */
	public static String getTrackThumbnail(String TrackUrl){
		String thumbnail = null;
		if (TrackUrl.contains("soundcloud.com/"))
			thumbnail = ((thumbnail = getSoundcloudThumbnail(TrackUrl)) != null) ? thumbnail : null;
		else if (TrackUrl.contains("youtube.com/") || TrackUrl.contains("youtu.be/"))
			thumbnail = ((thumbnail = getYoutubeThumbnail(TrackUrl)) != null) ? thumbnail : null;
		thumbnail = (thumbnail != null) ? thumbnail : ESBot.HOST_RAW_URL + "/thumbnails/info.png";
		return thumbnail;
	}



	/**
	 * Gets a Soundcloud thumbnail URL.
	 * @param  String      TrackUrl    The Soundcloud url linking to the track.
	 * @return             The URL of the thumbnail, with the size 500x500. May be null, if not found.
	 */
	public static String getSoundcloudThumbnail(String TrackUrl){
		String html = getPlainHtml(TrackUrl);
		int pos = html.indexOf("\"artwork_url\":") + 15;
		String artwork_url = html.substring(pos, html.indexOf("-large.jpg\"", pos) + 10);
		return artwork_url;
	}


	/**
	 * Gets a YouTube thumbnail URL.
	 * @param  String      TrackUrl    The YouTube url linking to the track.
	 * @return             The URL of the thumbnail. May be null, if not found.
	 */
	public static String getYoutubeThumbnail(String TrackUrl){
		String thumbnail_url = null;
		String id = getYoutubeId(TrackUrl);
		if (id != null)
			thumbnail_url = "http://i1.ytimg.com/vi/" + id + "/0.jpg";
		System.out.println(thumbnail_url);
		return thumbnail_url;
	}



	/**
	 * Gets a YouTube video ID.
	 * @param  String      url    The YouTube url linking to the track.
	 * @return             The video ID. May be null, if not found.
	 */
	public static String getYoutubeId(String url){
		String pattern = "(?<=watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(url);
		if(m.find()){
			return m.group();
		}
		else
			return null;
	}




	public static String getPlainHtml(String path) {
		URL url;
		StringBuilder sb = new StringBuilder();
		try {
			url = new URL(path);
			URLConnection c = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}
			br.close();
			return sb.toString();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}


}
