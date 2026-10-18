import { useEffect, useState } from "react"
import { ArrowRight } from "lucide-react"
import { fetchRecentEvents, type RecentEvent } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { EmptyBlock, ErrorBlock, LoadingBlock } from "./States"
import type { PageId } from "./Sidebar"

/** Same palette as the severity chart: the levels have to be told apart, not merely labelled. */
const severity: Record<string, { label: string; badge: string; dot: string }> = {
  CRITICAL: { label: "Crítico", badge: "bg-red-500/10 text-red-400", dot: "#EF4444" },
  HIGH: { label: "Alto", badge: "bg-orange-500/10 text-orange-400", dot: "#F97316" },
  MEDIUM: { label: "Médio", badge: "bg-yellow-500/10 text-yellow-300", dot: "#FACC15" },
  LOW: { label: "Baixo", badge: "bg-emerald-500/10 text-emerald-400", dot: "#22C55E" },
}

const FALLBACK = { label: "—", badge: "bg-dark-800 text-slate-400", dot: "#64748B" }

interface RecentEventsProps {
  onNavigate: (page: PageId) => void
}

export default function RecentEvents({ onNavigate }: RecentEventsProps) {
  const { signal, markUpdated, refresh } = useRefresh()
  const [events, setEvents] = useState<RecentEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    fetchRecentEvents()
      .then((data) => {
        if (!alive) return
        setEvents(data)
        markUpdated()
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Falha ao carregar os eventos")
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
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos Recentes</h3>

      {loading ? (
        <LoadingBlock />
      ) : error ? (
        <ErrorBlock message={error} onRetry={refresh} />
      ) : events.length === 0 ? (
        <EmptyBlock message="Nenhum evento registrado ainda" />
      ) : (
        <div className="space-y-1">
          {events.map((event) => {
            const level = severity[event.severity] ?? FALLBACK
            const time = event.occurredAt
              ? new Date(event.occurredAt).toLocaleTimeString("pt-BR", {
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })
              : "—"
            return (
              <div
                key={event.id}
                className="flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-dark-800/50 transition-colors"
              >
                <span
                  className="w-2 h-2 rounded-full shrink-0"
                  style={{ backgroundColor: level.dot }}
                  aria-hidden="true"
                />
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium text-gray-200 truncate">{event.action}</p>
                  <p className="text-[11px] text-slate-400 truncate">{event.description}</p>
                </div>
                <span className="text-[11px] text-slate-400 shrink-0 tabular-nums">{time}</span>
                <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${level.badge}`}>
                  {level.label}
                </span>
              </div>
            )
          })}
        </div>
      )}

      <button
        type="button"
        onClick={() => onNavigate("eventos")}
        className="flex items-center gap-1 mt-3 text-xs font-medium text-teal-500 hover:text-teal-400 transition-colors"
      >
        Ver todos os eventos <ArrowRight className="w-3 h-3" aria-hidden="true" />
      </button>
    </div>
  )
}
