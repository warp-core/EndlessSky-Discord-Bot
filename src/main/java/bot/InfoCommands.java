package bot;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;

public class InfoCommands 
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;

	public InfoCommands(CommandHandler commandHandler, ESBot bot) {
		this.commandHandler = commandHandler;
		this.bot = bot;
	}

	@Command(aliases = {"-help"}, description = "Shows all commands, or help to the specified command.", usage = "-help [X]")
	public void onHelpCommand(Guild guild, String[] args, MessageChannel channel) {
		if(args.length == 1){
			for (CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()){
				if(simpleCommand.getCommandAnnotation().usage().equals(args[0])||simpleCommand.getCommandAnnotation().aliases()[0].equals(args[0])){
					if (!simpleCommand.getCommandAnnotation().showInHelpPage()) {
						continue; // skip command
					}
					EmbedBuilder eb = new EmbedBuilder();
					String title = "";
					if (!simpleCommand.getCommandAnnotation().requiresMention()) {
						// the default prefix only works if the command does not require a mention
						title = commandHandler.getDefaultPrefix();
					}
					title += simpleCommand.getCommandAnnotation().usage();
					if (simpleCommand.getCommandAnnotation().usage().isEmpty()) { // no usage provided, using the first alias
						title += simpleCommand.getCommandAnnotation().aliases()[0];
					}
					eb.setTitle(title,null);
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					String description = simpleCommand.getCommandAnnotation().description();
					if (!description.equals("none")) {
						eb.setDescription(description);
					}					
					eb.setThumbnail("https://raw.githubusercontent.com/Wrzlprnft/EndlessSky-Discord-Bot/embed_thumbnails/cmd.png");
					channel.sendMessage(eb.build()).queue();
				}				
			}
		}else{
			StringBuilder builder = new StringBuilder();
			for (CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()){
				if (!simpleCommand.getCommandAnnotation().showInHelpPage()) {
					continue; // skip command
				}
				builder.append("\n");
				if (!simpleCommand.getCommandAnnotation().requiresMention()) {
					// the default prefix only works if the command does not require a mention
					builder.append(commandHandler.getDefaultPrefix());
				}
				String usage = simpleCommand.getCommandAnnotation().usage();
				if (usage.isEmpty()) { // no usage provided, using the first alias
					usage = simpleCommand.getCommandAnnotation().aliases()[0];
				}
				builder.append(usage);
			}
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("EndlessSky-Discord-Bot", "https://github.com/Wrzlprnft/EndlessSky-Discord-Bot");
			eb.setDescription("Available Commands");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail("https://raw.githubusercontent.com/Wrzlprnft/EndlessSky-Discord-Bot/embed_thumbnails/cmd.png");
			eb.addField("To get information about a command, use \"-help command\", e.g. \"-help -help\"", builder.toString(), false);
			channel.sendMessage(eb.build()).queue();
		}
	}


	@Command(aliases = {"-info"}, description = "Shows some information about the bot.", usage = "-info")
	public void onInfoCommand(Guild guild, MessageChannel channel){
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("EndlessSky-Discord-Bot", "https://github.com/Wrzlprnft/EndlessSky-Discord-Bot");
		String description = "- **Author:** Maximilian Korber\n" +
				"- **Language:** Java\n" +
				"- **Utilized Libraries:** JDA3,lavaplayer & sdcf4j";
		eb.setDescription(description);
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail("https://raw.githubusercontent.com/Wrzlprnft/EndlessSky-Discord-Bot/embed_thumbnails/info.png");
		channel.sendMessage(eb.build()).queue();

	}

	@Command(aliases = {"-ping"}, description = "Time in milliseconds that discord took to respond to the last heartbeat.", usage = "-ping")
	public void onPingCommand(Guild guild, MessageChannel channel){
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("EndlessSky-Discord-Bot", "https://github.com/Wrzlprnft/EndlessSky-Discord-Bot");
		eb.setDescription(bot.getPing() + "ms");
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail("https://raw.githubusercontent.com/Wrzlprnft/EndlessSky-Discord-Bot/embed_thumbnails/info.png");
		channel.sendMessage(eb.build()).queue();
	}
}