package bot;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.entities.Member;

import java.util.LinkedList;
import java.util.List;

public class PlayerVoteHandler{
	private LinkedList<Member> voters;
	private Member voteStarter;
	private Guild guild;
	private Member requester;



	public PlayerVoteHandler(Guild guild){
		this.guild = guild;
		this.voters = new LinkedList<Member>();
	}



	public void vote(Member voter){
		if(!voters.contains(voter)){
			if (voters.isEmpty())
				this.requester = voter;
			voters.add(voter);
		}
	}



	// Returns true if the vote has passed.
	public boolean checkVotes(){
		LinkedList<Member> tmpVoterList = new LinkedList<>();
		for(Member m : voters){
			if(guild.getAudioManager().getConnectedChannel() == m.getVoiceState().getChannel())
				tmpVoterList.add(m);
		}
		if(getVotes() >= getRequiredVotes()){
			clear();
			return true;
		}
		else
			return false;
	}



	public void clear(){
		voters.clear();
	}



	public int getVotes(){
		return voters.size();
	}



	public int getRequiredVotes(){
		return (int)((guild.getAudioManager().getConnectedChannel().getMembers().size()-1)/2.0 + 0.5);
	}



	public Member getRequester(){
		return requester;
	}
}
