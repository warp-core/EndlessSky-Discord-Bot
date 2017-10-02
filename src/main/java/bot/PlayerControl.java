package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PlayerControl
implements CommandExecutor{

	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private Map<String, PlayerVoteHandler> voteHandlers;
	private ESBot bot;



	public PlayerControl(ESBot bot, JDA jda){
		this.bot = bot;
		this.musicManagers = new HashMap<>();
		new AudioTimeoutControl(musicManagers,jda);
		this.playerManager = new DefaultAudioPlayerManager();
		this.voteHandlers = new HashMap<String, PlayerVoteHandler>();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
	}



	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild){
		long guildId = Long.parseLong(guild.getId());
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if(musicManager == null){
			musicManager = new GuildMusicManager(playerManager, this, guild);
			musicManagers.put(guildId, musicManager);
		}
		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
		return musicManager;
	}



	@Command(aliases = {"-play"}, description = "Use to request a song while in a voicechannel. If no url is given, it will perform a search on youtube with the given words.", usage = "-play URL", privateMessages = false)
	public void onPlayCommand(Guild guild, TextChannel channel, String[] args, User author, Message msg){
		Member requester = guild.getMember(author);
		if(guild != null && args.length > 0
				&& requester.getVoiceState().getChannel() != null
				&& !(requester.getRoles().containsAll(guild.getRolesByName("Anti-DJ", true)))) {
			checkVoiceChannel(guild.getAudioManager(), guild.getMember(author));
			for (String query : normalize(args))
				loadAndPlay(guild, channel, query, requester);
			msg.delete().queue();
		}
	}



	private String[] normalize(String[] args){
		if(args.length == 1){
			return args;
		}
		else{
			// set up an array of strings with a length equal to the amount of songs requested (separated by commata)
			int counter = 0;
			for(String s : args) {
				if(s.endsWith(",")) {
					counter++; } }
			String[] output = new String[counter + 1];

			int i = 0;
			for(String s : args){
				boolean newQuery = false;
				if (s.endsWith(",")){
					newQuery = true;
					s = s.substring(0,s.length() - 1);
				}
				if (output[i] == null)
					output[i] = "ytsearch: " + s;
				else
					output[i] += " " + s;

				if (newQuery)
					i++;
			}
			return output;
		}
	}



	@Command(aliases = {"-skip"}, description = "Skip the current song and start the next one in the queue.\n\nRequires the \"DJ\" role, or a vote will be started.", usage = "-skip, -skip #", privateMessages = false)
	public void onSkipCommand(Guild guild, TextChannel channel, User author, Message msg){
		String countStr = msg.getRawContent().indexOf(" ") < 0 ? ""
				: msg.getRawContent().substring(msg.getRawContent().indexOf(" ")).trim();
		int count = countStr.length() == 0 ? 1 : new Integer(countStr).intValue();
		Member requester = guild.getMember(author);
		PlayerVoteHandler voteHandler = getVoteHandler(guild, "skip");
		if((!(guild.getAudioManager().isAttemptingToConnect()
				|| guild.getAudioManager().isConnected())
				|| (requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel()))
				&& (guild != null)){
					if(hasDJPerms(requester, channel, guild)){
						skipTrack(channel, requester, count);
						voteHandler.clear();
						msg.delete().queue();
					}
					else if (vote(voteHandler, requester, channel, "skip")){
						skipTrack(channel, voteHandler.getRequester(), count);
					}
		}
	}



	@Command(aliases = {"-current"}, description = "Displays the current audiotrack.", usage = "-current", privateMessages = false)
	public void onCurrentCommand(Guild guild, TextChannel channel, Message msg){
		GuildMusicManager mng = getGuildAudioPlayer(guild);
		AudioPlayer player = mng.player;
		AudioTrack currentTrack = player.getPlayingTrack();
		if(currentTrack != null){
			String title = currentTrack.getInfo().title;
			String position = getTimestamp(currentTrack.getPosition());
			String duration = getTimestamp(currentTrack.getDuration());

			String nowplaying = String.format("**Playing:** %s\n**Time:** [%s / %s]",
					title, position, duration);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription(nowplaying);
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
		}
		else{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription("The player is not currently playing anything!");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
		}
		msg.delete().queue();
	}



	@Command(aliases = {"-queue"}, description = "Displays the current queue.", usage = "-queue", privateMessages = false)
	public void onqueueCommand(Guild guild, TextChannel channel, Message msg){
		GuildMusicManager mng = getGuildAudioPlayer(guild);
		TrackScheduler scheduler = mng.scheduler;
		LinkedList<AudioTrack> queue = scheduler.getQueue();
		if(!queue.isEmpty()){
			int trackCount = 0;
			long queueLength = 0;
			StringBuilder sb = new StringBuilder();
			sb.append("Current Queue: Entries: ").append(queue.size()).append("\n");
			for(AudioTrack track : queue){
				queueLength += track.getDuration();
				if(trackCount < 10){
					sb.append("`[").append(getTimestamp(track.getDuration())).append("]` ");
					sb.append(track.getInfo().title).append("\n");
					++trackCount;
				}
			}
			sb.append("\n").append("Total Queue Time Length: ").append(getTimestamp(queueLength));
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription(sb.toString());
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
			msg.delete().queue();
		}
	}



	@Command(aliases = {"-shuffle"}, description = "Shuffle the queue.\n\nRequires the \"DJ\" role, or starts a vote.", usage = "-shuffle", privateMessages = false)
	public void onShuffleCommand(Guild guild, TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		PlayerVoteHandler voteHandler = getVoteHandler(guild, "shuffle");
		if(!guild.getAudioManager().isAttemptingToConnect()
				|| guild.getAudioManager().isConnected()){
			if(hasDJPerms(requester, channel, guild)){
				shuffle(guild, requester, msg, channel);
				voteHandler.clear();
			}
			else if(vote(voteHandler, requester, channel, "shuffle"))
				shuffle(guild, voteHandler.getRequester(), msg, channel);
		}
	}




	@Command(aliases = {"-stop"}, description = "Stop the music, clear the queue, and disconnect the bot from the channel.\n\nRequires the \"DJ\" role, or starts a vote.", usage = "-stop", privateMessages = false)
	public void onStopCommand(Guild guild, TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		PlayerVoteHandler voteHandler = getVoteHandler(guild, "stop");
		if(hasDJPerms(requester, channel, guild)){
			stopPlayback(guild, requester, msg, channel);
			voteHandler.clear();
		}
		else if(vote(voteHandler, requester, channel, "stop"))
			stopPlayback(guild, requester, msg, channel);
	}



	@Command(aliases = {"-playban"}, description = "Ban or unban a user from requesting songs via '-play'.\n\nRequires the \"DJ\" role.", usage = "-playban @mention", privateMessages = false)
	public void onPlaybanCommand(Guild guild, TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		String by = " by `" + requester.getEffectiveName() + "`.";
		List<Permission> perm = requester.getPermissions(channel);
		if(requester.getRoles().containsAll(guild.getRolesByName("DJ", true))
				|| perm.contains(Permission.ADMINISTRATOR)
				|| requester.isOwner()){
			List<User> banned = msg.getMentionedUsers();
			if(guild.getRolesByName("Anti-DJ", true).size() == 0){
				guild.getController().createRole().queue( r -> {
					r.getManager().setName("Anti-DJ").queue();
				});
			}
			for(User u : banned){
				Member m = guild.getMember(u);
				if(!m.getRoles().containsAll(guild.getRolesByName("Anti-DJ", true))){
					guild.getController().addRolesToMember(m, guild.getRolesByName("Anti-DJ", true)).queue();
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					eb.setDescription(m.getEffectiveName() + " has been banned from requesting songs" + by);
					eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
					channel.sendMessage(eb.build()).queue();
				}
				else{
					guild.getController().removeRolesFromMember(m, guild.getRolesByName("Anti-DJ", true)).queue();
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					eb.setDescription(m.getEffectiveName() + " has been un-banned from requesting songs" + by);
					eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/check.png");
					channel.sendMessage(eb.build()).queue();
				}
			}
		}
	}



	@Command(aliases = {"-pause"}, description = "Pause the music player.\n\nRequires the \"DJ\" role.", usage = "-pause", privateMessages = false)
	public void onPauseCommand(Guild guild, TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		PlayerVoteHandler voteHandler = getVoteHandler(guild, "pause");
		List<Permission> perm = requester.getPermissions(channel);
		if(!(guild.getAudioManager().isAttemptingToConnect()
			|| guild.getAudioManager().isConnected())
			|| (requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel())){
			if(hasDJPerms(requester, channel, guild)){
				voteHandler.clear();
				pause(guild, requester, msg, channel);
			}
			else if (vote(voteHandler, requester, channel, "pause"))
				pause(guild, voteHandler.getRequester(), msg, channel);
		}
	}



	@Command(aliases = {"-resume"}, description = "Un-pause the music player.\n\nRequires the \"DJ\" role.", usage = "-resume", privateMessages = false)
	public void onResumeCommand(Guild guild, TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		PlayerVoteHandler voteHandler = getVoteHandler(guild, "resume");
		List<Permission> perm = requester.getPermissions(channel);
		if(!(guild.getAudioManager().isAttemptingToConnect()
			|| guild.getAudioManager().isConnected())
			|| (requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel())){
			if(hasDJPerms(requester, channel, guild)){
				voteHandler.clear();
				resume(guild, requester, msg, channel);
			}
			else if (vote(voteHandler, requester, channel, "resume"))
				resume(guild, voteHandler.getRequester(), msg, channel);
		}
	}



	@Command(aliases = {"-volume"}, description = "Displays the current volume or sets it to X (10-100). To change, the \"DJ\" role is required.", usage = "-volume X", privateMessages = false)
	public void onVolumeCommand(Guild guild, TextChannel channel, User author, String[] args, Message msg){
		Member requester = guild.getMember(author);
		String by = " by `" + requester.getEffectiveName() + "`.";
		List<Permission> perm = requester.getPermissions(channel);
		if(args.length == 1
				&& (!(guild.getAudioManager().isAttemptingToConnect() || guild.getAudioManager().isConnected())
					|| (requester.getVoiceState().getChannel() ==  guild.getAudioManager().getConnectedChannel()))
				&& (requester.getRoles().containsAll(guild.getRolesByName("DJ", true))
					|| perm.contains(Permission.ADMINISTRATOR)
					|| requester.isOwner())){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			try{
				int newVolume = Math.max(10, Math.min(100, Integer.parseInt(args[0])));
				int oldVolume = player.getVolume();
				player.setVolume(newVolume);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("Player volume changed from `" + oldVolume + "` to `" + newVolume + "`" + by);
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/volume.png");
				channel.sendMessage(eb.build()).queue();
				msg.delete().queue();
			}
			catch(NumberFormatException e){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("`" + args[0] + "` is not a valid integer. (10 - 100)");
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/volume.png");
				channel.sendMessage(eb.build()).queue();
			}
		}
		else{
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setDescription("Current player volume: **" + player.getVolume() + "**");
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/volume.png");
			channel.sendMessage(eb.build()).queue();
			msg.delete().queue();
		}
	}



	private void loadAndPlay(Guild guild, final TextChannel channel, final String trackUrl, final Member requester){
		GuildMusicManager musicManager = getGuildAudioPlayer(guild);

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler(){

		final String requestedby = "\n(requested by `" + requester.getEffectiveName() + "`)";

			@Override
			public void trackLoaded(AudioTrack track){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Adding to queue `" + track.getInfo().title + "`" + requestedby);
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/play.png");
				channel.sendMessage(eb.build()).queue();

				play(guild, musicManager, track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist){
				AudioTrack firstTrack = playlist.getSelectedTrack();

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/play.png");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());

				if (playlist.isSearchResult()){
					if(firstTrack == null)
						firstTrack = playlist.getTracks().get(0);
					eb.setDescription("Adding to queue `" + firstTrack.getInfo().title + "` (first track of `" + playlist.getName() + "`)" + requestedby);
					play(channel.getGuild(), musicManager, firstTrack);
				}
				else{
					int counter = 0;
					for (AudioTrack track : playlist.getTracks()){
						play(channel.getGuild(), musicManager, track);
						counter ++;
					}
					eb.setDescription("Adding to queue playlist `" + playlist.getName() + "` (" + counter + " tracks)" + requestedby);

				}
				channel.sendMessage(eb.build()).queue();
			}

			@Override
			public void noMatches(){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Nothing found by `" + trackUrl + "`" + requestedby);
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/cross.png");
				channel.sendMessage(eb.build()).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Could not play: `" + exception.getMessage() + "`" + requestedby);
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/info.png");
				channel.sendMessage(eb.build()).queue();
			}
		});
	}



	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track){
		musicManager.scheduler.queue(track);
	}



	private void skipTrack(TextChannel channel, Member requester, int count){
		String requestedby = "";
		if (requester != null)
			requestedby = "\n(requested by `" + requester.getEffectiveName() + "`)";

		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		for(int i = 0; i<count; ++i)
			musicManager.scheduler.nextTrack();
		
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
		eb.setDescription("Skipped " + (count == 1 ? "to next track."
				: "ahead " + count + " tracks.") + requestedby);
		eb.setColor(channel.getGuild().getMember(bot.getSelf()).getColor());
		eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/skip.png");
		channel.sendMessage(eb.build()).queue();
	}


	private void shuffle(Guild guild, Member requester, Message msg, TextChannel channel){
		TrackScheduler scheduler = getGuildAudioPlayer(guild).scheduler;
		if(!scheduler.getQueue().isEmpty()){
			scheduler.shuffle();
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription("The queue has been shuffled by `" + requester.getEffectiveName() + "`!");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/shuffle.png");
			channel.sendMessage(eb.build()).queue();
			msg.delete().queue();
		}
	}



 	private void pause(Guild guild, Member requester, Message msg, TextChannel channel){
		String requestedby = "\n(requested by `" + requester.getEffectiveName() + "`)";
		AudioPlayer player = getGuildAudioPlayer(guild).player;
		if(!(player.getPlayingTrack() == null)){
			player.setPaused(true);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setDescription("The audio-player has been paused." + requestedby);
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/pause.png");
			channel.sendMessage(eb.build()).queue();
			msg.delete().queue();
		}
	}



	private void resume(Guild guild, Member requester, Message msg, TextChannel channel)
	{
		String requestedby = "\n(requested by `" + requester.getEffectiveName() + "`)";
		AudioPlayer player = getGuildAudioPlayer(guild).player;
		if(!(player.getPlayingTrack() == null)){
			player.setPaused(false);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setDescription("The audio-player has been unpaused." + requestedby);
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/play.png");
			channel.sendMessage(eb.build()).queue();
			msg.delete().queue();
		}
	}



	private void stopPlayback(Guild guild, Member requester, Message msg, TextChannel channel)
	{
		String requestedby = "\n(requested by `" + requester.getEffectiveName() + "`)";
		GuildMusicManager mng = getGuildAudioPlayer(guild);
		AudioPlayer player = mng.player;
		TrackScheduler scheduler = mng.scheduler;
		scheduler.getQueue().clear();
		player.stopTrack();
		player.setPaused(false);
		guild.getAudioManager().closeAudioConnection();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
		eb.setDescription("Playback has been completely stopped and the queue has been cleared." + requestedby);
		eb.setColor(guild.getMember(bot.getSelf()).getColor());
		eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/stop.png");
		channel.sendMessage(eb.build()).queue();
	}



	private static String getTimestamp(long milliseconds){
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
		int hours   = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

		if(hours > 0)
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		else
			return String.format("%02d:%02d", minutes, seconds);
	}



	private static void checkVoiceChannel(AudioManager audioManager, Member request){
		if(!audioManager.isConnected() && !audioManager.isAttemptingToConnect() && request.getVoiceState().getChannel()!=null ){
			audioManager.openAudioConnection(request.getVoiceState().getChannel());
		}
	}



	private boolean hasDJPerms(Member member, TextChannel channel, Guild guild)
	{
		if (member.getRoles().containsAll(guild.getRolesByName("DJ", true))
			|| member.getPermissions(channel).contains(Permission.ADMINISTRATOR)
			|| member.isOwner())
			return true;
		else
			return false;
	}



	public void onNextTrack(Guild guild)
	{
		getVoteHandler(guild, "skip").clear();
	}



	public synchronized boolean vote(PlayerVoteHandler handler, Member requester, TextChannel channel, String subject)
	{
		handler.vote(requester);
		if (handler.checkVotes())
			return true;
		else{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setColor(channel.getGuild().getMember(bot.getSelf()).getColor());
			eb.setDescription("Currently are " + handler.getVotes() + " captains voting to " + subject + ", but " + handler.getRequiredVotes() + " are needed to " + subject + "!");
			eb.setThumbnail(bot.HOST_RAW_URL + "/thumbnails/vote.png");
			channel.sendMessage(eb.build()).queue();
			return false;
		}
	}



	public synchronized PlayerVoteHandler getVoteHandler(Guild guild, String key){
		if(voteHandlers.containsKey(key))
			return voteHandlers.get(key);
		else{
			PlayerVoteHandler handler = new PlayerVoteHandler(guild);
			voteHandlers.put(key, handler);
			return handler;
		}
	}
}
