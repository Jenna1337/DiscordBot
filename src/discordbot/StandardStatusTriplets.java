package discordbot;

import java.util.function.Function;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import utils.tuples.Triplet;

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
	public Triplet<OnlineStatus, Game, Boolean> newInstance(String text){
		return new Triplet<>(OnlineStatus.ONLINE, gamebuilder.apply(text), false);
	}
}
