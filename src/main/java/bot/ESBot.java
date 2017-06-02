package bot;

import javax.security.auth.login.LoginException;
import de.btobastian.sdcf4j.*;
import de.btobastian.sdcf4j.handler.JDA3Handler;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

public class ESBot {
	private JDA jda;

	public ESBot(String TOKEN){
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(TOKEN).buildBlocking();
			jda.getPresence().setGame(Game.of("-help"));
			CommandHandler cmdHandler = new JDA3Handler(jda);
			cmdHandler.registerCommand(new InfoCommands(cmdHandler,this));
			cmdHandler.registerCommand(new ModeratorCommands(this));
			cmdHandler.registerCommand(new LookupCommands());
			cmdHandler.registerCommand(new MemeCommands());
	        jda.addEventListener(new SpellCheckListener(this));
		} catch (LoginException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (RateLimitedException e) {
			e.printStackTrace();
		}
	}
	
	public long getPing(){
		return jda.getPing();
	}
	
	public void disconnect(){
		jda.shutdown(false);
	}
	
	public void shutdown(){
		jda.shutdown();
	}	

	public User getSelf(){
		return jda.getSelfUser();
	}
}
