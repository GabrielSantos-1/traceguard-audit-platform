import { useEffect, useState } from "react"
import { Building2, CheckCircle, XCircle } from "lucide-react"
import { api, type ApiMeta } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/States"
import Pagination from "../components/Pagination"

interface Tenant {
  id: string
  name: string
  slug: string
  status: string
  createdAt: string
}

const PAGE_SIZE = 12

export default function TenantsPage() {
  const { signal, markUpdated, refresh } = useRefresh()
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [meta, setMeta] = useState<ApiMeta | undefined>()
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    // This endpoint answers a plain array in `data` with the pagination in `meta` — reading
    // `data.content` here (as this page used to) yields undefined and an always-empty screen.
    api
      .get<Tenant[]>(`/api/v1/admin/tenants?page=${page}&size=${PAGE_SIZE}`)
      .then((res) => {
        if (!alive) return
        setTenants(res.data ?? [])
        setMeta(res.meta)
        markUpdated()
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Falha ao carregar os tenants")
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [page, signal, markUpdated])

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Tenants</h2>
        <p className="text-xs text-slate-400 mt-1">
          Gerenciamento de multi-tenancy e isolamento de dados
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
      ) : tenants.length === 0 ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <EmptyBlock message="Nenhum tenant cadastrado" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {tenants.map((t) => (
              <div
                key={t.id}
                className="rounded-xl bg-dark-900 border border-dark-800 p-5 hover:border-dark-700 transition-colors"
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="p-2 rounded-lg bg-blue-500/10">
                    <Building2 className="w-4 h-4 text-blue-400" aria-hidden="true" />
                  </div>
                  {t.status === "ACTIVE" ? (
                    <span className="flex items-center gap-1 text-[10px] text-emerald-400 font-medium">
                      <CheckCircle className="w-3 h-3" aria-hidden="true" /> Ativo
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-[10px] text-red-400 font-medium">
                      <XCircle className="w-3 h-3" aria-hidden="true" /> {t.status}
                    </span>
                  )}
                </div>
                <h3 className="text-sm font-semibold text-gray-200">{t.name}</h3>
                <p className="text-[11px] text-slate-400 mt-0.5">/{t.slug}</p>
                <p className="text-[10px] text-slate-500 mt-2 tabular-nums">
                  Criado em {new Date(t.createdAt).toLocaleDateString("pt-BR")}
                </p>
              </div>
            ))}
          </div>
          <div className="rounded-xl bg-dark-900 border border-dark-800">
            <Pagination meta={meta} onPageChange={setPage} count={tenants.length} />
          </div>
        </>
      )}
    </div>
  )
}
