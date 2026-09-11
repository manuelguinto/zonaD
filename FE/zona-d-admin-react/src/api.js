const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  "https://fichaszd-482614074090.us-central1.run.app";

const CONSULTA_PATH =
  import.meta.env.VITE_CONSULTA_PATH || "/fichas/consulta";

const GENERAR_PATH =
  import.meta.env.VITE_GENERAR_PATH || "/fichas/generar";

const LIMPIAR_PATH =
  import.meta.env.VITE_LIMPIAR_PATH || "/fichas/limpiar";

const LIMPIAR_METHOD =
  import.meta.env.VITE_LIMPIAR_METHOD || "DELETE";

const buildUrl = (path) => `${API_BASE_URL}${path}`;

async function validarRespuesta(response) {
  if (!response.ok) {
    let mensaje = `Error HTTP ${response.status}`;

    try {
      const texto = await response.text();
      if (texto) mensaje += `: ${texto}`;
    } catch {
      // No-op
    }

    throw new Error(mensaje);
  }

  return response;
}

export async function consultarFichas() {
  const response = await fetch(buildUrl(CONSULTA_PATH), {
    method: "GET",
    headers: { Accept: "application/json" }
  });

  await validarRespuesta(response);
  return response.json();
}

function obtenerNombreArchivo(response) {
  const disposition = response.headers.get("content-disposition");

  if (!disposition) return "ZonaD_fichas.csv";

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].replace(/["']/g, ""));
  }

  const simpleMatch = disposition.match(/filename="?([^"]+)"?/i);
  return simpleMatch?.[1]?.trim() || "ZonaD_fichas.csv";
}

export async function generarFichas(numFichas) {
  const params = new URLSearchParams({
    numFichas: String(numFichas)
  });

  const response = await fetch(
    `${buildUrl(GENERAR_PATH)}?${params.toString()}`,
    {
      method: "POST",
      headers: { Accept: "text/csv" }
    }
  );

  await validarRespuesta(response);

  const blob = await response.blob();
  const nombreArchivo = obtenerNombreArchivo(response);
  const blobUrl = URL.createObjectURL(blob);

  const enlace = document.createElement("a");
  enlace.href = blobUrl;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(blobUrl);

  return nombreArchivo;
}

export async function limpiarVendidas() {
  const response = await fetch(buildUrl(LIMPIAR_PATH), {
    method: "POST",
    headers: { Accept: "application/json, text/plain, */*" }
  });

  await validarRespuesta(response);

  const contentType = response.headers.get("content-type") || "";
  return contentType.includes("application/json")
    ? response.json()
    : response.text();
}
