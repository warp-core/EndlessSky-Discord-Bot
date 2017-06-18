package bot;

import java.util.List;

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

	public ModeratorCommands(ESBot bot) {
		super();
		this.bot = bot;
	}

	@Command(aliases = {"-purge"}, description = "Deletes the last X messages in this channel. Minimum: 2 Maximum: 100.Requires the \"manage messages\" permission", usage = "-purge X", privateMessages = false)
	public void onPurgeCommand(Guild guild, Message msg, TextChannel channel, String[] args){
		Member author = msg.getGuild().getMember(msg.getAuthor());
		List<Permission> perm = author.getPermissions(channel);
		if(args.length == 1 && (perm.contains(Permission.MESSAGE_MANAGE) || perm.contains(Permission.ADMINISTRATOR) || author.isOwner())){
			int amount = 0;
			try{
				amount = Integer.valueOf(args[0]);
			} catch (NumberFormatException e){
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
						eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/cross.png");
						channel.sendMessage(eb.build()).queue();
					}));
				});
			}
		}
	}

	/*
	@Command(aliases = {"-update"}, description = "Updates the data,memes and all other not-source-code changes.", usage = "-update", privateMessages = true)
	public void onUpdateCommand(Message msg, TextChannel channel){
		Member author = msg.getGuild().getMember(msg.getAuthor());
		List<Permission> perm = author.getPermissions(channel);
		if( perm.contains(Permission.ADMINISTRATOR) || author.isOwner()){
			channel.sendMessage("Updating...").queue();
			bot.update();
			channel.sendMessage("Update finished").queue();
		}
	}
	*/
}