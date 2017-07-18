package bot;

import java.io.IOException;
import java.util.Properties;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.JDA;

import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Channel;

public class MemberEventListener extends ListenerAdapter{

	public static final String MAIN_CHANNEL = "testing";
	private ESBot bot;


	public MemberEventListener(ESBot bot){
		this.bot = bot;
	}



	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event){
		User user = event.getMember().getUser();
		if (!user.isBot()){
			MessageChannel channel = event.getGuild().getTextChannelsByName(MAIN_CHANNEL, true).get(0);
			TextChannel rules = event.getGuild().getTextChannelsByName("news", true).get(0);
			channel.sendMessageFormat("Hello %s! Welcome to the Endless Sky Community Discord. Make sure to read the %s and enjoy your stay! I'll be watching...", user.getAsMention(), rules).queue();
		}
	}



	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent event){
		User user = event.getMember().getUser();
		if (!user.isBot()){
			MessageChannel channel = event.getGuild().getTextChannelsByName(MAIN_CHANNEL, true).get(0);
			channel.sendMessageFormat("%s has just left the server. Goodbye!", user).queue();
		}
	}

  @Override
  public void onGuildBan(GuildBanEvent event)
  {
		User user = event.getUser();
    if (!user.isBot()){
      MessageChannel channel = event.getGuild().getTextChannelsByName(MAIN_CHANNEL, true).get(0);
      channel.sendMessageFormat("Bad Boy/Girl %s has just been banned from the server. Cya never!", user).queue();
    }
  }

}
