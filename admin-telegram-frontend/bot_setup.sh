#!/bin/bash

# Скрипт для настройки Telegram бота с веб-приложением
# Использование: ./setup_bot.sh <имя_бота> <токен_бота>

# Проверка количества аргументов
if [ $# -ne 2 ]; then
    echo "Использование: $0 <имя_бота> <токен_бота>"
    echo "Пример: $0 example 1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"
    exit 1
fi

BOT_NAME="$1"
BOT_TOKEN="$2"

# Базовые URL
WEBAPP_URL="https://${BOT_NAME}.qq-pp.ru"
WEBHOOK_URL="https://${BOT_NAME}-api.qq-pp.ru/webhook/telegram"

# Функция для выполнения HTTP-запросов с обработкой ошибок
make_request() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    
    response=$(curl -s -w "\n%{http_code}" -X "$method" \
        -H "Content-Type: application/json" \
        -d "$data" \
        "https://api.telegram.org/bot${BOT_TOKEN}${endpoint}")
    
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ne 200 ]; then
        echo "Ошибка: HTTP $http_code"
        echo "Ответ: $response_body"
        return 1
    fi
    
    echo "$response_body"
    return 0
}

echo "=== Настройка Telegram бота: $BOT_NAME ==="
echo

# 1. Установка команд бота
echo "1. Установка команд бота..."
COMMANDS_DATA='{
    "commands": [
        {"command": "start", "description": "Запустить бота"},
        {"command": "request", "description": "Оставить заявку на менеджеру"}
    ],
    "scope": {
        "type": "default"
    },
    "language_code": "ru"
}'

result=$(make_request "POST" "/setMyCommands" "$COMMANDS_DATA")
if [ $? -eq 0 ]; then
    echo "✓ Команды бота успешно установлены"
    echo "  Доступные команды:"
    echo "    /start - Запустить бота"
else
    echo "✗ Ошибка установки команд бота"
    exit 1
fi

echo

# 1. Активация веб-приложения
echo "2. Активация веб-приложения..."
WEBAPP_DATA="{\"query_id\":\"setup_webapp_$(date +%s)\",\"user\":{\"id\":1,\"first_name\":\"Admin\"},\"auth_date\":$(date +%s),\"hash\":\"dummy_hash_for_setup\"}"

# Примечание: Активация веб-приложения обычно происходит через setChatMenuButton
# Устанавливаем кнопку меню с веб-приложением
MENU_BUTTON_DATA="{\"menu_button\":{\"type\":\"web_app\",\"text\":\"Open App\",\"web_app\":{\"url\":\"$WEBAPP_URL\"}}}"

result=$(make_request "POST" "/setChatMenuButton" "$MENU_BUTTON_DATA")
if [ $? -eq 0 ]; then
    echo "✓ Веб-приложение активировано"
    echo "  URL: $WEBAPP_URL"
else
    echo "✗ Ошибка активации веб-приложения"
    exit 1
fi

echo

# 2. Настройка URL для веб-приложения (через BotFather)
echo "3. Настройка URL веб-приложения..."
echo "   Примечание: URL веб-приложения устанавливается через BotFather"
echo "   Вы должны вручную установить через BotFather командами:"
echo "   /mybots → Выбрать бота → Bot Settings → Menu Button"
echo "   Установить URL: $WEBAPP_URL"
echo

# 3. Настройка вебхука
echo "4. Настройка вебхука..."
WEBHOOK_DATA="{\"url\":\"$WEBHOOK_URL\",\"max_connections\":100,\"allowed_updates\":[\"message\",\"callback_query\",\"inline_query\"]}"

echo "   URL вебхука: $WEBHOOK_URL"
echo "   Устанавливаю вебхук..."

result=$(make_request "POST" "/setWebhook" "$WEBHOOK_DATA")
if [ $? -eq 0 ]; then
    echo "✓ Вебхук успешно установлен"
    
    # Проверка информации о вебхуке
    echo "   Проверяю информацию о вебхуке..."
    info_result=$(make_request "GET" "/getWebhookInfo" "{}")
    if [ $? -eq 0 ]; then
        echo "   Статус вебхука: активен"
        echo "   URL: $WEBHOOK_URL"
    fi
else
    echo "✗ Ошибка установки вебхука"
    exit 1
fi

echo
echo "=== Настройка завершена ==="
echo
echo "Итоговая конфигурация:"
echo "  Бот: $BOT_NAME"
echo "  Команды: 1 команда установлена (см. выше)"
echo "  Веб-приложение: $WEBAPP_URL"
echo "  Вебхук: $WEBHOOK_URL"
echo
echo "Не забудьте:"
echo "  1. Настроить DNS записи для:"
echo "     - $BOT_NAME.qq-pp.ru → ваш сервер"
echo "     - $BOT_NAME-api.qq-pp.ru → ваш API сервер"
echo "  2. Установить URL веб-приложения через BotFather"
echo "  3. Настроить SSL сертификаты для HTTPS"
echo "  4. Проверить команды бота, введя / в чате с ботом"