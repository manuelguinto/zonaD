# Zona D API

API REST en Java 21 + Spring Boot para obtener una ficha disponible desde Firestore.

## Firestore

Colección:

`ZonaDFichas`

Cada documento debe tener al menos:

```json
{
  "estatus": "NUEVA"
}
```

También puede tener, por ejemplo:

```json
{
  "usuario": "956474",
  "password": "956474",
  "estatus": "NUEVA"
}
```

## Endpoint

```http
POST /fichas/vender
```

La operación:

1. Busca una ficha con `estatus = NUEVA`.
2. Dentro de una transacción la cambia a `VENDIDA`.
3. Agrega `fechaVenta` con timestamp del servidor.
4. Devuelve los datos de la ficha.

Ejemplo:

```json
{
  "ok": true,
  "ficha": {
    "id": "ABC123",
    "usuario": "956474",
    "password": "956474",
    "estatus": "VENDIDA"
  }
}
```

## Cloud Run

La aplicación escucha `PORT`, por lo que funciona con Cloud Run.

El repositorio contiene `Dockerfile`, así que puedes enlazar GitHub a Cloud Build y construir el contenedor desde el repositorio.

La cuenta de servicio de Cloud Run debe tener permisos para leer y actualizar Firestore.
