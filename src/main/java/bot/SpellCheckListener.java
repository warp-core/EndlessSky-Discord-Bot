package bot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class SpellCheckListener extends ListenerAdapter{
	private ESBot bot;
	private Properties spellErrors;

	public SpellCheckListener(ESBot bot){
		this.bot = bot;
		spellErrors = readSpellErrors(true);
	}



	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		if(event.getAuthor().isBot()) return;
		String msg = event.getMessage().getContent().toLowerCase();
		Enumeration<?> keys = getSpellErrors();
		// Find and send the first occurence of a match.
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			if(msg.contains(key.toLowerCase())){
				event.getChannel().sendMessage(getCorrection(key)).queue();
				break;
			}
		}
	}


	/**
	 * Create a key-value dictionary that holds matched words or phrases, and the "correction" that the bot should respond with.
	 * @return Properties
	 */
	private Properties readSpellErrors(Boolean local){
		Properties spellErrors = new Properties();
		try{
			if(local)
				spellErrors.load(new BufferedReader(Files.newBufferedReader(
						Paths.get("data", "spellErrors.txt"))));
			else
				spellErrors.load(new URL(bot.HOST_RAW_URL + "/data/spellErrors.txt").openStream());
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
			if(local){
				System.out.println("\nNo local spellErrors found. Loading published spellErrors instead...");
				return readSpellErrors(false);
			}
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("\nUnable to load spellchecking file.");
		}
		catch(IllegalArgumentException e){
			e.printStackTrace();
			System.out.println("\nMalformed Unicode escape in the input spellErrors file.");
		}
		return spellErrors;
	}



	private Enumeration<?> getSpellErrors(){
		return spellErrors.keys();
	}



	private String getCorrection(String key){
		return spellErrors.getProperty(key, "No correct spelling found!");
	}
}
