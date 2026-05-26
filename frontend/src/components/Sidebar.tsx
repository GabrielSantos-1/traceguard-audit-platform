import {
  LayoutDashboard, Activity, AlertTriangle, FileText, Users, Building2, Settings, Link2, ScrollText, BookOpen
} from "lucide-react"

export type PageId =
  | "dashboard"
  | "eventos"
  | "alertas"
  | "relatorios"
  | "usuarios"
  | "tenants"
  | "configuracoes"
  | "integracoes"
  | "logs"

const navItems: { label: string; id: PageId; icon: string }[] = [
  { label: "Dashboard", id: "dashboard", icon: "LayoutDashboard" },
  { label: "Eventos", id: "eventos", icon: "Activity" },
  { label: "Alertas", id: "alertas", icon: "AlertTriangle" },
  { label: "Relatórios", id: "relatorios", icon: "FileText" },
  { label: "Usuários", id: "usuarios", icon: "Users" },
  { label: "Tenants", id: "tenants", icon: "Building2" },
  { label: "Configurações", id: "configuracoes", icon: "Settings" },
  { label: "Integrações", id: "integracoes", icon: "Link2" },
  { label: "Logs do Sistema", id: "logs", icon: "ScrollText" },
]

const iconMap: Record<string, React.ReactNode> = {
  LayoutDashboard: <LayoutDashboard className="w-4 h-4" />,
  Activity: <Activity className="w-4 h-4" />,
  AlertTriangle: <AlertTriangle className="w-4 h-4" />,
  FileText: <FileText className="w-4 h-4" />,
  Users: <Users className="w-4 h-4" />,
  Building2: <Building2 className="w-4 h-4" />,
  Settings: <Settings className="w-4 h-4" />,
  Link2: <Link2 className="w-4 h-4" />,
  ScrollText: <ScrollText className="w-4 h-4" />,
}

interface SidebarProps {
  activePage: PageId
  onNavigate: (page: PageId) => void
}

export default function Sidebar({ activePage, onNavigate }: SidebarProps) {
  return (
    <aside className="w-[240px] shrink-0 flex flex-col border-r border-dark-800 bg-dark-950 overflow-hidden">
      <nav className="flex-1 p-3 space-y-0.5 overflow-y-auto">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => onNavigate(item.id)}
            className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
              activePage === item.id
                ? "bg-accent-purple/10 text-accent-purple"
                : "text-dark-700 hover:text-gray-200 hover:bg-dark-900"
            }`}
          >
            <span className={activePage === item.id ? "text-accent-purple" : "text-dark-700"}>
              {iconMap[item.icon]}
            </span>
            {item.label}
          </button>
        ))}
      </nav>

      <div className="p-3 border-t border-dark-800">
        <button className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-dark-700 hover:text-gray-200 hover:bg-dark-900 transition-colors">
          <BookOpen className="w-4 h-4" />
          Documentação
        </button>
        <p className="text-[10px] text-dark-700 text-center mt-2">v1.0.0</p>
      </div>
    </aside>
  )
}
