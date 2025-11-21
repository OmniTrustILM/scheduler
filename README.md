# CZERTAINLY Scheduler

> This repository is part of the commercial open-source project CZERTAINLY. You can find more information about the project at [CZERTAINLY](https://github.com/CZERTAINLY/CZERTAINLY) repository, including the contribution guide.

## Database requirements

`Scheduler` requires the PostgreSQL database to store the data.

## Docker container

`Scheudler` is provided as a Docker container. Use the `czertainly/czertainly-scheduler:tagname` to pull the required image from the repository. It can be configured using the following environment variables:

| Variable                       | Description                                                        | Required                                           | Default value |
|--------------------------------|--------------------------------------------------------------------|----------------------------------------------------|---------------|
| `JDBC_URL`                     | JDBC URL for database access                                       | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_USERNAME`                | Username to access the database                                    | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_PASSWORD`                | Password to access the database                                    | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `DB_SCHEMA`                    | Database schema to use                                             | ![](https://img.shields.io/badge/-NO-red.svg)      | `scheduler`   |
| `PORT`                         | Port where the service is exposed                                  | ![](https://img.shields.io/badge/-NO-red.svg)      | `8080`        |
| `JAVA_OPTS`                    | Customize Java system properties for running application           | ![](https://img.shields.io/badge/-NO-red.svg)      | `N/A`         |
| `BROKER_URL`                   | URL of Message broker includes protocol and port                   | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `BROKER_USER`                  | Username to access RabbitMQ service                                | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `BROKER_PASSWORD`              | Password to access RabbitMQ service                                | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `BROKER_VHOST`                 | RabbitMQ vhost                                                     | ![](https://img.shields.io/badge/-NO-red.svg)      | `czertainly`  |
| `BROKER_EXCHANGE`              | Message broker exchange name                                       | ![](https://img.shields.io/badge/-NO-red.svg)      | `czertainly`  |
| `BROKER_EXCHANGE_PREFIX`       | Message broker exchange prefix (RabbitMQ needs 'exchanges/' prefix | ![](https://img.shields.io/badge/-NO-red.svg)      | ``         |
| `BROKER_ROUTING_KEY_SCHEDULER` | Routing key for scheduler                                          | ![](https://img.shields.io/badge/-NO-red.svg)      | `scheduler`   |
