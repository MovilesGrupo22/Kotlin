# Restaurandes - Contexto de sustentacion

Este archivo resume el contexto tecnico y logico del proyecto para poder recuperar rapidamente lo hablado en la conversacion. Esta pensado como apoyo para sustentacion, wiki, issues o preparacion oral.

## 1. Idea general de la app

Restaurandes es una app Android en Kotlin para descubrir restaurantes. Permite autenticacion, exploracion de restaurantes, busqueda, mapa, favoritos, reviews, detalle de restaurante, comparacion de restaurantes, analytics internos y features context-aware/smart.

La app usa Firebase como backend principal:

- Firebase Authentication para login, registro, Google Sign-In y sesion.
- Cloud Firestore para restaurantes, usuarios, favoritos, reviews y analytics internos.
- Firebase Analytics para eventos de comportamiento.
- Google Maps y servicios de ubicacion para mapa, ubicacion y direcciones.

## 2. Arquitectura general

El proyecto sigue una arquitectura por capas combinada con MVVM, Repository Pattern, Service Pattern e inyeccion de dependencias con Hilt.

Capas principales:

- Presentation/UI: pantallas Compose y ViewModels.
- Domain: modelos, contratos de repositorio y casos de uso.
- Data: implementaciones concretas de repositorios que hablan con Firebase u otros servicios.
- Backend/External Services: Firebase, Google Maps, GPS, autenticacion externa.

Ejemplo de flujo:

1. El usuario abre Home.
2. `HomeScreen` muestra la UI y observa el estado del `HomeViewModel`.
3. `HomeViewModel` llama casos de uso como `GetRestaurantsUseCase`.
4. El caso de uso llama un repositorio de dominio.
5. La implementacion del repositorio consulta Firestore.
6. Los documentos se convierten en modelos como `Restaurant`.
7. El estado vuelve al ViewModel.
8. La UI se recompone y muestra los restaurantes.

Archivos clave:

- `app/src/main/java/com/restaurandes/presentation/home/HomeScreen.kt`
- `app/src/main/java/com/restaurandes/presentation/home/HomeViewModel.kt`
- `app/src/main/java/com/restaurandes/domain/model/Restaurant.kt`
- `app/src/main/java/com/restaurandes/data/repository/RestaurantRepositoryImpl.kt`
- `app/src/main/java/com/restaurandes/di/AppModule.kt`

## 3. MVVM en el proyecto

MVVM significa Model - View - ViewModel.

En este proyecto:

- View: las pantallas Compose, por ejemplo `HomeScreen`, `LoginScreen`, `RestaurantDetailScreen`.
- ViewModel: clases que contienen estado y logica de presentacion, por ejemplo `HomeViewModel`, `LoginViewModel`, `RestaurantDetailViewModel`.
- Model: entidades del dominio como `Restaurant`, `Review`, `User` y `RestaurantAnalytics`.

La View no deberia consultar Firebase directamente. La View llama al ViewModel. El ViewModel coordina casos de uso o repositorios. Esto permite separar UI de logica de negocio y datos.

Ejemplo:

- `HomeScreen` observa `uiState`.
- `HomeViewModel` actualiza `HomeUiState`.
- `RestaurantRepositoryImpl` obtiene datos desde Firestore.
- `Restaurant` representa el dato final para la app.

Frase para sustentar:

> "Usamos MVVM para separar la interfaz de usuario de la logica de estado y datos. La pantalla solo renderiza y envia acciones; el ViewModel decide que cargar, como filtrar y como actualizar el estado."

## 4. Data pipeline

El pipeline mezcla dos tipos de informacion:

- Estado del sistema: restaurantes, horarios, rating, reviews, favoritos, usuario autenticado.
- Comportamiento del usuario: vistas de restaurante, favoritos, filtros usados, secciones abiertas, interacciones.

Flujo general:

1. Data sources: Firestore, Firebase Auth, GPS y acciones del usuario.
2. Integration/Ingestion: ViewModels, UseCases, Repositories y Services reciben o consultan esos datos.
3. Storage: Firestore guarda restaurantes, usuarios, reviews, favoritos y analytics internos.
4. Processing: ViewModels y repositorios transforman datos en metricas o estado de UI.
5. Presentation: la app muestra Home, Detail, AnalyticsScreen y dashboards externos como Looker Studio.

