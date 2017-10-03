package bot;

import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
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
		Member author = guild.getMember(msg.getAuthor());
		// Always remove the requesting message.
		msg.delete().queue();
		if(!CanModerate(channel, author))
			channel.sendMessage(GetRandomDeniedMessage()).queue();
		else if(args.length == 1 && IsIntegerInRange(args[0], 2, 100)){
			int amount = Integer.valueOf(args[0]);
			channel.getHistory().retrievePast(amount).queue( m -> {
				channel.deleteMessages(m).queue( x -> {
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle("Moderation:");
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					eb.setDescription("Spaced " + m.size() + " messages! Who's next?!");
					eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
					channel.sendMessage(eb.build()).queue();
				});
			});
		}
	}



	@Command(aliases = {"-move", "-wormhole"}, description = "Moves the last X messages in this channel to the linked channel. Can also send participants to the gulag.\nX: Message count to move\nRange: 2 - 100.\n\nY: Total time in the gulag\nRange: 0 - 86400, optional.\n\nRequires moderation abilities.", usage = "-move X #room-name Y", privateMessages = false)
	public void onMoveCommand(Guild guild, Message msg, TextChannel channel, String[] args){
		final String moveHeader = "Incoming wormhole content:\n```";
		final String moveFooter = "```";
		if(!CanModerate(channel, msg.getGuild().getMember(msg.getAuthor()))){
			msg.delete().queue();
		}
		else{
			if(args.length < 2
					|| !IsIntegerInRange(args[0], 2, 100)
					|| msg.getMentionedChannels().size() != 1
					|| !IsValidChannel(channel, msg.getMentionedChannels().get(0))){
				msg.delete().queue();
			}
			// Act on the valid channel.
			else{
				int n = Integer.valueOf(args[0]);
				// Allow custom banlengths, including 0-length.
				int banLength = (args.length == 3 && IsIntegerInRange(args[2], 0, 86400)) ? Integer.valueOf(args[2]) : 0;
				TextChannel dest = msg.getMentionedChannels().get(0);
				// Always delete the requesting message.
				msg.delete().queue();
				// Acquire (in blocking fashion) the messages to move.
				List<Message> toMove = channel.getHistory().retrievePast(n).complete();
				HashSet<Member> toTempBan = new HashSet<Member>();
				LinkedList<String> moved = new LinkedList<String>();
				if(!toMove.isEmpty()){
					for(Message m : toMove){
						Member author = guild.getMember(m.getAuthor());
						// Do not ban any bots or moderators.
						if(!(m.getAuthor().isBot() || CanModerate(channel, author)))
							toTempBan.add(author);
						// Embeds are not moved. They are food for the wormhole.
						String what = m.getStrippedContent().trim();
						if(!what.isEmpty()){
							String by = author.getEffectiveName();
							String at = m.getCreationTime()
									.format(DateTimeFormatter.ISO_INSTANT).substring(11);
							moved.addFirst(at + " " + by + ": " + what + "\n");
						}
					}
					System.out.println("Writelist:\n" + moved);
					System.out.println("Banlist:\n" + toTempBan);

					// Remove the messages from the original channel.
					channel.deleteMessages(toMove).queue( x -> {
						EmbedBuilder log = new EmbedBuilder();
						log.setDescription(dest.getAsMention());
						log.setThumbnail("https://cdn.discordapp.com/emojis/344684586904584202.png");
						log.appendDescription("\n(" + moved.size() + " messages await)");
						if(toMove.size() - moved.size() > 0)
							log.appendDescription("\n(Some embeds were eaten)");
						channel.sendMessage(log.build()).queue();
					});
				}
				else
					System.out.println("No messages to delete.");

				// Transport the message content to the new channel.
				if(!moved.isEmpty()){
					StringBuilder chunk = new StringBuilder(moveHeader);
					int chunkSize = chunk.length() + moveFooter.length();
					final int sizeLimit = 1990;
					if(chunkSize > sizeLimit){
						System.out.println("Cannot ever print: header + footer too large.");
						return;
					}
					for(String str : moved){
						if(chunkSize + str.length() <= sizeLimit)
							chunk.append(str);
						else{
							dest.sendMessage(chunk.append(moveFooter).toString()).queue();
							chunk = new StringBuilder(moveHeader);
						}
						chunkSize = chunk.length() + moveFooter.length();
					}
					// Write the final chunk.
					if(chunk.length() > moveHeader.length())
						dest.sendMessage(chunk.append(moveFooter).toString()).queue();
				}
				else
					System.out.println("Writelist was empty.");

				// Place the temporary bans.
				if(!toTempBan.isEmpty() && banLength > 0){
					final String gulagRoleName = "Bad Boy/Girl";
					if(guild.getRolesByName(gulagRoleName, true).size() == 0){
						guild.getController().createRole().queue( r -> {
							r.getManager().setName(gulagRoleName).queue();
						});
					}
					List<Role> gulag = guild.getRolesByName(gulagRoleName, true);
					GuildController gc = guild.getController();
					Map<Member, List<Role>> oldRole = new HashMap<Member, List<Role>>();
					for(Member b : toTempBan){
						oldRole.put(b, b.getRoles());
						gc.modifyMemberRoles(b, gulag, oldRole.get(b)).queue( x -> {
							// Lambda to restore the user's roles after the cooldown.
							gc.modifyMemberRoles(b, oldRole.get(b), gulag).queueAfter(
									banLength, TimeUnit.SECONDS);
						});
					}
				}
				else
					System.out.println("Banlist was empty.");
			}
		}
	}


	/**
	 * Parse the input string to determine if it is a valid integer in the
	 * range of min and max.
	 * @param  String input         The input string
	 * @param  int    min           Minimum acceptable value
	 * @param  int    max           Maximum acceptable value
	 * @return        true / false depending on if number and in range.
	 */
	private static boolean IsIntegerInRange(String input, int min, int max){
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		try{
			int amount = Integer.valueOf(input);
			if(amount >= low && amount <= high)
				return true;
		}
		catch (NumberFormatException e){
			return false;
		}
		return false;
	}



	/**
	 * Determine if the input string is a valid channel that is different
	 * from the origin channel, and in which the bot can post.
	 * @param  TextChannel origin    The channel from which the command came.
	 * @param  TextChannel input     The passed "channel" to test.
	 * @return             true / false depending if a channel and also different.
	 */
	private static boolean IsValidChannel(TextChannel origin, TextChannel dest){
		// Compare the channels' string IDs.
		if(origin.getId().equals(dest.getId()))
			return false;
		
		// Ensure the bot is allowed to read & write in the destination.
		if(!dest.canTalk())
			return false;
		
		return true;
	}



	// Return a message indicating that the requestor was not authorized.
	public static String GetRandomDeniedMessage(){
		final String[] messageList = {
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


	/**
	 * Indicates if the given member has the rights to manage messages, the channel, or otherwise administrate the channel.
	 * @param  TextChannel channel       The channel to moderate.
	 * @param  Member      member        The member who is/is not a moderator.
	 * @return             true / false if the member is a mod of that channel.
	 */
	public static boolean CanModerate(TextChannel channel, Member member){
		if(member.isOwner())
			return true;
		
		List<Permission> p = member.getPermissions(channel);
		if(p.contains(Permission.ADMINISTRATOR)
				|| p.contains(Permission.MESSAGE_MANAGE)
				|| p.contains(Permission.MANAGE_CHANNEL))
			return true;
		
		return false;
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
