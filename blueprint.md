# 🕹️ Blueprint Técnico: VortexGaming

* **Agencia/Desarrollador:** LocalCode Digital
* **Referencia Visual:** Premium Dark Mode Cyberpunk (Estilo Slack/Gaming Inmersivo)
* **Plataforma Objetivo:** Android Studio (Java Nativo)

---

## 1. Configuración del Proyecto y Dependencias

### Metadatos Base
* **Nombre de la Aplicación:** VortexGaming
* **Nombre del Paquete:** `com.localcode.vortexgaming`
* **Min SDK:** API 26 (Android 8.0 Oreo)
* **Target SDK:** API 34 (Android 14)

### Dependencias Core (`build.gradle.kts` - app)
```kotlin
dependencies {
    // Red y Parseo de JSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Inyección y Caché de Contenido Multimedia
    implementation("com.github.bumptech.glide:glide:4.15.1")

    // Componentes de Interfaz de Usuario Premium
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
```

---

## 2. Arquitectura de Directorios

```text
app/src/main/java/com/localcode/vortexgaming/
│
├── api/
│   ├── RetrofitClient.java       # Inicializador Singleton de la conexión HTTP
│   └── RawgApiService.java       # Definición de Endpoints (Filtros, búsquedas y fechas)
│
├── models/
│   ├── GameResponse.java         # Modelo raíz para mapear el JSON de RAWG
│   ├── Game.java                 # POJO de datos del juego (ID, nombre, cover art, rating)
│   └── User.java                 # Estructura para el registro y mapeo local
│
├── adapters/
│   └── GameAdapter.java          # Adaptador único optimizado para Shared Elements y clics
│
├── utils/
│   ├── SessionManager.java       # Persistencia del estado del login (SharedPreferences)
│   └── FavoritesManager.java     # Motor de guardado local para favoritos (HashSet Strings)
│
└── views/
    ├── auth/
    │   ├── SplashActivity.java    # Pantalla inmersiva con animación de entrada (2 segundos)
    │   ├── LoginActivity.java     # Interfaz de acceso con interceptores de error
    │   └── RegisterActivity.java  # Captura de datos con validador regular y DatePicker
    │
    ├── main/
    │   ├── MainActivity.java      # Activity principal que aloja el ciclo de los Fragmentos
    │   ├── HomeFragment.java      # Dashboard multi-sección + Debounce + PullToRefresh
    │   ├── RequestFragment.java   # Módulo transaccional (Spinner de expansiones estilizado)
    │   └── ProfileFragment.java   # Configuración de usuario + Toggles visuales + Logout
    │
    └── details/
        └── GameDetailActivity.java # Ficha extendida + Transición inmersiva + Persistencia ♥
```

---

## 3. Sistema de Diseño (Tokens de Estilo)

### Paleta Cromática Premium (`res/values/colors.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_main">#0B0C10</color>       <color name="bg_surface">#161820</color>    <color name="purple_accent">#5C62F5</color> <color name="purple_light">#23263B</color>  <color name="text_primary">#FFFFFF</color>  <color name="text_muted">#808191</color>    <color name="accent_red">#FF4A4A</color>    </resources>
```

### Contenedor Estilizado Curvo (`res/drawable/card_premium_shape.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)">
    <solid android:color="@color/bg_surface" />
    <corners android:radius="20dp" />
</shape>
```

### Transición de Entrada (`res/anim/fade_in.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)" android:fillAfter="true">
    <alpha android:fromAlpha="0.0" android:toAlpha="1.0" android:duration="1500" />
