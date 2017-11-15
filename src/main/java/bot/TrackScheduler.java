package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {

	private final AudioPlayer player;
	private final LinkedList<AudioTrack> queue;
	private final PlayerControl control;
	private final GuildMusicManager manager;
	private Guild cachedManagerGuild;



	/**
	 * @param player The audio player this scheduler uses
	 */
	public TrackScheduler(AudioPlayer player, PlayerControl control, GuildMusicManager manager){
		this.player = player;
		this.queue = new LinkedList<>();
		this.control = control;
		this.manager = manager;
	}



	public LinkedList<AudioTrack> getQueue(){
		return queue;
	}



	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 *
	 * @param AudioTrack track The track to play or add to queue.
	 */
	public void queue(AudioTrack track){
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		if (!player.startTrack(track, true)){
			queue.offer(track);
		}
	}

	/**
	 * Queue a number of tracks, playing the first one if possible.
	 *
	 * @param AudioPlaylist playlist The playlist to begin playing (or queue).
	 */
	public void queue(AudioPlaylist playlist){
		// Play (or queue) the first track.
		List<AudioTrack> tracks = playlist.getTracks();
		queue(tracks.get(0)); //Removing it from the tracks will cause problems in playlistLoaded (see #88)
		// Queue the remaining tracks.
		if(!tracks.isEmpty())
			queue.addAll(tracks.subList(1, tracks.size()));
	}



	/**
	 * Stops playing the current song, and advances the queue.
	 *
	 * @param int skipCount The number of tracks to advance.
	 */
	public void nextTrack(int skipCount){
		// by caching the guild, we can call onNextTrack() even when manager.getGuild() would normally return null (when the queue is empty).
		cachedManagerGuild = manager.getGuild();
		// Decrease the skipped count by one since nextTrack() will advance by 1.
		skipCount = Math.min(--skipCount, queue.size());
		for(int i = 0; i < skipCount; ++i){
			queue.pop();
		}
		nextTrack();
	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack(){
		// Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the player.
		player.startTrack(queue.peek(), false);
		player.setPaused(false);
		control.onNextTrack(queue.poll() != null ? manager.getGuild() : cachedManagerGuild);
	}



	public void shuffle(){
		Collections.shuffle(queue);
	}



	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason){
		// Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
		if (endReason.mayStartNext)
			nextTrack();
	}
}
