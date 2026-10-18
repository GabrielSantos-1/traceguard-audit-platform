import { useEffect, useState } from "react"
import { Shield, Settings, Bell, User } from "lucide-react"
import { useAuth } from "../contexts/AuthContext"
import { fetchActiveAlertCount } from "../services/api"
import type { PageId } from "./Sidebar"

const roleLabels: Record<string, string> = {
  SUPER_ADMIN: "Super Admin",
  TENANT_ADMIN: "Administrador",
  ANALYST: "Analista",
  VIEWER: "Visualizador",
}

interface HeaderProps {
  onNavigate: (page: PageId) => void
}

export default function Header({ onNavigate }: HeaderProps) {
  const { user } = useAuth()
  const [alertCount, setAlertCount] = useState<number | null>(null)

  useEffect(() => {
    fetchActiveAlertCount()
      .then(setAlertCount)
      .catch((err) => console.error("alerts/count", err))
  }, [])

  const roleLabel = user?.role ? (roleLabels[user.role] ?? user.role) : "—"

  return (
    <header className="flex items-center justify-between px-6 py-3 border-b border-dark-800 bg-dark-950/80 backdrop-blur-sm">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-lg bg-teal-500/10">
            <Shield className="w-5 h-5 text-teal-500" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight text-white">TRACEGUARD</h1>
            <span className="text-[10px] font-medium text-slate-400 -mt-1 block leading-tight">
              Audit &amp; Security Platform
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => onNavigate("configuracoes")}
          title="Configurações"
          aria-label="Abrir configurações"
          className="p-2 rounded-lg hover:bg-dark-900 transition-colors"
        >
          <Settings className="w-4 h-4 text-slate-400 hover:text-gray-200" aria-hidden="true" />
        </button>

        <button
          type="button"
          onClick={() => onNavigate("alertas")}
          title={alertCount ? `${alertCount} alerta(s) ativo(s)` : "Alertas"}
          aria-label={alertCount ? `${alertCount} alertas ativos` : "Abrir alertas"}
          className="p-2 rounded-lg hover:bg-dark-900 transition-colors relative"
        >
          <Bell className="w-4 h-4 text-slate-400 hover:text-gray-200" aria-hidden="true" />
          {/* The badge carries a real count; it used to be a permanent red dot implying notifications. */}
          {alertCount !== null && alertCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-severity-critical text-[10px] font-bold text-white flex items-center justify-center">
              {alertCount > 99 ? "99+" : alertCount}
            </span>
          )}
        </button>

        <div className="flex items-center gap-2 pl-2 border-l border-dark-800">
          <div className="w-8 h-8 rounded-full bg-dark-800 flex items-center justify-center">
            <User className="w-4 h-4 text-teal-500" aria-hidden="true" />
          </div>
          <div className="hidden sm:block">
            <p className="text-xs font-medium text-gray-200">Sessão ativa</p>
            <p className="text-[10px] text-slate-400">{roleLabel}</p>
          </div>
        </div>
      </div>
    </header>
  )
}
