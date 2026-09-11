import React, { useCallback, useEffect, useState } from "react";
import {
  consultarFichas,
  generarFichas,
  limpiarVendidas
} from "./api";

function SummaryCard({ title, value, subtitle, accent }) {
  const styles = {
    gold: {
      line: "bg-[#e4a640]",
      badge: "bg-[#fff2d6] text-[#8a5b00]",
      dot: "bg-[#e4a640]"
    },
    wine: {
      line: "bg-[#74272b]",
      badge: "bg-[#f7e7e8] text-[#74272b]",
      dot: "bg-[#74272b]"
    }
  }[accent];

  return (
    <article className="relative overflow-hidden rounded-2xl border border-[#eadfd2] bg-white p-6 shadow-[0_8px_30px_rgba(93,54,37,0.06)]">
      <div className={`absolute left-0 top-0 h-full w-1.5 ${styles.line}`} />

      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-[#76696a]">{title}</p>
          <p className="mt-3 text-4xl font-black tracking-tight text-[#2c2324]">
            {value}
          </p>
          <p className="mt-2 text-sm text-[#9b8c8d]">{subtitle}</p>
        </div>

        <span
          className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-bold ${styles.badge}`}
        >
          <span className={`h-2 w-2 rounded-full ${styles.dot}`} />
          {accent === "gold" ? "Disponibles" : "Vendidas"}
        </span>
      </div>
    </article>
  );
}

export default function App() {
  const [resumen, setResumen] = useState({
    Nuevas: 0,
    Vendidas: 0
  });

  const [numFichas, setNumFichas] = useState(100);
  const [cargandoConsulta, setCargandoConsulta] = useState(false);
  const [generando, setGenerando] = useState(false);
  const [limpiando, setLimpiando] = useState(false);
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");

  const cargarResumen = useCallback(async () => {
    setCargandoConsulta(true);
    setError("");

    try {
      const datos = await consultarFichas();

      setResumen({
        Nuevas: Number(datos.Nuevas ?? 0),
        Vendidas: Number(datos.Vendidas ?? 0)
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setCargandoConsulta(false);
    }
  }, []);

  useEffect(() => {
    cargarResumen();
  }, [cargarResumen]);

  async function handleGenerar(event) {
    event.preventDefault();

    const cantidad = Number(numFichas);

    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      setError("La cantidad debe ser un número entero mayor a cero.");
      return;
    }

    setGenerando(true);
    setError("");
    setMensaje("");

    try {
      const archivo = await generarFichas(cantidad);

      setMensaje(
        `Se generaron ${cantidad} fichas y se descargó ${archivo}.`
      );

      await cargarResumen();
    } catch (e) {
      setError(e.message);
    } finally {
      setGenerando(false);
    }
  }

  async function handleLimpiar() {
    if (resumen.Vendidas <= 0) {
      setMensaje("No hay fichas vendidas para limpiar.");
      return;
    }

    const confirmado = window.confirm(
      `¿Deseas eliminar ${resumen.Vendidas} fichas vendidas?\n\nEsta acción elimina los registros vendidos.`
    );

    if (!confirmado) return;

    setLimpiando(true);
    setError("");
    setMensaje("");

    try {
      await limpiarVendidas();
      setMensaje("Las fichas vendidas fueron limpiadas correctamente.");
      await cargarResumen();
    } catch (e) {
      setError(e.message);
    } finally {
      setLimpiando(false);
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8f3ec_0%,#f3ece2_100%)] px-4 py-6 sm:px-6 lg:px-8">
      <section className="mx-auto w-full max-w-6xl">
        <header className="mb-6 overflow-hidden rounded-3xl border border-[#eadfd2] bg-white shadow-[0_14px_40px_rgba(82,45,32,0.08)]">
          <div className="h-2 bg-[linear-gradient(90deg,#74272b_0%,#74272b_62%,#e4a640_62%,#e4a640_100%)]" />

          <div className="grid grid-cols-1 gap-5 p-5 sm:p-7 lg:grid-cols-[1fr_auto_auto] lg:items-center">
            
            {/* IZQUIERDA: CETIS */}
            <div className="flex min-w-0 items-center gap-4">
              <div className="flex h-[82px] w-[82px] shrink-0 items-center justify-center rounded-2xl border border-[#eadfd2] bg-white p-2 shadow-sm sm:h-[92px] sm:w-[92px]">
                <img
                  src="/logo-cetis.jpg"
                  alt="Logo CETIS 117"
                  className="h-full w-full object-contain"
                />
              </div>

              <div className="min-w-0">
                <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[#a06f21]">
                  CETIS 117
                </p>

                <div className="mt-2">
                  <span className="rounded-full bg-[#f3e2c5] px-3 py-1 text-xs font-bold text-[#74272b]">
                    Administración
                  </span>
                </div>

                <p className="mt-3 text-sm text-[#806f70] sm:text-base">
                  Gestión de fichas de acceso a internet
                </p>
              </div>
            </div>

           {/* CENTRO: LOGO ZONA D */}
          <div className="w-full lg:w-auto">
            <div className="mx-auto flex h-[92px] w-[120px] items-center justify-center lg:mx-0">
              <img
                src="/logo-zonad-amarillo.png"
                alt="Logo Zona D"
                className="max-h-[112px] w-auto object-contain"
              />
            </div>
          </div>

            {/* DERECHA: BOTÓN */}
            <div className="flex justify-start lg:justify-end">
              <button
                onClick={cargarResumen}
                disabled={cargandoConsulta}
                className="inline-flex items-center justify-center rounded-xl border border-[#d9c8b7] bg-[#fffaf4] px-5 py-3 text-sm font-extrabold text-[#74272b] shadow-sm transition hover:-translate-y-0.5 hover:border-[#e4a640] hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {cargandoConsulta ? "Actualizando..." : "Actualizar resumen"}
              </button>
            </div>

          </div>
        </header>

        <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <SummaryCard
            title="Fichas nuevas"
            value={resumen.Nuevas}
            subtitle="Disponibles para venta"
            accent="gold"
          />

          <SummaryCard
            title="Fichas vendidas"
            value={resumen.Vendidas}
            subtitle="Pendientes de limpieza"
            accent="wine"
          />
        </section>

        <section className="mt-5 overflow-hidden rounded-3xl border border-[#eadfd2] bg-white shadow-[0_10px_30px_rgba(93,54,37,0.06)]">
          <div className="border-b border-[#f0e6dc] bg-[#fffaf4] px-6 py-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#f3e2c5] text-xl">
                🎟️
              </div>

              <div>
                <h2 className="text-lg font-black text-[#3b2c2d]">
                  Generar fichas
                </h2>
                <p className="text-sm text-[#8f7d7e]">
                  El CSV se descarga listo para importar en Altai.
                </p>
              </div>
            </div>
          </div>

          <form
            onSubmit={handleGenerar}
            className="grid gap-4 p-6 md:grid-cols-[1fr_auto] md:items-end"
          >
            <label className="grid gap-2">
              <span className="text-sm font-extrabold text-[#574849]">
                Cantidad de fichas
              </span>

              <input
                type="number"
                min="1"
                step="1"
                value={numFichas}
                onChange={(event) => setNumFichas(event.target.value)}
                disabled={generando}
                className="h-12 w-full rounded-xl border border-[#d9c8b7] bg-white px-4 text-base font-semibold text-[#2e2526] outline-none transition placeholder:text-[#baa9aa] focus:border-[#e4a640] focus:ring-4 focus:ring-[#e4a640]/15"
              />
            </label>

            <button
              type="submit"
              disabled={generando}
              className="inline-flex h-12 items-center justify-center rounded-xl bg-[#74272b] px-6 text-sm font-extrabold text-white shadow-[0_8px_20px_rgba(116,39,43,0.22)] transition hover:-translate-y-0.5 hover:bg-[#642125] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {generando ? "Generando..." : "Generar y descargar CSV"}
            </button>
          </form>
        </section>

        <section className="mt-5 overflow-hidden rounded-3xl border border-[#eadfd2] bg-white shadow-[0_10px_30px_rgba(93,54,37,0.06)]">
          <div className="flex flex-col gap-4 p-6 md:flex-row md:items-center md:justify-between">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#f7e7e8] text-xl">
                🧹
              </div>

              <div>
                <h2 className="text-lg font-black text-[#3b2c2d]">
                  Limpiar fichas vendidas
                </h2>
                <p className="mt-1 max-w-2xl text-sm leading-6 text-[#8f7d7e]">
                  Elimina de Firestore únicamente las fichas que ya tienen estado
                  de vendida.
                </p>
              </div>
            </div>

            <button
              onClick={handleLimpiar}
              disabled={limpiando || resumen.Vendidas <= 0}
              className="inline-flex h-12 items-center justify-center rounded-xl bg-[#c93843] px-6 text-sm font-extrabold text-white shadow-[0_8px_20px_rgba(201,56,67,0.18)] transition hover:-translate-y-0.5 hover:bg-[#b8303a] disabled:cursor-not-allowed disabled:bg-[#d8c4c6] disabled:shadow-none"
            >
              {limpiando
                ? "Limpiando..."
                : `Limpiar ${resumen.Vendidas} vendidas`}
            </button>
          </div>
        </section>

        {mensaje && (
          <div className="mt-5 rounded-2xl border border-[#d9c898] bg-[#fff8df] px-5 py-4 text-sm font-semibold text-[#74551e] shadow-sm">
            {mensaje}
          </div>
        )}

        {error && (
          <div className="mt-5 rounded-2xl border border-[#e7b6ba] bg-[#fff0f1] px-5 py-4 text-sm text-[#8e252e] shadow-sm">
            <strong>Error:</strong> {error}
          </div>
        )}

        <footer className="px-4 py-7 text-center text-xs font-medium text-[#9b8989]">
          Zona D · CETIS 117 · Panel administrativo
        </footer>
      </section>
    </main>
  );
}
