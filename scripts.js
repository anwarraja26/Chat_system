const wsUrl = (() => {
    const base = window.location.host || "localhost:8080";
    return `ws://${base}/chat-backend/chat`;
})();

const socket = new WebSocket(wsUrl);
const chatBox = document.getElementById("chatBox");

socket.onopen = () => appendSystemMessage("Connected to chat server");

socket.onmessage = (event) => {
    try {
        const msg = JSON.parse(event.data);
        if (msg.type === "error") {
            appendSystemMessage(msg.message);
            return;
        }
        appendChatMessage(msg.sender, msg.content, msg.timestamp);
    } catch (err) {
        appendSystemMessage("Received malformed message");
    }
};

socket.onerror = () => appendSystemMessage("WebSocket error");

socket.onclose = () => appendSystemMessage("Disconnected from chat server");

function sendMessage() {
    const sender = document.getElementById("username").value.trim();
    const content = document.getElementById("messageInput").value.trim();

    if (!sender || !content || socket.readyState !== WebSocket.OPEN) {
        return;
    }

    const msg = {
        sender: sender,
        content: content,
        timestamp: Date.now()
    };

    socket.send(JSON.stringify(msg));
    document.getElementById("messageInput").value = "";
}

function appendChatMessage(sender, content, timestamp) {
    const p = document.createElement("p");
    const time = timestamp ? new Date(timestamp).toLocaleTimeString() : "";
    p.innerHTML = `<b>${sender}:</b> ${content} <small>${time}</small>`;
    chatBox.appendChild(p);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function appendSystemMessage(message) {
    const p = document.createElement("p");
    p.className = "system";
    p.textContent = message;
    chatBox.appendChild(p);
    chatBox.scrollTop = chatBox.scrollHeight;
}
