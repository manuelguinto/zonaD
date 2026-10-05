# Contexto completo del proyecto Zona D – Expendedora digital de fichas

## 1. Objetivo general

El proyecto consiste en una expendedora digital de fichas de internet para **Zona D – CETIS 117**.

Flujo principal:

1. El usuario introduce monedas en un monedero multimoneda.
2. El ESP32 recibe pulsos mediante un optoacoplador PC817.
3. Cada pulso representa $1 MXN.
4. Al llegar a $10 MXN, el ESP32 consume un servicio REST alojado en Google Cloud Run.
5. El backend toma una ficha disponible en Firestore, la marca como vendida y devuelve la clave.
6. El ESP32 muestra la ficha en una pantalla LCD I2C.
7. Si hay saldo mayor a $10, el excedente se conserva.
8. Si ocurre un error al obtener la ficha, se genera un código local de reembolso.
9. Tanto la ficha como el código de reembolso permanecen visibles hasta 20 segundos o hasta que se detecta una nueva moneda.
10. Existe además una aplicación React administrativa para consultar, generar, limpiar y vender fichas manualmente.

---

# 2. Hardware utilizado

## ESP32

Se usa un ESP32 como controlador principal.

### Pines actuales importantes

- GPIO18 → entrada de pulsos del monedero mediante PC817.
- GPIO25 → SDA del LCD I2C.
- GPIO26 → SCL del LCD I2C.
- VIN / 5V → alimentación del ESP32 desde LM2596.
- GND → tierra común del lado de 5 V.

Originalmente se intentó usar GPIO21 y GPIO22 para I2C, pero el LCD no era detectado. Al probar con GPIO25 y GPIO26 funcionó correctamente, por lo que actualmente el LCD debe mantenerse en esos pines.

---

## Monedero multimoneda

El monedero trabaja aproximadamente a 12 V.

Configuración de pulsos:

- $1 → 1 pulso
- $2 → 2 pulsos
- $5 → 5 pulsos
- $10 → 10 pulsos

Cables identificados:

- Rojo → +12 V
- Negro → GND
- Blanco → COIN / salida de pulsos
- Cable adicional gris/café → no se usa actualmente

Consumos indicados por el manual:

- Consumo en reposo: hasta aproximadamente 50 mA
- Corriente máxima de operación: hasta aproximadamente 400 mA

---

## PC817

Se usa un optoacoplador PC817 para aislar eléctricamente el monedero del ESP32.

Conexión validada:

### Lado del monedero

+12 V → resistencia aproximada de 0.9 kΩ / 1 kΩ → pin 1 del PC817

Pin 2 del PC817 → cable blanco COIN del monedero

### Lado ESP32

Pin 4 del PC817 → GPIO18 del ESP32

Pin 3 del PC817 → GND del ESP32

El ESP32 usa:

```cpp
pinMode(PIN_MONEDERO, INPUT_PULLUP);
```

por lo que no se requiere un pull-up externo para el funcionamiento actual.

---

# 3. Filtrado de pulsos

El monedero inicialmente duplicaba pulsos.

Se diagnosticó que un pulso real duraba aproximadamente 49–50 ms.

La solución estable fue usar:

```cpp
const unsigned long FILTRO_PULSO_US = 70000;
```

y una interrupción:

```cpp
attachInterrupt(
  digitalPinToInterrupt(PIN_MONEDERO),
  contarPulso,
  FALLING
);
```

La ISR actual sigue este patrón:

```cpp
void IRAM_ATTR contarPulso() {

  unsigned long ahora = micros();

  if (ahora - ultimoPulsoUs >= FILTRO_PULSO_US) {

    pulsos++;
    ultimoPulsoUs = ahora;
  }
}
```

Se validó correctamente una secuencia:

- $1 → total 1
- + $5 → total 6
- + $2 → total 8
- + $10 → total 18

---

# 4. Pantalla LCD I2C

Actualmente se usa un LCD I2C, presumiblemente 16x2.

Conexiones actuales:

- GND LCD → GND ESP32
- VCC LCD → 5 V / VIN
- SDA LCD → GPIO25
- SCL LCD → GPIO26

Dirección I2C usada inicialmente:

