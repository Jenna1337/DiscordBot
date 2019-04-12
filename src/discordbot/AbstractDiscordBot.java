package discordbot;

import java.text.MessageFormat;
import java.util.Map;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;

public abstract class AbstractDiscordBot
{
	protected static final String INVITE_URL_BASE="https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions={1}";
	protected final String inviteUrl, user_id;
	protected String prefix;
	protected final DiscordMessageListener listener;
	protected final JDA jda;
	
	public AbstractDiscordBot(String token, String client_id, long permissions, String user_id) throws LoginException
	{
		this(token, client_id, permissions, user_id, "");
		System.out.println("Warning: bot "+client_id+" does not have a prefix");
	}
	public AbstractDiscordBot(String token, String client_id, long permissions, String user_id, String prefix) throws LoginException
	{
		this.inviteUrl = MessageFormat.format(INVITE_URL_BASE, client_id, permissions);
		this.user_id = user_id;
		this.prefix = prefix;
		
		this.jda = new JDABuilder(token)
				.addEventListener(listener = new DiscordMessageListener(this))
				.build();
		try{
			jda.awaitReady();
		}catch(InterruptedException e){}
		
		// TODO Auto-generated constructor stub
	}
	
	public String getInviteURL(){
		return inviteUrl;
	}
	abstract public void handleGenericMessageEvent(GenericMessageEvent event,
			User author, Member member, Message message);
	abstract public Map<Guild, MessageChannel> getAuditChannels();
}
