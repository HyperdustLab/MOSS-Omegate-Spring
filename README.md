# 


## Runtime

- Java 17
- Node.js 18+
- MySQL 8
- DashScope API KEY（or others）
- Redis-Stack

  extend redis to support vector query

    ```shell
    docker run -d \
    --name redis-stack \
    --restart=always \
    -v redis-data:/data \
    -p 6379:6379 \
    -p 8001:8001 \
    -e REDIS_ARGS="--requirepass 123456" redis/redis-stack:latest
    ```

## How to run it

### 1.clone代码

```shell
git clone 
```

### 2. idea open the project

### 3. modify the config
modify application.yml中的API-KEY, MySQL, Redis-Stack, Neo4j config
### 4. run the project

launch backend

1. run ServerApplication.java
2. target/generated-sources/annotations右键mark directory as/generated source root

run the front end，under /front-end/

- npm run install
- npm run api
- npm run dev

