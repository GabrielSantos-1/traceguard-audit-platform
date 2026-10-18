import { useEffect, useState } from "react"
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"
import { fetchTimeline, type TimelineItem } from "../services/api"

/** "14:00" -> "14h": shorter labels fit at the edges of the axis instead of being clipped. */
function shortHour(value: string): string {
  return value.endsWith(":00") ? `${value.slice(0, 2)}h` : value
}

export default function TimelineChart() {
  const [data, setData] = useState<TimelineItem[]>([])

  useEffect(() => {
    fetchTimeline().then(setData).catch((err) => console.error("timeline", err))
  }, [])

  // With every bucket at zero a domain of [0, 0] renders as a broken axis; keep one step of headroom.
  const maxCount = Math.max(1, ...data.map((d) => d.count))

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-200">Eventos ao Longo do Tempo</h3>
        <span className="text-[11px] font-medium text-slate-400 bg-dark-800 border border-dark-700 rounded-lg px-2.5 py-1">
          Últimas 24 horas
        </span>
      </div>
      <div className="h-52">
        {data.length === 0 ? (
          <div className="h-full flex items-center justify-center">
            <p className="text-xs text-slate-400">Nenhum evento no período</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 5, right: 16, left: -18, bottom: 6 }}>
              <defs>
                <linearGradient id="colorEvents" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
              <XAxis
                dataKey="hour"
                tick={{ fill: "#94A3B8", fontSize: 10 }}
                tickLine={false}
                tickMargin={6}
                axisLine={{ stroke: "#334155" }}
                interval="preserveStartEnd"
                minTickGap={28}
                tickFormatter={shortHour}
              />
              <YAxis
                tick={{ fill: "#94A3B8", fontSize: 10 }}
                tickLine={false}
                axisLine={false}
                allowDecimals={false}
                domain={[0, maxCount]}
                width={32}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "#1E293B",
                  border: "1px solid #334155",
                  borderRadius: "8px",
                  fontSize: "12px",
                  color: "#F1F5F9",
                }}
                labelFormatter={(label) => `Horário: ${label}`}
                formatter={(value) => [String(value), "Eventos"]}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#8B5CF6"
                strokeWidth={2}
                fill="url(#colorEvents)"
                dot={{ fill: "#8B5CF6", r: 2, strokeWidth: 0 }}
                activeDot={{ fill: "#8B5CF6", r: 5, strokeWidth: 2, stroke: "#1E293B" }}
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
