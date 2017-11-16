package bot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.URL;

import net.dv8tion.jda.core.entities.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import net.dv8tion.jda.core.EmbedBuilder;

public class MiscCommands
implements CommandExecutor{

	private final CommandHandler commandHandler;
	private ESBot bot;

	public MiscCommands(CommandHandler commandHandler, ESBot bot) {
		this.commandHandler = commandHandler;
		this.bot = bot;
	}



	@Command(aliases = {"-template"}, description = "Sends the template for X. Possible args: outfit, ship, or plugin.", usage = "-template X")
	public void onTemplatesCommand(Guild guild, MessageChannel channel, String[] args)
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
						eb.setColor(guild.getMember(bot.getSelf()).getColor());
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



	@Command(aliases = {"-apod"}, description = "posts a random APOD (NASA's Astronomy Picture of the Day).", usage = "-apod")
	public void onApodCommand(Guild guild, MessageChannel channel)
	{
		JSONObject json = null;
		try{
			javax.script.ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
			ScriptObjectMirror result = (ScriptObjectMirror)engine.eval(new java.io.FileReader("scripts/random_apod.js"));
			String sresult = result.values().toString();
			sresult = sresult.substring(sresult.indexOf("[")+1, sresult.indexOf("]"));
			URL url = new URL("https://api.nasa.gov/planetary/apod?api_key=" + bot.getKey("NASA")+ "&date=" + sresult);
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
			eb.setColor(guild.getMember(bot.getSelf()).getColor());
			channel.sendMessage(eb.build()).queue();
		}else if (json.getString("media_type").equals("video")){
			channel.sendMessage(json.getString("url").replace("embed/", "watch?v=")).queue();
		}else{
			channel.sendMessage("Incompatible Media Type, please try again").queue();
		}
	}



	@Command(aliases = {"-wav"}, description = "Converts an Audio file to a wav file suitable for ES.", usage = "-wav [attached file]")
	public void onWavCommand(Guild guild, MessageChannel channel, Message message)
	{
		channel.sendMessage("Conversion queued, this may take up to some minutes...").queue();
	try{
			OkHttpClient client = new OkHttpClient();
			String url = message.getAttachments().get(0).getUrl();
			String filename = message.getAttachments().get(0).getFileName();

			// Send POST to start up the conversion
			MediaType mediaType = MediaType.parse("application/json");
			String payload = String.format("{\"input\":[{\"type\":\"remote\",\"source\":\"%s\", \"filename\": \"%s\"}]" +
							",\"conversion\":[{\"category\":\"audio\",\"target\":\"wav\", \"options\":" +
							"{\"frequency\": 44100,\"channels\": \"mono\",\"normalize\": false,\"pcm_format\":" +
							" \"pcm_s16le\"}}]}", url, filename);
			RequestBody body = RequestBody.create(mediaType, payload);
			Request request = new Request.Builder()
			.url("http://api2.online-convert.com/jobs")
			.post(body)
			.addHeader("x-oc-api-key", bot.getKey("ONLINECONVERT"))
			.addHeader("cache-control", "no-cache")
			.build();

			// get Job Data
			Response response = client.newCall(request).execute();
			JSONObject json = new JSONObject(response.body().string());
			String conversion_id = json.getJSONArray("conversion").getJSONObject(0).getString("id");
			String input_id = json.getJSONArray("input").getJSONObject(0).getString("id");
			String token = json.getString("token");
			String job_id = json.getString("id");

			// prepare a request for the status history
			request = new Request.Builder()
			.url("https://api2.online-convert.com/jobs/" + job_id + "/history")
			.get()
			.addHeader("x-oc-api-key", bot.getKey("ONLINECONVERT"))
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
					if (json.getString("status").equals("completed")) {
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
				.addHeader("x-oc-api-key", bot.getKey("ONLINECONVERT"))
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
				.addHeader("x-oc-api-key", bot.getKey("ONLINECONVERT"))
				.addHeader("x-oc-token", token)
				.addHeader("cache-control", "no-cache")
				.build();
				response = client.newCall(request).execute();
				json = new JSONObject(response.body().string());
				String download = json.getString("uri");

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle(filename.split("\\.(?=[^\\.]+$)")[0] + ".wav", download);
				eb.setDescription("Here's your converted file, thank the guys at online-convert.com :)");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
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



	@Command(aliases = {"-wikia"}, description = "Displays a wikia article or search results for X.", usage = "-wikia [search] X")
	public void onWikiaCommand(Guild guild, MessageChannel channel, String[] args)
	{
		String baseUrl = "http://endless-sky.wikia.com/api/v1/";
		boolean search = false;
		if (args[0].toLowerCase().equals("search"))
			search = true;

		StringBuilder builder = new StringBuilder();
		if (search){
			for(int i = 1; i < args.length; ++i){
			    builder.append(" " + args[i]);
			}
		}
		else{
			for(String s : args){
				builder.append(" " + s);
			}
		}
		String query = "";
		try{
			query = java.net.URLEncoder.encode(builder.toString().trim(), "UTF-8");
		}catch(IOException e){
			e.printStackTrace();
		}

		try{
			JSONObject json = getJson(new URL(baseUrl + "Search/List?query=" + query));

			if (search){
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Results");
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription("Found the following results for '" + query + "':\n");
				for (Object o : json.getJSONArray("items")){
					JSONObject r = (JSONObject) o;
					eb.appendDescription("\n\n" + r.getString("title") + ":\n" + r.getString("url"));
				}
				channel.sendMessage(eb.build()).queue();
			}
			else{
				channel.sendMessage(json.getJSONArray("items").getJSONObject(0).getString("url")).queue();
			}
		}
		catch (IOException e){
			if (e instanceof FileNotFoundException)
				channel.sendMessage("Nothing found.").queue();
			else
				e.printStackTrace(System.out);
		}
	}


	@Command(aliases = {"-translate"}, description = "Translates a query from a language 'source' to a language 'target'. Both 'source' and 'target' are optional ('source' can be auto-detected, 'target' defaults to english). Use the `list` parameter to get all supported languages.", usage = "-translate [source] [target] <query>\n-translate list")
	public void onTranslateCommand(Guild guild, TextChannel channel, String[] args, User author) {
		if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
			if (channel.getTopic().contains("spam") && channel.getName().contains("spam")) {
				StringBuilder sb = new StringBuilder("**Languages Supported by the Yandex Translation API:**");
				for (String[] pair : yandexGetLangs())
					sb.append("\n`" + pair[0] + "` (" + pair[1] + ")");
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("EndlessSky-Discord-Bot", bot.HOST_PUBLIC_URL);
				eb.setColor(guild.getMember(bot.getSelf()).getColor());
				eb.setDescription(sb.toString());
				channel.sendMessage(eb.build()).queue();
			}
			else
				channel.sendMessage("This is a long list... please only use this command in a channel dedicated to spam").queue();
		}
		else if(args.length > 0){
			String result;
			if (yandexIsSupportedLang(args[0]))
				if (yandexIsSupportedLang(args[1]))
					result = yandexTranslate(args[0], args[1], String.join(" ", Arrays.asList(args).subList(2, args.length)));
				else
					result = yandexTranslate(null, args[0], String.join(" ", Arrays.asList(args).subList(1, args.length)));
			else
				result = yandexTranslate(null, "en", String.join(" ", args));
			channel.sendMessage(result).queue();
		}
	}


	/**
	 * Gets all supported languages of the Yandex Translation API.
	 * @return   ArrayList<>      possibly empty ArrayList containing key-Language pair Arrays such as ["en", "English"].
	 */
	private ArrayList<String[]> yandexGetLangs() {
		ArrayList<String[]> results = new ArrayList<>();
		try{
			JSONObject json = getJson(new URL("https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=" + bot.getKey("YANDEXTRANSLATE") + "&ui=en"));
			Iterator<?> keys = json.getJSONObject("langs").keys();
			while (keys.hasNext()) {
				String[] pair = new String[2];
				String key = (String)keys.next();
				pair[0] = key;
				pair[1] = json.getJSONObject("langs").getString(key);
				results.add(pair);
			}
			// sort alphabetically by keys
			Collections.sort(results, new Comparator<String[]>() {
				@Override
				public int compare(String[] pair1, String[] pair2) {
					return pair1[0].compareTo(pair2[0]);
				}
			});
		}
		catch (IOException e){
			e.printStackTrace();
		}
		return results;
	}


	/**
	 * Checks if a language is supported by the Yandex Translation API.
	 * @Param    String    language    A String representing a language code (e.g. "en").
	 * @return   boolean               True if the language is supported.
	 */
	private boolean yandexIsSupportedLang(String language) {
		for (String pair[] : yandexGetLangs())
			if (pair[0].equalsIgnoreCase(language))
				return true;
		return false;
	}



	/**
	 * Detects the language of a text.
	 * @Param    String    text    A String representing a language code (e.g. "en").
	 * @return   String            possibly null language code that has been returned by the API.
	 */
	private String yandexDetectLang(String text) {
		String result = null;
		try {
			text = URLEncoder.encode(text, "UTF-8");
			JSONObject json = getJson(new URL("https://translate.yandex.net/api/v1.5/tr.json/detect?key=" + bot.getKey("YANDEXTRANSLATE") + "&text=" + text));
			return json.getString("lang");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}



	/**
	 * Translates a text using the Yandex Translation API.
	 * @Param    String    source    the language code (e.g. "en") of the source language, may ne null (will be auto-detected).
	 * @Param    String    target    the language code (e.g. "en") of the target language, may ne null (defaults to english).
	 * @return   String              the translated text, possibly null.
	 */
	private String yandexTranslate(@Nullable String source, String target, String text) {
		try {
			text = URLEncoder.encode(text, "UTF-8");
			if (source == null)
				source = yandexDetectLang(text);
			String baseUrl= "https://translate.yandex.net/api/v1.5/tr.json/translate";
			URL url = new URL(baseUrl + "?key=" + bot.getKey("YANDEXTRANSLATE") + "&text=" + text + "&lang=" + source + "-" + target);
			return getJson(url).getJSONArray("text").getString(0);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return null;
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
