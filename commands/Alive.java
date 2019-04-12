import discordbot.Command;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;

class Alive extends Command<GenericMessageEvent>{
	Alive(){
		super("alive", "", (bot,msg)->{
			msg.getChannel().sendMessage("No.").queue();
		});
	}
}