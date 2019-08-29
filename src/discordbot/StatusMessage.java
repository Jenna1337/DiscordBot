package discordbot;

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import utils.tuples.Triplet;

/**
 * Wrapper class for {@code Triplet<OnlineStatus, Game, Boolean>}
 */
public class StatusMessage
{
	private final Triplet<OnlineStatus, Game, Boolean> s;
	public StatusMessage(OnlineStatus onlineStatus, Game game, Boolean idle){
		s = new Triplet<>(onlineStatus, game, idle);
	}
	public OnlineStatus getOnlineStatus(){
		return s.getFirst();
	}
	public Game getGame(){
		return s.getSecond();
	}
	public boolean isIdle(){
		return s.getThird();
	}
}
