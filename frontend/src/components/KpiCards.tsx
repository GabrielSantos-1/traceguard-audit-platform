import { useEffect, useState } from "react"
import { Activity, AlertTriangle, Users, Box } from "lucide-react"
import { fetchKpi, type DashboardKpi } from "../services/api"

/**
 * How a rise in each metric should be read:
 *   "good" — more is better (active users)
 *   "bad"  — more is worse (active alerts: on a security dashboard, growth in alerts is not a win)
 *   null   — neutral, the number is informational (events, tenants)
 *
 * A rise is never painted green by default; the colour has to be earned by the metric's meaning.
 */
type Direction = "good" | "bad" | null

interface Trend {
  text: string
  className: string
}

const NEUTRAL = "text-slate-400"
const GOOD = "text-emerald-400"
const BAD = "text-red-400"

function describeTrend(
  change: number | null | undefined,
  value: number,
  direction: Direction,
): Trend {
  // No such metric on this card (tenants): nothing to report at all.
  if (change === undefined) {
    return { text: "—", className: NEUTRAL }
  }

  // The API says there is no baseline (the previous window was empty). Say so instead of inventing 0%.
  if (change === null) {
    return { text: value > 0 ? "novo" : "—", className: NEUTRAL }
  }

  // No movement is not a movement: no arrow, no colour.
  if (change === 0) {
    return { text: "sem variação", className: NEUTRAL }
  }

  const rising = change > 0
  const text = `${rising ? "↑" : "↓"} ${Math.abs(change).toLocaleString("pt-BR")}%`

  if (direction === null) {
    return { text, className: NEUTRAL }
  }

  const isGood = direction === "good" ? rising : !rising
  return { text, className: isGood ? GOOD : BAD }
}

function KpiSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="rounded-xl bg-dark-900 border border-dark-800 p-4 animate-pulse">
          <div className="flex items-start justify-between">
            <div className="h-3 w-24 rounded bg-dark-800" />
            <div className="h-8 w-8 rounded-lg bg-dark-800" />
          </div>
          <div className="h-7 w-16 rounded bg-dark-800 mt-3" />
          <div className="h-3 w-20 rounded bg-dark-800 mt-2" />
        </div>
      ))}
    </div>
  )
}

interface KpiItem {
  title: string
  value: number
  change: number | null | undefined
  direction: Direction
  icon: typeof Activity
  iconBg: string
  iconColor: string
}

export default function KpiCards() {
  const [kpi, setKpi] = useState<DashboardKpi | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchKpi()
      .then(setKpi)
      .catch((err) => console.error("kpi", err))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <KpiSkeleton />

  const items: KpiItem[] = [
    {
      title: "EVENTOS (24h)",
      value: kpi?.totalEvents24h ?? 0,
      change: kpi?.eventsChangePercent,
      direction: null,
      icon: Activity,
      iconBg: "bg-blue-500/10",
      iconColor: "text-blue-400",
    },
    {
      title: "ALERTAS ATIVOS",
      value: kpi?.activeAlerts ?? 0,
      change: kpi?.alertsChangePercent,
      direction: "bad",
      icon: AlertTriangle,
      iconBg: "bg-orange-500/10",
      iconColor: "text-orange-400",
    },
    {
      title: "USUÁRIOS ATIVOS",
      value: kpi?.activeUsers ?? 0,
      change: kpi?.usersChangePercent,
      direction: "good",
      icon: Users,
      iconBg: "bg-emerald-500/10",
      iconColor: "text-emerald-400",
    },
    {
      title: "TENANTS",
      value: kpi?.totalTenants ?? 0,
      change: undefined,
      direction: null,
      icon: Box,
      iconBg: "bg-violet-500/10",
      iconColor: "text-violet-400",
    },
  ]

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {items.map((item) => {
        const trend = describeTrend(item.change, item.value, item.direction)
        return (
          <div
            key={item.title}
            className="rounded-xl bg-dark-900 border border-dark-800 p-4 transition-all duration-200 hover:border-dark-700 hover:shadow-lg hover:shadow-black/20 group"
          >
            <div className="flex items-start justify-between">
              <p className="text-[11px] font-semibold text-slate-400 tracking-wider">{item.title}</p>
              <div className={`p-2 rounded-lg ${item.iconBg} transition-transform duration-200 group-hover:scale-110`}>
                <item.icon className={`w-4 h-4 ${item.iconColor}`} aria-hidden="true" />
              </div>
            </div>
            <p className="text-2xl font-bold text-white mt-2 tabular-nums">
              {item.value.toLocaleString("pt-BR")}
            </p>
            <p className={`text-xs font-medium mt-1 ${trend.className}`}>
              {trend.text}
              {item.change === null && (
                <span className="text-slate-500"> — sem base de comparação</span>
              )}
              {item.change !== null && item.change !== undefined && item.change !== 0 && (
                <span className="text-slate-500"> vs. 24h anteriores</span>
              )}
            </p>
          </div>
        )
      })}
    </div>
  )
}
