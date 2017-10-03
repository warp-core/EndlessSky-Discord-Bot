package bot;

import java.util.List;
import java.util.Random;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;

public class ModeratorCommands 
implements CommandExecutor{
	ESBot bot;

	public ModeratorCommands(ESBot bot){
		super();
		this.bot = bot;
	}

	@Command(aliases = {"-purge"}, description = "Deletes the last X messages in this channel.\nRange: 2 - 100.\n\nRequires the \"manage messages\" permission", usage = "-purge X", privateMessages = false)
	public void onPurgeCommand(Guild guild, Message msg, TextChannel channel, String[] args){
		Member author = msg.getGuild().getMember(msg.getAuthor());
		List<Permission> perm = author.getPermissions(channel);
		if(args.length == 1 && (perm.contains(Permission.MESSAGE_MANAGE) || perm.contains(Permission.ADMINISTRATOR) || author.isOwner())){
			int amount = 0;
			try{
				amount = Integer.valueOf(args[0]);
			}
			catch (NumberFormatException e){
				//Silently fail
			}
			if(amount <= 100 && amount >= 2){
				MessageHistory history = channel.getHistory();
				history.retrievePast(amount).queue(m -> {
					channel.deleteMessages(m).queue((x -> {
						EmbedBuilder eb = new EmbedBuilder();
						eb.setTitle("Moderation:", null);
						eb.setColor(guild.getMember(bot.getSelf()).getColor());
						eb.setDescription("Successfully thrown " + Integer.valueOf(args[0]) + " messages out of the airlock!");
						eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
						channel.sendMessage(eb.build()).queue();
					}));
				});
			}
		}
		else if(!(perm.contains(Permission.MESSAGE_MANAGE)
				|| perm.contains(Permission.ADMINISTRATOR) || author.isOwner())){
			channel.sendMessage(GetRandomDeniedMessage()).queue();
		}
	}



	// Return a message indicating that the requestor was not authorized.
	public static String GetRandomDeniedMessage(){
		String[] messageList = {
			"You can't order me around.",
			"I don't listen to you.",
			"You're not my boss.",
			"Try harder.",
			"You think you're a hotshot pirate?",
			"Your attempt at using 'Pug Magic' has failed.",
			"You're no Admiral Danforth.",
			"As if.",
			"That prison on Clink is looking rather empty...",
			"Oh yeah?",
			"Nice try.",
			"I may be old, but I'm not dumb.",
			"I'll pretend you didn't say that.",
			"Not today.",
			"Oh, to be young again...",
			"*yawn*",
			"I have the power. You don't.",
			"Go play in a hyperspace lane.",
			"How about I put *you* in the airlock?"
		};
		Random rGen = new Random();
		int choice = rGen.nextInt(messageList.length);
		return messageList[choice];
	}




	@Command(aliases = {"-update"}, description = "Reloads the memes and content of known GitHub files.", usage = "-update", privateMessages = true)
	public void onUpdateCommand(Message msg, TextChannel channel){
		Member author = msg.getGuild().getMember(msg.getAuthor());
		List<Permission> perm = author.getPermissions(channel);
		if(author.isOwner() || perm.contains(Permission.ADMINISTRATOR)){
			channel.sendMessage("Updating...").queue();
			bot.update();
			channel.sendMessage("Update finished").queue();
		}
		else
			channel.sendMessage(GetRandomDeniedMessage()).queue();
	}

}
