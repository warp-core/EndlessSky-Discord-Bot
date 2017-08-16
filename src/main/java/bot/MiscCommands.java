package bot;

import java.io.File;
import java.io.FileReader;
import java.lang.String;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptException;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.MessageBuilder;

public class MiscCommands
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;
	// Please don't use these keys for any other purpose than this bot, thank you.
	public static final String ONLINECONVERT_APIKEY = "25eea0ad93bf9160043acf83b21f8056";
	public static final String NASA_APIKEY = "Lk7O2c43wwYEUWZXHESdlARCbnSVs5gT0qgWGJKL";

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
						String url = bot.HOST_RAW_URL + "/data/templates/" + name;
						EmbedBuilder eb = new EmbedBuilder();
						eb.setTitle(name, url);
						eb.setDescription("Here's your " + str + " template, served hot and crunchy :)");
						channel.sendMessage(eb.build()).queue();
					}
			}
	}


	@Command(aliases = {"-cat"}, description = "Posts a random cat picture from random.cat", usage = "-cat")
	public void onCatCommand(MessageChannel channel)
	{
		try{
			URL url = new URL("https://random.cat/meow");
			channel.sendMessage(getJson(url).getString("file")).queue();
		}
		catch (IOException e){
			e.printStackTrace(System.out);
		}
	}


	@Command(aliases = {"-dog"}, description = "Posts a random dog picture from random.dog", usage = "-dog")
	public void onDogCommand(MessageChannel channel)
	{
		try{
			URL url = new URL("https://random.dog/woof.json");
			channel.sendMessage(getJson(url).getString("url")).queue();
		}
		catch (IOException e){
			e.printStackTrace(System.out);
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
			URL url = new URL("https://api.nasa.gov/planetary/apod?api_key=" + NASA_APIKEY+ "&date=" + sresult);
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

	@Command(aliases = {"-wav"}, description = "Converts an Audio file to a wav file suitable for ES.", usage = "-wav [attached file]")
	public void onWavCommand(MessageChannel channel, Message message)
	{
		channel.sendMessage("Conversion queued, this may take up to some minutes...").queue();
	try{
			OkHttpClient client = new OkHttpClient();
			String url = message.getAttachments().get(0).getUrl();
			String filename = message.getAttachments().get(0).getFileName();

			// Send POST to start up the conversion
			MediaType mediaType = MediaType.parse("application/json");
			String payload = "{\"input\":[{\"type\":\"remote\",\"source\":\"" + url + "\", \"filename\": \"" + filename + "\"}],\"conversion\":[{\"category\":\"audio\",\"target\":\"wav\", \"options\":{\"frequency\": 44100,\"channels\": \"mono\",\"normalize\": false,\"pcm_format\": \"pcm_s16le\"}}]}";
			System.out.println(payload);
			RequestBody body = RequestBody.create(mediaType, payload);
			Request request = new Request.Builder()
			.url("http://api2.online-convert.com/jobs")
			.post(body)
			.addHeader("x-oc-api-key", ONLINECONVERT_APIKEY)
			.addHeader("cache-control", "no-cache")
			.build();

			// get Job Data
			Response response = client.newCall(request).execute();
			JSONObject json = new JSONObject(response.body().string());
			System.out.print(json.toString(3));
			String conversion_id = json.getJSONArray("conversion").getJSONObject(0).getString("id");
			String input_id = json.getJSONArray("input").getJSONObject(0).getString("id");
			String token = json.getString("token");
			String job_id = json.getString("id");

			// prepare a request for the status history
			request = new Request.Builder()
			.url("https://api2.online-convert.com/jobs/" + job_id + "/history")
			.get()
			.addHeader("x-oc-api-key", ONLINECONVERT_APIKEY)
			.addHeader("x-oc-token", token)
			.addHeader("cache-control", "no-cache")
			.build();

			//check the status every 5 seconds, until it is either failed, done or took longer than ~50 seconds
			boolean done = false;
			boolean failed = false;
			int counter = 0;
			while (!done) {
			counter ++;
				if (counter > 10){
					done = true;
					failed = true; }
				try {
				TimeUnit.SECONDS.sleep(5); }
				catch (InterruptedException e) {
					e.printStackTrace(System.out); }
				response = client.newCall(request).execute();
				JSONArray jsonarray = new JSONArray(response.body().string());
				for (Object object : jsonarray){
					json = (JSONObject)object;
					System.out.println(json.toString(3));
					if (json.getString("status").equals("completed")) {
					System.out.println(json.getString("status"));
						done = true;
						failed = false;
					}
					else if (json.getString("status").equals("failed")) {
						done = true;
						failed = true;
					}
				}
			}
			if (!failed) {

				// Get File ID
				request = new Request.Builder()
				.url("https://api2.online-convert.com/jobs/" + job_id)
				.get()
				.addHeader("x-oc-api-key", ONLINECONVERT_APIKEY)
				.addHeader("x-oc-token", token)
				.addHeader("cache-control", "no-cache")
				.build();
				response = client.newCall(request).execute();
				json = new JSONObject(response.body().string());
				String file_id = json.getJSONArray("output").getJSONObject(0).getString("id");

				// Get download Link
				request = new Request.Builder()
				.url("https://api2.online-convert.com/jobs/" + job_id + "/output/" + file_id)
				.get()
				.addHeader("x-oc-api-key", ONLINECONVERT_APIKEY)
				.addHeader("x-oc-token", token)
				.addHeader("cache-control", "no-cache")
				.build();
				response = client.newCall(request).execute();
				json = new JSONObject(response.body().string());
				String download = json.getString("uri");

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle(filename.split("\\.(?=[^\\.]+$)")[0] + ".wav", download);
				eb.setDescription("Here's your converted file, thank the guys at online-convert.com :)");
				channel.sendMessage(eb.build()).queue();
			}
			else{
				channel.sendMessage("Conversion failed. Maybe try again?").queue();
			}
		}
		catch (IOException e){
			e.printStackTrace(System.out);
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