Archivos clave del pipeline:

- `app/src/main/java/com/restaurandes/data/analytics/AnalyticsService.kt`
- `app/src/main/java/com/restaurandes/data/repository/RestaurantAnalyticsRepositoryImpl.kt`
- `app/src/main/java/com/restaurandes/presentation/detail/RestaurantDetailViewModel.kt`
- `app/src/main/java/com/restaurandes/presentation/analytics/AnalyticsViewModel.kt`
- `app/src/main/java/com/restaurandes/presentation/analytics/AnalyticsScreen.kt`

Frase para sustentar:

> "El pipeline no solo lee datos estaticos de restaurantes. Tambien captura comportamiento real del usuario, como vistas y favoritos, y lo transforma en metricas para responder business questions."

## 5. BQ1 vs BQ2

### BQ1: Most viewed food place pages

Pregunta:

> What are the most viewed food place pages in the app?

Mide especificamente vistas de paginas de detalle. Se responde con `viewCount` o eventos de `restaurant_view`.

Codigo relacionado:

- `RestaurantDetailViewModel.kt` llama `trackView`.
- `RestaurantAnalyticsRepositoryImpl.kt` incrementa `viewCount`.

Frase:

> "BQ1 mide atencion directa sobre la pagina de detalle: cuantas veces se abre cada restaurante."

### BQ2: Most visited/interacted food places

Pregunta:

> What are the most visited food places according to app interactions?

Mide interaccion mas amplia, no solo vistas. Incluye vistas y favoritos. En el codigo existe `interactionScore`, calculado asi:

```kotlin
val interactionScore: Long get() = viewCount + favoriteCount * 2
```

Esto esta en:

- `app/src/main/java/com/restaurandes/domain/model/RestaurantAnalytics.kt`

El tracking se hace en:

- `trackView`: incrementa vistas.
- `trackFavorite`: incrementa favoritos.
- `getTopInteractedRestaurants`: ordena por `interactionScore`.

Frase:

> "BQ2 no pregunta solo que restaurante se vio mas, sino cual genero mas engagement. Por eso combina vistas y favoritos en un score de interaccion."

## 6. Context-Aware System (CAS)

El CAS esta implementado principalmente en Home.

La app cambia segun el contexto temporal del celular. Usa la hora del dispositivo con:

```kotlin
Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
```

Archivo:

- `app/src/main/java/com/restaurandes/presentation/home/HomeViewModel.kt`

Escenarios:

- 6 a 10: mensaje de desayuno.
- 11 a 13: mensaje de almuerzo.
- 14 a 17: mensaje de snack.
- 18 a 20: mensaje de cena.
- Fuera de ese horario: mensaje de que muchos restaurantes pueden estar cerrados.

Ademas, durante el dia la app aplica automaticamente el filtro `Open` si el usuario entra desde `All`.

Metodo clave:

```kotlin
private fun getInitialContextCategory(currentCategory: String): String
```

El mensaje visual se construye en:

```kotlin
private fun buildContextCanvas(...)
```

Y se muestra en:

- `app/src/main/java/com/restaurandes/presentation/home/HomeScreen.kt`
- Composable `ContextCanvasCard`

Frase para sustentar:

> "El CAS adapta la experiencia segun la hora real del celular. No es solo texto: tambien modifica el filtro inicial para priorizar restaurantes abiertos durante horas utiles de comida."

## 7. Smart Feature: Trending now on campus

La Smart Feature muestra un restaurante destacado automaticamente en Home.

Archivo principal:

- `app/src/main/java/com/restaurandes/presentation/home/HomeViewModel.kt`

Metodo clave:

```kotlin
private fun buildTrendingCampusInsight(restaurants: List<Restaurant>): TrendingCampusInsight?
```

La app escoge el restaurante usando atributos de datos:

- Si esta abierto ahora.
- Rating.
- Review count.
- Cantidad de tags.

