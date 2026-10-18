import { useEffect, useState } from "react"
import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts"
import { fetchSeverityDistribution, type SeverityItem } from "../services/api"

/**
 * Severity colours have to be told apart at a glance. The previous set used #F59E0B (amber) for HIGH
 * and #EAB308 (yellow) for MEDIUM — two neighbouring hues on the same slice of the wheel, which read as
 * one colour in a small chart. These keep the red-to-green scale but step the hue and luminance apart.
 */
const colorMap: Record<string, string> = {
  CRITICAL: "#EF4444",
  HIGH: "#F97316",
  MEDIUM: "#FACC15",
  LOW: "#22C55E",
}

const labelMap: Record<string, string> = {
  CRITICAL: "Crítico",
  HIGH: "Alto",
  MEDIUM: "Médio",
  LOW: "Baixo",
}

const UNKNOWN_COLOR = "#64748B"

export default function SeverityChart() {
  const [data, setData] = useState<SeverityItem[]>([])
  const [total, setTotal] = useState(0)

  useEffect(() => {
    fetchSeverityDistribution()
      .then((items) => {
        setData(items)
        setTotal(items.reduce((acc, i) => acc + i.count, 0))
      })
      .catch((err) => console.error("severity", err))
  }, [])

  if (data.length === 0) {
    return (
      <div className="rounded-xl bg-dark-900 border border-dark-800 p-5">
        <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos por Severidade (24h)</h3>
        <div className="h-44 flex items-center justify-center">
          <p className="text-xs text-slate-400">Nenhum evento nas últimas 24h</p>
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos por Severidade (24h)</h3>
      <div className="flex flex-col md:flex-row items-center gap-4">
        <div
          className="relative w-40 h-40"
          role="img"
          aria-label={`Distribuição por severidade: ${data
            .map((d) => `${labelMap[d.severity] ?? d.severity} ${d.count}`)
            .join(", ")}`}
        >
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={45}
                outerRadius={70}
                paddingAngle={3}
                dataKey="count"
                nameKey="severity"
                stroke="none"
              >
                {data.map((entry) => (
                  <Cell key={entry.severity} fill={colorMap[entry.severity] || UNKNOWN_COLOR} />
                ))}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <p className="text-lg font-bold text-white tabular-nums">
                {total.toLocaleString("pt-BR")}
              </p>
              <p className="text-[10px] text-slate-400">total</p>
            </div>
          </div>
        </div>
        <div className="flex-1 space-y-2 w-full">
          {data.map((item) => (
            <div key={item.severity} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: colorMap[item.severity] || UNKNOWN_COLOR }}
                  aria-hidden="true"
                />
                <span className="text-slate-300 font-medium">
                  {labelMap[item.severity] || item.severity}
                </span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-gray-300 font-medium tabular-nums">
                  {item.count.toLocaleString("pt-BR")}
                </span>
                <span className="text-slate-400 w-12 text-right tabular-nums">
                  {item.percentage.toFixed(1).replace(".", ",")}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
