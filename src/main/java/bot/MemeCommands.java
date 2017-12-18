package bot;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class MemeCommands
implements CommandExecutor{

	private Properties memes;
	private Properties memeImgs;
	private static final String HOST_RAW_URL = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master";

	private ESBot bot;

	public MemeCommands(ESBot bot){
		this.bot = bot;
		this.memes = readMemes(true, "memes.txt");
		// Image memes must be published on the bot's public repo, as the image is linked and not uploaded.
		this.memeImgs = readMemes(false, "memeImgs.txt");
	}



	@Command(aliases = {"-meme"}, description = "Posts meme X, or a random Endless Sky meme if no X is given.", usage = "-meme [X]", privateMessages = true)
	public void onMemeCommand(Guild guild, MessageChannel channel, String[] args, User author) {
		if (author.isBot()) return;
		if(args.length == 0){
			channel.sendMessage(getRandomMeme()).queue();
		}
		else if(args.length == 1){
			if(!isImgMeme(args[0])){
				channel.sendMessage(getMeme(args[0])).queue();
			}
			else{
				String path = HOST_RAW_URL + "/data/memes/" + getImgMemePath(args[0]);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setImage(path);
				channel.sendMessage(eb.build()).queue();
			}
		}
	}



	@Command(aliases = {"-memelist", "-memes", "-memeList"}, description = "PMs you the current list of memes.", usage = "-memelist", privateMessages = true)
	public void onListMemesCommand(Guild guild, User user, MessageChannel channel, Message message, String[] args){
		if (user.isBot()) return;
		if(args.length == 0){
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Available Memes:", "https://github.com/MCOfficer/EndlessSky-Discord-Bot/tree/master/data");
			eb.addField("Text-based Memes", getMemelist(), false);
			eb.addField("Image-based Memes", getMemelistImgs(), false);
			eb.setThumbnail(HOST_RAW_URL + "/thumbnails/meme.png");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			user.openPrivateChannel().queue(c -> {
				c.sendMessage(eb.build()).queue();
			});
			if(channel instanceof TextChannel){
				message.delete().queue();
			}
		}
	}



	/**
	 * Attempts to load memes from the given location and the given file in data/.
	 * If no local file is found, the bot's public repo is checked instead.
	 * @param Boolean local    Controls if this read should be from disk or web.
	 * @param String           dataFile The name of the file to load, i.e. "memes.txt" or "memeImgs.txt"
	 * @return Properties      A set of memes (either image-based or text).
	 */
	private Properties readMemes(Boolean local, String dataFile){
		Properties props = new Properties();
		try{
			if(local)
				props.load(new BufferedReader(Files.newBufferedReader(
						Paths.get("data", dataFile))));
			else
				props.load(new URL(HOST_RAW_URL + "/data/" + dataFile).openStream());
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
			if(local){
				System.out.println("\nNo local memes found. Loading published memes instead...");
				return readMemes(false, dataFile);
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return props;
	}



	private boolean isImgMeme(String key){
		return memeImgs.containsKey(key);
	}



	private String getMeme(String meme){
		return memes.getProperty(meme, "Please don't joke about that sort of thing.");
	}



	private String getRandomMeme(){
		Enumeration<?> keys = memes.propertyNames();
		Random rGen = new Random();
		int random = rGen.nextInt(memes.size());
		while(--random >= 0)
			keys.nextElement();
		
		return memes.getProperty((String) keys.nextElement());
	}



	private String getImgMemePath(String string){
		return memeImgs.getProperty(string);
	}



	private String getMemelist(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		memes.list(ps);
		ps.close();
		String output = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		return output.substring(output.indexOf('\n'));
	}



	private String getMemelistImgs(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		memeImgs.list(ps);
		ps.close();
		String output = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		return output.substring(output.indexOf('\n'));
	}
}
