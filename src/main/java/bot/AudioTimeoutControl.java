package bot;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.JDA;

import static java.util.concurrent.TimeUnit.*;

import java.util.HashMap;

public class AudioTimeoutControl {
	private final Map<Long, GuildMusicManager> musicManagers;
	private final Map<Long, Integer> idleValues;
	private final ScheduledExecutorService scheduler =
			Executors.newScheduledThreadPool(1);
	private JDA jda;

	public AudioTimeoutControl(Map<Long, GuildMusicManager> musicManagers,JDA jda) {
		this.jda = jda;
		this.musicManagers = musicManagers;
		idleValues = new HashMap<>();
		checkIdle();
		closeIdle();
	}

	public void checkIdle(){
		final IdleChecker checker = new IdleChecker(musicManagers,idleValues);
		scheduler.scheduleAtFixedRate(checker, 5,5, SECONDS);
	}

	public void closeIdle(){
		final IdleCloser checker = new IdleCloser(jda,idleValues);
		scheduler.scheduleAtFixedRate(checker, 10,10, SECONDS);
	}

	private class IdleChecker
	implements Runnable{
		private final Map<Long, GuildMusicManager> musicManagers;
		private final Map<Long, Integer> idleValues;

		public IdleChecker(Map<Long, GuildMusicManager> musicManagers, Map<Long, Integer> idleValues) {
			this.musicManagers = musicManagers;
			this.idleValues = idleValues;
		}


		@Override
		public void run() {
			musicManagers.forEach( (l,mM) -> {
				if(mM.player.getPlayingTrack() == null || jda.getGuildById(l.toString()).getAudioManager().getConnectedChannel().getMembers().size() == 1){
					if(idleValues.get(l) != null){
						synchronized(this){
						idleValues.put(l, idleValues.get(l)+1);
						}
					}
					else{
						idleValues.put(l, 0);
					}
				}
			});
		}

	}

	private class IdleCloser
	implements Runnable{
		private final Map<Long, Integer> idleValues;
		private JDA jda;

		public IdleCloser(JDA jda, Map<Long, Integer> idleValues) {
			this.jda = jda;
			this.idleValues = idleValues;
		}


		@Override
		public void run() {
			idleValues.forEach( (l,i) -> {
				if(i>12){
					synchronized(this){
					jda.getGuildById(l.toString()).getAudioManager().closeAudioConnection();
					idleValues.put(l, 0);
					}
				}
			});
		}

	}
}
