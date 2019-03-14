package discordbot;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import utils.DomainNameBlocker;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscordMessageListener extends ListenerAdapter
{
	SimpleDiscordBot bot;
	public DiscordMessageListener(SimpleDiscordBot bot){
		this.bot=bot;
	}
	@Override
	public void onMessageUpdate(MessageUpdateEvent event){
		handleMessageEvent(event, event.getAuthor(), event.getMember(), event.getMessage());
	}
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		handleMessageEvent(event, event.getAuthor(), event.getMember(), event.getMessage());
	}
	private void handleMessageEvent(GenericMessageEvent event, final User author, final Member member, final Message message){
		boolean blacklisted = checkBlacklists(event, author, member, message);
		//TODO use blacklisted for something?
		
		MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
		
		String msg = message.getContentDisplay().trim();
		
		if(author.isBot())
			return;
		
		bot.handleGenericMessageEvent(event, author, member, message);
		
		if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
		{
			Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
			TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
			
			String name = (message.isWebhookMessage())
					? author.getName() : member.getEffectiveName();
					
					System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
		}
		else if (event.isFromType(ChannelType.PRIVATE))
		{
			@SuppressWarnings("unused")
			PrivateChannel privateChannel = event.getPrivateChannel();
			
			System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
		}
		
		
		if (msg.equals("!ping"))
		{
			channel.sendMessage("pong!").queue();
		}
	}
	private boolean checkBlacklists(GenericMessageEvent event, final User author, final Member member, final Message message){
		boolean locatedinblacklist = false;
		locatedinblacklist|=handleBlacklistedText(event.getGuild(), event.getChannel(), TextLocation.username, author.getName(), null);
		if(member!=null){
			String nickname = member.getNickname();
			if(nickname!=null)
				locatedinblacklist|=handleBlacklistedText(event.getGuild(), event.getChannel(), TextLocation.nickname, nickname, null);
		}
		locatedinblacklist|=handleBlacklistedText(event.getGuild(), event.getChannel(),
				TextLocation.message, message.getContentDisplay(), " posted by user <"+author.getId()+"> \""+author.getName()+"\"");
		return locatedinblacklist;
	}
	private boolean handleBlacklistedText(Guild guild, MessageChannel channel, TextLocation location, String text, String cause){
		Map<String, Set<String>> containsblacklists = DomainNameBlocker.getBlacklistedEntriesInText(text, location.getMask());
		if(containsblacklists.isEmpty())
			return false;
		String channelname = channel.getName();
		String statusmessage = (guild!=null?("Guild \""+guild.getName()+"\":"):"")
				+(channelname!=null?("Channel \""+channelname+"\":"):"")
				+"Found blacklisted text in "+location+" \""+text.replace("\\", "\\\\")
				.replace("\r\n", "\\r\\n").replace("\r", "\\r").replace("\n", "\\n")
				.replace("\t", "\\t")+"\" "+(cause!=null?(cause+" "):"")+"matching the following regex"
				+(containsblacklists.size()!=1?"es":"")+":"
				+containsblacklists.entrySet().stream().map(mapentry->{
					return "From list \""+mapentry.getKey()+"\": "+mapentry.getValue().toString();
				}).collect(Collectors.joining("\r\n","\r\n","\r\n"));
		MessageChannel auditchannel = bot.auditchannels.get(guild);
		if(auditchannel!=null)
			auditchannel.sendMessage(statusmessage).queue();
		else
			System.err.println(statusmessage);
		return true;
	}
}
