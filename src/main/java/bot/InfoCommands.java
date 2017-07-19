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



	@Command(aliases = {"-help"}, description = "Shows all commands, or the help text for the specified command(s) 'X'.", usage = "-help X")
	public void onHelpCommand(Guild guild, String[] args, MessageChannel channel) {
		if(args.length > 0 && !args[0].equals(" ")){
			for(String arg : args){
				for(CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()){
					arg = arg.trim();
					Command cmd = simpleCommand.getCommandAnnotation();
					if(cmd.usage().equals(arg) || cmd.aliases()[0].equals(arg)
							|| arg.equals(cmd.usage().substring(1))
							|| arg.equals(cmd.aliases()[0].substring(1))){
						if(!cmd.showInHelpPage()){
							continue;
						}
						EmbedBuilder eb = new EmbedBuilder();
						String title = "";
						if(!cmd.requiresMention()){
							// The default prefix only works if the command does not require a mention.
							title = commandHandler.getDefaultPrefix();
						}
						title += cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage();
						eb.setTitle(title, null);
						eb.setColor(guild.getMember(bot.getSelf()).getColor());
						if(!cmd.description().equals("none")){
							eb.setDescription(cmd.description());
						}
						eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cmd.png");
						channel.sendMessage(eb.build()).queue();
					}
				}
			}
		}
		else{
			StringBuilder builder = new StringBuilder();
			for(CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()){
				Command cmd = simpleCommand.getCommandAnnotation();
				if(!cmd.showInHelpPage()){
					continue;
				}
				builder.append("\n");
				if(!cmd.requiresMention()){
					// The default prefix only works if the command does not require a mention.
					builder.append(commandHandler.getDefaultPrefix());
				}
				builder.append(cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage());
			}
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
			eb.setDescription("Available Commands");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cmd.png");
			eb.addField("To get information about 'command', use \"-help 'command'\", e.g. \"-help -help\"", builder.toString(), false);
			channel.sendMessage(eb.build()).queue();
		}
	}


	@Command(aliases = {"-info"}, description = "Shows some information about the bot.", usage = "-info")
	public void onInfoCommand(Guild guild, MessageChannel channel){
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("**EndlessSky-Discord-Bot**", bot.HOST_PUBLIC_URL);
		String issues = bot.HOST_PUBLIC_URL + "/issues";
		String description =
				"- **Author:** Maximilian Korber\n" +
				"- **Language:** Java\n" +
				"- **Utilized Libraries:** JDA3, lavaplayer, & sdcf4j\n" +
				"- **Maintainers:** M\\*C\\*O, tehhowch\n\n" +
				"**Version Information:**\n";
		if(bot.version.hasCommitInfo()){
			description += "- **Branch:** " + bot.version.getBranch() +
						" @[" + bot.version.getCommitHash() + "](" +
						bot.HOST_PUBLIC_URL + "/commit/" +
						bot.version.getCommitHash() + ")\n" +
				"- **Commit:** " + bot.version.getCommitMessage() + "\n\n";
		}
		else{
			description += "- **Branch:** " + bot.version.getBranch() + "\n\n";
		}
		description += "[View known issues and feature requests](" + issues + ")";
		eb.setDescription(description);
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
		channel.sendMessage(eb.build()).queue();

	}

	@Command(aliases = {"-ping"}, description = "Time in milliseconds that discord took to respond to the last heartbeat.", usage = "-ping")
	public void onPingCommand(Guild guild, MessageChannel channel){
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
		eb.setDescription(bot.getPing() + "ms");
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
		channel.sendMessage(eb.build()).queue();
	}
}
