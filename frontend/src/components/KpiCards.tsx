import { useEffect, useState } from "react"
import { Activity, AlertTriangle, Users, Box } from "lucide-react"
import { fetchKpi, type DashboardKpi } from "../services/api"

export default function KpiCards() {
  const [kpi, setKpi] = useState<DashboardKpi | null>(null)

  useEffect(() => {
    fetchKpi().then(setKpi).catch(console.error)
  }, [])

  const items = [
    {
      title: "EVENTOS (24h)",
      value: kpi ? kpi.totalEvents24h.toLocaleString("pt-BR") : "—",
      change: kpi ? `${kpi.eventsChangePercent > 0 ? "↑" : "↓"} ${Math.abs(kpi.eventsChangePercent)}%` : "—",
      trend: kpi && kpi.eventsChangePercent >= 0 ? ("up" as const) : ("down" as const),
      icon: Activity,
      iconBg: "bg-blue-500/10",
      iconColor: "text-blue-400",
    },
    {
      title: "ALERTAS ATIVOS",
      value: kpi ? kpi.activeAlerts.toLocaleString("pt-BR") : "—",
      change: kpi ? `${kpi.alertsChangePercent > 0 ? "↑" : "↓"} ${Math.abs(kpi.alertsChangePercent)}%` : "—",
      trend: kpi && kpi.alertsChangePercent >= 0 ? ("up" as const) : ("down" as const),
      icon: AlertTriangle,
      iconBg: "bg-orange-500/10",
      iconColor: "text-orange-400",
    },
    {
      title: "USUÁRIOS ATIVOS",
      value: kpi ? kpi.activeUsers.toLocaleString("pt-BR") : "—",
      change: kpi ? `${kpi.usersChangePercent > 0 ? "↑" : "↓"} ${Math.abs(kpi.usersChangePercent)}%` : "—",
      trend: kpi && kpi.usersChangePercent >= 0 ? ("up" as const) : ("down" as const),
      icon: Users,
      iconBg: "bg-green-500/10",
      iconColor: "text-green-400",
    },
    {
      title: "TENANTS",
      value: kpi ? kpi.totalTenants.toLocaleString("pt-BR") : "—",
      change: "—",
      trend: "neutral" as const,
      icon: Box,
      iconBg: "bg-purple-500/10",
      iconColor: "text-purple-400",
    },
  ]

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {items.map((item) => (
        <div
          key={item.title}
          className="rounded-xl bg-dark-900 border border-dark-800 p-4 transition-all duration-200 hover:border-dark-700 hover:shadow-lg hover:shadow-black/20 group"
        >
          <div className="flex items-start justify-between">
            <p className="text-[11px] font-semibold text-dark-700 tracking-wider">{item.title}</p>
            <div className={`p-2 rounded-lg ${item.iconBg} transition-transform duration-200 group-hover:scale-110`}>
              <item.icon className={`w-4 h-4 ${item.iconColor}`} />
            </div>
          </div>
          <p className="text-2xl font-bold text-white mt-2">{item.value}</p>
          <p className={`text-xs font-medium mt-1 ${
            item.trend === "up" ? "text-green-400" :
            item.trend === "down" ? "text-severity-critical" :
            "text-dark-700"
          }`}>
            {item.change} vs ontem
          </p>
        </div>
      ))}
    </div>
  )
}
