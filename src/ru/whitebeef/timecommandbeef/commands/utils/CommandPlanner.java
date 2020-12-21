package ru.whitebeef.timecommandbeef.commands.utils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.tuple.Pair;

import ru.whitebeef.timecommandbeef.Main;

public class CommandPlanner {

	private static List<Pair<LocalDateTime, PlannedCommand>> commands = new LinkedList<>();
	private static Timer timer = new Timer();
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public CommandPlanner() {
	}

	public static void plan(String command, LocalDateTime time) {
		plan(new PlannedCommand(command, UUID.randomUUID()), time);
	}

	static void plan(PlannedCommand cmd, LocalDateTime time) {
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

	public static void stop() {
		timer.purge();
		timer.cancel();
		timer = new Timer();
		commands.clear();
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
			try {
				Main plugin = Main.getPlugin(Main.class);
				Bukkit.getScheduler().runTask(plugin, () -> {
					World world = Bukkit.getWorlds().get(0);
					Boolean value = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
					world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
					world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, value);
					commands.removeIf(e -> e.getRight().equals(this));
				});
			} catch (IllegalStateException e) {
				return;
			}
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
			File saveFile = new File(plugin.getDataFolder(), "plan.yml");
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
