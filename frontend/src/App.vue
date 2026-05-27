<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const token = ref(localStorage.getItem('talking-together-token') || '')
const currentUser = ref(JSON.parse(localStorage.getItem('talking-together-user') || 'null'))
const authMode = ref('login')
const authForm = ref({ username: '', password: '' })
const rooms = ref([])
const managedRooms = ref([])
const joinedRooms = ref([])
const selectedRoom = ref(null)
const newRoomName = ref('')
const blacklistUsername = ref('')
const blacklistReason = ref('')
const content = ref('')
const messages = ref([])
const connected = ref(false)
const errorMessage = ref('')
let socket = null

const isLoggedIn = computed(() => Boolean(token.value && currentUser.value))
const canManageSelectedRoom = computed(() => selectedRoom.value?.role === 'ADMIN')
const connectionLabel = computed(() => (connected.value ? '已连接' : '未连接'))

function authHeaders() {
  return {
    Authorization: `Bearer ${token.value}`,
    'Content-Type': 'application/json'
  }
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(token.value ? authHeaders() : { 'Content-Type': 'application/json' }),
      ...(options.headers || {})
    }
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || '请求失败')
  }

  return response.status === 204 ? null : response.json()
}

async function submitAuth() {
  try {
    const result = await api(`/api/auth/${authMode.value}`, {
      method: 'POST',
      body: JSON.stringify(authForm.value)
    })
    token.value = result.token
    currentUser.value = result.user
    localStorage.setItem('talking-together-token', result.token)
    localStorage.setItem('talking-together-user', JSON.stringify(result.user))
    authForm.value.password = ''
    errorMessage.value = ''
    await refreshRooms()
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

function logout() {
  closeSocket()
  token.value = ''
  currentUser.value = null
  selectedRoom.value = null
  messages.value = []
  localStorage.removeItem('talking-together-token')
  localStorage.removeItem('talking-together-user')
}

async function refreshRooms() {
  if (!isLoggedIn.value) {
    return
  }
  try {
    const [all, managed, joined] = await Promise.all([
      api('/api/rooms'),
      api('/api/rooms/managed'),
      api('/api/rooms/joined')
    ])
    rooms.value = all
    managedRooms.value = managed
    joinedRooms.value = joined
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

async function createRoom() {
  const name = newRoomName.value.trim()
  if (!name) {
    return
  }
  try {
    const room = await api('/api/rooms', {
      method: 'POST',
      body: JSON.stringify({ name })
    })
    newRoomName.value = ''
    await refreshRooms()
    await selectRoom(room)
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

async function joinRoom(room) {
  try {
    const joined = await api(`/api/rooms/${room.id}/join`, { method: 'POST' })
    await refreshRooms()
    await selectRoom(joined)
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

async function stopRoom(room) {
  try {
    await api(`/api/rooms/${room.id}/stop`, { method: 'POST' })
    if (selectedRoom.value?.id === room.id) {
      closeSocket()
      selectedRoom.value = null
      messages.value = []
    }
    await refreshRooms()
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

async function blacklistUser() {
  const username = blacklistUsername.value.trim()
  if (!selectedRoom.value || !username) {
    return
  }
  try {
    await api(`/api/rooms/${selectedRoom.value.id}/blacklist`, {
      method: 'POST',
      body: JSON.stringify({ username, reason: blacklistReason.value.trim() })
    })
    blacklistUsername.value = ''
    blacklistReason.value = ''
    errorMessage.value = '已拉黑该用户'
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

async function selectRoom(room) {
  selectedRoom.value = room
  messages.value = []
  closeSocket()
  try {
    messages.value = await api(`/api/rooms/${room.id}/messages`)
    connect(room.id)
  } catch (error) {
    errorMessage.value = friendlyError(error)
  }
}

function connect(roomId) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  socket = new WebSocket(`${protocol}://${window.location.host}/ws/chat?roomId=${roomId}&token=${token.value}`)

  socket.onopen = () => {
    connected.value = true
    errorMessage.value = ''
  }

  socket.onmessage = (event) => {
    messages.value.push(JSON.parse(event.data))
  }

  socket.onclose = () => {
    connected.value = false
  }

  socket.onerror = () => {
    errorMessage.value = 'WebSocket 连接异常，请确认你已加入聊天室且未被拉黑'
  }
}

function closeSocket() {
  if (socket) {
    socket.close()
    socket = null
  }
  connected.value = false
}

function sendMessage() {
  const trimmedContent = content.value.trim()
  if (!trimmedContent || !socket || socket.readyState !== WebSocket.OPEN) {
    return
  }
  socket.send(JSON.stringify({ content: trimmedContent }))
  content.value = ''
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : ''
}

function friendlyError(error) {
  return error.message
    .replaceAll('"', '')
    .replaceAll('{timestamp:', '')
    .slice(0, 180)
}

onMounted(() => {
  refreshRooms()
})

onBeforeUnmount(() => {
  closeSocket()
})
</script>

<template>
  <main class="page">
    <section v-if="!isLoggedIn" class="auth-card">
      <p class="eyebrow">Talking Together</p>
      <h1>账号登录</h1>
      <p class="muted">注册账号后即可创建、加入和管理聊天室。</p>

      <form class="auth-form" @submit.prevent="submitAuth">
        <input v-model="authForm.username" maxlength="50" placeholder="账号" />
        <input v-model="authForm.password" maxlength="80" type="password" placeholder="密码" />
        <button type="submit">{{ authMode === 'login' ? '登录' : '注册' }}</button>
      </form>

      <button class="link-button" type="button" @click="authMode = authMode === 'login' ? 'register' : 'login'">
        {{ authMode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
      </button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </section>

    <section v-else class="app-shell">
      <aside class="sidebar">
        <header class="user-bar">
          <div>
            <p class="eyebrow">当前用户</p>
            <strong>{{ currentUser.username }}</strong>
          </div>
          <button class="secondary" type="button" @click="logout">退出</button>
        </header>

        <form class="room-create" @submit.prevent="createRoom">
          <input v-model="newRoomName" maxlength="80" placeholder="新聊天室名称" />
          <button type="submit">创建</button>
        </form>

        <div class="room-section">
          <h2>可加入聊天室</h2>
          <article v-for="room in rooms" :key="room.id" class="room-item" :class="{ selected: selectedRoom?.id === room.id }">
            <button type="button" @click="room.joined ? selectRoom(room) : joinRoom(room)">
              <strong>{{ room.name }}</strong>
              <span>创建者：{{ room.ownerUsername }}</span>
            </button>
            <span v-if="room.joined" class="badge">{{ room.role === 'ADMIN' ? '管理员' : '已加入' }}</span>
          </article>
        </div>

        <div class="room-section">
          <h2>我管理的聊天室</h2>
          <article v-for="room in managedRooms" :key="room.id" class="room-item" :class="{ selected: selectedRoom?.id === room.id }">
            <button type="button" @click="selectRoom(room)">
              <strong>{{ room.name }}</strong>
              <span>{{ room.active ? '运行中' : '已停用' }}</span>
            </button>
            <button v-if="room.active" class="danger" type="button" @click="stopRoom(room)">停用</button>
          </article>
          <p v-if="managedRooms.length === 0" class="empty small">还没有创建聊天室。</p>
        </div>

        <div class="room-section">
          <h2>我加入的聊天室</h2>
          <article v-for="room in joinedRooms" :key="room.id" class="room-item" :class="{ selected: selectedRoom?.id === room.id }">
            <button type="button" @click="selectRoom(room)">
              <strong>{{ room.name }}</strong>
              <span>{{ room.active ? '可聊天' : '已停用' }}</span>
            </button>
          </article>
          <p v-if="joinedRooms.length === 0" class="empty small">还没有加入聊天室。</p>
        </div>
      </aside>

      <section class="chat-card">
        <header class="chat-header">
          <div>
            <p class="eyebrow">聊天室</p>
            <h1>{{ selectedRoom ? selectedRoom.name : '请选择聊天室' }}</h1>
          </div>
          <span class="status" :class="{ online: connected }">{{ connectionLabel }}</span>
        </header>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <div v-if="selectedRoom" class="messages">
          <article v-for="message in messages" :key="message.id || `${message.sender}-${message.sentAt}`" class="message">
            <div class="message-meta">
              <strong>{{ message.sender }}</strong>
              <span>{{ formatTime(message.sentAt) }}</span>
            </div>
            <p>{{ message.content }}</p>
          </article>
          <p v-if="messages.length === 0" class="empty">还没有消息，发一句开始聊天吧。</p>
        </div>
        <div v-else class="messages placeholder">
          <p>从左侧选择或创建一个聊天室。</p>
        </div>

        <form v-if="selectedRoom" class="composer" @submit.prevent="sendMessage">
          <input v-model="content" maxlength="1000" placeholder="输入消息，按回车发送" />
          <button type="submit" :disabled="!connected || !content.trim()">发送</button>
        </form>

        <form v-if="selectedRoom && canManageSelectedRoom" class="blacklist-form" @submit.prevent="blacklistUser">
          <input v-model="blacklistUsername" maxlength="50" placeholder="要拉黑的账号" />
          <input v-model="blacklistReason" maxlength="200" placeholder="原因，可选" />
          <button class="danger" type="submit">拉黑用户</button>
        </form>
      </section>
    </section>
  </main>
</template>
