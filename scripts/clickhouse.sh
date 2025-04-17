#!/usr/local/bin/fish

# Init ClickHouse Database inside docker container

docker pull bitnami/clickhouse:24.8.4
docker run -e ALLOW_EMPTY_PASSWORD=yes -v /root/docker_db_data/clickhouse:/bitnami/clickhouse -p 9000:9000 -p 8123:8123 --name clickhouse -d bitnami/clickhouse:24.8.4
