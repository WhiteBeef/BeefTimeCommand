package ru.whitebeef.timecommandbeef.commands;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import ru.whitebeef.timecommandbeef.commands.utils.TimeConverter;

public class RepeatCommandExecutor implements TabExecutor {

	public RepeatCommandExecutor() {
	}

	@Override
	public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s,
			@Nonnull String[] args) {
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
		LocalDateTime timeStart = TimeConverter.parseTime(args[0]);
		LocalDateTime timeRepeat = TimeConverter.parseTime(args[0]);
		if (timeStart == null || timeRepeat == null)
			return false;
		String command = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
		PlanCommandExecutor planCommand = new PlanCommandExecutor();
		planCommand.onCommand(s, cmd, label, new String[] { args[0], command });
		planCommand.onCommand(s, cmd, label,
				new String[] { args[0], "repeatcommand " + args[1] + " " + args[1] + " " + command });
		return true;
	}

}
