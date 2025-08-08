package io.fabianbuthere.simplecars.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarConstantsCommand {

    // Map to store original values for reference
    private static final Map<String, Double> ORIGINAL_VALUES = new HashMap<>();
    private static final Map<String, Field> CONSTANT_FIELDS = new HashMap<>();

    private static List<String> players;

    // Initialize the constants mapping
    static {
        try {
            Class<BaseCarEntity> clazz = BaseCarEntity.class;

            // Get all the static double fields and their original values
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        (field.getType() == double.class || field.getType() == Double.class)) {

                    field.setAccessible(true);
                    String constantName = field.getName().toLowerCase();
                    CONSTANT_FIELDS.put(constantName, field);
                    ORIGINAL_VALUES.put(constantName, (Double) field.get(null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final SuggestionProvider<CommandSourceStack> CONSTANT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(CONSTANT_FIELDS.keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> ACTION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(new String[]{"get", "set"}, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("carconst")
                        .requires(source -> source.hasPermission(2)) // Requires OP level 2
                        .then(Commands.argument("constant", StringArgumentType.word())
                                .suggests(CONSTANT_SUGGESTIONS)
                                .then(Commands.literal("get")
                                        .executes(CarConstantsCommand::getConstant))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(CarConstantsCommand::setConstant))))
                        .executes(CarConstantsCommand::listConstants)
        );
    }

    private static int listConstants(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("Available car constants:").withStyle(ChatFormatting.GOLD), false);

        for (String constantName : CONSTANT_FIELDS.keySet()) {
            try {
                Field field = CONSTANT_FIELDS.get(constantName);
                double currentValue = (Double) field.get(null);
                double originalValue = ORIGINAL_VALUES.get(constantName);

                Component message;
                if (currentValue != originalValue) {
                    message = Component.literal("  " + constantName + ": ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(String.format("%.6f", currentValue))
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" (original: " + String.format("%.6f", originalValue) + ")")
                                    .withStyle(ChatFormatting.GRAY));
                } else {
                    message = Component.literal("  " + constantName + ": ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(String.format("%.6f", currentValue))
                                    .withStyle(ChatFormatting.GREEN));
                }

                source.sendSuccess(() -> message, false);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error reading constant: " + constantName));
            }
        }

        return 1;
    }

    private static int getConstant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String constantName = StringArgumentType.getString(context, "constant").toLowerCase();
        CommandSourceStack source = context.getSource();

        if (!CONSTANT_FIELDS.containsKey(constantName)) {
            source.sendFailure(Component.literal("Unknown constant: " + constantName)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        try {
            Field field = CONSTANT_FIELDS.get(constantName);
            double currentValue = (Double) field.get(null);
            double originalValue = ORIGINAL_VALUES.get(constantName);

            Component message = Component.literal(constantName + ": ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(String.format("%.6f", currentValue))
                            .withStyle(ChatFormatting.YELLOW));

            if (currentValue != originalValue) {
                message = message.copy().append(Component.literal(" (original: " + String.format("%.6f", originalValue) + ")")
                        .withStyle(ChatFormatting.GRAY));
            }

            Component finalMessage = message;
            source.sendSuccess(() -> finalMessage, false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reading constant: " + constantName));
            return 0;
        }
    }

    private static int setConstant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String constantName = StringArgumentType.getString(context, "constant").toLowerCase();
        double newValue = DoubleArgumentType.getDouble(context, "value");
        CommandSourceStack source = context.getSource();

        if (!CONSTANT_FIELDS.containsKey(constantName)) {
            source.sendFailure(Component.literal("Unknown constant: " + constantName)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (source.isPlayer()) {
            if (players == null) { players = new java.util.ArrayList<>(); }
            String playerName = source.getPlayerOrException().getName().getString();
            if (!players.contains(playerName)) {
                source.sendSystemMessage(Component.literal("This is a development option. All constants will be reset on server restart.").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
                players.add(source.getPlayerOrException().getName().getString());
            }
        }

        try {
            Field field = CONSTANT_FIELDS.get(constantName);

            double oldValue = (Double) field.get(null);
            field.set(null, newValue);

            Component message = Component.literal("Set " + constantName + " from ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(String.format("%.6f", oldValue))
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal(" to ")
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format("%.6f", newValue))
                            .withStyle(ChatFormatting.GREEN));

            source.sendSuccess(() -> message, true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error setting constant: " + constantName + " - " + e.getMessage()));
            return 0;
        }
    }
}