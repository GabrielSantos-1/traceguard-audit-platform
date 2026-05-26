import { Shield, UserCheck } from "lucide-react"

const mockUsers = [
  { name: "Admin Principal", email: "admin@traceguard.local", role: "SUPER_ADMIN", status: "active" },
]

const roles = [
  { name: "SUPER_ADMIN", desc: "Acesso total ao sistema", count: 1, color: "text-purple-400" },
  { name: "TENANT_ADMIN", desc: "Administração por tenant", count: 0, color: "text-blue-400" },
  { name: "AUDITOR", desc: "Consultas e relatórios", count: 0, color: "text-green-400" },
  { name: "OPERATOR", desc: "Operações do dia a dia", count: 0, color: "text-yellow-400" },
]

export default function UsuariosPage() {
  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Usuários</h2>
        <p className="text-xs text-dark-700 mt-1">Gerenciamento de contas e permissões RBAC</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {roles.map((r) => (
          <div key={r.name} className="rounded-xl bg-dark-900 border border-dark-800 p-4">
            <div className="flex items-center gap-2 mb-2">
              <Shield className={`w-4 h-4 ${r.color}`} />
              <span className={`text-xs font-bold ${r.color}`}>{r.name}</span>
            </div>
            <p className="text-2xl font-bold text-white">{r.count}</p>
            <p className="text-[11px] text-dark-700 mt-0.5">{r.desc}</p>
          </div>
        ))}
      </div>

      <div className="rounded-xl bg-dark-900 border border-dark-800 overflow-hidden">
        <div className="px-4 py-3 border-b border-dark-800">
          <h3 className="text-xs font-semibold text-gray-200">Usuários Cadastrados</h3>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-dark-800">
              <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase">Nome</th>
              <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase">Email</th>
              <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase">Papel</th>
              <th className="text-left px-4 py-3 text-[11px] font-semibold text-dark-700 uppercase">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-dark-800/50">
            {mockUsers.map((u) => (
              <tr key={u.email} className="hover:bg-dark-800/30">
                <td className="px-4 py-3 text-xs font-medium text-gray-200">{u.name}</td>
                <td className="px-4 py-3 text-xs text-dark-700">{u.email}</td>
                <td className="px-4 py-3">
                  <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-purple-500/10 text-purple-400">
                    {u.role}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="flex items-center gap-1 text-xs text-green-400">
                    <UserCheck className="w-3 h-3" /> Ativo
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
