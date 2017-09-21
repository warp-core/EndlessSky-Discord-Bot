package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import net.dv8tion.jda.core.entities.Guild;


/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class GuildMusicManager {

	/**
	 * Audio player for the guild.
	 */
	public final AudioPlayer player;

	/**
	 * Track scheduler for the player.
	 */
	public final TrackScheduler scheduler;

	public final Guild guild;


	/**
	 * Creates a player and a track scheduler.
	 * @param manager Audio player manager to use for creating the player.
	 */
	public GuildMusicManager(AudioPlayerManager manager, PlayerControl control, Guild guild){
		this.guild = guild;
		player = manager.createPlayer();
		scheduler = new TrackScheduler(player, control, this);
		player.addListener(scheduler);
	}


	/**
	 * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
	 */
	public AudioPlayerSendHandler getSendHandler(){
		return new AudioPlayerSendHandler(player);
	}


	/**
	 * @return The associated with this Manager.
	 */
	public Guild getGuild(){
		return guild;
	}
}
