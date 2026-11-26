package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The lab's web server. It listens on this computer only, answers one request at a time, and refuses requests that are
 * not from its own page: the Host header must be local, and anything that changes a value must carry a header that a
 * page from another site cannot send without permission this server never gives.
 */
public final class LabServer
{
	private static final int FIRST_PORT = 8917;
	private static final Gson GSON = new Gson();

	/** A control as the page sees it. */
	private static final class Control
	{
		String id;
		String file;
		String name;
		String type;
		Double value;
		int line;
		String doc;
		/** The generated class that holds a value written into drawing code, or null for a named value. */
		String holder;
		String key;
	}

	private final Path web;
	private final Map<String, Control> controls = new LinkedHashMap<>();
	private final Map<String, Double> changed = new LinkedHashMap<>();
	private final Map<String, Supplier<LabScenes.Scene>> factories = LabScenes.all();
	private final Map<String, LabScenes.Scene> scenes = new HashMap<>();
	private int port;

	private LabServer(Path web, Path controlFile) throws IOException
	{
		this.web = web;
		List<Control> read = GSON.fromJson(new String(Files.readAllBytes(controlFile), StandardCharsets.UTF_8),
			new TypeToken<List<Control>>()
			{
			}.getType());
		for (Control control : read)
			controls.put(control.id, control);
	}

	public static void main(String[] args) throws IOException
	{
		LabElements.sources = Paths.get(args[1]).resolveSibling("src").resolve("com/oveduumnakal/gielinorgo");
		LabServer lab = new LabServer(Paths.get(args[0]), Paths.get(args[1]));
		HttpServer server = lab.bind();
		server.createContext("/", lab::handle);
		server.start();
		String url = "http://127.0.0.1:" + lab.port + "/";
		System.out.println("UI Lab is running at " + url + "  (Ctrl+C stops it)");
		if (args.length > 2 && "open".equals(args[2]))
		{
			try
			{
				new ProcessBuilder(System.getProperty("os.name").startsWith("Windows")
					? new String[] {"rundll32", "url.dll,FileProtocolHandler", url}
					: new String[] {"xdg-open", url}).start();
			}
			catch (IOException e)
			{
				System.out.println("Open the address above in your browser.");
			}
		}
	}

