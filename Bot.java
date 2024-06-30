import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.EmbedBuilder;
import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class Bot extends ListenerAdapter {

    public static void main(String[] args) throws LoginException, InterruptedException {
        // Укажите токен вашего бота здесь
        String token = "bot token";

        JDABuilder builder = JDABuilder.createDefault(token);
        builder.addEventListeners(new Bot());
        JDA jda = builder.build().awaitReady();

        System.out.println("Bot is running!");

        // Создание и добавление команд
        SlashCommandData pingCommand = Commands.slash("ping", "Ответ Pong!");
        SlashCommandData helloCommand = Commands.slash("hello", "Приветствие!");
        SlashCommandData infoCommand = Commands.slash("info", "Получить информацию о сервере");
        SlashCommandData clearCommand = Commands.slash("clear", "Очистить все сообщения в канале");
        SlashCommandData diceCommand = Commands.slash("dice", "Бросить кубик (число от 1 до 10)");

        jda.updateCommands().addCommands(pingCommand, helloCommand, infoCommand, clearCommand, diceCommand).queue(
                success -> System.out.println("Commands registered globally successfully!"),
                error -> System.err.println("Failed to register global commands: " + error.getMessage())
        );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                sendPingResponse(event);
                break;
            case "hello":
                sendHelloResponse(event);
                break;
            case "info":
                sendServerInfo(event);
                break;
            case "clear":
                clearMessages(event);
                break;
            case "dice":
                rollDice(event);
                break;
            default:
                event.reply("Неизвестная команда!").queue();
                break;
        }
    }

    private void sendPingResponse(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        long userPing = System.currentTimeMillis() - event.getTimeCreated().toInstant().toEpochMilli();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Ping Command");
        embed.setDescription("Pong!");
        embed.addField("Пинг бота", gatewayPing + " ms", false);
        embed.addField("Пинг пользователя", userPing + " ms", false);
        embed.setColor(Color.GREEN);
        event.replyEmbeds(embed.build()).queue();
    }

    private void sendHelloResponse(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Hello Command");
        embed.setDescription("Привет, мир!");
        embed.setColor(Color.BLUE);
        event.replyEmbeds(embed.build()).queue();
    }

    private void sendServerInfo(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild != null) {
            long onlineCount = guild.getMembers().stream().filter(member -> member.getOnlineStatus().equals(net.dv8tion.jda.api.OnlineStatus.ONLINE)).count();
            long offlineCount = guild.getMembers().stream().filter(member -> member.getOnlineStatus().equals(net.dv8tion.jda.api.OnlineStatus.OFFLINE)).count();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Server Information");
            embed.setDescription("Информация о сервере:");
            embed.addField("Название", guild.getName(), false);
            embed.addField("Количество участников", String.valueOf(guild.getMemberCount()), false);
            embed.addField("Участников онлайн", String.valueOf(onlineCount), false);
            embed.addField("Участников оффлайн", String.valueOf(offlineCount), false);
            embed.addField("Владелец", guild.getOwner() != null ? guild.getOwner().getEffectiveName() : "Неизвестно", false);
            embed.setThumbnail(guild.getIconUrl());
            embed.setColor(Color.ORANGE);
            event.replyEmbeds(embed.build()).queue();
        } else {
            event.reply("Не удалось получить информацию о сервере.").queue();
        }
    }

    private void clearMessages(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getTextChannel();

        event.reply("Очистка всех сообщений в канале...").queue(response -> {
            purgeChannel(channel).thenRun(() -> {
                response.editOriginal("Все сообщения в канале были успешно удалены.").queue();
                channel.sendMessage("Канал был очищен!").queue();
            });
        });
    }

    private CompletableFuture<Void> purgeChannel(TextChannel channel) {
        return channel.getIterableHistory().takeAsync(100).thenCompose(messages -> {
            if (messages.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            List<Message> messagesToDelete = messages.subList(0, Math.min(100, messages.size()));
            channel.purgeMessages(messagesToDelete);
            return purgeChannel(channel);
        });
    }

    private void rollDice(SlashCommandInteractionEvent event) {
        Random random = new Random();
        int diceRoll = random.nextInt(10) + 1;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Dice Roll");
        embed.setDescription("Вы бросили кубик и выпало число: " + diceRoll);
        embed.setColor(Color.MAGENTA);
        event.replyEmbeds(embed.build()).queue();
    }
}
