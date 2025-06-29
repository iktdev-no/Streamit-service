
# Media Server Configuration
This document provides a guide to configure your media server environment. It includes settings for the database, content management, server configuration, security, and debugging.
## Prerequisites
- Nginx or Open Resty (or any other web server with reverse proxy capabilities)
- Ensure you have a database set up and accessible. (MySQL or MariaDB is recommended)
- Have your media content organized in a folder structure.

## Folder structure
```plaintext
/media
└── content
    └── <media-name>
        ├── <media-file>.mp4 # Main media file
        ├── <media-file>.jpg # Optional, cover image for the media
        └── <subtitle>
            └── <language code iso3 e.g. eng>
                ├── <media-file>.srt
                ├── <media-file>.vtt
                ├── <media-file>.ass
                └── <media-file>.smi
```

The media content is mixed within the folders known as "collections" in the program
Movies are just single files, while episodes keeps their definition of season and episode in the filename, e.g.: `Show name - S01E01 - Episode Name.mp4`


## Environment Setup
```yaml
# Database
DATABASE_ADDRESS: #<your-database-address>
DATABASE_PORT: #(optional)
DATABASE_USERNAME: #<your-database-username>
DATABASE_PASSWORD: #<your-database-password>
DATABASE_NAME: #<your-database-name>

# Content and assets
CONTENT_FOLDER: /media/content # Path to where your media content is stored
CONFIG_FOLDER: /conf # Default, Path to where your configuration files are stored
ASSETS_FOLDER: /assets # Default, Path to where your assets are stored and where it unpacks
CONTENT_FRESH_DAYS: 5 # Number of days to show the content as "new"
SERIE_AGE: 30 # Default, Number of days to show episodes as "new"
CONTENT_CONTINUE: 10 # Default, Number of series to show in the "Continue Watching" section


# Server
SERVER_PORT: 8080 # Default port for the server
CONFIG_IS_SELF_SIGNED: true # Set to true if you are using a self-signed certificate
CONFIG_LAN_ADDRESS: # Set to where the server is accessible, e.g., http://localhost:8080
CONFIG_WAN_ADDRESS: # Set to where the server is accessible from outside, e.g., http://example.com
AVAHI_SERVICE_FOLDER: /etc/avahi/services # Default, Path to where the avahi service file is stored, if it does not exist it will fall back to using `CONFIG_FOLDER` under "avahi" subfolder

# Security
JWT_SECRET: #<your-jwt-secret> # Secret key for JWT authentication, required for access over internet
JWT_EXPIRY: "1h" #(optional) Expiry time for JWT tokens, e.g., "1h", "2d" 

# PFNS
PFNS_API_TOKEN: #(optional) Required for setup with QR-code

# Debug and Logging
SINGLE_ENTRY_PATHS: false # Set to true to log only single entry paths, no secure and open
MODE: "prod" # default
```

## Automatic Discovery
While you can manually input the lan address, the server can automatically discover it. This is useful for local networks where the server is accessed via a LAN address.
AN avahi service file is automatically generated and placed in the assets folder, which can be used to discover the server on the local network.
It will require `CONFIG_LAN_ADDRESS` to be set, otherwise it will not work.
If you are using Docker, you can set the `AVAHI_SERVICE_FOLDER` to `/etc/avahi/services` to make it available on the local network.

# Setup
## Database Setup
If you are not usign a root user, you need to create a database and a user with the necessary permissions.
```sql
START TRANSACTION;

CREATE DATABASE streamit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'streamit'@'%' IDENTIFIED BY 'your_password_here';

GRANT ALL PRIVILEGES ON streamit.* TO 'streamit'@'%';

FLUSH PRIVILEGES;

COMMIT;
```

## Docker-compose Setup
If you are using Docker, you can set up the media server using a `docker-compose.yml` file. Below is an example configuration:
It's also recommended to use a `.env` file

```yaml
version: '3.2'

networks:
  streamit:
    name: streamit
    driver: bridge
  services_service:
    external: true

services:

  streamit:
    hostname: streamit.service
    image: bskjon/streamit-service:latest
    container_name: streamit.service
    restart: always
    mem_limit: 512M
#    cpus: 1
    environment:
      TZ: ${TIME_ZONE}
      DATABASE_NAME: ${streamit_database_name}
      DATABASE_ADDRESS: ${streamit_database_address}
      DATABASE_PORT: ${streamit_database_port}
      DATABASE_USERNAME: ${streamit_database_username}
      DATABASE_PASSWORD: ${streamit_database_password}
      CONTENT_FRESH_DAYS: ${streamit_content_fresh_days}
      SERIE_AGE: ${streamit_serie_age}
      CONTENT_CONTINUE: ${streamit_content_continue}
      CONTENT_FOLDER: ${streamit_content_folder}
      JWT_SECRET: ${streamit_jwt_secret}
      JWT_EXPIRY: ${streamit_jwt_expiry}
      PFNS_API_TOKEN: ${streamit_pns_api_token}
    ports:
      - 192.168.2.250:8181:8080
    networks:
      - streamit
      - services_service
    volumes:
      - ${media_folder}:${streamit_content_folder}
      - ${data_folder}:/data/config
      - /etc/avahi/services:/etc/avahi/services
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/open/heartbeat"]

```

# `.env` File
```dotenv
streamit_time_zone=Europe/Oslo

streamit_database_name=streamit
streamit_database_address=db
streamit_database_port=3306
streamit_database_username=streamit
streamit_database_password=<your_database_password_here>

streamit_content_fresh_days=5
streamit_serie_age=1m
streamit_content_continue=10
streamit_content_folder=/media/stream

streamit_jwt_secret=<your_jwt_secret_here>
streamit_jwt_expiry=1y

streamit_pns_api_token=<your_pns_api_token_here>

media_folder=/media/raid16/streamit/
data_folder=./data/streamit

streamit_content_folder=/media/stream
streamit_config_folder=./data/config
streamit_assets_folder=./data/assets
streamit_avahi_service_folder=/etc/avahi/services

```

## OpenResty Configuration
If you are using OpenResty, you can set up a reverse proxy to the media server. Below is an example configuration for OpenResty:

**Important:** You must include `/secure` in the proxy path (e.g., `proxy_pass http://192.168.2.250:8181/secure`). If omitted, no requests will be authenticated.

### Wan / Internet Access
```nginx
server {
    listen 443;
    server_name streamit.example.com;
    ssl_certificate /etc/letsencrypt/live/streamit.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/streamit.example.com/privkey.pem;
    
    location / {
        default_type application/json;
    
        proxy_pass http://192.168.2.250:8181/secure
        proxy_set_header Host $server_name;
    
        # Preserve the original IP address
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    
        # Proxy timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

```

### Lan / Local Access
```nginx
server {
    listen       192.168.2.250:80;
    server_name  streamit.lan streamit.local;

    add_header 'Access-Control-Allow-Origin' *;
    add_header 'Access-Control-Allow-Credentials' 'true';
    add_header 'Access-Control-Allow-Headers' 'Authorization,Accept,Origin,DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Content-Range,Range';


    location / {
        index  app.html app.htm;
        proxy_pass http://192.168.2.250:8085/open;

        # Add headers to be passed to the second reverse proxy
        proxy_set_header Host $server_name;

        # Preserve the original IP address
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # Proxy timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```
![Docker Pulls](https://img.shields.io/docker/pulls/bskjon/streamit-service?style=flat-square&logo=docker&link=https%3A%2F%2Fhub.docker.com%2Fr%2Fbskjon%2Fstreamit-service)