```cpp
#define LCD_ADDRESS 0x27
```

Si fuera necesario, la alternativa común es 0x3F.

Librería usada:

```cpp
#include <LiquidCrystal_I2C.h>
```

El LCD sí funcionó correctamente al mover I2C a GPIO25 y GPIO26.

Para centrar una ficha de 6 caracteres en un LCD de 16 columnas:

```cpp
int columna = (16 - ficha.length()) / 2;
lcd.setCursor(columna, 1);
lcd.print(ficha);
```

---

# 5. Alimentación

## Batería

Actualmente se eligió una batería:

- 12 V
- 6.5 Ah
- AGM
- Modelo visible: YB6L-B

Energía nominal aproximada:

```text
12 V × 6.5 Ah = 78 Wh
```

Consumo estimado de la expendedora:

- ESP32 + Wi-Fi: ~0.8–1.2 W
- LCD I2C: ~0.1–0.2 W
- Monedero en reposo: ~0.6 W
- Pérdidas del LM2596: ~0.1–0.2 W

Consumo promedio estimado:

```text
~1.8–2.2 W
```

Autonomía práctica estimada:

```text
~28–34 horas
```

El objetivo es que la máquina aguante al menos 24 horas.

---

## LM2596

Se usa un convertidor step-down LM2596 para bajar los 12 V de la batería a 5 V.

Conexión:

- Batería + → IN+
- Batería - → IN-
- OUT+ → 5 V / VIN del ESP32
- OUT- → GND del ESP32

La salida se ajustó aproximadamente a:

```text
5.0 V
```

El monedero se alimenta directamente desde los 12 V de la batería.

Arquitectura:

```text
Batería 12 V
   |
   +----> Monedero 12 V
   |
   +----> LM2596 → 5 V
                    |
                    +--> ESP32
                    +--> LCD
```

Se recomendó colocar un fusible de aproximadamente 1–2 A cerca del positivo de la batería.

---

# 6. Lógica del ESP32

Precio actual de una ficha:

```cpp
const unsigned long PRECIO_FICHA = 10;
```

Cada pulso equivale a $1.

Al llegar a $10 o más:

1. Se muestra “Generando ficha”.
2. Se consume Cloud Run una sola vez.
3. Si se recibe una ficha:
   - Se descuentan únicamente $10.
   - Se conserva cualquier excedente.
   - Se muestra la ficha.
4. Si falla:
   - Se descuentan únicamente $10.
   - Se genera un código de reembolso.
   - Se conserva cualquier excedente.

Ejemplo:

```text
Saldo ingresado: $11
Precio ficha:    $10
Saldo restante:   $1
```

---

# 7. Estados de pantalla

La versión refactorizada usa:

```cpp
enum EstadoPantalla {
  ESTADO_NORMAL,
  ESTADO_FICHA,
  ESTADO_REEMBOLSO
};
```

Estados:

- ESTADO_NORMAL → muestra saldo y permite venta.
- ESTADO_FICHA → muestra la ficha obtenida.
- ESTADO_REEMBOLSO → muestra un código de reembolso.

Duración de ficha o reembolso:

```cpp
const unsigned long TIEMPO_MENSAJE_MS = 20000;
```

Reglas:

- Si no entra ninguna moneda, después de 20 segundos regresa al saldo.
- Si existe saldo a favor, lo muestra.
- Si entra una nueva moneda antes de los 20 segundos, cambia inmediatamente a la pantalla de saldo.
- Esa moneda nueva no se pierde.

---

# 8. Código de reembolso

Si el servicio falla, el ESP32 genera un código local aleatorio con formato:

```text
ZD-211230
```

Ejemplo de función:

```cpp
String generarCodigoReembolso() {

  unsigned long numero =
    random(100000, 1000000);

  return "ZD-" + String(numero);
}
```

Este código representa los $10 de la operación fallida.

Cualquier dinero excedente queda como saldo a favor.

---

# 9. Wi-Fi

SSID usado en el proyecto:

```text
Zona-D
```

El código usa:

```cpp
WiFi.begin(ssid, password);
```

Para mejorar la reconexión se recomendó:

