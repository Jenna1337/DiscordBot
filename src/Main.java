import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import discordbot.SimpleDiscordBot;
import discordbot.TextLocation;
import discordbot.commands.CommandManager;
import jdk.nashorn.api.scripting.ScriptUtils;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import discordbot.Permissions;
import utils.DomainNameBlocker;
import utils.Utils;
import utils.sql.SQLite3DatabaseLink;
import utils.tuples.Triplet;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@SuppressWarnings("unused")
public class Main
{
	public static void main(String[] args) throws Exception
	{
		System.setOut(new TimestampingPrintStream(System.out));
		System.setErr(new TimestampingPrintStream(System.err));
		//DomainNameBlocker.updateBlacklists();
		
		/*
		CommandManager.addCommand("test", "A test command", a->{
			
		}, GenericMessageEvent.class);
		*/
		
		//SQLite3DatabaseLink.test();
		
		//System.exit(0);
		
		
		Properties botprops = Utils.loadProperties("bot.properties");
		
		//updatePerms();
		
		@SuppressWarnings("resource")
		long perms = Arrays.stream(botprops.getProperty("PERMISSIONS").split("\\W")).map(
				a->{
					try{
						return (long)Permissions.class.getDeclaredField(a).get(null);
					}catch(Exception e){
						throw new RuntimeException(e);
					}
				}).reduce((a,b)->(a|b)).get();
		
			/**
			 * malicious
			 */
		System.out.println("Starting");
		SimpleDiscordBot bot = new SimpleDiscordBot(botprops.getProperty("TOKEN"), botprops.getProperty("CLIENT_ID"), perms, botprops.getProperty("USER_ID"), botprops.getProperty("PREFIX"));
		System.out.println(bot.getInviteURL());
		bot.setAutoStatusMessageList(Arrays.asList(
				new Triplet<OnlineStatus, Game, Boolean>(OnlineStatus.ONLINE, Game.listening(" for malicious links"), false),
				new Triplet<OnlineStatus, Game, Boolean>(OnlineStatus.ONLINE, Game.watching(" server" + (bot.getJDA().getGuilds().size()!=1 ? "s" : "")), false),
				new Triplet<OnlineStatus, Game, Boolean>(OnlineStatus.ONLINE, Game.playing("Global Thermonuclear War"), false)
		));
		System.out.println("Ready");
	}
	public static void updatePerms() throws IOException{
		Properties perms = Utils.loadProperties("perms.properties");
		FileWriter writer = new FileWriter("src/Permissions.java");
		writer.write("@javax.annotation.Generated(\""+Utils.getDateTime_ISO_8601()+"\")\npublic final class Permissions{\n\tprivate Permissions(){}\n\tpublic static final long\n");
		perms.entrySet().stream().map(
				entry->
				
				new SimpleEntry<String, Integer>(
						entry.getKey().toString(),
						Integer.parseInt(entry.getValue().toString())
						)
				
				).sorted(((a,b)->(a.getValue()-b.getValue()))).forEachOrdered(a->{
					try{
						writer.write((a.getValue()>1?",\n\t\t\t":"\t\t\t")+a.getKey()+"="+a.getValue());
					}catch(Exception e){
						e.printStackTrace();
						System.exit(1);
					}
				});
		writer.write(";\n}");
		writer.close();
		System.out.println("Finished updating Permissions.java");
		System.exit(0);
	}
}

class TimestampingPrintStream extends PrintStream{
	public TimestampingPrintStream(OutputStream out){
		super(out);
	}
	public TimestampingPrintStream(OutputStream out, boolean autoFlush){
		super(out,autoFlush);
	}
	public TimestampingPrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException{
		super(out,autoFlush,encoding);
	}
	
	@Override
	public void println(String x){
		super.println(Utils.getDateTime()+": "+x);
	}
}
