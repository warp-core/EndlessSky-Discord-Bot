package bot;

import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.GuildController;

public class ModeratorCommands 
implements CommandExecutor{
	ESBot bot;

	public ModeratorCommands(ESBot bot){
		super();
		this.bot = bot;
	}

	@Command(aliases = {"-purge"}, description = "Deletes the last X messages in this channel.\nRange: 2 - 100.\n\nRequires the \"manage messages\" permission", usage = "-purge X", privateMessages = false)
	public void onPurgeCommand(Guild guild, Message msg, TextChannel channel, String[] args){
		if (msg.getAuthor().isBot())
			return;
		Member author = guild.getMember(msg.getAuthor());
		// Always remove the requesting message, and delete many after it.
		msg.delete().queue( y -> {
			if(!Helper.CanModerate(channel, author))
				channel.sendMessage(Helper.GetRandomDeniedMessage()).queue();
			else if(args.length == 1 && Helper.IsIntegerInRange(args[0], 2, 100)){
				int amount = Integer.valueOf(args[0]);
				channel.getHistory().retrievePast(amount).queue( m -> {
					channel.deleteMessages(m).queue( x -> {
						EmbedBuilder eb = new EmbedBuilder();
						eb.setTitle("Moderation:");
						eb.setColor(guild.getMember(bot.getSelf()).getColor());
						eb.setDescription("Spaced " + m.size() + " messages! Who's next?!");
						eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
						channel.sendMessage(eb.build()).queue();
						TextChannel modLog = guild.getTextChannelsByName("mod-log", false).get(0);
						modLog.sendMessage("Purged " + amount + " messages in " + channel.getAsMention() + ", ordered by `" + author.getEffectiveName() + "`.").queue();
					});
				});
			}
		});
	}



	@Command(aliases = {"-move", "-wormhole"}, description = "Moves the last X messages in this channel to the linked channel. Can also send participants to the gulag.\nX: Message count to move\nRange: 2 - 100.\n\nY: Total time in the gulag\nRange: 0 - 86400, optional.\n\nRequires moderation abilities.", usage = "-move X #room-name Y", privateMessages = false)
	public void onMoveCommand(Guild guild, Message msg, TextChannel channel, String[] args){
		if (msg.getAuthor().isBot())
			return;
		final String moveHeader = "Incoming wormhole content:\n```";
		final String moveFooter = "```";
		if(!Helper.CanModerate(channel, msg.getGuild().getMember(msg.getAuthor()))){
			msg.delete().queue();
		}
		else{
			if(args.length < 2
					|| !Helper.IsIntegerInRange(args[0], 2, 100)
					|| msg.getMentionedChannels().size() != 1
					|| !Helper.IsDiffAndWritable(channel, msg.getMentionedChannels().get(0))){
				msg.delete().queue();
			}
			// Act on the valid channel.
			else{
				int n = Integer.valueOf(args[0]);
				// Allow custom banlengths, including 0-length.
				int banLength = (args.length > 2
						&& Helper.IsIntegerInRange(args[2], 0, 86400))
								? Integer.valueOf(args[2]) : 0;
				TextChannel dest = msg.getMentionedChannels().get(0);
				// Always delete the requesting message.
				msg.delete().complete();
				// Use a lambda to asynchronously perform this request:
				channel.getHistory().retrievePast(n).queue( toDelete -> {
					if(toDelete.isEmpty())
						return;
					HashSet<Member> toTempBan = new HashSet<Member>();
					LinkedList<String> toMove = new LinkedList<String>();
					for(Message m : toDelete){
						Member author = guild.getMember(m.getAuthor());
						if(Helper.isBannable(channel, author))
							toTempBan.add(author);
						String what = m.getStrippedContent().trim();
						if(what.isEmpty())
							continue;
						toMove.addFirst(m.getCreationTime()
								.format(DateTimeFormatter.ISO_INSTANT).substring(11, 19)
								+ "Z " + author.getEffectiveName() + ": " + what +"\n"
						);
					}
					// Remove the messages from the original channel and log the move.
					channel.deleteMessages(toDelete).queue( x -> {
						EmbedBuilder log = new EmbedBuilder();
						log.setDescription(dest.getAsMention());
						log.setThumbnail("https://cdn.discordapp.com/emojis/344684586904584202.png");
						log.appendDescription("\n(" + toMove.size() + " messages await)");
						if(toDelete.size() - toMove.size() > 0)
							log.appendDescription("\n(Some embeds were eaten)");
						channel.sendMessage(log.build()).queue();
					});

					// Transport the message content to the new channel.
					if(!toMove.isEmpty())
						Helper.writeChunks(dest, toMove, moveHeader, moveFooter);

					// Place the temporary bans.
					if(!toTempBan.isEmpty() && banLength > 0){
						Helper.EnsureRole(guild, Helper.ROLE_GULAG);
						List<Role> gulag = guild.getRolesByName(Helper.ROLE_GULAG, true);
						GuildController gc = guild.getController();
						for(Member b : toTempBan)
							temporaryGulag(gc, b, gulag, banLength);
					}
				});
			}
		}
	}



	@Command(aliases = {"-gulag"}, description = "Sends the mentioned member to the gulag.\nX: Total time in the gulag\nRange: 1 - 86400\n\nRequires moderation and role change abilities.", usage = "-gulag @member X", privateMessages = true)
	public void onGulagCommand(Guild guild, TextChannel channel, Message msg, String[] args){
		if (msg.getAuthor().isBot())
			return;
		if(!Helper.CanModAndRoleChange(channel, msg.getGuild().getMember(msg.getAuthor())))
			channel.sendMessage(Helper.GetRandomDeniedMessage()).queue();
		else if(args.length >= 2 && Helper.IsIntegerInRange(args[1], 1, 86400)){
			// Ban the mentioned user.
			Member toBan = guild.getMember(msg.getMentionedUsers().get(0));
			int banLength = Math.max(1, Integer.valueOf(args[1]));
			Helper.EnsureRole(guild, Helper.ROLE_GULAG);
			temporaryGulag(guild, toBan, Helper.ROLE_GULAG, banLength);
			String message = "Gulagged: `" + toBan.getEffectiveName() + "` for " + banLength + " seconds.";
			if(args.length > 2){
				message += "\nReason: ";
				for(int i = 2; i < args.length - 1; i++){
					message += (args[i] + " ");
				}
				message += args[args.length - 1] + ".";
			}
			TextChannel modLog = guild.getTextChannelsByName("mod-log", false).get(0);
			modLog.sendMessage(message).queue();
		}
	}



	@Command(aliases = {"-update"}, description = "Reloads the memes and content of known GitHub files.", usage = "-update", privateMessages = true)
	public void onUpdateCommand(Message msg, TextChannel channel){
		if (msg.getAuthor().isBot())
			return;
		if(!Helper.CanModerate(channel, msg.getGuild().getMember(msg.getAuthor())))
			channel.sendMessage(Helper.GetRandomDeniedMessage()).queue();
		else{
			channel.sendMessage("Updating...").queue();
			bot.update();
			channel.sendMessage("Update finished").queue();
		}
	}



	/**
	 * Utility functions to send the given member to a timeout room by means of
	 * changing the assigned roles.
	 * TODO: The callback does not persist if the bot is destroyed before the
	 * duration is reached, so some more permanent storage is needed, and should
	 * be checked upon startup.
	 * @param Guild   guild        The server with a gulag and a naughty member.
	 * @param Member  member       The member who needs a time-out.
	 * @param String  newRoleName  The result of a name-search for the role.
	 * @param int     duration     The length of the time-out.
	 */
	private void temporaryGulag(Guild guild, Member member, String newRoleName, int duration){
		List<Role> newRole = guild.getRolesByName(newRoleName, true);
		List<Role> oldRoles = member.getRoles();
		GuildController gc = guild.getController();
		temporaryRoleSwapAfterDuration(gc, member, newRole, oldRoles, duration);
	}

	private void temporaryGulag(GuildController gc, Member member, List<Role> newRoles, int duration){
		List<Role> oldRoles = member.getRoles();
		temporaryRoleSwapAfterDuration(gc, member, newRoles, oldRoles, duration);
	}

	private void temporaryRoleSwapAfterDuration(GuildController gc, Member m, List<Role> newRoles, List<Role> oldRoles, int duration){
		gc.modifyMemberRoles(m, newRoles, oldRoles).queue( x -> {
			System.out.println(x);
			gc.modifyMemberRoles(m, oldRoles, newRoles).queueAfter(
					duration, TimeUnit.SECONDS);
		});
	}
}
