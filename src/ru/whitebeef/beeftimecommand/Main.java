package ru.whitebeef.timecommandbeef;

import java.io.File;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ru.whitebeef.timecommandbeef.commands.PlanCommandExecutor;

public class Main extends JavaPlugin implements Runnable {

	public PlanCommandExecutor planCommandExecutor;

	public final Logger log = Logger.getLogger("Minecraft");
	public final String PLUGIN_NAME = "[BeefTimeCommand] ";
	public HashMap<LocalTime, Boolean> dateList = new HashMap<>();
	public int CONFIG_MODE = 1;

	@Override
	public void onEnable() {
		File config = new File(getDataFolder() + File.separator + "config.yml");
		if (!config.exists()) {
			log.info(PLUGIN_NAME + "Конфиг не найден. Создаю новый");
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
		}
		CONFIG_MODE = getConfig().getInt("configMode");
		for (LocalTime time : getArraysWithDate())
			dateList.put(time, false);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this, 20 * 60L, 20L);

		planCommandExecutor = new PlanCommandExecutor();
		planCommandExecutor.initialize();
		super.getCommand("plancommand").setExecutor(planCommandExecutor);

		log.info(PLUGIN_NAME + "Успешно включился");
	}

	@Override
	public void onDisable() {
		planCommandExecutor.disable();
		log.info(PLUGIN_NAME + "Успешно выключился");
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
		log.info(PLUGIN_NAME + "Загруженное время: " + dateListIn);
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
				int hour = Integer.parseInt(stringArrayTime[0]);
				int minute = Integer.parseInt(stringArrayTime[1]);
				dateListIn.add(LocalTime.of(hour, minute));
			} catch (NumberFormatException | DateTimeException e) {
				log.warning(PLUGIN_NAME + "Не удалось считать время " + stringTime
						+ "! Проверьте правильность конфига! \\n Время " + stringTime + " пропущено!");
			}
		} else
			log.warning(PLUGIN_NAME + "Не удалось считать время " + stringTime
					+ "! Проверьте правильность конфига! \\n Время " + stringTime + " пропущено!");
		return dateListIn;
	}
}
