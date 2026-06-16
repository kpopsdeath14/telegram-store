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
lein new test-template "$PROJECT_NAME"
cd "$PROJECT_NAME"
npm install
npx shadow-cljs watch app