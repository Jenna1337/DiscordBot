package discordbot;

import java.util.function.Function;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;

public enum StandardStatusTriplets
{
	WATCHING(Game::watching),
	LISTENING(Game::listening),
	PLAYING(Game::playing);
	
	private Function<String,Game> gamebuilder;
	private StandardStatusTriplets(Function<String,Game> gamebuilder){
		this.gamebuilder = gamebuilder;
	}
	//TODO Have a "StatusMessage" class that uses "Triplet<OnlineStatus, Game, Boolean>".
	public StatusMessage newInstance(String text){
		return new StatusMessage(OnlineStatus.ONLINE, gamebuilder.apply(text), false);
	}
}
