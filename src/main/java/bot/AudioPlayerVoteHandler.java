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
	 * 
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
	 * 
	 * @param Member voter    The member who is voting.
	 */
	public void vote(Member voter){
		if(voters.isEmpty())
			this.requester = voter;

		// possibly fix #101
		voters.remove(null);
		// Remove any voters who have left the voice channel after voting.
		voters.retainAll(channel.getMembers());
		// Add in the new voter.
		voters.add(voter);
	}



	/**
	 * Checks the vote status. If the vote is successful, this
	 * clears the vote. Otherwise, no-op.
	 * 
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
	 * eligible members are counted.
	 */
	public int getVotes(){
		int votes = 0;
		List<Member> nonVoters = NonVoters(channel);
		for(Member voter : voters)
			if(channel == voter.getVoiceState().getChannel()
					&& !nonVoters.contains(voter))
				++votes;

		return votes;
	}



	public void clear(){
		voters.clear();
	}



	/**
	 * Indicate how many votes are needed to perform a given requested action.
	 * Excludes any deafened members or those who shouldn't vote.
	 * 
	 * @return The number of votes needed.
	 */
	public int getRequiredVotes(){
		double canVote = 0.;
		List<Member> nonVoters = NonVoters(channel);
		for(Member member : channel.getMembers())
			if(!nonVoters.contains(member))
				++canVote;

		return (int)(Math.ceil(canVote / 2));
	}



	public Member getRequester(){
		return requester;
	}



	/**
	 * Get a list of those members that should not be counted when determining
	 * who votes, or should be able to vote.
	 * 
	 * @param  Guild guild         The guild whose members should be checked.
	 * @return       A list of members whose vote does not count.
	 */
	private static List<Member> NonVoters(VoiceChannel channel){
		Guild guild = channel.getGuild();
		List<Role> noVote = new LinkedList<Role>();
		// Anti-DJs cannot vote.
		noVote.addAll(guild.getRolesByName(Helper.ROLE_PLAYBANNED, true));
		// Anyone in a time-out cannot vote.
		noVote.addAll(guild.getRolesByName(Helper.ROLE_NAUGHTY, true));
		List<Member> nonVoters = guild.getMembersWithRoles(noVote);
		// Anyone who is deafened in this channel cannot vote.
		for(Member member : channel.getMembers())
			if(member.getVoiceState().isDeafened())
				nonVoters.add(member);

		return nonVoters;
	}
}
