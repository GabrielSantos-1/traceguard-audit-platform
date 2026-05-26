import { useEffect, useState } from "react"
import { Building2, CheckCircle, XCircle } from "lucide-react"
import { api } from "../services/api"

interface Tenant {
  id: string
  name: string
  slug: string
  status: string
  createdAt: string
}

export default function TenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<{ content: Tenant[] }>("/api/v1/admin/tenants?size=50")
      .then(res => setTenants(res.data.content || []))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Tenants</h2>
        <p className="text-xs text-dark-700 mt-1">Gerenciamento de multi-tenancy e isolamento de dados</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {tenants.map((t) => (
            <div key={t.id} className="rounded-xl bg-dark-900 border border-dark-800 p-5 hover:border-dark-700 transition-colors">
              <div className="flex items-start justify-between mb-3">
                <div className="p-2 rounded-lg bg-purple-500/10">
                  <Building2 className="w-4 h-4 text-purple-400" />
                </div>
                {t.status === "ACTIVE" ? (
                  <span className="flex items-center gap-1 text-[10px] text-green-400 font-medium">
                    <CheckCircle className="w-3 h-3" /> Ativo
                  </span>
                ) : (
                  <span className="flex items-center gap-1 text-[10px] text-red-400 font-medium">
                    <XCircle className="w-3 h-3" /> {t.status}
                  </span>
                )}
              </div>
              <h3 className="text-sm font-semibold text-gray-200">{t.name}</h3>
              <p className="text-[11px] text-dark-700 mt-0.5">/{t.slug}</p>
              <p className="text-[10px] text-dark-700/60 mt-2">
                Criado em {new Date(t.createdAt).toLocaleDateString("pt-BR")}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
