package discordbot.commands;

import java.util.function.Consumer;
import net.dv8tion.jda.core.events.Event;

class Command<T extends Event>
{
	private final String name, info;
	private final Consumer<T> consumer;

	Command(String name, String info, Consumer<T> consumer){
		this.name = name;
		this.info = info;
		this.consumer = consumer;
	}
	
	public final String getName(){
		return name;
	}
	public final String getInfo(){
		return info;
	}
	public final Consumer<T> getConsumer(){
		return consumer;
	}
	public String toString(){
		return "Command:"+name;
	}
}
