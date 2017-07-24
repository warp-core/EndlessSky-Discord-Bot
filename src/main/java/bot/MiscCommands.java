package bot;

import java.io.File;
import java.io.FileReader;
import java.lang.String;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptException;
import java.io.IOException;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.MessageBuilder;

public class MiscCommands
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;

	public MiscCommands(CommandHandler commandHandler, ESBot bot) {
		this.commandHandler = commandHandler;
		this.bot = bot;
	}



	@Command(aliases = {"-template"}, description = "Sends the template for X. Possible args: outfit, ship, or plugin.", usage = "-template X")
	public void onTemplatesCommand(MessageChannel channel, String[] args)
	{
		if(args.length == 0)
			channel.sendMessage("Which template would you like? I have three flavours available: 'outfit', 'ship' and 'plugin'.").queue();
		else
			for(String str : args){
				String name = "";
				if(str.equals("plugin"))
					name = "exampleplugin.zip";
				else if(str.equals("outfit"))
					name = "outfittemplate.blend";
				else if(str.equals("ship"))
					name = "shiptemplate.blend";
				else
					channel.sendMessage("Sorry, I only have templates for 'outfit', 'ship' and 'plugin'.").queue();

				if(name.length() > 0){
					String url = bot.HOST_RAW_URL + "data/templates/" + name;
					channel.sendMessage("Here's your " + str + " template:\n[" + str + "](" + url + ")").queue();
				}
			}
	}

//	@Command(aliases = {"-quote"}, description = "quotes a person X.", usage = "-quote X")
	public void onQuoteCommand(MessageChannel channel, String args)
	{
		if(args == null)
			channel.sendMessage("A person! Give me a person!").queue();
		else{
			String quote = LookupCommands.generateQuote(args);
			channel.sendMessage("```" + quote + "```").queue();
		}
	}

	@Command(aliases = {"-apod"}, description = "posts a random APOD(NASA's Astronomy Picture of the Day).", usage = "-apod")
	public void onApodCommand(MessageChannel channel)
	{
		JSONObject json = null;
		try{
			javax.script.ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
			ScriptObjectMirror result = (ScriptObjectMirror)engine.eval(new java.io.FileReader("scripts/random_apod.js"));
			String sresult = result.values().toString();
			sresult = sresult.substring(sresult.indexOf("[")+1, sresult.indexOf("]"));
			URL url = new URL("https://api.nasa.gov/planetary/apod?api_key=Lk7O2c43wwYEUWZXHESdlARCbnSVs5gT0qgWGJKL&date=" + sresult);
			json = getJson(url);
		}
		catch (ScriptException|IOException e){
			e.printStackTrace(System.out);
		}
		EmbedBuilder eb = new EmbedBuilder();
		if (json.getString("media_type").equals("image")){
			eb.setImage(json.getString("hdurl"));
			eb.setTitle(json.getString("title"));
			eb.setDescription(json.getString("explanation"));
			channel.sendMessage(eb.build()).queue();
		}else if (json.getString("media_type").equals("video")){
			channel.sendMessage(json.getString("url").replace("embed/", "watch?v=")).queue();
		}else{
			channel.sendMessage("Incompatible Media Type, please try again").queue();
		}
	}

	private static JSONObject getJson(URL url) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = br.read()) != -1) {
			sb.append((char) cp);
	 }
	 JSONObject json = new JSONObject(sb.toString());
	 return json;
 }

}
