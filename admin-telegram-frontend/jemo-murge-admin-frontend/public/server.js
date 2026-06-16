const express = require('express');
const path = require('path');

const app = express();
const port = 3700;

// 1. Исправленный express.static
//  указываем текущую директорию как местоположение для статических файлов
app.use(express.static(path.join(__dirname)));

// 2. res.sendFile: Используем __dirname для формирования абсолютного пути
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(port, () => {
    console.log(`Сервер запущен на http://localhost:${port}`);
});
