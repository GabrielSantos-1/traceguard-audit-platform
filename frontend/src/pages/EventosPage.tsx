import { useEffect, useState } from "react"
import { api, type RecentEvent } from "../services/api"

export default function EventosPage() {
  const [events, setEvents] = useState<RecentEvent[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<RecentEvent[]>("/api/v1/admin/dashboard/recent-events")
      .then(res => setEvents(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const severityClass = (s: string) => {
    const map: Record<string, string> = {
      LOW: "bg-green-500/10 text-green-400",
      MEDIUM: "bg-yellow-500/10 text-yellow-400",
      HIGH: "bg-orange-500/10 text-orange-400",
      CRITICAL: "bg-red-500/10 text-red-400",
    }
    return map[s] || "bg-dark-700 text-dark-700"
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Eventos de Auditoria</h2>
        <p className="text-xs text-dark-700 mt-1">Registro de todos os eventos capturados pelo TraceGuard</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : events.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800 p-8 text-center">
          <p className="text-sm text-dark-700">Nenhum evento registrado ainda</p>
          <p className="text-xs text-dark-700/60 mt-1">Eventos aparecerão aqui conforme forem ingeridos via API Key</p>
        </div>
      ) : (
        <div className="rounded-xl bg-dark-900 border border-dark-800 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-dark-800">
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase tracking-wider">Ação</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase tracking-wider">Descrição</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase tracking-wider">Severidade</th>
                <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase tracking-wider">IP</th>
                <th className="text-right px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase tracking-wider">Data/Hora</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-dark-800/50">
              {events.map((ev) => (
                <tr key={ev.id} className="hover:bg-dark-800/30 transition-colors">
                  <td className="px-4 py-3 text-xs font-medium text-gray-200">{ev.action}</td>
                  <td className="px-4 py-3 text-xs text-dark-700">{ev.description}</td>
                  <td className="px-4 py-3">
                    <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${severityClass(ev.severity)}`}>
                      {ev.severity}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-dark-700">{ev.ip || "—"}</td>
                  <td className="px-4 py-3 text-xs text-dark-700 text-right">
                    {new Date(ev.occurredAt).toLocaleString("pt-BR")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
