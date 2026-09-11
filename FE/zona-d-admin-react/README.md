# Zona D Admin - Tailwind

Panel administrativo React + Vite + Tailwind CSS.

## Instalar y ejecutar

```bash
npm install
cp .env.example .env
npm run dev
```

Abre:

```text
http://localhost:5173
```

## Tailwind

Este proyecto usa Tailwind CSS v4 mediante `@tailwindcss/vite`, por lo que no necesita
`tailwind.config.js` para esta versión.

## Logo

El logo suministrado está en:

```text
public/logo-cetis.jpg
```

Se muestra en el encabezado en un contenedor pequeño para que no domine la interfaz.

## Colores utilizados

- Vino principal: `#74272b`
- Dorado: `#e4a640`
- Beige claro: `#f3e2c5`
- Rojo de acción: `#c93843`
- Fondo: `#f6f1ea`

## CORS

Para desarrollo local, tu backend Spring Boot debe aceptar:

```text
http://localhost:5173
```

Por ejemplo:

```java
@CrossOrigin(origins = "http://localhost:5173")
```
