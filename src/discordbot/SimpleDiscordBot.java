package discordbot;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import utils.DomainNameBlocker;

public class SimpleDiscordBot
{
	static{
		try{
			DomainNameBlocker.updateBlacklists();
		}catch(Exception e){
			throw new InternalError(e);
		}
	}
	private static final String INVITE_URL_BASE="https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions={1}";
	private final String inviteUrl, user_id;
	private String prefix;
	private final DiscordMessageListener listener;
	static Map<Guild, MessageChannel> auditchannels = new HashMap<>();
	
	
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
		inviteUrl = MessageFormat.format(INVITE_URL_BASE, client_id, permissions);
		this.user_id = user_id;
		this.prefix = prefix;
		
		JDA jda = new JDABuilder(token)
				.addEventListener(listener = new DiscordMessageListener(this))
				.build();
		try{
			jda.awaitReady();
		}catch(InterruptedException e){}
		
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
	public String getInviteURL(){
		return inviteUrl;
	}
	protected void addCommand(){
		
		//TODO
	}
	public void handleGenericMessageEvent(GenericMessageEvent event, User author,
			Member member, Message message)
	{
		// TODO Auto-generated method stub
		MessageChannel channel = event.getChannel();
		String msg = message.getContentDisplay().trim();
		if(msg.startsWith(prefix+"setauditchannel") && member.hasPermission(Permission.ADMINISTRATOR)){
			auditchannels.put(event.getGuild(), channel);
			channel.sendMessage("Audit channel set.").queue();
		}
		
	}
}
