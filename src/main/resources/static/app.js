const API_URL = '/api';
let currentToken = localStorage.getItem('jwt_token');
let currentUser = null;
let authAction = 'login';
let targetDrawingId = null;
let ws = null;
let isDrawing = false;
let context = null;
let drawingState = [];
let currentStroke = null;
let currentDrawingName = "Drawing";

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('drawingId')) {
        targetDrawingId = urlParams.get('drawingId');
    }

    if (currentToken) {
        parseJwtAndSetUser();
        if (targetDrawingId) {
            navigate('paint', targetDrawingId);
        } else {
            navigate('dashboard');
        }
    } else {
        navigate('auth');
    }
});

function bindEvents() {

    document.getElementById('nav-dashboard').addEventListener('click', (e) => { e.preventDefault(); navigate('dashboard'); });
    document.getElementById('logout-btn').addEventListener('click', logout);
    document.getElementById('auth-form').addEventListener('submit', handleAuth);
    document.getElementById('btn-login').addEventListener('click', () => authAction = 'login');
    document.getElementById('btn-register').addEventListener('click', () => authAction = 'register');
    document.getElementById('create-drawing-form').addEventListener('submit', createDrawing);
    document.getElementById('btn-undo').addEventListener('click', undo);
    document.getElementById('btn-export').addEventListener('click', exportCanvas);
    document.getElementById('btn-exit').addEventListener('click', exitPainting);
    document.getElementById('btn-save-rename').addEventListener('click', saveRename);
    document.addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 'z' && !document.getElementById('paint-view').classList.contains('d-none')) {
            undo();
        }
    });
}

function parseJwtAndSetUser() {
    try {
        const payload = JSON.parse(atob(currentToken.split('.')[1]));
        currentUser = payload['sub'];
    } catch (e) {
        console.error("Invalid token format");
        logout();
    }
}

function navigate(view, drawingId = null) {
    document.getElementById('auth-view').classList.add('d-none');
    document.getElementById('dashboard-view').classList.add('d-none');
    document.getElementById('paint-view').classList.add('d-none');
    
    const isAuthenticated = view !== 'auth';
    document.getElementById('logout-btn').classList.toggle('d-none', !isAuthenticated);
    document.getElementById('nav-dashboard').classList.toggle('d-none', !isAuthenticated);
    const userDisplay = document.getElementById('user-display');
    userDisplay.classList.toggle('d-none', !isAuthenticated);
    if (isAuthenticated && currentUser) {
        userDisplay.innerText = currentUser;
    }

    if (view === 'auth') {
        document.getElementById('auth-form').reset();
        document.getElementById('auth-view').classList.remove('d-none');
    } else if (view === 'dashboard') {
        document.getElementById('dashboard-view').classList.remove('d-none');
        loadDrawings();
        window.history.replaceState({}, document.title, "/");
    } else if (view === 'paint') {
        document.getElementById('paint-view').classList.remove('d-none');
        initCanvas(drawingId);
    }
}

