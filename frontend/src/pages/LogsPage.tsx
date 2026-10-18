import { useEffect, useState } from "react"
import { api, type ApiMeta } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/States"
import Pagination from "../components/Pagination"

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

// Audit logs are read in small chunks; 10 rows keep the page light and make paging obvious.
const PAGE_SIZE = 10

export default function LogsPage() {
  const { signal, markUpdated, refresh } = useRefresh()
  const [logs, setLogs] = useState<AdminAuditEvent[]>([])
  const [meta, setMeta] = useState<ApiMeta | undefined>()
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    api
      .get<AdminAuditEvent[]>(`/api/v1/admin/audit-events?page=${page}&size=${PAGE_SIZE}`)
      .then((res) => {
        if (!alive) return
        // `data` is a plain array here; the pagination lives in `meta` (verified against the running API).
        setLogs(res.data ?? [])
        setMeta(res.meta)
        markUpdated()
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Falha ao carregar os logs")
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [page, signal, markUpdated])

  const actionColor = (action: string) => {
    if (action.includes("SUCCESS")) return "text-emerald-400"
    if (action.includes("FAILED") || action.includes("REUSE")) return "text-severity-critical"
    if (action.includes("CREATED") || action.includes("ISSUED")) return "text-blue-400"
    return "text-yellow-300"
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Logs do Sistema</h2>
        <p className="text-xs text-slate-400 mt-1">
          Trilha de auditoria administrativa — todas as ações de admin
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
      ) : logs.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <EmptyBlock message="Nenhuma ação administrativa registrada" />
        </div>
      ) : (
        <div className="rounded-xl bg-dark-900 border border-dark-800 overflow-hidden">
          <div className="space-y-2 p-4">
            {logs.map((log) => (
              <div
                key={log.id}
                className="rounded-lg bg-dark-950/40 border border-dark-800 p-3 hover:border-dark-700 transition-colors"
              >
                <div className="flex items-start gap-3">
                  <div className="w-1.5 h-1.5 rounded-full bg-teal-500 mt-1.5 shrink-0" aria-hidden="true" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`text-xs font-semibold ${actionColor(log.action)}`}>{log.action}</span>
                      <span className="text-[10px] text-slate-500">|</span>
                      <span className="text-[10px] text-slate-400">{log.resourceType}</span>
                      {log.resourceId && (
                        <>
                          <span className="text-[10px] text-slate-500">|</span>
                          <span className="text-[10px] text-slate-400">
                            ID: {log.resourceId.substring(0, 8)}
                          </span>
                        </>
                      )}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-[10px] text-slate-500">
                      <span>Correlation: {log.correlationId.substring(0, 8)}...</span>
                      <span className="tabular-nums">{new Date(log.occurredAt).toLocaleString("pt-BR")}</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <Pagination meta={meta} onPageChange={setPage} count={logs.length} />
        </div>
      )}
    </div>
  )
}
