package ru.whitebeef.timecommandbeef;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ru.whitebeef.timecommandbeef.commands.PlanCommandExecutor;
import ru.whitebeef.timecommandbeef.commands.PlanCommandFromExecutor;
import ru.whitebeef.timecommandbeef.commands.RepeatCommandExecutor;
import ru.whitebeef.timecommandbeef.commands.utils.CommandPlanner;

public class Main extends JavaPlugin implements Runnable {

	public PlanCommandExecutor planCommandExecutor;
	public CommandPlanner commandPlanner;
	public final Logger log = Logger.getLogger("Minecraft");
	public HashMap<LocalTime, Boolean> dateList = new HashMap<>();
	public int CONFIG_MODE = 1;

	@Override
	public void onEnable() {
		File config = new File(getDataFolder() + File.separator + "config.yml");
		if (!config.exists()) {
			getLogger().warning("Конфиг не найден. Создаю новый");
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
		}
		CONFIG_MODE = getConfig().getInt("configMode");
		for (LocalTime time : getArraysWithDate())
			dateList.put(time, false);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this, 20 * 60L, 20L);

		planCommandExecutor = new PlanCommandExecutor();
		commandPlanner = new CommandPlanner();

		commandPlanner.initialize();
		super.getCommand("plancommand").setExecutor(planCommandExecutor);
		super.getCommand("repeatcommand").setExecutor(new RepeatCommandExecutor());
		super.getCommand("plancommandfrom").setExecutor(new PlanCommandFromExecutor());

		onServerLoad();
		getLogger().info("Успешно включился");
	}

	private void onServerLoad() {
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				try {
					FileConfiguration commands = new YamlConfiguration();
					File saveFile = new File(getDataFolder(), "reloadcmds.yml");
					if (!saveFile.exists()) {
						getLogger().warning("Файл reloadcmds.yml не найден. Создаю новый");
						saveFile.createNewFile();
						commands.load(saveFile);
						commands.set("commands", new ArrayList<>());
						commands.save(saveFile);
					}
					commands.load(saveFile);
					for (String command : commands.getStringList("commands"))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				} catch (IOException | InvalidConfigurationException exception) {
					getLogger().severe("Возникла ошибка при загрузке команд с reloadcmds.yml:");
					exception.printStackTrace();
				}

			}
		}, 0l);
	}

	@Override
	public void onDisable() {
		commandPlanner.disable();
		getLogger().info("Успешно выключился");
	}

	@Override
	public void run() {
		LocalTime time = LocalTime.now();
		for (LocalTime date : dateList.keySet())
			if (date.getHour() == time.getHour() && Math.abs(date.getMinute() - time.getMinute()) == 0)
				if (!dateList.get(date)) {
					for (String command : getCommand(date))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
					dateList.put(date, true);
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							dateList.put(date, false);
						}
					}, 20 * 60L);
				}

	}

	public List<String> getCommand(LocalTime time) {
		String hour = time.getHour() < 10 ? 0 + "" + time.getHour() : time.getHour() + "";
		String minute = time.getMinute() < 10 ? 0 + "" + time.getMinute() : time.getMinute() + "";
		if (CONFIG_MODE == 1)
			return getConfig().getStringList("CommandsTime." + hour + ":" + minute);
		if (CONFIG_MODE == 2) {
			LocalDateTime dateTime = LocalDateTime.now();
			return getConfig().getStringList(
					"CommandsTime." + dateTime.getDayOfWeek().toString().toLowerCase() + "." + hour + ":" + minute);
		}
		return new ArrayList<String>();
	}

	public ArrayList<LocalTime> getArraysWithDate() {
		ArrayList<LocalTime> dateListIn = new ArrayList<LocalTime>();
		if (CONFIG_MODE == 1)
			for (String stringTime : getConfig().getConfigurationSection("CommandsTime").getKeys(true))
				dateListIn.addAll(parseStringToLocalTime(stringTime));
		if (CONFIG_MODE == 2)
			for (DayOfWeek day : DayOfWeek.values()) {
				for (String stringTime : getConfig()
						.getConfigurationSection("CommandsTime." + day.toString().toLowerCase()).getKeys(true))
					dateListIn.addAll(parseStringToLocalTime(stringTime));
			}
		dateListIn = optimizeArrayList(dateListIn);
		getLogger().info("Загруженное время: " + dateListIn);
		return dateListIn;
	}

	public ArrayList<LocalTime> optimizeArrayList(ArrayList<LocalTime> collection) {
		ArrayList<LocalTime> temp = new ArrayList<LocalTime>();
		for (LocalTime time : collection)
			if (!temp.contains(time))
				temp.add(time);
		return temp;
	}

	public ArrayList<LocalTime> parseStringToLocalTime(String stringTime) {
		ArrayList<LocalTime> dateListIn = new ArrayList<LocalTime>();
		String[] stringArrayTime = stringTime.split(":");
		if (stringArrayTime.length >= 2) {
			try {
				dateListIn
						.add(LocalTime.of(Integer.parseInt(stringArrayTime[0]), Integer.parseInt(stringArrayTime[1])));
			} catch (NumberFormatException | DateTimeException e) {
				getLogger().warning("Не удалось считать время " + stringTime
						+ "! Проверьте правильность конфига! \\n Время " + stringTime + " пропущено!");
			}
		} else
			getLogger().warning("Не удалось считать время " + stringTime
					+ "! Проверьте правильность конфига! \\n Время " + stringTime + " пропущено!");
		return dateListIn;
	}
}
