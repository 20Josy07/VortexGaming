# VortexGaming API

Backend propio de VortexGaming. Node.js + Express + JSON file database.

## Instalación

```bash
cd backend
npm install
npm start
```

Para desarrollo con auto-reload:
```bash
npm run dev
```

La API queda en `http://localhost:3000`

## Endpoints

### Auth
| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/register` | `{name, email, password, dob}` | Registrar usuario |
| POST | `/api/auth/login` | `{email, password}` | Iniciar sesión → devuelve `token` |

### Solicitudes (requieren header `Authorization: Bearer TOKEN`)
| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| POST | `/api/requests` | `{game_name, game_id, expansion_name, expansion_id}` | Guardar solicitud |
| GET | `/api/requests` | — | Ver todas las solicitudes |
| GET | `/api/requests/mine` | — | Ver mis solicitudes |

## Usar con dispositivo físico

Cambia `VORTEX_BASE_URL` en `RetrofitClient.java`:
```java
public static final String VORTEX_BASE_URL = "http://TU_IP_LOCAL:3000/";
```

Tu IP local: ejecuta `ipconfig` en Windows y busca "Dirección IPv4".

## Ver datos guardados

Los datos se guardan en `backend/data/db.json`. Puedes abrirlo en cualquier editor.
