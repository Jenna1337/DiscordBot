package discordbot;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import utils.DomainNameBlocker;
import utils.sql.BackedHashmap;
import utils.tuples.Triplet;

public class SimpleDiscordBot extends AbstractDiscordBot
{
	static{
		try{
			DomainNameBlocker.updateBlacklists();
		}catch(Exception e){
			throw new InternalError(e);
		}
	}
	private Map<Guild, MessageChannel> auditchannels = new HashMap<>();
	
	//TODO workaround Guild and MessageChannel being not Serializable (probably could use IDs instead
	//static Map<Guild, MessageChannel> auditchannels = new BackedHashmap<Guild, MessageChannel>("data/botdb.sqlite3", "auditchannels");
	
	
	private <F> void printNameAndId(F obj,int indentcount){
		try{
			char[] chrs = new char[indentcount];
			Arrays.fill(chrs, '\t');
			String indentspace = new String(chrs);
			System.out.println(indentspace+"\""+obj.getClass().getMethod("getName").invoke(obj)+"\" ("+((ISnowflake) obj).getId()+")");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public SimpleDiscordBot(String token, String client_id, long permissions, String user_id, String prefix) throws LoginException
	{
		super(token, client_id, permissions, user_id, prefix);
		
		jda.getGuilds().forEach(guild->{
			printNameAndId(guild,0);
			guild.getCategories().forEach(category->{
				printNameAndId(category,1);
				category.getChannels().forEach(channel->{
					printNameAndId(channel,2);
				});
			});
		});
		System.out.println("Users:");
		jda.getUsers().forEach(user->{
			printNameAndId(user, 1);
		});
	}
	protected void addCommand(){
		
		//TODO
	}
	public void handleGenericMessageEvent(GenericMessageEvent event, User author,
			Member member, Message message)
	{
		MessageChannel channel = event.getChannel();
		String msg = message.getContentDisplay().trim();
		if(member!=null && msg.startsWith(prefix+"setauditchannel") && member.hasPermission(Permission.ADMINISTRATOR)){
			auditchannels.put(event.getGuild(), channel);
			channel.sendMessage("Audit channel set.").queue();
		}
	}
	public Map<Guild, MessageChannel> getAuditChannels(){
		return auditchannels;
	}
	/**
	 * The thread to cycle through the status messages.
	 */
	private Thread autoStatusCycler;
	/**
	 * The list of status messages to cycle through.
	 */
	private List<Triplet<OnlineStatus, Game, Boolean>> autoStatusList;
	/**
	 * Default sleep time is 30 minutes.
	 */
	private long autoStatusSleepTimeMillis = 1000 * 60 * 30;
	boolean autoStatusCyclerStopped = false;
	public boolean setAutoStatusMessageList(List<Triplet<OnlineStatus, Game, Boolean>> statuses){
		if(autoStatusCycler!=null && (autoStatusCycler.isAlive() || !autoStatusCycler.getState().equals(State.TERMINATED))){
			autoStatusCyclerStopped = true;
			try{
				autoStatusCycler.join(3000);
			}
			catch(InterruptedException e){
				autoStatusCycler.interrupt();
				try{
					autoStatusCycler.join(3000);
				}
				catch(InterruptedException e1){
					System.err.println("Failed to stop autoStatusCycler Thread.");
					return false;
				}
			}
		}
		autoStatusList = statuses;
		autoStatusCycler = new Thread((Runnable)()->{
			try{
				for(int i=0;i<autoStatusList.size();++i){
					try{
						setStatusMessage(autoStatusList.get(i));
					}
					catch(Exception e){
						System.err.println("[autoStatusCycler] Could not set status message.");
					}
					if(autoStatusCyclerStopped)
						return;
					
					Thread.sleep(autoStatusSleepTimeMillis);
				}
			}
			catch(InterruptedException e){
				return;
			}
		});
		autoStatusCycler.setName("autoStatusCycler");
		System.out.println("autoStatusCycler Thread starting.");
		autoStatusCyclerStopped = false;
		autoStatusCycler.start();
		return true;
	}
	public void setStatusMessage(Triplet<OnlineStatus, Game, Boolean> stmsg){
		this.jda.getPresence().setPresence(stmsg.getFirst(), stmsg.getSecond(), stmsg.getThird());
	}
	public void setStatusMessage(OnlineStatus status, Game game, boolean idle){
		this.jda.getPresence().setPresence(status, game, idle);
	}
}
