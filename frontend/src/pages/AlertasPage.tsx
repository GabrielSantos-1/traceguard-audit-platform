import { useEffect, useState } from "react"
import { AlertTriangle } from "lucide-react"
import { fetchCriticalAlerts, type CriticalAlert } from "../services/api"

export default function AlertasPage() {
  const [alerts, setAlerts] = useState<CriticalAlert[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchCriticalAlerts()
      .then(setAlerts)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Alertas de Segurança</h2>
        <p className="text-xs text-dark-700 mt-1">Eventos de alta severidade que requerem atenção imediata</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : alerts.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800 p-8 text-center">
          <div className="inline-flex p-3 rounded-full bg-green-500/10 mb-3">
            <AlertTriangle className="w-5 h-5 text-green-400" />
          </div>
          <p className="text-sm text-gray-300 font-medium">Nenhum alerta ativo</p>
          <p className="text-xs text-dark-700 mt-1">Sistema operando normalmente — sem eventos críticos nas últimas 24h</p>
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="rounded-xl bg-dark-900 border border-dark-800 p-5 flex items-start gap-4 hover:border-dark-700 transition-colors"
            >
              <div className={`p-2 rounded-lg shrink-0 ${
                alert.severity === "CRITICAL" ? "bg-red-500/10" : "bg-orange-500/10"
              }`}>
                <AlertTriangle className={`w-5 h-5 ${
                  alert.severity === "CRITICAL" ? "text-severity-critical" : "text-severity-high"
                }`} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-semibold text-gray-200">{alert.title}</p>
                  <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                    alert.severity === "CRITICAL"
                      ? "bg-red-500/10 text-red-400"
                      : "bg-orange-500/10 text-orange-400"
                  }`}>
                    {alert.severity}
                  </span>
                </div>
                <p className="text-xs text-dark-700 mt-1">{alert.description}</p>
                <p className="text-[10px] text-dark-700/60 mt-1">
                  {new Date(alert.occurredAt).toLocaleString("pt-BR")}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
