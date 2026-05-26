import { useEffect, useState } from "react"
import { AlertTriangle, ArrowRight } from "lucide-react"
import { fetchCriticalAlerts, type CriticalAlert } from "../services/api"

export default function CriticalAlerts() {
  const [alerts, setAlerts] = useState<CriticalAlert[]>([])

  useEffect(() => {
    fetchCriticalAlerts().then(setAlerts).catch(console.error)
  }, [])

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Alertas Críticos</h3>
      {alerts.length === 0 ? (
        <div className="h-32 flex items-center justify-center">
          <p className="text-xs text-dark-700">Nenhum alerta crítico</p>
        </div>
      ) : (
        <div className="space-y-2">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="flex items-start gap-3 px-2 py-2.5 rounded-lg hover:bg-dark-800/50 transition-colors"
            >
              <AlertTriangle className={`w-4 h-4 mt-0.5 shrink-0 ${
                alert.severity === "CRITICAL" ? "text-severity-critical" : "text-severity-high"
              }`} />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-xs font-semibold text-gray-200">{alert.title}</p>
                  <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${
                    alert.severity === "CRITICAL"
                      ? "bg-red-500/10 text-red-400"
                      : "bg-orange-500/10 text-orange-400"
                  }`}>
                    {alert.severity}
                  </span>
                </div>
                <p className="text-[11px] text-dark-700 mt-0.5">{alert.description}</p>
              </div>
            </div>
          ))}
        </div>
      )}
      <button className="flex items-center gap-1 mt-3 text-xs font-medium text-teal-500 hover:text-teal-400 transition-colors">
        Ver todos os alertas <ArrowRight className="w-3 h-3" />
      </button>
    </div>
  )
}
