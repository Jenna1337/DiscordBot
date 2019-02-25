package discordbot.commands;

import java.util.HashMap;
import java.util.function.Consumer;
import net.dv8tion.jda.core.events.Event;

public class CommandManager{
	private CommandManager(){}
		
	private static HashMap<Class<?>, HashMap<String, Command<?>>> commands = new HashMap<>();
	
	public static <T extends Event> boolean addCommand(String name, String info, Consumer<T> consumer, Class<T> clazz){
		if(name==null || info==null || consumer==null)
			throw new NullPointerException();
		System.out.println(java.util.Arrays.deepToString(new Object[]{clazz.getName()}));
		HashMap<String, Command<?>> cmds = commands.get(clazz);
		if(cmds==null)
			commands.put(clazz, cmds=new HashMap<>());
		if(cmds.containsKey(name))
			return false;
		cmds.put(name, new Command<T>(name, info, consumer));
		
		System.out.println(commands);
		return true;
	}
}
