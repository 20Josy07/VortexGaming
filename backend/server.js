const express = require('express');
const bcrypt  = require('bcryptjs');
const jwt     = require('jsonwebtoken');
const cors    = require('cors');
const low     = require('lowdb');
const FileSync = require('lowdb/adapters/FileSync');
const path    = require('path');
const fs      = require('fs');

// ── Setup ────────────────────────────────────────────────────────────────────
const app  = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'vortex_secret_2024';

app.use(cors());
app.use(express.json());

// ── Database (JSON file) ─────────────────────────────────────────────────────
const dataDir = path.join(__dirname, 'data');
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir);

const adapter = new FileSync(path.join(dataDir, 'db.json'));
const db = low(adapter);
db.defaults({ users: [], requests: [] }).write();

// ── Helper ───────────────────────────────────────────────────────────────────
function authMiddleware(req, res, next) {
    const header = req.headers['authorization'];
    if (!header) return res.status(401).json({ error: 'Token requerido' });
    try {
        req.user = jwt.verify(header.replace('Bearer ', ''), JWT_SECRET);
        next();
    } catch {
        res.status(401).json({ error: 'Token inválido' });
    }
}

// ── POST /api/auth/register ──────────────────────────────────────────────────
app.post('/api/auth/register', async (req, res) => {
    const { name, email, password, dob } = req.body;

    if (!name || !email || !password || !dob)
        return res.status(400).json({ error: 'Todos los campos son obligatorios' });

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email))
        return res.status(400).json({ error: 'Correo inválido' });

    if (password.length < 8)
        return res.status(400).json({ error: 'La contraseña debe tener al menos 8 caracteres' });

    const existing = db.get('users').find({ email: email.toLowerCase() }).value();
    if (existing)
        return res.status(409).json({ error: 'Este correo ya está registrado' });

    const hashed = await bcrypt.hash(password, 10);
    const user = { id: Date.now(), name, email: email.toLowerCase(), password: hashed, dob, createdAt: new Date().toISOString() };
    db.get('users').push(user).write();

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });
    res.status(201).json({ token, user: { id: user.id, name: user.name, email: user.email, dob: user.dob } });
});

// ── POST /api/auth/login ─────────────────────────────────────────────────────
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password)
        return res.status(400).json({ error: 'Correo y contraseña son obligatorios' });

    const user = db.get('users').find({ email: email.toLowerCase() }).value();
    if (!user)
        return res.status(404).json({ error: 'Usuario no encontrado' });

    const match = await bcrypt.compare(password, user.password);
    if (!match)
        return res.status(401).json({ error: 'Contraseña incorrecta' });

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });
    res.json({ token, user: { id: user.id, name: user.name, email: user.email, dob: user.dob } });
});

// ── POST /api/requests ───────────────────────────────────────────────────────
app.post('/api/requests', authMiddleware, (req, res) => {
    const { game_name, game_id, expansion_name, expansion_id } = req.body;

    if (!game_name || !expansion_name)
        return res.status(400).json({ error: 'game_name y expansion_name son obligatorios' });

    const request = {
        id: Date.now(),
        user_email: req.user.email,
        game_name,
        game_id: game_id || null,
        expansion_name,
        expansion_id: expansion_id || null,
        requested_at: new Date().toISOString()
    };
    db.get('requests').push(request).write();
    res.status(201).json({ message: 'Solicitud guardada', request });
});

// ── GET /api/requests ────────────────────────────────────────────────────────
app.get('/api/requests', authMiddleware, (req, res) => {
    const all = db.get('requests').value();
    res.json({ total: all.length, requests: all });
});

// ── GET /api/requests/mine ───────────────────────────────────────────────────
app.get('/api/requests/mine', authMiddleware, (req, res) => {
    const mine = db.get('requests').filter({ user_email: req.user.email }).value();
    res.json({ total: mine.length, requests: mine });
});

// ── GET /api/users ───────────────────────────────────────────────────────────
app.get('/api/users', authMiddleware, (req, res) => {
    const users = db.get('users').map(u => ({
        id: u.id, name: u.name, email: u.email, dob: u.dob, createdAt: u.createdAt
    })).value();
    res.json({ total: users.length, users });
});

// ── GET /dashboard ────────────────────────────────────────────────────────────
app.get('/dashboard', (req, res) => {
    res.sendFile(path.join(__dirname, 'dashboard.html'));
});

// ── GET / ─────────────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
    res.json({ name: 'VortexGaming API', version: '1.0.0', status: 'running' });
});

// ── Start ────────────────────────────────────────────────────────────────────
app.listen(PORT, '0.0.0.0', () => {
    console.log(`VortexGaming API corriendo en http://localhost:${PORT}`);
    console.log(`Para emulador Android usar: http://10.0.2.2:${PORT}`);
});
