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



	@Command(aliases = {"-template"}, description = "Sends the template for X. Possible args: outfit, ship, plugin.", usage = "-template X")
	public void onTemplatesCommand(MessageChannel channel, String[] args)
	{
		String name = "undefined";
		if(args.length == 0)
			channel.sendMessage("Which template do you want? I'm offering three flavours: 'outfit', 'ship' and 'plugin'.").queue();
		else
			for(String str : args)
				if(str.equals("plugin"))
					name = "exampleplugin.zip";
				else if(str.equals("outfit"))
					name = "outfittemplate.blend";
				else if(str.equals("ship"))
					name = "shiptemplate.blend";
				else
					channel.sendMessage("Sorry, i only have templates for 'outfit', 'ship' and 'plugin'.").queue();
				if(!name.equals("undefined"))
					channel.sendFile(new File("data/templates/" + name), new MessageBuilder().append(" ").build()).queue();
	}

}
