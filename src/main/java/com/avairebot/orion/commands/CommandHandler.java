package com.avairebot.orion.commands;

import com.avairebot.orion.Orion;
import com.avairebot.orion.contracts.commands.Command;
import com.avairebot.orion.database.controllers.GuildController;
import com.avairebot.orion.database.transformers.GuildTransformer;
import com.avairebot.orion.exceptions.InvalidCommandPrefixException;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.utils.Checks;

import java.util.*;

public class CommandHandler {

    private static final Map<List<String>, CommandContainer> COMMANDS = new HashMap<>();

    /**
     * Get command container from the given command instance.
     *
     * @param command The command instance.
     * @return Possibly-null, The registered command container instance.
     */
    public static CommandContainer getCommand(Command command) {
        for (Map.Entry<List<String>, CommandContainer> entry : COMMANDS.entrySet()) {
            if (entry.getValue().getCommand().isSame(command)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Get the command matching the message raw contents first argument, both
     * the command prefix and the command trigger must match for the command
     * to be returned, if the guild/server that the command was executed
     * in has a custom prefix set the custom prefix will be used to
     * match the command instead.
     * <p>
     * If a commands priority is set to {@link CommandPriority#IGNORED}
     * the command will be omitted from the search.
     *
     * @param message The JDA message object for the current message.
     * @return Possibly-null, The command matching the given command with the highest priority.
     */
    public static CommandContainer getCommand(Message message) {
        return getCommand(message, message.getRawContent().split(" ")[0].toLowerCase());
    }

    /**
     * Gets the command matching the given command, both the command prefix
     * and the command trigger must match for the command to be returned,
     * if the guild/server that the command was executed in has a
     * custom prefix set, the custom prefix will be used to
     * match the command instead.
     * <p>
     * If no commands was found matching the given command string, the guilds
     * aliases will be checked instead if the current guild has any.
     *
     * @param orion   The Orion application class instance.
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given command with the highest priority, or the alias command matching the given command.
     */
    public static CommandContainer getCommand(Orion orion, Message message, String command) {
        CommandContainer commandContainer = getCommand(message);
        if (commandContainer != null) {
            return commandContainer;
        }
        return getCommandByAlias(orion, message, command);
    }

    /**
     * Get the command matching the given command, both the command prefix
     * and the command trigger must match for the command to be returned,
     * if the guild/server that the command was executed in has a
     * custom prefix set the custom prefix will be used to
     * match the command instead.
     * <p>
     * If a commands priority is set to {@link CommandPriority#IGNORED}
     * the command will be omitted from the search.
     *
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given command with the highest priority.
     */
    public static CommandContainer getCommand(Message message, String command) {
        List<CommandContainer> commands = new ArrayList<>();
        for (Map.Entry<List<String>, CommandContainer> entry : COMMANDS.entrySet()) {
            String commandPrefix = entry.getValue().getCommand().generateCommandPrefix(message);
            for (String trigger : entry.getKey()) {
                if (command.equalsIgnoreCase(commandPrefix + trigger)) {
                    commands.add(entry.getValue());
                }
            }
        }

        return getHighPriorityCommandFromCommands(commands);
    }

    /**
     * Gets the command matching the given command alias for the current message if
     * the message was sent in a guild and the guild has at least one alias set.
     *
     * @param orion   The Orion application class instance.
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given alias with the highest priority.
     */
    public static CommandContainer getCommandByAlias(Orion orion, Message message, String command) {
        GuildTransformer transformer = GuildController.fetchGuild(orion, message);
        if (transformer == null || transformer.getAliases().isEmpty()) {
            return null;
        }

        String[] aliasArguments = null;
        String commandString = command.split(" ")[0].toLowerCase();
        List<CommandContainer> commands = new ArrayList<>();
        for (Map.Entry<String, String> entry : transformer.getAliases().entrySet()) {
            if (commandString.startsWith(entry.getKey())) {
                CommandContainer commandContainer = getCommand(message, entry.getValue().split(" ")[0]);
                if (commandContainer != null) {
                    commands.add(commandContainer);
                    aliasArguments = entry.getValue().split(" ");
                }
            }
        }

        CommandContainer commandContainer = getHighPriorityCommandFromCommands(commands);

        if (commandContainer == null) {
            return null;
        }

        if (aliasArguments == null || aliasArguments.length == 1) {
            return commandContainer;
        }
        return new AliasCommandContainer(commandContainer, Arrays.copyOfRange(aliasArguments, 1, aliasArguments.length));
    }

    /**
     * Get any command matching the given command trigger, this method will
     * use a lazy comparison by omitting the command prefix and only
     * comparing the command triggers, if a commands priority is
     * set to {@link CommandPriority#IGNORED} the command will
     * be omitted from the search.
     *
     * @param commandTrigger The command trigger that should be lazy searched for.
     * @return Possibly-null, The command matching the given command trigger with the highest priority.
     */
    public static CommandContainer getLazyCommand(String commandTrigger) {
        List<CommandContainer> commands = new ArrayList<>();
        for (Map.Entry<List<String>, CommandContainer> entry : COMMANDS.entrySet()) {
            if (entry.getValue().getPriority().equals(CommandPriority.IGNORED)) {
                continue;
            }

            for (String trigger : entry.getKey()) {
                if (commandTrigger.equalsIgnoreCase(trigger)) {
                    commands.add(entry.getValue());
                }
            }
        }

        return getHighPriorityCommandFromCommands(commands);
    }

    /**
     * Gets the highest priority command from the given command
     * list, if the list is empty null is returned instead.
     *
     * @param commands The list of commands matching some query.
     * @return Possibly-null, The command container with the highest priority.
     */
    private static CommandContainer getHighPriorityCommandFromCommands(List<CommandContainer> commands) {
        if (commands.isEmpty()) {
            return null;
        }

        if (commands.size() == 1) {
            return commands.get(0);
        }

        return commands.stream().sorted((first, second) -> {
            if (first.getPriority().equals(second.getPriority())) {
                return 0;
            }
            return first.getPriority().isGreaterThan(second.getPriority()) ? -1 : 1;
        }).findFirst().get();
    }

    /**
     * Register the given command into the command handler, creating the
     * command container and saving it into the commands collection.
     *
     * @param command The command that should be registered into the command handler.
     */
    public static void register(Command command) {
        Category category = CategoryHandler.fromCommand(command);
        Checks.notNull(category, String.format("%s :: %s", command.getName(), "Invalid command category, command category"));
        Checks.notNull(command.getDescription(), String.format("%s :: %s", command.getName(), "Command description"));

        for (String trigger : command.getTriggers()) {
            for (Map.Entry<List<String>, CommandContainer> entry : COMMANDS.entrySet()) {
                for (String subTrigger : entry.getKey()) {
                    if (Objects.equals(category.getPrefix() + trigger, entry.getValue().getDefaultPrefix() + subTrigger)) {
                        throw new InvalidCommandPrefixException(category.getPrefix() + trigger, command.getName(), entry.getValue().getCommand().getName());
                    }
                }
            }
        }

        COMMANDS.put(command.getTriggers(), new CommandContainer(command, category));
    }

    /**
     * Gets a collection of all the commands
     * registered into the command handler.
     *
     * @return A collection of all the commands registered with the command handler.
     */
    public static Collection<CommandContainer> getCommands() {
        return COMMANDS.values();
    }
}
