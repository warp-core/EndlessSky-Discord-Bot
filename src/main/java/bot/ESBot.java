package bot;

import javax.security.auth.login.LoginException;
import de.btobastian.sdcf4j.*;
import de.btobastian.sdcf4j.handler.JDA3Handler;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.util.Properties;

public class ESBot {
	private JDA jda;
	public VersionInfo version = new VersionInfo();

	// Set global URL paths for use by commands.
	public static final String HOST_RAW_URL = "https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master";
	public static final String HOST_PUBLIC_URL = "https://github.com/MCOfficer/EndlessSky-Discord-Bot";
	public static final String CONTENT_URL = "https://github.com/endless-sky/endless-sky/raw/master";
	public static final String DATA_URL = "https://raw.githubusercontent.com/endless-sky/endless-sky/master/data/";


	public Properties keys;


	public ESBot(String TOKEN){
		try{
			loadKeys();
			jda = new JDABuilder(AccountType.BOT).setToken(TOKEN).buildBlocking();
			setGameListening("-help");
			update();
			System.out.println("\nESBot instantiation successful. Ready for chatroom commands.");
		}
		catch(LoginException e){
			e.printStackTrace();
			jda.shutdown();
		} catch (IllegalArgumentException | InterruptedException e){
			e.printStackTrace();
		}
	}



	/**
	 * Removes any existing event listeners, then re-loads everything.
	 * Does not refresh the known filename lists.
	 */
	public synchronized void update(){
		jda.getRegisteredListeners().forEach(jda::removeEventListener);
		CommandHandler cmdHandler = new JDA3Handler(jda);
		cmdHandler.registerCommand(new LookupCommands(this));
		cmdHandler.registerCommand(new PlayerControl(this, jda));
		cmdHandler.registerCommand(new InfoCommands(cmdHandler,this));
		cmdHandler.registerCommand(new ModeratorCommands(this));
		cmdHandler.registerCommand(new MemeCommands(this));
		cmdHandler.registerCommand(new MiscCommands(cmdHandler,this));
		jda.addEventListener(new SpellCheckListener(this));
		jda.addEventListener(new MemberEventListener(this));
	}


	public String getKey(String id){
		return keys.getProperty(id, "");
	}



	public void loadKeys(){
		Properties keys = new Properties();
		try{
			keys.load(new FileReader("keys.txt"));
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		this.keys = keys;
	}


	public void setGamePlaying(String game) {
		jda.getPresence().setGame(Game.playing(game));
	}


	public void setGameListening(String game) {
		jda.getPresence().setGame(Game.playing(game));
	}


	public long getPing(){
		return jda.getPing();
	}



	public void disconnect(){
		jda.shutdown();
	}



	public void shutdown(){
		jda.shutdown();
	}



	public User getSelf(){
		return jda.getSelfUser();
	}
}
