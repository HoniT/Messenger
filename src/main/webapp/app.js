let currentUsername = null;
let activeChatTarget = null;
let pollingInterval = null;

const API_BASE = '/api';

async function apiCall(endpoint, method, body = null) {
    const options = {
        method: method,
        headers: { 'Content-Type': 'application/json' }
    };

    if (body && method !== 'GET') {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE}${endpoint}`, options);

    if (response.status === 401) {
        handleLogoutUI();
        throw new Error("Session expired. Please log in again.");
    }

    if (!response.ok) {
        throw new Error(await response.text());
    }
    return response.status !== 204 ? response.json() : null;
}

async function register() {
    const username = document.getElementById('auth-username').value;
    const password = document.getElementById('auth-password').value;
    try {
        const res = await apiCall('/users/register', 'POST', { username, password });
        handleAuthSuccess(res);
    } catch (e) {
        document.getElementById('auth-error').innerText = e.message;
    }
}

async function login() {
    const username = document.getElementById('auth-username').value;
    const password = document.getElementById('auth-password').value;
    try {
        const res = await apiCall('/users/login', 'POST', { username, password });
        handleAuthSuccess(res);
    } catch (e) {
        document.getElementById('auth-error').innerText = e.message;
    }
}

function handleAuthSuccess(userData) {
    currentUsername = userData.username;
    localStorage.setItem('chat_username', currentUsername);

    document.getElementById('auth-error').innerText = '';
    document.getElementById('auth-container').classList.add('hidden');
    document.getElementById('chat-container').classList.remove('hidden');
    document.getElementById('current-user-label').innerText = `Logged in as: ${currentUsername}`;

    loadContacts();
    pollingInterval = setInterval(() => {
        if (activeChatTarget) loadMessages();
        loadContacts();
    }, 5000);
}

async function logout() {
    try {
        await apiCall('/users/logout', 'POST');
    } catch (e) {
        console.error(e);
    } finally {
        handleLogoutUI();
    }
}

function handleLogoutUI() {
    currentUsername = null;
    activeChatTarget = null;
    clearInterval(pollingInterval);
    localStorage.removeItem('chat_username');

    // Wipe the old user's data from the screen
    document.getElementById('contact-list').innerHTML = '';
    document.getElementById('message-list').innerHTML = '';
    document.getElementById('chat-target-label').innerText = 'Select a user to chat';
    document.getElementById('message-input-container').classList.add('hidden');
    document.getElementById('new-contact-username').value = '';

    // Hide chat and show auth
    document.getElementById('chat-container').classList.add('hidden');
    document.getElementById('auth-container').classList.remove('hidden');
}

async function loadContacts() {
    try {
        const users = await apiCall('/messages', 'GET');
        const list = document.getElementById('contact-list');
        list.innerHTML = '';

        users.forEach(user => {
            const li = document.createElement('li');
            li.innerText = user;
            li.onclick = () => openChat(user);
            list.appendChild(li);
        });
    } catch (e) {
        console.error("Failed to load contacts:", e);
    }
}

function openChatFromInput() {
    const target = document.getElementById('new-contact-username').value;
    if (target) openChat(target);
}

function openChat(targetUsername) {
    activeChatTarget = targetUsername;
    document.getElementById('chat-target-label').innerText = `Chatting with: ${targetUsername}`;
    document.getElementById('message-input-container').classList.remove('hidden');
    loadMessages();
}

async function loadMessages() {
    if (!activeChatTarget) return;
    try {
        const messages = await apiCall(`/messages?username=${activeChatTarget}`, 'GET');
        const list = document.getElementById('message-list');
        list.innerHTML = '';

        messages.forEach(msg => {
            const div = document.createElement('div');
            div.className = `message ${msg.senderUsername === currentUsername ? 'sent' : 'received'}`;

            const payload = document.createElement('div');
            payload.innerText = msg.payload;

            const time = document.createElement('div');
            time.className = 'message-time';
            time.innerText = new Date(msg.sentAt).toLocaleString();

            div.appendChild(payload);
            div.appendChild(time);
            list.appendChild(div);
        });

        list.scrollTop = list.scrollHeight;
    } catch (e) {
        console.error("Failed to load messages:", e);
    }
}

async function sendMessage() {
    const input = document.getElementById('message-input');
    const message = input.value;
    if (!message || !activeChatTarget) return;

    try {
        await apiCall('/messages', 'POST', {
            targetUsername: activeChatTarget,
            message: message
        });
        input.value = '';
        loadMessages();
        loadContacts();
    } catch (e) {
        alert(e.message);
        console.error("Failed to send message:", e);
    }
}

function init() {
    const savedUsername = localStorage.getItem('chat_username');
    if (savedUsername) {
        handleAuthSuccess({ username: savedUsername });
    }
}

document.getElementById('message-input').addEventListener('keypress', function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        sendMessage();
    }
});

init();