import { useEffect, useState } from "react"
import { Info } from "lucide-react"
import { api } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/States"

interface RecentEvent {
  id: string
  action: string
  description: string
  severity: string
  ip: string | null
  occurredAt: string
}

const severity: Record<string, { label: string; badge: string }> = {
  CRITICAL: { label: "Crítico", badge: "bg-red-500/10 text-red-400" },
  HIGH: { label: "Alto", badge: "bg-orange-500/10 text-orange-400" },
  MEDIUM: { label: "Médio", badge: "bg-yellow-500/10 text-yellow-300" },
  LOW: { label: "Baixo", badge: "bg-emerald-500/10 text-emerald-400" },
}

const FALLBACK = { label: "—", badge: "bg-dark-800 text-slate-400" }

export default function EventosPage() {
  const { signal, markUpdated, refresh } = useRefresh()
  const [events, setEvents] = useState<RecentEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    api
      .get<RecentEvent[]>("/api/v1/admin/dashboard/recent-events")
      .then((res) => {
        if (!alive) return
        setEvents(res.data)
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
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Eventos Recentes</h2>
        <p className="text-xs text-slate-400 mt-1">
          Últimos eventos de segurança capturados pelo TraceGuard
        </p>
      </div>

      {/* The page used to promise "todos os eventos" while showing only the dashboard's short list. */}
      <div className="flex items-start gap-2 rounded-xl bg-dark-900 border border-dark-800 px-4 py-3">
        <Info className="w-3.5 h-3.5 text-slate-400 mt-0.5 shrink-0" aria-hidden="true" />
        <p className="text-[11px] text-slate-400">
          Esta visão traz os eventos mais recentes. O histórico completo, com filtros por período,
          severidade e ação, está disponível na API de consulta{" "}
          <code className="text-slate-300">GET /api/v1/audit/events</code> — que exige uma chave de API,
          não a sessão do painel.
        </p>
      </div>

      {loading ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <LoadingBlock />
        </div>
      ) : error ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <ErrorBlock message={error} onRetry={refresh} />
        </div>
      ) : events.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <EmptyBlock message="Nenhum evento registrado ainda" />
        </div>
      ) : (
        <div className="rounded-xl bg-dark-900 border border-dark-800 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-dark-800">
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Ação</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Descrição</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Severidade</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">IP</th>
                <th className="text-right px-4 py-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Data/Hora</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-dark-800/50">
              {events.map((ev) => {
                const level = severity[ev.severity] ?? FALLBACK
                return (
                  <tr key={ev.id} className="hover:bg-dark-800/30 transition-colors">
                    <td className="px-4 py-3 text-xs font-medium text-gray-200">{ev.action}</td>
                    <td className="px-4 py-3 text-xs text-slate-400">{ev.description}</td>
                    <td className="px-4 py-3">
                      <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${level.badge}`}>
                        {level.label}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400 tabular-nums">{ev.ip || "—"}</td>
                    <td className="px-4 py-3 text-xs text-slate-400 text-right tabular-nums">
                      {new Date(ev.occurredAt).toLocaleString("pt-BR")}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
