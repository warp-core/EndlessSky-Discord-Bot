package bot;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class AudioPlayerVoteHandler{
	private Guild guild;
	private HashSet<Member> voters;
	private Member requester;
	private VoiceChannel channel;



	/**
	 * Construct a new vote manager for this guild. Each vote manager handles
	 * a single vote. It does not know what its vote is for.
	 * @param  Guild guild     The server for which a vote is being conducted.
	 */
	public AudioPlayerVoteHandler(Guild guild){
		this.guild = guild;
		this.voters = new HashSet<Member>();
		this.channel = guild.getAudioManager().getConnectedChannel();
	}



	/**
	 * Adds the given voter to the list of voters. Removes any now-invalid
	 * votes (e.g. from someone who left the channel).
	 * @param Member voter    The member who is voting.
	 */
	public void vote(Member voter){
		if(voters.isEmpty())
			this.requester = voter;

		// Remove any voters who have left the voice channel after voting.
		voters.retainAll(channel.getMembers());
		// Add in the new voter.
		voters.add(voter);
	}



	/**
	 * Checks the vote status. If the vote is successful, this
	 * clears the vote. Otherwise, no-op.
	 * @return true if the vote passed, false if it is ongoing.
	 */
	public boolean checkVotes(){
		if(getVotes() >= getRequiredVotes()){
			clear();
			return true;
		}
		else
			return false;
	}



	/**
	 * Someone may have left the channel after the most recent vote but
	 * before checkVotes() runs. This function ensures only votes from
	 * non-playbanned and non-gulaged members are counted.
	 */
	public int getVotes(){
		int votes = 0;
		List<Role> noVote = new LinkedList<Role>();
		noVote.addAll(guild.getRolesByName(Helper.ROLE_PLAYBANNED, true));
		noVote.addAll(guild.getRolesByName(Helper.ROLE_GULAG, true));
		List<Member> nonVoters = guild.getMembersWithRoles(noVote);
		for(Member voter : voters)
			if(channel == voter.getVoiceState().getChannel()
					&& !nonVoters.contains(voter))
				++votes;

		return votes;
	}



	public void clear(){
		voters.clear();
	}



	public int getRequiredVotes(){
		return (int)((channel.getMembers().size()-1)/2.0 + 0.5);
	}



	public Member getRequester(){
		return requester;
	}
}
