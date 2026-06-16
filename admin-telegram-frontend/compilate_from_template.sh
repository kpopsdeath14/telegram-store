#!/bin/bash

# Проверяем, передан ли параметр с названием проекта
if [ -z "$1" ]; then
    echo "Usage: $0 <project-name>"
    echo "Example: $0 my-awesome-project"
    exit 1
fi

PROJECT_NAME="$1"

echo "Creating project: $PROJECT_NAME"

rm -rf "$PROJECT_NAME"
lein new admin-telegram-frontend "$PROJECT_NAME"
cd "$PROJECT_NAME"
npm install --legacy-peer-deps --include=dev
if [ ! -x "./node_modules/.bin/shadow-cljs" ]; then
    echo "Error: shadow-cljs not found. Dev dependencies may not be installed."
    exit 1
fi
./node_modules/.bin/shadow-cljs release app
