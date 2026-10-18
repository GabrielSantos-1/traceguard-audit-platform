import { useEffect, useState } from "react"
import { Info, Shield, Users } from "lucide-react"
import { fetchKpi } from "../services/api"
import { useRefresh } from "../contexts/RefreshContext"
import { ErrorBlock, LoadingBlock } from "../components/States"

/**
 * The four roles the backend actually enforces (com.traceguard.auth.AdminRole).
 * This page used to render a hardcoded user list and invented per-role counts, which reads as data
 * on a security tool; the only real number available today is the active-user count from the KPI.
 */
const roles = [
  {
    name: "SUPER_ADMIN",
    desc: "Acesso total, inclusive entre tenants",
    color: "text-violet-400",
    bg: "bg-violet-500/10",
  },
  {
    name: "TENANT_ADMIN",
    desc: "Administração restrita ao próprio tenant",
    color: "text-blue-400",
    bg: "bg-blue-500/10",
  },
  {
    name: "AUDITOR",
    desc: "Consultas, relatórios e trilha de auditoria",
    color: "text-emerald-400",
    bg: "bg-emerald-500/10",
  },
  {
    name: "OPERATOR",
    desc: "Operações do dia a dia, sem administração",
    color: "text-amber-400",
    bg: "bg-amber-500/10",
  },
]

export default function UsuariosPage() {
  const { signal, markUpdated, refresh } = useRefresh()
  const [activeUsers, setActiveUsers] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)
    fetchKpi()
      .then((kpi) => {
        if (!alive) return
        setActiveUsers(kpi.activeUsers)
        markUpdated()
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Falha ao carregar os usuários")
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
        <h2 className="text-lg font-bold text-white">Usuários</h2>
        <p className="text-xs text-slate-400 mt-1">Contas e permissões RBAC</p>
      </div>

      {loading ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <LoadingBlock />
        </div>
      ) : error ? (
        <div className="rounded-xl bg-dark-900 border border-dark-800">
          <ErrorBlock message={error} onRetry={refresh} />
        </div>
      ) : (
        <>
          <div className="rounded-xl bg-dark-900 border border-dark-800 p-4 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-500/10">
              <Users className="w-4 h-4 text-emerald-400" aria-hidden="true" />
            </div>
            <div>
              <p className="text-[11px] font-semibold text-slate-400 tracking-wider">USUÁRIOS ATIVOS</p>
              <p className="text-2xl font-bold text-white tabular-nums">
                {(activeUsers ?? 0).toLocaleString("pt-BR")}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {roles.map((role) => (
              <div key={role.name} className="rounded-xl bg-dark-900 border border-dark-800 p-4">
                <div className="flex items-center gap-2 mb-1.5">
                  <div className={`p-1.5 rounded-lg ${role.bg}`}>
                    <Shield className={`w-3.5 h-3.5 ${role.color}`} aria-hidden="true" />
                  </div>
                  <span className={`text-xs font-bold ${role.color}`}>{role.name}</span>
                </div>
                <p className="text-[11px] text-slate-400">{role.desc}</p>
              </div>
            ))}
          </div>

          <div className="flex items-start gap-2 rounded-xl bg-dark-900 border border-dark-800 px-4 py-3">
            <Info className="w-3.5 h-3.5 text-slate-400 mt-0.5 shrink-0" aria-hidden="true" />
            <p className="text-[11px] text-slate-400">
              A listagem e a gestão de contas ainda não têm endpoint no backend — por isso esta tela
              mostra apenas os papéis que o sistema aplica e a contagem real de usuários ativos. Os
              dados de usuários por papel aparecerão aqui quando a API existir.
            </p>
          </div>
        </>
      )}
    </div>
  )
}
