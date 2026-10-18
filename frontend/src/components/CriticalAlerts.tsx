import { useEffect, useState } from "react"
import { AlertTriangle, ArrowRight } from "lucide-react"
import { fetchCriticalAlerts, type CriticalAlert } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { EmptyBlock, ErrorBlock, LoadingBlock } from "./States"
import type { PageId } from "./Sidebar"

const severity: Record<string, { label: string; badge: string; icon: string }> = {
  CRITICAL: { label: "Crítico", badge: "bg-red-500/10 text-red-400", icon: "text-severity-critical" },
  HIGH: { label: "Alto", badge: "bg-orange-500/10 text-orange-400", icon: "text-severity-high" },
}

const FALLBACK = { label: "—", badge: "bg-dark-800 text-slate-400", icon: "text-slate-400" }

interface CriticalAlertsProps {
  onNavigate: (page: PageId) => void
}

export default function CriticalAlerts({ onNavigate }: CriticalAlertsProps) {
  const { signal, markUpdated, refresh } = useRefresh()
  const [alerts, setAlerts] = useState<CriticalAlert[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    fetchCriticalAlerts()
      .then((data) => {
        if (!alive) return
        setAlerts(data)
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
  }, [signal, markUpdated])

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Alertas Críticos</h3>

      {loading ? (
        <LoadingBlock />
      ) : error ? (
        <ErrorBlock message={error} onRetry={refresh} />
      ) : alerts.length === 0 ? (
        <EmptyBlock message="Nenhum alerta crítico no momento" />
      ) : (
        <div className="space-y-2">
          {alerts.map((alert) => {
            const level = severity[alert.severity] ?? FALLBACK
            return (
              <div
                key={alert.id}
                className="flex items-start gap-3 px-2 py-2.5 rounded-lg hover:bg-dark-800/50 transition-colors"
              >
                <AlertTriangle className={`w-4 h-4 mt-0.5 shrink-0 ${level.icon}`} aria-hidden="true" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-xs font-semibold text-gray-200">{alert.title}</p>
                    <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${level.badge}`}>
                      {level.label}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">{alert.description}</p>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <button
        type="button"
        onClick={() => onNavigate("alertas")}
        className="flex items-center gap-1 mt-3 text-xs font-medium text-teal-500 hover:text-teal-400 transition-colors"
      >
        Ver todos os alertas <ArrowRight className="w-3 h-3" aria-hidden="true" />
      </button>
    </div>
  )
}
