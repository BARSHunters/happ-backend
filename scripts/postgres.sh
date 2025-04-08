#!/usr/local/bin/fish

# Init PostgreSQL Database inside docker container

docker pull postgres
docker run -itd -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -v ./data:/var/lib/postgresql/data --name postgres postgres
