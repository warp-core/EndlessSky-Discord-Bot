package bot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class MemeCommands 
implements CommandExecutor{

	private Properties memes;
	private Properties memeImgs;

	public MemeCommands() {
		readMemes();
	}

	@Command(aliases = {"-meme"}, description = "Posts meme X or a random one when no X given (Only Endless Sky related).", usage = "-meme [X]", privateMessages = true)
	public void onMemeCommand(MessageChannel channel, String[] args){
		if(args.length == 0){	//random meme
			channel.sendMessage(getRandomMeme()).queue();
		}else if(args.length == 1){	//specified meme
			if(!isImgMeme(args[0])){
				channel.sendMessage(getMeme(args[0])).queue();
			}else{
				String path = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/data/memes/" +getImgMemePath(args[0]);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setImage(path);
				channel.sendMessage(eb.build()).queue();
			}
		}else{
			//silently fail
		}
	}

	@Command(aliases = {"-memelist"}, description = "Posts the current list of memes in private chat.", usage = "-memelist", privateMessages = true)
	public void onListmemesCommand(User user, MessageChannel channel, Message message, String[] args){
		if(args.length == 0){
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Available Memes:", "https://github.com/MCOfficer/EndlessSky-Discord-Bot/tree/data");
			eb.addField("Text-based Memes", getMemelist(), false);
			eb.addField("Image-based Memes", getMemelistImgs(), false);
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/meme.png");
			if(user.hasPrivateChannel()){
				user.getPrivateChannel().sendMessage(eb.build()).queue();
			}else{
				user.openPrivateChannel().queue( c -> {
					c.sendMessage(eb.build()).queue();
				});
			}
			if(channel instanceof TextChannel){
				message.delete().queue();
			}
		}else{
			//silently fail
		}
	}	
	
	private void readMemes() {
		Properties memes = new Properties();
		try {
			memes.load(new URL("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/data/memes.txt").openStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.memes = memes;
		memes = new Properties();
		try {
			memes.load(new URL("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/data/memeImgs.txt").openStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.memeImgs = memes;
	}	
	
	public boolean isImgMeme(String key){
		return memeImgs.containsKey(key);
	}
	
	public String getMeme(String meme){
		return memes.getProperty(meme, "Please don't joke about that sort of thing.");
	}

	public String getRandomMeme() {
		Enumeration<?> keys = memes.propertyNames();
		Random rGen = new Random();
		int random = rGen.nextInt(memes.size());
		String key = (String) keys.nextElement();
		for(int i = 0; i < random; i++){
			key = (String) keys.nextElement();
		}
		return memes.getProperty(key);
	}

	public String getImgMemePath(String string) {
		return memeImgs.getProperty(string);
	}

	public String getMemelist() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		memes.list(ps);
		ps.close();
		String output = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		return output.substring(output.indexOf('\n'));
	}
	
	public String getMemelistImgs() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		memeImgs.list(ps);
		ps.close();
		String output = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		return output.substring(output.indexOf('\n'));
	}

	public void OutputHelper(MessageChannel channel,String output){
		if(output.length()<1993){
			channel.sendMessage(":\n```" + output + "```").queue();
		}else{
			int cut = output.lastIndexOf('\n', 0+1992);
			String o = output.substring(0, cut);			
			channel.sendMessage(":\n```" + o + "```").queue(x -> {
				OutputHelper(channel, output.substring(cut+1));
			});
		}
	}


}
