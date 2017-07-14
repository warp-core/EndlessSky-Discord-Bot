package bot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class SpellCheckListener extends ListenerAdapter
{
	private ESBot bot;
	private Properties spellErrors;

	public SpellCheckListener(ESBot bot) {
		this.bot = bot;
		spellErrors = readSpellErrors();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		if(event.getAuthor() != bot.getSelf()){
			String msg = event.getMessage().getContent();
			Enumeration<?> keys = getSpellErrors();
			while(keys.hasMoreElements()){
				String key = (String) keys.nextElement();
				if(msg.contains(key)||msg.contains(key.toLowerCase())){
					event.getChannel().sendMessage(getCorrection(key)).queue();
					break;
				}
			}
		}
	}
	
	private Properties readSpellErrors() {
		Properties spellErrors = new Properties();
		try {
			spellErrors.load(new URL("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/data/spellErrors.txt").openStream());
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return spellErrors;
	}	

	public Enumeration<?> getSpellErrors() {
		return spellErrors.keys();
	}

	public String getCorrection(String key) {
		return spellErrors.getProperty(key,"No correct spelling found!");
	}
}
