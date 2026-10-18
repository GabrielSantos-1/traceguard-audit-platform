import { useState } from "react"
import { BarChart3, Bell, Download, FileText, Loader2, Clock, Users, AlertCircle } from "lucide-react"
import { api } from "../services/api"

/**
 * Exports walk every page of the API instead of asking for one large slice: the list endpoints cap
 * `size` at 100 rows, so a single request can never return a full report.
 */
const PAGE_SIZE = 100
const MAX_PAGES = 50 // 5.000 linhas por relatório

type Row = Record<string, unknown>

async function fetchAllRows(path: string): Promise<Row[]> {
  const rows: Row[] = []
  for (let page = 0; page < MAX_PAGES; page++) {
    const res = await api.get<Row[] | { content?: Row[] }>(`${path}?page=${page}&size=${PAGE_SIZE}`)
    const data = res.data
    // Some endpoints answer a Spring Page inside `data` ({content, last}), others a plain array with
    // the pagination in `meta`. Detecting the shape beats trusting a flag that can drift from the API.
    const batch: Row[] = Array.isArray(data) ? data : (data?.content ?? [])
    rows.push(...batch)

    const hasNext = Array.isArray(data)
      ? Boolean(res.meta?.hasNext) || batch.length === PAGE_SIZE
      : !(data as { last?: boolean }).last
    if (!hasNext || batch.length === 0) break
  }
  return rows
}

/** Excel in pt-BR expects ";" as the separator and a BOM to read accents as UTF-8. */
function toCsv(rows: Row[]): string {
  if (rows.length === 0) return ""
  const headers = Object.keys(rows[0])
  const cell = (value: unknown) => {
    if (value === null || value === undefined) return ""
    // Nested objects/arrays (e.g. detailsJson) must be serialised, not coerced to "[object Object]".
    const text = typeof value === "object" ? JSON.stringify(value) : String(value)
    return /[";\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  return [
    headers.join(";"),
    ...rows.map((row) => headers.map((h) => cell(row[h])).join(";")),
  ].join("\r\n")
}

function downloadCsv(filename: string, rows: Row[]) {
  const blob = new Blob([`\uFEFF${toCsv(rows)}`], { type: "text/csv;charset=utf-8;" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

interface ReportDef {
  id: string
  title: string
  description: string
  icon: typeof BarChart3
  path: string
}

const reports: ReportDef[] = [
  {
    id: "eventos",
    title: "Eventos de auditoria",
    description: "Todos os eventos administrativos registrados: ação, recurso, autor e correlação.",
    icon: BarChart3,
    path: "/api/v1/admin/audit-events",
  },
  {
    id: "alertas",
    title: "Alertas de detecção",
    description: "Alertas gerados pelo motor de detecção, com regra, severidade e reconhecimento.",
    icon: Bell,
    path: "/api/v1/admin/detection/alerts",
  },
  {
    id: "acessos",
    title: "Atividade de usuários",
    description: "Trilha de autenticação e autorização por usuário, com resultado de cada tentativa.",
    icon: Users,
    path: "/api/v1/admin/activity",
  },
]

type Status = { kind: "idle" | "working" | "done" | "error"; message?: string }

export default function RelatoriosPage() {
  const [status, setStatus] = useState<Record<string, Status>>({})

  const runExport = async (report: ReportDef) => {
    setStatus((s) => ({ ...s, [report.id]: { kind: "working" } }))
    try {
      const rows = await fetchAllRows(report.path)
      if (rows.length === 0) {
        setStatus((s) => ({
          ...s,
          [report.id]: { kind: "error", message: "Nenhum registro para exportar" },
        }))
        return
      }
      const stamp = new Date().toISOString().slice(0, 10)
      downloadCsv(`traceguard-${report.id}-${stamp}.csv`, rows)
      setStatus((s) => ({
        ...s,
        [report.id]: { kind: "done", message: `${rows.length.toLocaleString("pt-BR")} registros exportados` },
      }))
    } catch (err) {
      console.error("export", report.id, err)
      setStatus((s) => ({
        ...s,
        [report.id]: { kind: "error", message: "Falha ao gerar o relatório" },
      }))
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Relatórios</h2>
        <p className="text-xs text-slate-400 mt-1">
          Exportação em CSV dos registros da plataforma — até 5.000 linhas por relatório
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {reports.map((report) => {
          const state = status[report.id] ?? { kind: "idle" as const }
          const working = state.kind === "working"
          return (
            <button
              key={report.id}
              type="button"
              onClick={() => runExport(report)}
              disabled={working}
              className="text-left rounded-xl bg-dark-900 border border-dark-800 p-5 hover:border-teal-500/40 transition-colors group disabled:opacity-60 disabled:cursor-wait"
            >
              <div className="flex items-start justify-between">
                <div className="p-2 rounded-lg bg-teal-500/10 w-fit mb-3 transition-transform group-hover:scale-110">
                  <report.icon className="w-4 h-4 text-teal-500" aria-hidden="true" />
                </div>
                {working ? (
                  <Loader2 className="w-4 h-4 text-teal-500 animate-spin" aria-hidden="true" />
                ) : (
                  <Download className="w-4 h-4 text-slate-400 group-hover:text-teal-500 transition-colors" aria-hidden="true" />
                )}
              </div>
              <h3 className="text-sm font-semibold text-gray-200">{report.title}</h3>
              <p className="text-xs text-slate-400 mt-1">{report.description}</p>
              {state.kind === "done" && (
                <p className="text-[11px] text-emerald-400 mt-2">{state.message}</p>
              )}
              {state.kind === "error" && (
                <p className="text-[11px] text-red-400 mt-2 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" aria-hidden="true" />
                  {state.message}
                </p>
              )}
            </button>
          )
        })}

        {/* Honest placeholder: no data source exists for this one yet, so it does not pretend to be clickable. */}
        <div className="rounded-xl bg-dark-900/50 border border-dashed border-dark-800 p-5">
          <div className="flex items-start justify-between">
            <div className="p-2 rounded-lg bg-dark-800 w-fit mb-3">
              <FileText className="w-4 h-4 text-slate-400" aria-hidden="true" />
            </div>
            <span className="text-[10px] font-semibold text-slate-400 bg-dark-800 border border-dark-700 rounded px-2 py-0.5 flex items-center gap-1">
              <Clock className="w-3 h-3" aria-hidden="true" />
              EM BREVE
            </span>
          </div>
          <h3 className="text-sm font-semibold text-slate-300">Relatório de conformidade</h3>
          <p className="text-xs text-slate-400 mt-1">
            Aderência a políticas de segurança por período. Requer agregação histórica que ainda não é
            calculada pelo backend.
          </p>
        </div>
      </div>
    </div>
  )
}
