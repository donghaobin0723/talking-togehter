CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(80) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_auth_tokens_token_expires_at (token, expires_at),
  CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_rooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  owner_id BIGINT NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  stopped_at DATETIME NULL,
  INDEX idx_chat_rooms_active_created_at (active, created_at),
  INDEX idx_chat_rooms_owner_id_created_at (owner_id, created_at),
  CONSTRAINT fk_chat_rooms_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  joined_at DATETIME NOT NULL,
  UNIQUE KEY uk_room_members_room_user (room_id, user_id),
  INDEX idx_room_members_user_role_joined_at (user_id, role, joined_at),
  CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
  CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_blacklists (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  reason VARCHAR(200) NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_room_blacklists_room_user (room_id, user_id),
  CONSTRAINT fk_room_blacklists_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
  CONSTRAINT fk_room_blacklists_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_room_blacklists_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_username VARCHAR(50) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  sent_at DATETIME NOT NULL,
  INDEX idx_chat_messages_room_sent_at (room_id, sent_at),
  CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
  CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
