# telegram-store

Leiningen-шаблоны для развёртывания Telegram-магазина на Clojure / ClojureScript.

## Компоненты

| Папка | Назначение |
|---|---|
| `telegram-store-backend` | Шаблон бэкенда каталога (Clojure, http-kit) |
| `telegram-store-frontend` | Шаблон фронтенда каталога (ClojureScript, Reagent) |
| `admin-telegram-backend` | Шаблон бэкенда админки (Clojure, http-kit) |
| `admin-telegram-frontend` | Шаблон фронтенда админки (ClojureScript, Reagent) |

## Развёртывание из шаблона

```bash
# Создать проект из шаблона
./setup_project.sh

# Пересобрать после изменений в шаблоне
./recompile_project.sh
```

## Конфигурация

Каждый компонент требует файл `app_state.edn` с токеном бота, ключами API и параметрами подключения к БД. Шаблон файла — в `resources/leiningen/new/`.
