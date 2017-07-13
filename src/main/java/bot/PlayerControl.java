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
implements CommandExecutor {

	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private Map<Long,LinkedList<Member>> skipvoters;
	private ESBot bot;
	
	public PlayerControl(ESBot bot, JDA jda) {	
		this.bot = bot;
		this.musicManagers = new HashMap<>();
		new AudioTimeoutControl(musicManagers,jda);
		this.playerManager = new DefaultAudioPlayerManager();
		this.skipvoters = new HashMap<>();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
	}

	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager);
			musicManagers.put(guildId, musicManager);
		}
		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
		return musicManager;
	}

	private synchronized LinkedList<Member> getGuildSkipvoters(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
		LinkedList<Member> sk = skipvoters.get(guildId);

		if (sk == null) {
			sk = new LinkedList<>();
			skipvoters.put(guildId, sk);
		}
		return sk;
	}

	@Command(aliases = {"-play"}, description = "Use to request a song while in a voicechannel. If no url is given, it will perform a search on youtube with the given words.", usage = "-play URL", privateMessages = false)
	public void onPlayCommand(Guild guild,TextChannel channel, String[] args,User author){
		Member requester = guild.getMember(author);
		if (guild != null && args.length > 0 && requester.getVoiceState().getChannel() != null && !(requester.getRoles().containsAll(guild.getRolesByName("Anti-DJ", true)))) {		
			checkVoiceChannel(guild.getAudioManager(), guild.getMember(author));
			loadAndPlay(channel, normalize(args));
		} 
	}

	private String normalize(String[] args) {
		if(args.length == 1){
			return args[0];
		}
		else{
			String output = "ytsearch:";
			for(String s: args){
				output += " " + s;
			}
			return output;
		}
	}

	@Command(aliases = {"-skip"}, description = "Skip current song to start the next one. Requires the \"DJ\" role.", usage = "-skip", privateMessages = false)
	public void onSkipCommand(Guild guild,TextChannel channel, User author){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if((!(guild.getAudioManager().isAttemptingToConnect()||guild.getAudioManager().isConnected())||(requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel()))&&(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner())){
			if (guild != null) {
				skipTrack(channel);
				skipvoters.clear();
			} 
		}
	}
	
	@Command(aliases = {"-current"}, description = "Displays the current autiotrack.", usage = "-current", privateMessages = false)
	public void onCurrentCommand(Guild guild, TextChannel channel){
		GuildMusicManager mng = getGuildAudioPlayer(guild);
		AudioPlayer player = mng.player;
		AudioTrack currentTrack = player.getPlayingTrack();
		if (currentTrack != null){
			String title = currentTrack.getInfo().title;
			String position = getTimestamp(currentTrack.getPosition());
			String duration = getTimestamp(currentTrack.getDuration());

			String nowplaying = String.format("**Playing:** %s\n**Time:** [%s / %s]",
					title, position, duration);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription(nowplaying);
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
		}
		else{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription("The player is not currently playing anything!");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
		}
	}

	@Command(aliases = {"-playlist"}, description = "Displays the current playlist.", usage = "-playlist", privateMessages = false)
	public void onplaylistCommand(Guild guild,TextChannel channel){
		GuildMusicManager mng = getGuildAudioPlayer(guild);
		TrackScheduler scheduler = mng.scheduler;
		LinkedList<AudioTrack> queue = scheduler.getQueue();
		if(!queue.isEmpty()){
			int trackCount = 0;
			long queueLength = 0;
			StringBuilder sb = new StringBuilder();
			sb.append("Current Queue: Entries: ").append(queue.size()).append("\n");
			for (AudioTrack track : queue){
				queueLength += track.getDuration();
				if (trackCount < 10){
					sb.append("`[").append(getTimestamp(track.getDuration())).append("]` ");
					sb.append(track.getInfo().title).append("\n");
					trackCount++;
				}
			}
			sb.append("\n").append("Total Queue Time Length: ").append(getTimestamp(queueLength));
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription(sb.toString());
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/info.png");
			channel.sendMessage(eb.build()).queue();
		}
	}

	@Command(aliases = {"-shuffle"}, description = "Shuffle the playlist. Requires the \"DJ\" role.", usage = "-shuffle", privateMessages = false)
	public void onShuffleCommand(Guild guild,TextChannel channel, User author){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if(!(guild.getAudioManager().isAttemptingToConnect()||guild.getAudioManager().isConnected())||(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner())){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			TrackScheduler scheduler = mng.scheduler;
			if(!scheduler.getQueue().isEmpty()){
				scheduler.shuffle();
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("The queue has been shuffled!");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/shuffle.png");
				channel.sendMessage(eb.build()).queue();
			}
		}
	}

	@Command(aliases = {"-stop"}, description = "Stop the music, clean playlist and disconnect the bot from the channel. Requires the \"DJ\" role.", usage = "-stop", privateMessages = false)
	public void onStopCommand(Guild guild,TextChannel channel,User author){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if(requester.getRoles().containsAll(guild.getRolesByName("DJ",true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner()){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			TrackScheduler scheduler = mng.scheduler;
			scheduler.getQueue().clear();
			player.stopTrack();
			player.setPaused(false);
			guild.getAudioManager().closeAudioConnection();
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setDescription("Playback has been completely stopped and the queue has been cleared.");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/stop.png");
			channel.sendMessage(eb.build()).queue();
			System.out.println("AXAXAXAX");
		}
	}

	@Command(aliases = {"-voteskip"}, description = "Start a vote/Vote to skip the current song. Needs 50% in the voicechannel to do the same to skip it.", usage = "-voteskip", privateMessages = false)
	public synchronized void onVoteskipCommand(Guild guild,TextChannel channel, User author){
		Member skipvoter = guild.getMember(author);
		LinkedList<Member> guildVoters = getGuildSkipvoters(guild);
		if(!guildVoters.contains(skipvoter)){
			guildVoters.add(skipvoter);
		}
		LinkedList<Member> temp = new LinkedList<>();
		for(Member m: guildVoters){
			if(guild.getAudioManager().getConnectedChannel() == m.getVoiceState().getChannel()){
				temp.add(m);
			}
		}
		guildVoters = temp;		
		if((int)((guild.getAudioManager().getConnectedChannel().getMembers().size()-1)/2+0.5) <= guildVoters.size()){
			skipTrack(channel);
			guildVoters.clear();
			skipvoters.remove(Long.parseLong(guild.getId()));
		}
		else{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			eb.setDescription("Currently are " + guildVoters.size() + " captains voting to skip, but " + (int)((guild.getAudioManager().getConnectedChannel().getMembers().size()-1)/2+0.5) + " are needed to skip!");
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/voteskip.png");
			channel.sendMessage(eb.build()).queue();
		}
		skipvoters.put(Long.parseLong(guild.getId()), guildVoters);

	}

	@Command(aliases = {"-playban"}, description = "Ban or unban a user from requesting songs via -play. Requires the \"DJ\" role.", usage = "-playban @mention", privateMessages = false)
	public synchronized void onPlaybanCommand(Guild guild,TextChannel channel, User author, Message msg){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner()){
			List<User> banned = msg.getMentionedUsers();
			if(guild.getRolesByName("Anti-DJ", true).size() == 0){
				guild.getController().createRole().queue( r -> {
					r.getManager().setName("Anti-DJ").queue();
				});
			}
			for(User u:banned){
				Member m = guild.getMember(u);
				if(!m.getRoles().containsAll(guild.getRolesByName("Anti-DJ", true))){
					guild.getController().addRolesToMember(m, guild.getRolesByName("Anti-DJ", true)).queue();
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					eb.setDescription(m.getEffectiveName() + " has been banned from requesting songs.");
					eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/cross.png");
					channel.sendMessage(eb.build()).queue();
				}else{
					guild.getController().removeRolesFromMember(m, guild.getRolesByName("Anti-DJ", true)).queue();
					EmbedBuilder eb = new EmbedBuilder();
					eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
					eb.setColor(guild.getMember(bot.getSelf()).getColor());
					eb.setDescription(m.getEffectiveName() + " has been un-banned from requesting songs.");
					eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/check.png");
					channel.sendMessage(eb.build()).queue();
				}
			}
		}
	}

	@Command(aliases = {"-pause"}, description = "Pauses the player. Requires the \"DJ\" role.", usage = "-pause", privateMessages = false)
	public void onPauseCommand(Guild guild,TextChannel channel, User author){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if((!(guild.getAudioManager().isAttemptingToConnect()||guild.getAudioManager().isConnected())||(requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel()))&&(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner())){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			if (!(player.getPlayingTrack() == null)){
				player.setPaused(true);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("The audio-player has been paused.");
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/pause.png");
				channel.sendMessage(eb.build()).queue();
			}
		}
	}

	@Command(aliases = {"-resume"}, description = "Un-pauses the player. Requires the \"DJ\" role.", usage = "-resume", privateMessages = false)
	public void onResumeCommand(Guild guild,TextChannel channel, User author){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if((!(guild.getAudioManager().isAttemptingToConnect()||guild.getAudioManager().isConnected())||(requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel()))&&(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner())){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			if (!(player.getPlayingTrack() == null)){
				player.setPaused(false);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("The audio-player has been unpaused.");
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/play.png");
				channel.sendMessage(eb.build()).queue();
			}
		}
	}

	@Command(aliases = {"-volume"}, description = "Displays the current volume or sets it to X (10-100). To change it the \"DJ\" role is required.", usage = "-volume [X]", privateMessages = false)
	public void onVolumeCommand(Guild guild,TextChannel channel, User author,String[] args){
		Member requester = guild.getMember(author);
		List<Permission> perm = requester.getPermissions(channel);
		if(args.length==1&&(!(guild.getAudioManager().isAttemptingToConnect()||guild.getAudioManager().isConnected())||(requester.getVoiceState().getChannel() == guild.getAudioManager().getConnectedChannel()))&&(requester.getRoles().containsAll(guild.getRolesByName("DJ", true)) || perm.contains(Permission.ADMINISTRATOR) || requester.isOwner())){
			GuildMusicManager mng = getGuildAudioPlayer(guild);
			AudioPlayer player = mng.player;
			try{
				int newVolume = Math.max(10, Math.min(100, Integer.parseInt(args[0])));
				int oldVolume = player.getVolume();
				player.setVolume(newVolume);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("Player volume changed from `" + oldVolume + "` to `" + newVolume + "`");
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/volume.png");
				channel.sendMessage(eb.build()).queue();
			}
			catch (NumberFormatException e){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("`" + args[0] + "` is not a valid integer. (10 - 100)");
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/volume.png");
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
			eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/volume.png");
			channel.sendMessage(eb.build()).queue();
		}
	}

	private void loadAndPlay(final TextChannel channel, final String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			
			@Override
			public void trackLoaded(AudioTrack track) {
				channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

				play(channel.getGuild(), musicManager, track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack firstTrack = playlist.getSelectedTrack();

				if (firstTrack == null) {
					firstTrack = playlist.getTracks().get(0);
				}
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/play.png");
				channel.sendMessage(eb.build()).queue();

				play(channel.getGuild(), musicManager, firstTrack);
			}

			@Override
			public void noMatches() {
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Nothing found by " + trackUrl);
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/cross.png");
				channel.sendMessage(eb.build()).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
				eb.setDescription("Could not play: " + exception.getMessage());
				eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/info.png");
				channel.sendMessage(eb.build()).queue();
			}
		});
	}

	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
		musicManager.scheduler.queue(track);
	}

	private void skipTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Audio-Player:", "https://github.com/sedmelluq/lavaplayer");
		eb.setDescription("Skipped to next track.");
		eb.setThumbnail("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/thumbnails/skip.png");
		channel.sendMessage(eb.build()).queue();
	}

	private static String getTimestamp(long milliseconds)
	{
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
		int hours   = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

		if (hours > 0)
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		else
			return String.format("%02d:%02d", minutes, seconds);
	}

	private static void checkVoiceChannel(AudioManager audioManager, Member request) {
		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect() && request.getVoiceState().getChannel()!=null ) {	
			audioManager.openAudioConnection(request.getVoiceState().getChannel());
		}
	}

}
