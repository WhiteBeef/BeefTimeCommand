package ru.whitebeef.timecommandbeef.commands;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.tuple.Pair;

import ru.whitebeef.timecommandbeef.Main;

public class PlanCommandExecutor implements TabExecutor {

	private final Timer timer;
	private final List<Pair<LocalDateTime, PlannedCommand>> commands = new LinkedList<>();

	private static final List<String> digits;
	private static final Map<String, Duration> timeUnits;

	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final Pattern timePattern = Pattern.compile("\\G([0-9]+[hmsdwy])");

	static {
		digits = Arrays.stream("123456789".split("")).collect(Collectors.toList());
		timeUnits = new HashMap<>();
		timeUnits.put("h", Duration.ofHours(1));
		timeUnits.put("m", Duration.ofMinutes(1));
		timeUnits.put("s", Duration.ofSeconds(1));
		timeUnits.put("d", Duration.ofDays(1));
		timeUnits.put("w", Duration.ofDays(7));
		timeUnits.put("y", Duration.ofSeconds(31556952));

	}

	public PlanCommandExecutor() {
		timer = new Timer();
	}

	@Override
	public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s,
			@Nonnull String[] args) {
		switch (args.length) {
		case 0:
			return digits;
		case 1:
			if (args[0].isEmpty())
				return digits;
			String lastChar = args[0].split("")[args[0].length() - 1];
			if (digits.contains(lastChar)) {
				return prefix(args[0], timeUnits.keySet());
			} else if (timeUnits.containsKey(lastChar)) {
				return prefix(args[0], digits);
			} else {
				return new ArrayList<>();
			}
		default:
			return null;
		}
	}

	@Override
	public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s,
			@Nonnull String[] args) {
		if (args.length < 2)
			return false;
		LocalDateTime time = parseTime(args[0]);
		if (time == null)
			return false;
		String command = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
		plan(command, time);
		return true;
	}

	/*
	 * @param s Время в формате 1h10m...
	 * 
	 * @return Время указанное в s в формате LocalDateTime. Если в s не время,
	 * возвращает null
	 */
	@Nullable
	public static LocalDateTime parseTime(String s) {
		Duration duration = Duration.ZERO;
		Matcher matcher = timePattern.matcher(s);
		while (matcher.find()) {
			String group = matcher.group();
			Duration d = timeUnits.get(group.split("")[group.length() - 1]);
			int multiplier = Integer.parseInt(group.substring(0, group.length() - 1));
			duration = duration.plus(d.multipliedBy(multiplier));
		}
		return duration.equals(Duration.ZERO) ? null : LocalDateTime.now().plus(duration);
	}

	private List<String> prefix(String prefix, Collection<String> collection) {
		return collection.stream().map(s -> prefix + s).collect(Collectors.toList());
	}

	void plan(String command, LocalDateTime time) {
		plan(new PlannedCommand(command, UUID.randomUUID()), time);
	}

	void plan(PlannedCommand cmd, LocalDateTime time) {
		commands.add(Pair.of(time, cmd));
		timer.schedule(cmd, Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
	}

	public void initialize() {
		load();
		int autoSaveMinutes = 30;
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(Main.class), this::save, 20 * 60,
				20 * 60 * autoSaveMinutes);
	}

	public void disable() {
		timer.purge();
		timer.cancel();
		save();
	}

	private static class PlannedCommand extends TimerTask {
		private final String command;
		private final UUID uuid;

		PlannedCommand(String command, UUID uuid) {
			this.command = command;
			this.uuid = uuid;
		}

		@Override
		public void run() {
			Main plugin = Main.getPlugin(Main.class);
			Bukkit.getScheduler().runTask(plugin, () -> {
				World world = Bukkit.getWorlds().get(0);
				Boolean value = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
				world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, value);
				plugin.planCommandExecutor.commands.removeIf(e -> e.getRight().equals(this));
			});
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			PlannedCommand that = (PlannedCommand) o;

			if (!command.equals(that.command))
				return false;
			return uuid.equals(that.uuid);
		}

		@Override
		public int hashCode() {
			int result = command.hashCode();
			result = 31 * result + uuid.hashCode();
			return result;
		}
	}

	private void save() {
		Main plugin = Main.getPlugin(Main.class);
		try {
			FileConfiguration plan = new YamlConfiguration();
			File saveFile = new File(plugin.getDataFolder(), "plan.yml");
			if (!saveFile.exists()) {
				if (!plugin.getDataFolder().exists())
					plugin.getDataFolder().mkdir();
				saveFile.createNewFile();
			}
			commands.forEach(pair -> {
				String key = pair.getRight().uuid.toString();
				String command = pair.getRight().command;
				String time = pair.getLeft().format(formatter);
				ConfigurationSection section = plan.createSection(key);
				section.set("command", command);
				section.set("time", time);
			});
			plan.save(saveFile);
		} catch (IOException exception) {
			plugin.getLogger().severe("Возникла ошибка при сохранении:");
			exception.printStackTrace();
		}
	}

	private void load() {
		Main plugin = Main.getPlugin(Main.class);
		try {
			File saveFile = new File(plugin.getDataFolder(), "plan.yaml");
			if (!saveFile.exists()) {
				return;
			}
			FileConfiguration plan = new YamlConfiguration();
			plan.load(saveFile);
			plan.getKeys(false).forEach(uuid -> {
				String command = plan.getString(uuid + ".command");
				String timeString = plan.getString(uuid + ".time");
				if (command == null || timeString == null)
					return;
				LocalDateTime time = LocalDateTime.parse(timeString, formatter);
				PlannedCommand cmd = new PlannedCommand(command, UUID.fromString(uuid));
				plan(cmd, time);
			});
			plan.save(saveFile);
		} catch (IOException | InvalidConfigurationException exception) {
			plugin.getLogger().severe("Возникла ошибка при загрузке команд с plan.yml:");
			exception.printStackTrace();
		}
	}
}