Logica:

```kotlin
restaurants.maxWithOrNull(
    compareBy<Restaurant> { if (it.isCurrentlyOpen()) 1 else 0 }
        .thenBy { it.rating }
        .thenBy { it.reviewCount }
        .thenBy { it.tags.size }
)
```

Luego genera una razon legible para el usuario:

- `Open now with strong campus feedback`
- `Open now and highly rated`
- `Popular pick based on reviews`
- `Recommended from current restaurant data`

Se muestra en:

- `app/src/main/java/com/restaurandes/presentation/home/HomeScreen.kt`
- Composable `TrendingCampusSection`

Frase para sustentar:

> "La Smart Feature no es una tarjeta hardcodeada. La app procesa atributos de cada restaurante y elige automaticamente una recomendacion destacada para campus."

Importante:

Si preguntan por Firebase, responder con honestidad:

> "El sistema ya recolecta vistas y favoritos en `restaurant_analytics`. El trending visual del Home actualmente usa datos del restaurante como apertura, rating y reviews; el pipeline de analytics complementa esa logica con interacciones reales para medir popularidad."

## 8. Analytics interno de restaurantes

Modelo:

- `app/src/main/java/com/restaurandes/domain/model/RestaurantAnalytics.kt`

Repositorio de dominio:

- `app/src/main/java/com/restaurandes/domain/repository/RestaurantAnalyticsRepository.kt`

Implementacion:

- `app/src/main/java/com/restaurandes/data/repository/RestaurantAnalyticsRepositoryImpl.kt`

Coleccion Firestore:

- `restaurant_analytics`

Campos:

- `restaurantId`
- `restaurantName`
- `viewCount`
- `favoriteCount`

Metodos importantes:

- `trackView(restaurantId, restaurantName)`: incrementa `viewCount`.
- `trackFavorite(restaurantId, restaurantName)`: incrementa `favoriteCount`.
- `getTopViewedRestaurants(limit)`: ordena por vistas.
- `getTopInteractedRestaurants(limit)`: ordena por `interactionScore`.

En `RestaurantDetailViewModel.kt`:

- Al cargar detalle se ejecuta `trackView`.
- Al marcar favorito se ejecuta `trackFavorite`.

Frase:

> "Cada vez que el usuario abre un restaurante o lo guarda como favorito, se actualiza una coleccion agregada en Firestore. Esto permite construir rankings de vistas e interacciones."

## 9. Authentication

La app tiene varias rutas de autenticacion:

- Login con email/password.
- Registro.
- Logout.
- Google Sign-In.
- Biometria para desbloqueo de sesion existente.

Archivos clave:

- `app/src/main/java/com/restaurandes/presentation/auth/LoginScreen.kt`
- `app/src/main/java/com/restaurandes/presentation/auth/LoginViewModel.kt`
- `app/src/main/java/com/restaurandes/data/repository/UserRepositoryImpl.kt`
- `app/src/main/java/com/restaurandes/security/BiometricAuthManager.kt`
- `app/src/main/java/com/restaurandes/MainActivity.kt`

Frase:

> "Authentication esta implementado con Firebase Auth y se extendio con Google Sign-In y biometria para mejorar seguridad y experiencia de sesion."

## 10. Design patterns

### Repository Pattern

Separa la app de Firebase. La UI y los ViewModels no consultan Firestore directamente.

Ejemplos:

- `RestaurantRepository`
- `UserRepository`
- `ReviewRepository`
- `LocationRepository`
- `RestaurantAnalyticsRepository`

Frase:

> "El Repository Pattern centraliza el acceso a datos y oculta si la informacion viene de Firestore, GPS u otro servicio."

### Adapter / Mapper

Convierte documentos de Firestore o DTOs en modelos de dominio.

Ejemplos:

- `RestaurantDto.kt`
- `RestaurantRepositoryImpl.kt`
- `Restaurant.kt`

Frase:

> "El Adapter permite transformar datos crudos de Firebase en objetos del dominio que la app entiende."

### Dependency Injection con Hilt

Permite que ViewModels y repositorios reciban dependencias sin crearlas manualmente.

