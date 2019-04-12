package discordbot.commands;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import discordbot.AbstractDiscordBot;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;

class Command<T extends Event>
{
	public static enum CommandTypes{
		GENERAL_TEXT,
		GENERAL_VOICE,
		GENERAL_REACTION,
	}
	private final String name, info;
	private final BiFunction<AbstractDiscordBot, T,Boolean> f;
	
	Command(String name, String info, BiConsumer<AbstractDiscordBot,T> consumer){
		this.name = name;
		this.info = info;
		this.f = (b, e)->{
			consumer.accept(b, e);
			return true;
		};
	}
	Command(String name, String info, BiFunction<AbstractDiscordBot,T,Boolean> f){
		this.name = name;
		this.info = info;
		this.f = f;
	}
	
	public final String getName(){
		return name;
	}
	public final String getInfo(){
		return info;
	}
	public final BiFunction<AbstractDiscordBot, T,Boolean> getFunction(){
		return f;
	}
	public String toString(){
		return "Command:"+name;
	}
	public boolean fire(AbstractDiscordBot bot, T event){
		return f.apply(bot, event);
	}
}

class TestTextCommand extends Command<GenericMessageEvent>{
	TestTextCommand(){
		super("test", "A test command", (bot,event)->{
			event.getChannel().sendMessage("testing 123").queue();
		});
	}
}
class TestReactionCommand extends Command<GenericMessageReactionEvent>{
	TestReactionCommand(){
		super("testreact", "A test reaction command", (bot,event)->{
			System.out.println("User "+event.getUser().getName()+" reacted with "+event.getReactionEmote().getName());
			event.getChannel().sendMessage("testing 123").queue();
		});
	}
}
