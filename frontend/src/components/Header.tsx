import { Shield, Search, Settings, Bell, User } from "lucide-react"

export default function Header() {
  return (
    <header className="flex items-center justify-between px-6 py-3 border-b border-dark-800 bg-dark-950/80 backdrop-blur-sm">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-lg bg-teal-500/10">
            <Shield className="w-5 h-5 text-teal-500" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight text-white">TRACEGUARD</h1>
            <span className="text-[10px] font-medium text-dark-700 -mt-1 block leading-tight">Audit & Security Platform</span>
          </div>
        </div>
      </div>

      <div className="flex-1 max-w-md mx-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-700" />
          <input
            type="text"
            placeholder="Buscar..."
            className="w-full pl-9 pr-4 py-2 rounded-lg bg-dark-900 border border-dark-800 text-sm text-gray-300 placeholder-dark-700 focus:outline-none focus:border-teal-500/50 transition-colors"
          />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button className="p-2 rounded-lg hover:bg-dark-900 transition-colors">
          <Settings className="w-4 h-4 text-dark-700 hover:text-gray-300" />
        </button>
        <button className="p-2 rounded-lg hover:bg-dark-900 transition-colors relative">
          <Bell className="w-4 h-4 text-dark-700 hover:text-gray-300" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-severity-critical" />
        </button>
        <div className="flex items-center gap-2 pl-2 border-l border-dark-800">
          <div className="w-8 h-8 rounded-full bg-dark-800 flex items-center justify-center">
            <User className="w-4 h-4 text-teal-500" />
          </div>
          <div className="hidden sm:block">
            <p className="text-xs font-medium text-gray-300">Administrador</p>
            <p className="text-[10px] text-dark-700">Super Admin</p>
          </div>
        </div>
      </div>
    </header>
  )
}
