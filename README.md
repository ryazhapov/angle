# ANGLE: Language Learning Platform

![](/images/angle-logo.png =x250)

### Сервис для изучения английского языка (аналог skyeng.ru)

- Ролевая модель: клиенты, преподаватели, администраторы
- Назначение уроков для клиентов
- Биллинг для клиентов и преподавателей

## Stack
- **zio** — Core effect / runtime
- **http4s** — API server
- **liquibase** — DB migration
- **postgres** — Database
- **doobie** — JDBC layer
- **hikari** — Connection pool for DB
- **quill** — QDSL for DB management
- **circe** — JSON serialization
- **zio-logging** — Logging
- **zio-config** — Reading config file
- **docker** — Container
- **zio-test** — Tests // TODO
- **tapir** — Api documentation // TODO

## Запуск

`docker-compose up -d
`

`sbt compile
`

`sbt run
`

## Схема базы данных

![](/images/database.png)


## Примеры запросов


* `POST /api/v1/auth/sign_up` - Регистрация пользователя

* `POST /api/v1/schedule/create` - Создание расписания учителем

* `POST /api/v1/schedule/find` - Поиск расписания для урока

* `POST /api/v1/lesson/create` - Бронирование урока учеником

* `POST /api/v1/payment/lesson_completed/{id}` - Подтверждение учителем окончания урока id

* `GET /api/v1/replenishment/` - Список всех пополнений учеником

* `GET /api/v1/payment/{id}` - Получение оплаты с id 1


## API для Postman

[API.json![](\API.json)](API.json)
