package bot;

import java.io.IOException;
import java.util.Properties;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Channel;

public class MemberEventListener extends ListenerAdapter
{
  public static final String MAIN_CHANNEL = "testing";
  private ESBot bot;

  public MemberEventListener(ESBot bot){
    this.bot = bot;
  }

  @Override
  public void onGuildMemberJoin(GuildMemberJoinEvent event)
  {
    User user = event.getMember().getUser();
    if (!user.isBot()){
      String name = user.getAsMention();
      TextChannel channel = event.getGuild().getTextChannelsByName(MAIN_CHANNEL, true).get(0);
      channel.sendMessage("Hello " + name + "! Welcome to the Endless Sky Community Discord. Make sure to read the #rules and enjoy your stay! I'll be watching...").queue();
    }
  }

}