Archivo:

- `app/src/main/java/com/restaurandes/di/AppModule.kt`

Frase:

> "Hilt provee instancias compartidas y desacopla la construccion de objetos. Asi un ViewModel puede pedir un repositorio sin saber como se construye."

### Singleton

Firebase usa instancias compartidas:

- `FirebaseAuth`
- `FirebaseFirestore`
- `FirebaseAnalytics`

Frase:

> "Usamos instancias compartidas para evitar crear multiples conexiones innecesarias a servicios externos."

## 11. Pull requests recientes importantes

### PR biometria y perfil

Cambios principales:

- Agrego `BiometricAuthManager`.
- `MainActivity` usa autenticacion biometrica para sesiones existentes.
- Perfil muestra mas informacion, como reviews y favoritos.

### PR Smart comparison

Cambios principales:

- Agrego comparacion inteligente de restaurantes.
- `RestaurantComparisonScreen` y `RestaurantComparisonViewModel`.
- Calcula un veredicto usando rating, precio, reviews, estado abierto y distancia.

### PR filtros en mapa

Cambios principales:

- Agrego filtros por precio en Map.
- `MapViewModel` conserva restaurantes originales y filtra por `$`, `$$`, `$$$`.

### PR Google Sign-In y analytics internos

Cambios principales:

- Agrego Google Sign-In.
- Agrego `RestaurantAnalytics`.
- Agrego `RestaurantAnalyticsRepository`.
- Agrego tracking de vistas y favoritos.
- Agrego `AnalyticsScreen`.

## 12. Como probar rapido en Android Studio

CAS:

1. Cambiar la hora del emulador o celular.
2. Abrir Home.
3. Verificar que el banner cambia segun la hora.
4. Durante el dia, verificar que el filtro `Open` queda aplicado automaticamente.
5. De noche, verificar mensaje de restaurantes posiblemente cerrados.

Smart Feature:

1. Abrir Home.
2. Verificar que aparece `Trending now on campus`.
3. Confirmar que muestra un restaurante con datos reales.
4. Cambiar datos como rating, reviews o estado abierto en Firestore para explicar que la seleccion depende de datos.

BQ1:

1. Abrir detalle de un restaurante.
2. Verificar que `trackView` se ejecuta en `RestaurantDetailViewModel`.
3. Revisar Firestore en `restaurant_analytics`.
4. Confirmar incremento de `viewCount`.

BQ2:

1. Abrir detalle de un restaurante.
2. Guardarlo como favorito.
3. Revisar `restaurant_analytics`.
4. Confirmar incremento de `viewCount` y `favoriteCount`.
5. Explicar que `interactionScore = viewCount + favoriteCount * 2`.

## 13. Respuestas cortas para sustentacion

CAS:

> "Nuestro CAS usa la hora real del dispositivo para adaptar el Home. Cambia el mensaje contextual y aplica automaticamente el filtro de restaurantes abiertos durante el dia."

Smart Feature:

> "Nuestra Smart Feature calcula un restaurante trending usando datos del sistema como apertura, rating, reviews y tags. Es una recomendacion automatica, no una tarjeta fija."

Data pipeline:

> "El pipeline mezcla estado del sistema y comportamiento del usuario. Firestore guarda restaurantes y usuarios, mientras que analytics captura vistas, favoritos, filtros y secciones."

MVVM:

> "La View renderiza, el ViewModel maneja estado y logica de presentacion, y los repositorios obtienen datos. Eso separa responsabilidades."

BQ1:

> "BQ1 mide paginas de restaurante mas vistas usando `viewCount`."

BQ2:

> "BQ2 mide interacciones agregadas usando vistas y favoritos para calcular engagement."

Architecture pattern:

> "La arquitectura por capas separa UI, dominio, datos y servicios externos. Esto reduce acoplamiento y hace que el sistema sea mas mantenible."

Repository pattern:

> "Los repositorios evitan que la UI dependa directamente de Firebase."

Dependency Injection:

> "Hilt construye y entrega dependencias automaticamente, evitando crear repositorios o servicios manualmente en cada clase."

