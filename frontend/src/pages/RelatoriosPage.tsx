import { FileText, BarChart3 } from "lucide-react"

const reports = [
  { title: "Relatório de Eventos", desc: "Sumário completo de todos os eventos de auditoria por período", icon: BarChart3 },
  { title: "Relatório de Conformidade", desc: "Métricas de aderência a políticas de segurança e conformidade", icon: FileText },
  { title: "Análise de Tendências", desc: "Identificação de padrões e anomalias ao longo do tempo", icon: BarChart3 },
  { title: "Relatório de Acessos", desc: "Logs de autenticação e autorização por usuário/tenant", icon: FileText },
]

export default function RelatoriosPage() {
  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Relatórios</h2>
        <p className="text-xs text-dark-700 mt-1">Insights e análises gerados automaticamente pela plataforma</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {reports.map((r) => (
          <div key={r.title} className="rounded-xl bg-dark-900 border border-dark-800 p-5 hover:border-dark-700 transition-colors cursor-pointer group">
            <div className="p-2 rounded-lg bg-teal-500/10 w-fit mb-3 group-hover:scale-110 transition-transform">
              <r.icon className="w-4 h-4 text-teal-500" />
            </div>
            <h3 className="text-sm font-semibold text-gray-200">{r.title}</h3>
            <p className="text-xs text-dark-700 mt-1">{r.desc}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
