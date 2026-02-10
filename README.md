# CZERTAINLY Scheduler

> This repository is part of the commercial open-source project CZERTAINLY. You can find more information about the project at [CZERTAINLY](https://github.com/CZERTAINLY/CZERTAINLY) repository, including the contribution guide.

## Database requirements

`Scheduler` requires the PostgreSQL database to store the data.

## Docker container

`Scheduler` is provided as a Docker container. Use the `czertainly/czertainly-scheduler:tagname` to pull the required image from the repository. It can be configured using the following environment variables:

| Variable                       | Description                                                        | Required                                              | Default value |
|--------------------------------|--------------------------------------------------------------------|-------------------------------------------------------|---------------|
| `JDBC_URL`                     | JDBC URL for database access                                       | ![](https://img.shields.io/badge/-YES-success.svg)    | `N/A`         |
| `JDBC_USERNAME`                | Username to access the database                                    | ![](https://img.shields.io/badge/-YES-success.svg)    | `N/A`         |
| `JDBC_PASSWORD`                | Password to access the database                                    | ![](https://img.shields.io/badge/-YES-success.svg)    | `N/A`         |
| `DB_SCHEMA`                    | Database schema to use                                             | ![](https://img.shields.io/badge/-NO-red.svg)         | `scheduler`   |
| `PORT`                         | Port where the service is exposed                                  | ![](https://img.shields.io/badge/-NO-red.svg)         | `8080`        |
| `JAVA_OPTS`                    | Customize Java system properties for running application           | ![](https://img.shields.io/badge/-NO-red.svg)         | `N/A`         |
| `BROKER_TYPE`                  | Message broker type (`RABBITMQ` or `SERVICEBUS`)                   | ![](https://img.shields.io/badge/-NO-red.svg)         | `SERVICEBUS`  |
| `BROKER_URL`                   | URL of Message broker includes protocol and port                   | ![](https://img.shields.io/badge/-YES-success.svg)    | `N/A`         |
| `BROKER_USER`                  | Username/SAS key name for broker authentication                    | ![](https://img.shields.io/badge/-CONDITIONAL-yellow.svg) | `N/A`     |
| `BROKER_PASSWORD`              | Password/SAS key for broker authentication                         | ![](https://img.shields.io/badge/-CONDITIONAL-yellow.svg) | `N/A`     |
| `BROKER_AZURE_TENANT_ID`       | Azure AD tenant ID for AAD authentication                          | ![](https://img.shields.io/badge/-CONDITIONAL-yellow.svg) | `N/A`     |
| `BROKER_AZURE_CLIENT_ID`       | Azure AD application (client) ID                                   | ![](https://img.shields.io/badge/-CONDITIONAL-yellow.svg) | `N/A`     |
| `BROKER_AZURE_CLIENT_SECRET`   | Azure AD client secret value                                       | ![](https://img.shields.io/badge/-CONDITIONAL-yellow.svg) | `N/A`     |
| `BROKER_VHOST`                 | RabbitMQ vhost (for RabbitMQ use `/` as default)                   | ![](https://img.shields.io/badge/-NO-red.svg)         | `/`           |
| `BROKER_SESSION_CACHE_SIZE`    | For RabbitMQ - CachingSessionFacotry pool size           | ![](https://img.shields.io/badge/-NO-red.svg)      | `10`          |
| `BROKER_EXCHANGE`              | Message broker exchange name                                       | ![](https://img.shields.io/badge/-NO-red.svg)         | `czertainly`  |
| `BROKER_ROUTING_KEY_SCHEDULER` | Routing key for scheduler                                          | ![](https://img.shields.io/badge/-NO-red.svg)         | `scheduler`   |

### Message Broker Authentication

The scheduler supports two message brokers with different authentication methods:

**RabbitMQ:**
- Requires `BROKER_USER` and `BROKER_PASSWORD`

**Azure Service Bus:**
- **SAS Authentication:** Use `BROKER_USER` (SAS key name) and `BROKER_PASSWORD` (SAS key)
- **AAD Authentication:** Use `BROKER_AZURE_TENANT_ID`, `BROKER_AZURE_CLIENT_ID`, and `BROKER_AZURE_CLIENT_SECRET`

> **Note:** For Azure Service Bus, you must provide either SAS credentials or AAD credentials, but not both. When using AAD authentication, ensure you use the client secret **value**, not the secret ID.
