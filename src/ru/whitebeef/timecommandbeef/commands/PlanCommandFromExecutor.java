package ru.whitebeef.timecommandbeef.commands;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import ru.whitebeef.timecommandbeef.commands.utils.CommandPlanner;
import ru.whitebeef.timecommandbeef.commands.utils.TimeConverter;

public class PlanCommandFromExecutor implements TabExecutor {

	public PlanCommandFromExecutor() {

	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		switch (args.length) {
		case 0:
			return TimeConverter.digits;
		case 1:
			return TimeConverter.getTimeComplete(args, 1);
		case 2:
			return TimeConverter.getTimeComplete(args, 2);
		default:
			return new ArrayList<>();
		}
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (args.length < 3)
			return false;
		long fromTime = args[0].equalsIgnoreCase("now") ? System.currentTimeMillis() : Long.parseLong(args[0]);
		LocalDateTime time = TimeConverter.parseTime(args[1], fromTime);
		if (time == null)
			return false;
		String command = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
		CommandPlanner.plan(command, time);
		return true;
	}

}
