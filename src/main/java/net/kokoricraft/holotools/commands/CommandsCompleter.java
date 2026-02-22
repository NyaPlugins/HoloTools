package net.kokoricraft.holotools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandsCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {
            case 1 : {
                ArrayList<String> cmds = new ArrayList<>();
                if(sender.hasPermission("holotools.command.give")) cmds.add("give");
                if(sender.hasPermission("holotools.command.reload")) cmds.add("reload");
                return cmds.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            case 2 : {
                if(!sender.hasPermission("holotools.command.give")) break;
                return "give".equalsIgnoreCase(args[0]) ?
                        Arrays.asList("holocrafter", "holowardrobe", "holobridge").stream()
                                .filter(s -> s.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList()) : null;
            }
            case 3 : {
                if(!sender.hasPermission("holotools.command.give")) break;
                return "give".equalsIgnoreCase(args[0]) && isNumeric(args[1]) ?
                        Arrays.asList("1", "2", "3", "4", "5").stream()
                                .filter(s -> s.startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList()) : null;
            }
            default : break;
        };
        return new ArrayList<>();
    }

    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }
}
