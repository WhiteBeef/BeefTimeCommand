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

import net.md_5.bungee.api.ChatColor;
import ru.whitebeef.timecommandbeef.commands.utils.CommandPlanner;
import ru.whitebeef.timecommandbeef.commands.utils.TimeConverter;

public class PlanCommandExecutor implements TabExecutor {

	public PlanCommandExecutor() {
	}

	@Override
	public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label,
			@Nonnull String[] args) {
		switch (args.length) {
		case 0:
			return TimeConverter.digits;
		case 1:
			return TimeConverter.getTimeComplete(args, 1);
		default:
			return new ArrayList<>();
		}
	}

	@Override
	public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label,
			@Nonnull String[] args) {
		if (args[0].equalsIgnoreCase("disableAll")) {
			CommandPlanner.stop();
			sender.sendMessage(ChatColor.GREEN + "Вы успешно остановили все запланированные команды!");
			return true;
		}
		if (args.length < 2)
			return false;
		LocalDateTime time = TimeConverter.parseTime(args[0]);
		if (time == null)
			return false;
		String command = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
		CommandPlanner.plan(command, time);
		return true;
	}

}
