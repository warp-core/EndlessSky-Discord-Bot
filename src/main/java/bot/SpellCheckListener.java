package bot;

import java.io.BufferedReader;
import java.io.IOException;
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
		spellErrors = readSpellErrors();
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
	private Properties readSpellErrors(){
		Properties spellErrors = new Properties();
		try{
			BufferedReader br = new BufferedReader(Files.newBufferedReader(
					Paths.get("data", "spellErrors.txt")));
			spellErrors.load(br);
		}
		catch(IOException e){
			System.out.println("\nNo datafile found for spellchecking.\nNo spellchecking will be done.\n");
			System.out.println(e.toString());
			e.printStackTrace();
		}
		catch(IllegalArgumentException e){
			System.out.println("\nMalformed Unicode escape in the input spellErrors file.");
			e.printStackTrace();
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