	private HttpServer bind() throws IOException
	{
		IOException last = null;
		for (port = FIRST_PORT; port < FIRST_PORT + 10; port++)
		{
			try
			{
				return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress().getHostAddress(), port), 0);
			}
			catch (IOException e)
			{
				last = e;
			}
		}

		throw last;
	}

	private void handle(HttpExchange exchange) throws IOException
	{
		try
		{
			String host = exchange.getRequestHeaders().getFirst("Host");
			if (host == null || !(host.equals("127.0.0.1:" + port) || host.equals("localhost:" + port)))
			{
				send(exchange, 403, "text/plain", "forbidden");
				return;
			}

			boolean post = "POST".equals(exchange.getRequestMethod());
			if (post && !"1".equals(exchange.getRequestHeaders().getFirst("X-UI-Lab")))
			{
				send(exchange, 403, "text/plain", "forbidden");
				return;
			}

			URI uri = exchange.getRequestURI();
			Map<String, String> query = query(uri.getRawQuery());
			switch (uri.getPath())
			{
				case "/":
					send(exchange, 200, "text/html; charset=utf-8",
						new String(Files.readAllBytes(web.resolve("index.html")), StandardCharsets.UTF_8));
					break;
				case "/api/schema":
					send(exchange, 200, "application/json", GSON.toJson(schema()));
					break;
				case "/api/boxes":
					send(exchange, 200, "application/json", GSON.toJson(boxes(query)));
					break;
				case "/api/elements":
					send(exchange, 200, "application/json", GSON.toJson(elements(query)));
					break;
				case "/api/frame":
					frame(exchange, query);
					break;
				case "/api/set":
					requirePost(post);
					set(query.get("id"), query.get("value"));
					send(exchange, 200, "application/json", "{}");
					break;
				case "/api/reset":
					requirePost(post);
					reset(query.get("id"));
					send(exchange, 200, "application/json", "{}");
					break;
				case "/api/input":
					requirePost(post);
					current(query).input(query.get("type"), number(query, "x"), number(query, "y"), number(query, "button"),
						number(query, "t"));
					send(exchange, 200, "application/json", "{}");
					break;
				case "/api/restart":
					requirePost(post);
					scenes.clear();
					send(exchange, 200, "application/json", "{}");
					break;
				default:
					send(exchange, 404, "text/plain", "not found");
					break;
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			send(exchange, 500, "text/plain", String.valueOf(e));
		}
		finally
		{
			exchange.close();
		}
	}

	private static void requirePost(boolean post)
	{
		if (!post)
			throw new IllegalArgumentException("POST only");
	}

	private static Map<String, String> query(String raw) throws IOException
	{
		Map<String, String> out = new HashMap<>();
		if (raw == null)
			return out;

		for (String pair : raw.split("&"))
		{
			int at = pair.indexOf('=');
			if (at > 0)
				out.put(pair.substring(0, at), URLDecoder.decode(pair.substring(at + 1), "UTF-8"));
		}

		return out;
	}

	private static int number(Map<String, String> query, String name)
	{
		String value = query.get(name);
		return value == null ? 0 : (int) Math.round(Double.parseDouble(value));
	}

	private Map<String, Object> schema()
	{
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("scenes", new ArrayList<>(factories.keySet()));
		schema.put("themes", new ArrayList<>(LabTheme.themes().keySet()));
		schema.put("controls", controls.values());
		List<Map<String, Object>> colors = new ArrayList<>();
		for (Map.Entry<String, WindowTheme> theme : LabTheme.themes().entrySet())
		{
			for (ThemeColor name : ThemeColor.values())
			{
				String key = theme.getKey() + "/" + name.name();
				Color override = LabTheme.all().get(key);
				LabTheme.set(key, null);
				Color original = theme.getValue().color(name);
				LabTheme.set(key, override);
				if (original == null)
					continue;

				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("id", "theme:" + key);
				entry.put("theme", theme.getKey());
				entry.put("name", name.name());
				entry.put("value", original.getRGB() & 0xFFFFFFFFL);
				colors.add(entry);
			}
		}

		schema.put("themeColors", colors);
		schema.put("depth", LabDepth.settings());
		schema.put("changed", changed);
		return schema;
	}

	private void set(String id, String text)
	{
		double value = Double.parseDouble(text);
		apply(id, value);
		changed.put(id, value);
		refresh();
	}

	private void reset(String id)
	{
		List<String> ids = id == null ? new ArrayList<>(changed.keySet()) : java.util.Collections.singletonList(id);
		for (String one : ids)
		{
			changed.remove(one);
			if (one.startsWith("theme:"))
				LabTheme.set(one.substring(6), null);
			else if (one.startsWith("depth:"))
				LabDepth.reset(one);
			else if (one.startsWith("layout:"))
				LabBoxes.set(one.substring(7), 0);
			else if (controls.containsKey(one))
				apply(one, controls.get(one).value);
		}

		refresh();
	}

	private void apply(String id, double value)
	{
		if (id.startsWith("theme:"))
		{
			LabTheme.set(id.substring(6), new Color((int) (long) value, true));
			return;
		}

		if (id.startsWith("depth:"))
		{
			LabDepth.set(id, value);
			return;
		}

		if (id.startsWith("layout:"))
		{
			LabBoxes.set(id.substring(7), value);
			return;
		}

		Control control = controls.get(id);
		if (control == null)
			throw new IllegalArgumentException("no such control: " + id);

		String target = control.holder != null ? control.holder : control.file;
		String name = control.holder != null ? control.key : control.name;
		if ("Color".equals(control.type))
			LabRegistry.color(target, name, (int) (long) value);
		else
			LabRegistry.set(target, name, value);
	}

	private void refresh()
	{
		LabRegistry.refresh();
		scenes.clear();
	}

	private LabScenes.Scene scene(Map<String, String> query)
	{
		String name = query.get("scene");
		String theme = query.getOrDefault("theme", "Glass bronze");
		double scale = Math.max(0.5, Math.min(3.0, Double.parseDouble(query.getOrDefault("scale", "1"))));
		String key = name + "|" + theme + "|" + scale;
		LabScenes.Scene scene = scenes.get(key);
		if (scene == null)
		{
			Supplier<LabScenes.Scene> factory = factories.get(name);
			if (factory == null)
				throw new IllegalArgumentException("no such scene: " + name);

			scene = factory.get();
			scene.theme = LabTheme.themes().get(theme);
			scene.scale = scale;
			scenes.put(key, scene);
		}

		return scene;
	}

	/** @return the scene asked for, with the layouts set to report to it */
	private LabScenes.Scene current(Map<String, String> query)
	{
		LabScenes.Scene scene = scene(query);
		LabBoxes.target = scene.boxes;
		LabBoxes.unit = scene.scale;
		LabElements.target = scene.elements;
		return scene;
	}

	/** @return the panes and buttons of the page the scene shows, with where they are and how far they have moved */
	private List<Map<String, Object>> boxes(Map<String, String> query)
	{
		LabScenes.Scene scene = current(query);
		LabDepth.frame();
		scene.frame(number(query, "t"));
		String name = query.get("scene");
		String page = name.startsWith("Window: ") ? name.substring(8).split(" ")[0] + ": " : "-";
		List<Map<String, Object>> found = new ArrayList<>();
		scene.boxes.forEach((box, at) ->
		{
			if (!box.startsWith(page))
				return;

			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("name", box);
			entry.put("x", at.x);
			entry.put("y", at.y);
			entry.put("w", at.width);
			entry.put("h", at.height);
			entry.put("change", LabBoxes.change(box));
			found.add(entry);
		});
		return found;
	}

	/** @return every bar, text, icon, chip and portrait drawn, with how far each has been moved */
	private List<Map<String, Object>> elements(Map<String, String> query)
	{
		LabScenes.Scene scene = current(query);
		LabDepth.frame();
		LabElements.frame();
		scene.frame(number(query, "t"));
		List<Map<String, Object>> found = new ArrayList<>();
		for (LabElements.Drawn drawn : scene.elements)
		{
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("key", drawn.key);
			entry.put("name", drawn.name);
			entry.put("x", drawn.at.x);
			entry.put("y", drawn.at.y);
			entry.put("w", drawn.at.width);
			entry.put("h", drawn.at.height);
			entry.put("change", LabBoxes.change(drawn.key));
			found.add(entry);
		}

		return found;
	}

	private void frame(HttpExchange exchange, Map<String, String> query) throws IOException
	{
		boolean original = "1".equals(query.get("original"));
		Map<String, Double> kept = new LinkedHashMap<>(changed);
		if (original && !kept.isEmpty())
			reset(null);

		BufferedImage image;
		try
		{
			LabScenes.Scene scene = current(query);
			LabDepth.frame();
			LabElements.frame();
			image = scene.frame(number(query, "t"));
		}
		finally
		{
			if (original && !kept.isEmpty())
			{
				changed.putAll(kept);
				kept.forEach(this::apply);
				refresh();
			}
		}

		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(LookSheet.scene(image.getWidth(), image.getHeight()), 0, 0, null);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(out, "png", bytes);
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(200, bytes.size());
		try (OutputStream body = exchange.getResponseBody())
		{
			bytes.writeTo(body);
		}
	}

	private static void send(HttpExchange exchange, int status, String type, String text) throws IOException
	{
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", type);
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream body = exchange.getResponseBody())
		{
			body.write(bytes);
		}
	}
}
