package bot;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.CommandHandler.SimpleCommand;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class InfoCommands
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;

	public InfoCommands(CommandHandler commandHandler, ESBot bot) {
		this.commandHandler = commandHandler;
		this.bot = bot;
	}



	@Command(aliases = {"-help"}, description = "Shows all commands, or the help text for the specified command(s) 'X'.", usage = "-help X")
	public void onHelpCommand(Guild guild, String[] args, MessageChannel channel, User author) {
		if (author.isBot()) return;
		if(args.length > 0 && !args[0].equals(" ")){
			for(String arg : args){
				for(CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()){
					arg = arg.trim();
					Command cmd = simpleCommand.getCommandAnnotation();
					if(!cmd.showInHelpPage())
						continue;
					boolean isPrimary = cmd.usage().equals(arg)
							|| arg.equals(cmd.usage().substring(1));
					int aliasIndex = -1;
					boolean isAlias = false;
					if(!isPrimary)
						for(String alias : cmd.aliases()){
							++aliasIndex;
							if(arg.equals(alias) || arg.equals(alias.substring(1))){
								isAlias = true;
								break;
							}
						}
					
					if(isPrimary || isAlias){
						EmbedBuilder eb = new EmbedBuilder();
						String title = "";
						if(!cmd.requiresMention()){
							// The default prefix only works if the command does not require a mention.
							title = commandHandler.getDefaultPrefix();
						}
						title += isAlias ? cmd.aliases()[aliasIndex]
								: (cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage());
						eb.setTitle(title);
						eb.setColor(guild.getMember(bot.getSelf()).getColor());
						eb.setDescription(cmd.usage() + "\n-----\n");
						if(!cmd.description().equals("none")){
							eb.appendDescription(cmd.description());
						}
						eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cmd.png");
						channel.sendMessage(eb.build()).queue();
					}
				}
			}
		}
		else{
			List<SimpleCommand> commandList = new ArrayList<>();
			for (SimpleCommand cmd : commandHandler.getCommands()) {
				commandList.add(cmd);
			}
			Collections.sort(commandList, new Sorter(commandHandler));
			StringBuilder builder = new StringBuilder();
			String playerControl = "";
			String lookupCommands = "";
			String otherCommands = "";
			for(CommandHandler.SimpleCommand simpleCommand : commandList){
				Command cmd = simpleCommand.getCommandAnnotation();
				if(!cmd.showInHelpPage()){
					continue;
				}
				if(!cmd.requiresMention()){
					// The default prefix only works if the command does not require a mention.
					if(simpleCommand.getMethod().getDeclaringClass().getName().equals("bot.PlayerControl")){
							playerControl = playerControl + "\n" + commandHandler.getDefaultPrefix() + (cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage());
					}
					else if(simpleCommand.getMethod().getDeclaringClass().getName().equals("bot.LookupCommands")){
						lookupCommands = lookupCommands + "\n" + commandHandler.getDefaultPrefix() + (cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage());
					}
					else{
						otherCommands = otherCommands + "\n" + commandHandler.getDefaultPrefix() + (cmd.usage().isEmpty() ? cmd.aliases()[0] : cmd.usage());
					}
				}
			}
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
			eb.setDescription("Available Commands");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cmd.png");
			eb.addField("Player Control", playerControl, true);
			eb.addField("Lookup Commands", lookupCommands, true);
			eb.addField("Other Commands", otherCommands, true);
			eb.addField("To get information about 'command', use \"-help 'command'\", e.g. \"-help -help\"", builder.toString(), false);
			channel.sendMessage(eb.build()).queue();
		}
	}


	@Command(aliases = {"-info"}, description = "Shows some information about the bot.", usage = "-info")
	public void onInfoCommand(Guild guild, MessageChannel channel, User author){
		if (author.isBot()) return;
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
	public void onPingCommand(Guild guild, MessageChannel channel, User author){
		if (author.isBot()) return;
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
		eb.setDescription(bot.getPing() + "ms");
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
		channel.sendMessage(eb.build()).queue();
	}


	private class Sorter implements Comparator<SimpleCommand>
	{
		private final CommandHandler commandHandler;
		public Sorter(CommandHandler commandHandler) {
			this.commandHandler = commandHandler;
		}
		@Override
		public int compare(CommandHandler.SimpleCommand simpleCommand1, CommandHandler.SimpleCommand simpleCommand2) {
			Command cmd1 = simpleCommand1.getCommandAnnotation();
			Command cmd2 = simpleCommand2.getCommandAnnotation();
			return cmd1.aliases()[0].compareToIgnoreCase(cmd2.aliases()[0]);
		}
	}
}