async function handleAuth(event) {
    event.preventDefault();
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const errorDiv = document.getElementById('auth-error');

    try {
        const response = await fetch(`${API_URL}/${authAction}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Authentication failed');
        }

        currentToken = data.token;
        localStorage.setItem('jwt_token', currentToken);
        parseJwtAndSetUser();
        errorDiv.classList.add('d-none');

        if (targetDrawingId) {
            navigate('paint', targetDrawingId);
            targetDrawingId = null;
        } else {
            navigate('dashboard');
        }
    } catch (error) {
        errorDiv.innerText = error.message;
        errorDiv.classList.remove('d-none');
    }
}

function logout() {
    currentToken = null;
    currentUser = null;
    targetDrawingId = null;
    localStorage.removeItem('jwt_token');
    window.history.replaceState({}, document.title, "/");
    navigate('auth');
}

async function loadDrawings() {
    try {
        const response = await fetch(`${API_URL}/drawings`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });

        if (response.status === 401) {
            logout(); return;
        }

        const data = await response.json();
        renderDrawingsList('drawings-list-owned', data.owned, true);
        renderDrawingsList('drawings-list-shared', data.shared, false);
    } catch (e) {
        console.error("Failed to load drawings", e);
    }
}

function renderDrawingsList(containerId, drawings, isOwner) {
    const listDiv = document.getElementById(containerId);
    listDiv.replaceChildren();
    
    if (!drawings || drawings.length === 0) {
        const p = document.createElement('p');
        p.className = 'text-muted';
        p.textContent = 'No drawings found.';
        listDiv.appendChild(p);
        return;
    }

    drawings.forEach(d => {
        const card = document.createElement('div');
        card.className = 'col-md-4 mb-3';
        const shareLink = `${window.location.origin}/?drawingId=${d.id}`;

        const cardInner = document.createElement('div');
        cardInner.className = 'card h-100 shadow-sm';
        
        const cardBody = document.createElement('div');
        cardBody.className = 'card-body d-flex flex-column';
        
        const headerDiv = document.createElement('div');
        headerDiv.className = 'd-flex justify-content-between align-items-start';
        
        const title = document.createElement('h5');
        title.className = 'card-title text-break';
        title.textContent = d.name;
        headerDiv.appendChild(title);
        
        const actionsDiv = document.createElement('div');
        if (isOwner) {
            const renameBtn = document.createElement('button');
            renameBtn.className = 'btn btn-outline-warning btn-sm me-1';
            const renameIcon = document.createElement('i');
            renameIcon.className = 'bi bi-pencil';
            renameBtn.appendChild(renameIcon);
            renameBtn.addEventListener('click', () => openRenameModal(d.id, d.name));
            
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn btn-outline-danger btn-sm';
            const deleteIcon = document.createElement('i');
            deleteIcon.className = 'bi bi-trash';
            deleteBtn.appendChild(deleteIcon);
            deleteBtn.addEventListener('click', () => deleteDrawing(d.id));
            
            actionsDiv.appendChild(renameBtn);
            actionsDiv.appendChild(deleteBtn);
        }
        headerDiv.appendChild(actionsDiv);
        cardBody.appendChild(headerDiv);
        
        const footerDiv = document.createElement('div');
        footerDiv.className = 'mt-auto d-flex justify-content-between align-items-center mt-3';
        
        const openBtn = document.createElement('button');
        openBtn.className = 'btn btn-primary btn-sm';
        openBtn.textContent = 'Open';
        openBtn.addEventListener('click', () => navigate('paint', d.id));
        
        const shareBtn = document.createElement('button');
        shareBtn.className = 'btn btn-outline-secondary btn-sm';
        shareBtn.textContent = 'Share';
        shareBtn.addEventListener('click', () => copyToClipboard(shareLink));
        
        footerDiv.appendChild(openBtn);
        footerDiv.appendChild(shareBtn);
        cardBody.appendChild(footerDiv);
        
        cardInner.appendChild(cardBody);
        card.appendChild(cardInner);

        listDiv.appendChild(card);
    });
}

async function createDrawing(event) {
    event.preventDefault();
    const nameInput = document.getElementById('new-drawing-name');

    try {
        const response = await fetch(`${API_URL}/drawings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ name: nameInput.value })
        });

        if (response.ok) {
            nameInput.value = '';
            const newDrawing = await response.json();
            loadDrawings();
        }
    } catch (e) {
        alert("Failed to create drawing");
    }
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => alert("Link copied to clipboard!"));
}

window.openRenameModal = function(drawingId, currentName) {
    document.getElementById('rename-drawing-id').value = drawingId;
    document.getElementById('rename-input').value = currentName;
    const modal = new bootstrap.Modal(document.getElementById('renameModal'));
    modal.show();
}

async function saveRename() {
    const drawingId = document.getElementById('rename-drawing-id').value;
    const newName = document.getElementById('rename-input').value;

    try {
        const res = await fetch(`${API_URL}/drawings/${drawingId}/name`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ name: newName })
        });
        if (res.ok) {
            const modalEl = document.getElementById('renameModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            modal.hide();
            loadDrawings();
        } else {
            alert("Failed to rename");
        }
    } catch (e) {
        console.error(e);
    }
}