```cpp
WiFi.mode(WIFI_STA);
WiFi.setAutoReconnect(true);
WiFi.persistent(true);
```

También se propuso una función para intentar reconectar antes de declarar error:

```cpp
bool asegurarWiFi() {

  if (WiFi.status() == WL_CONNECTED) {
    return true;
  }

  WiFi.reconnect();

  unsigned long inicio = millis();

  while (
    WiFi.status() != WL_CONNECTED &&
    millis() - inicio < 10000
  ) {
    delay(500);
  }

  return WiFi.status() == WL_CONNECTED;
}
```

Esto evita generar reembolsos innecesarios si el router se reinicia momentáneamente.

---

# 10. Backend Spring Boot / Cloud Run

El backend está desplegado en Google Cloud Run.

URL base:

```text
https://fichaszd-482614074090.us-central1.run.app
```

Cloud Run está actualmente configurado con acceso público.

Por ello el ESP32 puede consumir directamente los endpoints sin token IAM de Google.

---

# 11. Endpoint de venta

Endpoint:

```text
POST /fichas/vender
```

URL:

```text
https://fichaszd-482614074090.us-central1.run.app/fichas/vender
```

Body usado:

```json
{
  "name": "Developer"
}
```

Respuesta exitosa:

```json
{
  "Ficha": "211210"
}
```

El ESP32 extrae únicamente el valor de `Ficha`.

Se configuró timeout HTTP de 30 segundos:

```cpp
http.setTimeout(30000);
```

Esto fue necesario porque Cloud Run podía tardar en responder aunque Firestore ya hubiera cambiado el estado de la ficha.

---

# 12. Firestore

Colección principal:

```text
ZonaDFichas
```

Campos:

```text
Estado
FechaVenta
Ficha
NombreCarga
```

Ejemplo:

```text
Estado: "NUEVA"
FechaVenta: ""
Ficha: "211210"
NombreCarga: "09-09-2026"
```

Cuando una ficha se vende:

```text
Estado = "VENDIDA"
```

---

# 13. Servicio para generar fichas

Se diseñó un endpoint:

```text
POST /fichas/generar
```

Objetivo:

- Generar 500 fichas por llamada.
- Cada ficha tiene 6 dígitos.
- No se repiten dentro del mismo lote.
- No se consulta Firestore para validar históricos.
- Se guardan en `ZonaDFichas`.
- Se usan IDs automáticos de Firestore.
- Se devuelve solamente la lista de claves separadas por coma.

Ejemplo de respuesta:

```text
211210,584921,740163,193552,...
```

Características almacenadas:

```text
Estado: "NUEVA"
FechaVenta: ""
Ficha: valor generado
NombreCarga: fecha de carga como String
```

---

# 14. Servicio de limpieza

Se creó un endpoint administrativo:

```text
POST /fichas/limpiar
```

Objetivo:

Eliminar todos los documentos de `ZonaDFichas` cuyo:

```text
Estado = "VENDIDA"
```

Se procesan en bloques de hasta 500 documentos usando `WriteBatch`.

Respuesta ejemplo:

```json
{
  "ok": true,
  "eliminadas": 327,
  "mensaje": "Limpieza completada"
}
```

Import importante:

```java
import com.google.cloud.firestore.DocumentSnapshot;
```

También:

```java
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
```

---

# 15. Aplicación React administrativa

Existe una aplicación React para administración de fichas.

Funciones existentes:

- Consultar resumen de fichas.
- Generar fichas.
- Limpiar fichas vendidas.
- Mostrar fichas nuevas y vendidas.
- Interactuar con Cloud Run.

Se agregó recientemente una nueva funcionalidad:

## Venta manual de ficha

Objetivo:

Cuando la expendedora no alcanza a mostrar una ficha por error, el administrador puede vender una ficha manualmente desde React.

Endpoint usado:

```text
POST /fichas/vender
```

La aplicación:

1. Llama al servicio.
2. Obtiene la ficha.
3. La muestra en pantalla.
4. Refresca el resumen.
5. La ficha queda marcada como vendida en Firestore.

Ejemplo de función agregada:

