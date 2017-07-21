package bot;

import java.io.File;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.MessageBuilder;

public class MiscCommands
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;

	public MiscCommands(CommandHandler commandHandler, ESBot bot) {
		this.commandHandler = commandHandler;
		this.bot = bot;
	}



	@Command(aliases = {"-template"}, description = "Sends the template for X. Possible args: outfit, ship, or plugin.", usage = "-template X")
	public void onTemplatesCommand(MessageChannel channel, String[] args)
	{
		if(args.length == 0)
			channel.sendMessage("Which template would you like? I have three flavours available: 'outfit', 'ship' and 'plugin'.").queue();
		else
			for(String str : args){
				String name = "";
				if(str.equals("plugin"))
					name = "exampleplugin.zip";
				else if(str.equals("outfit"))
					name = "outfittemplate.blend";
				else if(str.equals("ship"))
					name = "shiptemplate.blend";
				else
					channel.sendMessage("Sorry, I only have templates for 'outfit', 'ship' and 'plugin'.").queue();
				
				if(name.length() > 0){
					String url = bot.HOST_RAW_URL + "data/templates/" + name;
					channel.sendMessage("Here's your " + str + " template:\n[" + str + "](" + url + ")").queue();
				}
			}
	}

}
