# Talking Together

一个基于 Spring Boot、MyBatis Plus、原生 WebSocket、MySQL、Vue 3 和 Vite 的在线交流网站基础项目，支持用户注册登录、聊天室创建/加入/停用、管理员拉黑用户和按聊天室实时通信。

## 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 18+
- MySQL 8 或兼容版本

## 初始化数据库

项目默认连接本地 MySQL `3306` 端口，用户名 `root`，密码 `dhb0723.`，数据库名 `talking_together`。

```sql
CREATE DATABASE IF NOT EXISTS talking_together
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

创建数据库后，请执行 [`src/main/resources/schema.sql`](src/main/resources/schema.sql) 初始化表结构。

如果需要修改连接信息，可以设置环境变量：

```bash
MYSQL_URL=jdbc:mysql://localhost:3306/talking_together?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=dhb0723.
```

## 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

可用接口：

- `GET /api/health`：健康检查
- `POST /api/auth/register`：注册账号
- `POST /api/auth/login`：登录并获取 Token
- `GET /api/auth/me`：获取当前用户
- `GET /api/rooms`：可加入的运行中聊天室
- `POST /api/rooms`：创建聊天室，创建者自动成为聊天室管理员
- `GET /api/rooms/managed`：我管理的聊天室
- `GET /api/rooms/joined`：我加入的聊天室
- `POST /api/rooms/{roomId}/join`：加入聊天室
- `POST /api/rooms/{roomId}/stop`：管理员停用聊天室
- `POST /api/rooms/{roomId}/blacklist`：管理员拉黑用户
- `GET /api/rooms/{roomId}/messages`：聊天室最近 50 条消息
- `WS /ws/chat?roomId={roomId}&token={token}`：指定聊天室的 WebSocket

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发服务器会代理 `/api` 和 `/ws` 到后端。

## 消息格式

前端发送到 WebSocket 的消息：

```json
{
  "content": "你好"
}
```

后端广播并保存后的消息：

```json
{
  "id": 1,
  "roomId": 1,
  "sender": "游客",
  "content": "你好",
  "sentAt": "2026-05-26T18:00:00"
}
```

## 权限说明

- 账号由用户自定义注册，密码使用 BCrypt 哈希后保存。
- 用户创建聊天室后自动成为该聊天室管理员。
- 普通成员需要先加入聊天室，才能读取历史消息和建立 WebSocket 聊天连接。
- 停用聊天室和拉黑用户仅允许聊天室管理员操作。
- 被拉黑用户不能加入、进入或在该聊天室发送消息。
