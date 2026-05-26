import { Hexagon, Shield, Database, Search, ArrowRight } from "lucide-react"

export default function Architecture() {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Arquitetura</h3>
      <div className="space-y-3">
        <div className="flex items-center gap-2 flex-wrap">
          {["SaaS", "Serviços"].map((app) => (
            <div key={app} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-dark-800 border border-dark-700">
              <Hexagon className="w-3.5 h-3.5 text-teal-500" />
              <span className="text-[11px] font-medium text-gray-300">{app}</span>
            </div>
          ))}
          <ArrowRight className="w-3.5 h-3.5 text-dark-700" />
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-teal-500/10 border border-teal-500/30">
            <Shield className="w-3.5 h-3.5 text-teal-500" />
            <span className="text-[11px] font-semibold text-teal-400">API Gateway</span>
          </div>
          <ArrowRight className="w-3.5 h-3.5 text-dark-700" />
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-accent-purple/10 border border-accent-purple/30">
            <Shield className="w-3.5 h-3.5 text-accent-purple" />
            <span className="text-[11px] font-semibold text-accent-purple">TRACEGUARD</span>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 mt-3">
          {[
            { icon: Shield, label: "Auth JWT/OAuth2", color: "text-blue-400" },
            { icon: Database, label: "PostgreSQL", color: "text-blue-400" },
            { icon: Search, label: "Elasticsearch", color: "text-orange-400" },
            { icon: Shield, label: "Notificações", color: "text-green-400" },
          ].map((item) => (
            <div key={item.label} className="flex items-center gap-1.5 px-2 py-1.5 rounded-lg bg-dark-800/50">
              <item.icon className={`w-3 h-3 ${item.color}`} />
              <span className="text-[10px] text-dark-700">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