async function deleteDrawing(drawingId) {
    if (!confirm("Are you sure you want to delete this drawing?")) return;
    try {
        const res = await fetch(`${API_URL}/drawings/${drawingId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        if (res.ok) {
            loadDrawings();
        } else {
            alert("Failed to delete drawing");
        }
    } catch (e) {
        console.error("Failed to delete", e);
    }
}

async function initCanvas(drawingId) {
    const canvas = document.getElementById('board');
    context = canvas.getContext('2d');
    context.lineCap = 'round';
    context.lineJoin = 'round';
    
    fillWhiteBackground();
    drawingState = [];
    targetDrawingId = drawingId;

    try {
        const res = await fetch(`${API_URL}/drawings/${drawingId}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        if (res.status === 401) {
            logout();
            return;
        }
        if (!res.ok) {
            alert("Drawing doesn't exist");
            targetDrawingId = null;
            navigate('dashboard');
            return;
        }
        const d = await res.json();
        currentDrawingName = d.name;
        document.getElementById('current-drawing-title').innerText = d.name;
        if (d.data) {
            drawingState = JSON.parse(d.data);
            redrawCanvas();
        }
    } catch(e) {
        console.error(e);
        alert("Drawing doesn't exist");
        targetDrawingId = null;
        navigate('dashboard');
        return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    ws = new WebSocket(`${protocol}//${window.location.hostname}:8081/drawing/${drawingId}?token=${currentToken}`);
    ws.binaryType = "arraybuffer";

    ws.onopen = () => console.log(`Joined drawing ${drawingId}`);

    ws.onmessage = async (event) => {
        if (event.data instanceof ArrayBuffer) {
            const buffer = new Uint8Array(event.data);
            if (buffer[0] === 0x13) {
                const payloadBytes = buffer.slice(1);
                const jsonStr = new TextDecoder().decode(payloadBytes);
                const data = JSON.parse(jsonStr);

                if (data.type === "state_override") {
                    drawingState = data.state;
                    redrawCanvas();
                } else if (data.type === "stroke") {
                    drawingState.push(data.stroke);
                    drawStroke(data.stroke);
                } else if (data.type === "undo") {
                    for (let i = drawingState.length - 1; i >= 0; i--) {
                        if (drawingState[i].userId === data.userId) {
                            drawingState.splice(i, 1);
                            redrawCanvas();
                            break;
                        }
                    }
                }
            }
        }
    };

    ws.onerror = (error) => console.error("WebSocket Error:", error);
    ws.onclose = () => console.log("Disconnected.");

    canvas.onmousedown = (e) => {
        isDrawing = true;
        const color = document.getElementById('color-picker').value;
        const size = document.getElementById('brush-size').value;
        
        currentStroke = {
            strokeId: currentUser + '-' + Date.now(),
            userId: currentUser,
            color: color,
            size: size,
            points: [{ x: e.offsetX, y: e.offsetY }]
        };
    };

    canvas.onmousemove = (e) => {
        if (!isDrawing) return;
        
        const pt = { x: e.offsetX, y: e.offsetY };
        currentStroke.points.push(pt);
        
        const pts = currentStroke.points;
        if (pts.length >= 2) {
            const p1 = pts[pts.length - 2];
            const p2 = pts[pts.length - 1];
            context.beginPath();
            context.moveTo(p1.x, p1.y);
            context.lineTo(p2.x, p2.y);
            context.strokeStyle = currentStroke.color;
            context.lineWidth = currentStroke.size;
            context.stroke();
            context.closePath();
        }
    };

    const finishStroke = () => {
        if (!isDrawing) return;
        isDrawing = false;
        
        if (currentStroke && currentStroke.points.length > 0) {
            drawingState.push(currentStroke);
            broadcastStroke(currentStroke);
            saveStateToDb();
        }
        currentStroke = null;
    };

    canvas.onmouseup = finishStroke;
    canvas.onmouseout = finishStroke;
}

function fillWhiteBackground() {
    const canvas = document.getElementById('board');
    context.fillStyle = "white";
    context.fillRect(0, 0, canvas.width, canvas.height);
}

function redrawCanvas() {
    fillWhiteBackground();
    drawingState.forEach(stroke => drawStroke(stroke));
}

function drawStroke(stroke) {
    if (!stroke.points || stroke.points.length < 2) return;
    context.beginPath();
    context.moveTo(stroke.points[0].x, stroke.points[0].y);
    for (let i = 1; i < stroke.points.length; i++) {
        context.lineTo(stroke.points[i].x, stroke.points[i].y);
    }
    context.strokeStyle = stroke.color;
    context.lineWidth = stroke.size;
    context.stroke();
    context.closePath();
}

function broadcastStroke(stroke) {
    sendWsMessage({ type: "stroke", stroke: stroke });
}

function broadcastState() {
    sendWsMessage({ type: "state_override", state: drawingState });
}

function sendWsMessage(obj) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        const dataString = JSON.stringify(obj);
        const payloadBytes = new TextEncoder().encode(dataString);
        const packet = new Uint8Array(1 + payloadBytes.length);
        packet[0] = 0x13; 
        packet.set(payloadBytes, 1);
        ws.send(packet);
    }
}

async function saveStateToDb() {
    try {
        await fetch(`${API_URL}/drawings/${targetDrawingId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ data: JSON.stringify(drawingState) })
        });
    } catch(e) { console.error("Failed to save state", e); }
}

function undo() {
    for (let i = drawingState.length - 1; i >= 0; i--) {
        if (drawingState[i].userId === currentUser) {
            drawingState.splice(i, 1);
            redrawCanvas();
            sendWsMessage({ type: "undo", userId: currentUser });
            saveStateToDb();
            break;
        }
    }
}

function exitPainting() {
    if (ws) {
        ws.close();
        ws = null;
    }
    targetDrawingId = null;
    fillWhiteBackground();
    document.getElementById('current-drawing-title').innerText = "Drawing...";
    navigate('dashboard');
}

function exportCanvas() {
    const canvas = document.getElementById('board');
    const image = canvas.toDataURL("image/png");
    const link = document.createElement('a');
    const safeName = currentDrawingName.replace(/[^a-z0-9]/gi, '_').toLowerCase();
    link.download = `${safeName}.png`;
    link.href = image;
    link.click();
}