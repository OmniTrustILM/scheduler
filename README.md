# CZERTAINLY Scheduler

> This repository is part of the commercial open-source project CZERTAINLY. You can find more information about the project at [CZERTAINLY](https://github.com/CZERTAINLY/CZERTAINLY) repository, including the contribution guide.

## Database requirements

`Scheduler` requires the PostgreSQL database to store the data.

## Docker container

`Scheudler` is provided as a Docker container. Use the `czertainly/czertainly-scheduler:tagname` to pull the required image from the repository. It can be configured using the following environment variables:

| Variable            | Description                                              | Required                                           | Default value |
|---------------------|----------------------------------------------------------|----------------------------------------------------|---------------|
| `JDBC_URL`          | JDBC URL for database access                             | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_USERNAME`     | Username to access the database                          | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_PASSWORD`     | Password to access the database                          | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `DB_SCHEMA`         | Database schema to use                                   | ![](https://img.shields.io/badge/-NO-red.svg)      | `scheduler`   |
| `PORT`              | Port where the service is exposed                        | ![](https://img.shields.io/badge/-NO-red.svg)      | `8080`        |
| `JAVA_OPTS`         | Customize Java system properties for running application | ![](https://img.shields.io/badge/-NO-red.svg)      | `N/A`         |
| `RABBITMQ_HOST`     | Host to access RabbitMQ service                          | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `RABBITMQ_PORT`     | AMQP port where the RabbitMQ service is exposed          | ![](https://img.shields.io/badge/-NO-red.svg)      | `5672`        |
| `RABBITMQ_USERNAME` | Username to access RabbitMQ service                      | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `RABBITMQ_PASSWORD` | Password to access RabbitMQ service                      | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `RABBITMQ_VHOST`    | Virtual host to access RabbitMQ service                  | ![](https://img.shields.io/badge/-NO-red.svg)      | `czertainly`  |
