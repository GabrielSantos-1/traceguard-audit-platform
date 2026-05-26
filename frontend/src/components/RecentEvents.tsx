import { useEffect, useState } from "react"
import { ArrowRight } from "lucide-react"
import { fetchRecentEvents, type RecentEvent } from "../services/api"

const severityBadge: Record<string, { label: string; class: string }> = {
  LOW: { label: "LOW", class: "bg-green-500/10 text-green-400" },
  MEDIUM: { label: "MEDIUM", class: "bg-yellow-500/10 text-yellow-400" },
  HIGH: { label: "HIGH", class: "bg-orange-500/10 text-orange-400" },
  CRITICAL: { label: "CRITICAL", class: "bg-red-500/10 text-red-400" },
}

const dotColorMap: Record<string, string> = {
  LOW: "#22C55E",
  MEDIUM: "#EAB308",
  HIGH: "#F59E0B",
  CRITICAL: "#EF4444",
}

export default function RecentEvents() {
  const [events, setEvents] = useState<RecentEvent[]>([])

  useEffect(() => {
    fetchRecentEvents().then(setEvents).catch(console.error)
  }, [])

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos Recentes</h3>
      {events.length === 0 ? (
        <div className="h-32 flex items-center justify-center">
          <p className="text-xs text-dark-700">Nenhum evento recente</p>
        </div>
      ) : (
        <div className="space-y-1">
          {events.map((event) => {
            const badge = severityBadge[event.severity] || severityBadge.LOW
            const dotColor = dotColorMap[event.severity] || "#22C55E"
            const time = event.occurredAt
              ? new Date(event.occurredAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
              : "—"
            return (
              <div
                key={event.id}
                className="flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-dark-800/50 transition-colors"
              >
                <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: dotColor }} />
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium text-gray-200 truncate">{event.action}</p>
                  <p className="text-[11px] text-dark-700 truncate">{event.description}</p>
                </div>
                <span className="text-[11px] text-dark-700 shrink-0">{time}</span>
                <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${badge.class}`}>
                  {badge.label}
                </span>
              </div>
            )
          })}
        </div>
      )}
      <button className="flex items-center gap-1 mt-3 text-xs font-medium text-teal-500 hover:text-teal-400 transition-colors">
        Ver todos os eventos <ArrowRight className="w-3 h-3" />
      </button>
    </div>
  )
}
