import { useEffect, useState } from "react"
import { AlertTriangle, CheckCircle2 } from "lucide-react"
import { api, type ApiMeta } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { ErrorBlock, LoadingBlock } from "../components/States"
import Pagination from "../components/Pagination"

interface Alert {
  id: string
  ruleName: string
  severity: string
  title: string
  description: string
  sourceIp: string | null
  acknowledged: boolean
  createdAt: string
}

const severity: Record<string, { label: string; badge: string; icon: string; bg: string }> = {
  CRITICAL: { label: "Crítico", badge: "bg-red-500/10 text-red-400", icon: "text-severity-critical", bg: "bg-red-500/10" },
  HIGH: { label: "Alto", badge: "bg-orange-500/10 text-orange-400", icon: "text-severity-high", bg: "bg-orange-500/10" },
  MEDIUM: { label: "Médio", badge: "bg-yellow-500/10 text-yellow-300", icon: "text-yellow-400", bg: "bg-yellow-500/10" },
  LOW: { label: "Baixo", badge: "bg-emerald-500/10 text-emerald-400", icon: "text-emerald-400", bg: "bg-emerald-500/10" },
}

const FALLBACK = { label: "—", badge: "bg-dark-800 text-slate-400", icon: "text-slate-400", bg: "bg-dark-800" }

const PAGE_SIZE = 20

export default function AlertasPage() {
  const { signal, markUpdated, refresh } = useRefresh()
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [meta, setMeta] = useState<ApiMeta | undefined>()
  const [page, setPage] = useState(0)
  const [severityFilter, setSeverityFilter] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    // The dashboard endpoint only returns a short "critical" list; this is the real alert feed,
    // with pagination and filters the API has always supported.
    const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (severityFilter) query.set("severity", severityFilter)
    api
      .get<Alert[]>(`/api/v1/admin/detection/alerts?${query.toString()}`)
      .then((res) => {
        if (!alive) return
        setAlerts(res.data)
        setMeta(res.meta)
        markUpdated()
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Falha ao carregar os alertas")
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [page, severityFilter, signal, markUpdated])

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-white">Alertas de Segurança</h2>
          <p className="text-xs text-slate-400 mt-1">
            Alertas gerados pelo motor de detecção, com regra, severidade e reconhecimento
          </p>
        </div>
        <label className="flex items-center gap-2 text-[11px] text-slate-400">
          Severidade
          <select
            value={severityFilter}
            onChange={(e) => {
              setSeverityFilter(e.target.value)
              setPage(0)
            }}
            className="px-2.5 py-1.5 rounded-lg bg-dark-900 border border-dark-800 text-xs text-gray-200 focus:outline-none focus:border-teal-500/50"
          >
            <option value="">Todas</option>
            <option value="CRITICAL">Crítico</option>
            <option value="HIGH">Alto</option>
            <option value="MEDIUM">Médio</option>
            <option value="LOW">Baixo</option>
          </select>
        </label>
      </div>

      {loading ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <LoadingBlock />
        </div>
      ) : error ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <ErrorBlock message={error} onRetry={refresh} />
        </div>
      ) : alerts.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800 p-8 text-center">
          <div className="inline-flex p-3 rounded-full bg-emerald-500/10 mb-3">
            <CheckCircle2 className="w-5 h-5 text-emerald-400" aria-hidden="true" />
          </div>
          <p className="text-sm text-gray-300 font-medium">
            {severityFilter ? "Nenhum alerta com essa severidade" : "Nenhum alerta registrado"}
          </p>
          <p className="text-xs text-slate-400 mt-1">
            {severityFilter
              ? "Ajuste o filtro para ver outras severidades"
              : "O motor de detecção não gerou alertas até agora"}
          </p>
        </div>
      ) : (
        <div className="rounded-xl bg-dark-900 border border-dark-800 overflow-hidden">
          <div className="space-y-3 p-4">
            {alerts.map((alert) => {
              const level = severity[alert.severity] ?? FALLBACK
              return (
                <div
                  key={alert.id}
                  className="rounded-xl bg-dark-950/40 border border-dark-800 p-4 flex items-start gap-4 hover:border-dark-700 transition-colors"
                >
                  <div className={`p-2 rounded-lg shrink-0 ${level.bg}`}>
                    <AlertTriangle className={`w-5 h-5 ${level.icon}`} aria-hidden="true" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-semibold text-gray-200">{alert.title}</p>
                      <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${level.badge}`}>
                        {level.label}
                      </span>
                      <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-dark-800 text-slate-400">
                        {alert.ruleName}
                      </span>
                      {alert.acknowledged && (
                        <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" aria-hidden="true" />
                          Reconhecido
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-400 mt-1">{alert.description}</p>
                    <p className="text-[10px] text-slate-500 mt-1 tabular-nums">
                      {new Date(alert.createdAt).toLocaleString("pt-BR")}
                      {alert.sourceIp ? ` · ${alert.sourceIp}` : ""}
                    </p>
                  </div>
                </div>
              )
            })}
          </div>
          <Pagination meta={meta} onPageChange={setPage} count={alerts.length} />
        </div>
      )}
    </div>
  )
}