```js
export async function venderFicha() {
  const response = await fetch(buildUrl(VENDER_PATH), {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ name: "Developer" })
  });

  await validarRespuesta(response);

  const datos = await response.json();
  const ficha = datos.Ficha ?? datos.ficha;

  if (!ficha) {
    throw new Error(
      "El servicio respondió correctamente, pero no devolvió una ficha."
    );
  }

  return String(ficha);
}
```

En `.env`:

```env
VITE_VENDER_PATH=/fichas/vender
```

---

# 16. Ejecutar React localmente

Desde la carpeta del proyecto:

```bash
npm install
npm run dev
```

Vite normalmente mostrará:

```text
http://localhost:5173
```

Para desplegar:

```bash
npm run build
firebase deploy
```

---

# 17. Seguridad / arquitectura actual

Cloud Run está actualmente con acceso público.

Eso permite que:

- ESP32 consuma `/fichas/vender`
- React consuma endpoints administrativos

Se discutió anteriormente implementar JWT propio o proteger endpoints administrativos, pero actualmente no está aplicado.

Recomendación futura:

- Mantener `/fichas/vender` accesible para el ESP32.
- Proteger `/fichas/generar` y `/fichas/limpiar` con una clave administrativa o autenticación propia.

---

# 18. Comportamiento esperado de la expendedora

Caso normal:

```text
Usuario mete $5
Saldo: $5

Usuario mete $2
Saldo: $7

Usuario mete $2
Saldo: $9

Usuario mete $2
Saldo: $11

Se genera una ficha por $10

Ficha:
211210

Saldo restante:
$1
```

Después de 20 segundos:

```text
Saldo acumulado:
$1
```

Si entra una moneda antes de 20 segundos, la ficha desaparece y se muestra el nuevo saldo inmediatamente.

---

# 19. Caso de error

Ejemplo:

```text
Usuario ingresó $11
Servicio falla
```

Se genera:

```text
Codigo de Reembolso:
ZD-211230
```

Los $10 quedan asociados al reembolso.

Saldo restante:

```text
$1
```

Después de 20 segundos:

```text
Saldo acumulado:
$1
```

Si entra una moneda antes de los 20 segundos, se regresa inmediatamente a la pantalla de saldo.

---

# 20. Estado actual del proyecto

Actualmente ya está implementado y probado:

- Lectura estable del monedero.
- PC817.
- Filtro de pulsos.
- ESP32.
- Alimentación con batería de 12 V.
- LM2596 a 5 V.
- LCD I2C.
- I2C por GPIO25 y GPIO26.
- Conexión Wi-Fi.
- REST en Cloud Run.
- Firestore.
- Venta automática.
- Saldo a favor.
- Pantalla temporal de 20 segundos.
- Código de reembolso.
- Generación de lotes de fichas.
- Limpieza de fichas vendidas.
- Panel React administrativo.
- Venta manual desde React.

---

# 21. Datos importantes a conservar

## Hardware

```text
Monedero → 12 V
ESP32 → 5 V desde LM2596
LCD → 5 V
GPIO18 → pulsos
GPIO25 → SDA LCD
GPIO26 → SCL LCD
```

## Parámetros importantes

```cpp
PRECIO_FICHA = 10
FILTRO_PULSO_US = 70000
HTTP_TIMEOUT_MS = 30000
TIEMPO_MENSAJE_MS = 20000
```

## Backend

```text
Cloud Run:
https://fichaszd-482614074090.us-central1.run.app
```

## Endpoints

```text
POST /fichas/vender
POST /fichas/generar
POST /fichas/limpiar
```

## Firestore

```text
Colección:
ZonaDFichas

Estados:
NUEVA
VENDIDA
```

---

# 22. Mejoras futuras sugeridas

- Reconexión Wi-Fi robusta antes de declarar error.
- Protección de endpoints administrativos.
- Guardar códigos de reembolso en Firestore.
- Registrar fecha/hora y estado de cada reembolso.
- Crear endpoint para validar/redeemir códigos de reembolso.
- Medir consumo real del circuito para estimar autonomía exacta.
- Agregar protección por bajo voltaje de batería.
- Implementar monitoreo de voltaje de batería desde ESP32.
- Agregar indicador de batería baja en LCD.
- Agregar conector rápido XT60 o Anderson para cambio diario de batería.
