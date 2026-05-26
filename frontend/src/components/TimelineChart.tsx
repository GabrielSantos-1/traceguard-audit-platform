import { useEffect, useState } from "react"
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"
import { ChevronDown } from "lucide-react"
import { fetchTimeline, type TimelineItem } from "../services/api"

export default function TimelineChart() {
  const [data, setData] = useState<TimelineItem[]>([])
  const [period, setPeriod] = useState("Últimas 24 horas")

  useEffect(() => {
    fetchTimeline().then(setData).catch(console.error)
  }, [])

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-200">Eventos ao Longo do Tempo</h3>
        <div className="relative">
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="appearance-none bg-dark-800 border border-dark-700 rounded-lg px-3 py-1.5 pr-8 text-xs text-gray-300 font-medium focus:outline-none focus:border-teal-500/50 cursor-pointer"
          >
            <option>Últimas 24 horas</option>
          </select>
          <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-dark-700 pointer-events-none" />
        </div>
      </div>
      <div className="h-52">
        {data.length === 0 ? (
          <div className="h-full flex items-center justify-center">
            <p className="text-xs text-dark-700">Nenhum evento no período</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="colorEvents" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
              <XAxis
                dataKey="hour"
                tick={{ fill: "#475569", fontSize: 10 }}
                tickLine={false}
                axisLine={{ stroke: "#334155" }}
                interval={3}
              />
              <YAxis
                tick={{ fill: "#475569", fontSize: 10 }}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "#1E293B",
                  border: "1px solid #334155",
                  borderRadius: "8px",
                  fontSize: "12px",
                  color: "#F1F5F9",
                }}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#8B5CF6"
                strokeWidth={2}
                fill="url(#colorEvents)"
                dot={{ fill: "#8B5CF6", r: 3, strokeWidth: 0 }}
                activeDot={{ fill: "#8B5CF6", r: 5, strokeWidth: 2, stroke: "#1E293B" }}
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
