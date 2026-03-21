# Restaurandes

Aplicación móvil para descubrir, comparar y guardar restaurantes en el campus de la Universidad de los Andes.

## Descripción

Restaurandes centraliza la información de los establecimientos de comida del campus y sus alrededores. Los usuarios pueden buscar restaurantes, ver su ubicación en el mapa, leer y escribir reseñas, guardar favoritos y comparar dos opciones lado a lado.

## Funcionalidades

- Listado de restaurantes con filtros por categoría, estado (abierto/cerrado) y calificación
- Mapa interactivo con marcadores por categoría
- Búsqueda por nombre, categoría o características
- Detalle de restaurante con información de contacto, horarios y dirección
- Reseñas con calificación y comentarios
- Favoritos por usuario
- Comparación lado a lado entre dos restaurantes con sugerencia automática
- Autenticación con correo y contraseña (Firebase Auth)
- Perfil de usuario
- Analytics de uso con Firebase Analytics

## Tech Stack

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Arquitectura**: MVVM + Clean Architecture
- **Inyección de dependencias**: Dagger Hilt
- **Backend**: Firebase (Firestore, Auth, Analytics)
- **Mapas**: Google Maps Compose
- **Imágenes**: Coil 3
- **Ubicación**: Fused Location Provider (Google Play Services)
- **SDK mínimo**: 24 (Android 7.0)
- **SDK objetivo**: 35 (Android 15)

## Configuración

### Requisitos

- Android Studio Ladybug 2024.2.1 o superior
- JDK 17 o superior
- Cuenta de Firebase con proyecto configurado

### Instalación

1. Clonar el repositorio

```bash
git clone <repository-url>
cd Restaurandes-Kotlin
```

2. Agregar el archivo `google-services.json` del proyecto de Firebase en `app/`

3. Abrir el proyecto en Android Studio y sincronizar Gradle

4. Ejecutar en emulador o dispositivo físico

## Estructura del proyecto

```
app/src/main/java/com/restaurandes/
├── data/
│   ├── analytics/          # AnalyticsService (Firebase Analytics)
│   ├── remote/dto/         # DTOs y mappers
│   └── repository/         # Implementaciones de repositorios
├── domain/
│   ├── model/              # Restaurant, User, Review, Location
│   ├── repository/         # Interfaces de repositorios
│   └── usecase/            # Casos de uso
├── presentation/
│   ├── auth/               # Login y registro
│   ├── home/               # Pantalla principal
│   ├── detail/             # Detalle, reseñas y comparación
│   ├── map/                # Vista de mapa
│   ├── search/             # Búsqueda
│   ├── favorites/          # Favoritos
│   ├── profile/            # Perfil de usuario
│   └── navigation/         # Grafo de navegación
├── di/                     # Módulo de Hilt
└── ui/theme/               # Tema Material 3
```

## Arquitectura

El proyecto sigue Clean Architecture con tres capas:

**Domain**: modelos y contratos (`Restaurant`, `User`, `Review`, `Location`), interfaces de repositorios y casos de uso. Sin dependencias de Android.

**Data**: implementaciones de repositorios contra Firebase Firestore y Firebase Auth. Incluye `AnalyticsService` para registrar eventos de uso.

**Presentation**: ViewModels con `StateFlow` + pantallas Compose. Cada pantalla tiene su propio ViewModel inyectado por Hilt.

## Equipo

Grupo 22 - Desarrollo Móvil, Universidad de los Andes
