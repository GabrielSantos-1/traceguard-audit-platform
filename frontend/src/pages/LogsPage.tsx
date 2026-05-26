import { useEffect, useState } from "react"
import { ScrollText } from "lucide-react"
import { api } from "../services/api"

interface AdminAuditEvent {
  id: string
  adminId: string
  tenantId: string | null
  action: string
  resourceType: string
  resourceId: string | null
  correlationId: string
  occurredAt: string
}

export default function LogsPage() {
  const [logs, setLogs] = useState<AdminAuditEvent[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<{ content: AdminAuditEvent[] }>("/api/v1/admin/audit-events?size=50")
      .then(res => setLogs(res.data.content || []))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const actionColor = (action: string) => {
    if (action.includes("SUCCESS")) return "text-green-400"
    if (action.includes("FAILED") || action.includes("REUSE")) return "text-severity-critical"
    if (action.includes("CREATED") || action.includes("ISSUED")) return "text-blue-400"
    return "text-yellow-400"
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Logs do Sistema</h2>
        <p className="text-xs text-dark-700 mt-1">Trilha de auditoria administrativa — todas as ações de admin</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : logs.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800 p-8 text-center">
          <div className="inline-flex p-3 rounded-full bg-dark-800 mb-3">
            <ScrollText className="w-5 h-5 text-dark-700" />
          </div>
          <p className="text-sm text-gray-300 font-medium">Nenhum log disponível</p>
          <p className="text-xs text-dark-700 mt-1">Ações administrativas serão registradas automaticamente</p>
        </div>
      ) : (
        <div className="space-y-2">
          {logs.map((log) => (
            <div key={log.id} className="rounded-xl bg-dark-900 border border-dark-800 p-4 hover:border-dark-700 transition-colors">
              <div className="flex items-start gap-3">
                <div className="w-1.5 h-1.5 rounded-full bg-teal-500 mt-1.5 shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`text-xs font-semibold ${actionColor(log.action)}`}>{log.action}</span>
                    <span className="text-[10px] text-dark-700">|</span>
                    <span className="text-[10px] text-dark-700">{log.resourceType}</span>
                    {log.resourceId && (
                      <>
                        <span className="text-[10px] text-dark-700">|</span>
                        <span className="text-[10px] text-dark-700">ID: {log.resourceId.substring(0, 8)}</span>
                      </>
                    )}
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-[10px] text-dark-700">
                    <span>Correlation: {log.correlationId.substring(0, 8)}...</span>
                    <span>{new Date(log.occurredAt).toLocaleString("pt-BR")}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
