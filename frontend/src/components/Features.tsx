import { Shield, Building2, Users, Lock, Bell, BarChart3, Network, Globe, CheckCircle } from "lucide-react"

const features = [
  { icon: Shield, label: "Auditoria completa e imutável" },
  { icon: Building2, label: "Multi-tenant nativo" },
  { icon: Users, label: "RBAC granular" },
  { icon: Lock, label: "Criptografia em trânsito e repouso" },
  { icon: Bell, label: "Alertas em tempo real" },
  { icon: BarChart3, label: "Dashboards avançados" },
  { icon: Network, label: "Integração SIEM / SOAR" },
  { icon: Globe, label: "API RESTful e Webhooks" },
  { icon: CheckCircle, label: "Conformidade LGPD, ISO 27001, SOC 2" },
]

export default function Features() {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Recursos Principais</h3>
      <div className="grid grid-cols-1 gap-1.5">
        {features.map((feat) => (
          <div key={feat.label} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-dark-800/50 transition-colors">
            <feat.icon className="w-3.5 h-3.5 text-teal-500 shrink-0" />
            <span className="text-[11px] text-gray-300">{feat.label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