</set>
```

---

## 4. Mapa de Lógica y Comportamiento de Vistas

### Fase 1: Control de Acceso e Inmersión

#### 1. SplashActivity
* **UI:** Oculta por completo la barra de navegación y estados (`Theme.MaterialComponents.NoActionBar.FullScreen`). El logo de *VortexGaming* se renderiza en el centro aplicando `fade_in.xml`.
* **Comportamiento:** Un temporizador frena la ejecución durante 2000ms. Al expirar, consulta a `SessionManager`: si detecta credenciales activas redirige de inmediato a `MainActivity`; si no, salta al `LoginActivity`.

#### 2. LoginActivity
* **UI:** Estructura oscura basada en campos limpios sobre el drawable `card_premium_shape`.
* **Validación:** Intercepta el evento del botón mediante `TextUtils.isEmpty()`. Si hay campos vacíos, activa un `.setError("Este campo es obligatorio")` dinámico deteniendo la simulación del servicio.

#### 3. RegisterActivity
* **UI:** Mismo lenguaje de diseño oscuro. Suma un input inactivo para la fecha.
* **Manejo de Datos:** Al pulsar el campo de fecha, levanta un `DatePickerDialog`. Captura los datos numéricos y los transforma a formato de texto mediante `SimpleDateFormat("dd/MM/yyyy")`.
* **Validación Extra:** Comprueba la estructura del correo electrónico usando la clase nativa `Patterns.EMAIL_ADDRESS.matcher(email).matches()`.

---

### Fase 2: Panel de Experiencia de Usuario (`MainActivity`)

#### 1. HomeFragment (Dashboard)
* **Pull To Refresh:** Toda la pantalla se envuelve en un `SwipeRefreshLayout`. Al estirar la lista, oculta las vistas de datos, activa el `layout_skeleton` (bloques opacos simulando tarjetas) y re-ejecuta las peticiones hacia la API.
* **Buscador Inteligente (Debounce 500ms):** En el evento `onQueryTextChange` del `SearchView`, se destruye cualquier tarea en cola usando `searchHandler.removeCallbacks(searchRunnable)`. Si el usuario se detiene por medio segundo y escribió un mínimo de 3 letras, ejecuta la consulta remota reduciendo drásticamente el consumo innecesario de la API.
* **Arquitectura Multi-Sección:** Compuesto por un `NestedScrollView` con 3 `RecyclerViews` de orientación horizontal alimentados de la siguiente manera:
    * **Trending Now:** `/api/games?ordering=-metacritic`
    * **Top Rated:** `/api/games?ordering=-rating`
    * **Upcoming Releases:** `/api/games?dates=2026-06-01,2026-12-31&ordering=-added`

#### 2. GameAdapter & Material Motion
* **UI:** Renderiza portadas curvas procesadas de forma asíncrona mediante Glide. Las celdas implementan un `MaterialCardView` con elevación base de `4dp` que sube dinámicamente a `8dp` al tacto mediante un `stateListAnimator`.
* **Transición Fluida (Shared Element):** El componente de imagen tiene asignado el token `android:transitionName="transition_game_cover"`. Al presionar la tarjeta, el adaptador empaqueta la animación usando `ActivityOptionsCompat` provocando que la imagen se expanda orgánicamente en el salto de pantalla.

#### 3. GameDetailActivity (Ficha Técnico-Inmersiva)
* **UI:** Diseño inmersivo donde el cover art superior se desvanece de manera invisible hacia el color `#0B0C10` mediante un degradado oscuro.
* **Persistencia de Favoritos:** Al cargar la vista, consulta al `FavoritesManager` si el ID del juego existe dentro del `HashSet` de `SharedPreferences`. De ser verdadero, ilumina el icono del corazón (♥) con el color `@color/purple_accent`. Al pulsarlo, conmuta su estado guardando o eliminando el registro local en tiempo real.

#### 4. RequestFragment (Módulo Transaccional)
* **UI:** Combina un banner promocional de estética gaming, un `Spinner` personalizado con fondo oscuro y un botón estilizado tipo píldora.
* **Lógica de Control:** Al presionar el botón "SOLICITAR", la app evalúa `spinner.getSelectedItemPosition()`. Si el índice es igual a `0` (que corresponde a la opción por defecto: "Seleccione una expansión..."), frena el proceso y notifica al usuario usando un `Toast` de advertencia.

#### 5. ProfileFragment (Centro de Control)
* **Visualización:** Extrae en tiempo real el Nombre, Correo y Fecha de Nacimiento desde el almacenamiento interno de la sesión. Muestra una etiqueta fija con el texto `"Miembro desde: Junio 2026"`.
* **Interacciones Simuladas:** Cuenta con dos `MaterialSwitch` (Tema Oscuro y Permitir Notificaciones) enlazados a listeners que responden con mensajes emergentes fluidos ante cambios de estado.
* **Cierre de Sesión:** El botón de salida está tintado con `@color/accent_red`. Al pulsarlo, invoca a `SessionManager.clear()`, elimina el historial completo de la sesión activa, dispara un `Intent` directo hacia `LoginActivity` y limpia la pila del sistema de ejecución llamando a `getActivity().finish()`.