package org.example.telgrambotaiassistant.configuration;

import lombok.extern.slf4j.Slf4j;
import org.example.telgrambotaiassistant.Bot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class TelegramBotStartup {

  private final Bot bot;

  public TelegramBotStartup(Bot bot) {
    this.bot = bot;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(0)
  public void registerTelegramBot() {
    try {
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      botsApi.registerBot(bot);
      log.info("Telegram-бот @{} зарегистрирован, ожидаем сообщения", bot.getBotUsername());
    } catch (TelegramApiException e) {
      log.error("Не удалось зарегистрировать Telegram-бота", e);
      throw new IllegalStateException("Telegram bot registration failed", e);
    }
  }
}
