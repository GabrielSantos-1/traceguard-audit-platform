import { useEffect, useState } from "react"
import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts"
import { fetchSeverityDistribution, type SeverityItem } from "../services/api"

const colorMap: Record<string, string> = {
  CRITICAL: "#EF4444",
  HIGH: "#F59E0B",
  MEDIUM: "#EAB308",
  LOW: "#22C55E",
}

const labelMap: Record<string, string> = {
  CRITICAL: "CRITICAL",
  HIGH: "HIGH",
  MEDIUM: "MEDIUM",
  LOW: "LOW",
}

export default function SeverityChart() {
  const [data, setData] = useState<SeverityItem[]>([])
  const [total, setTotal] = useState(0)

  useEffect(() => {
    fetchSeverityDistribution()
      .then((items) => {
        setData(items)
        setTotal(items.reduce((acc, i) => acc + i.count, 0))
      })
      .catch(console.error)
  }, [])

  if (data.length === 0) {
    return (
      <div className="rounded-xl bg-dark-900 border border-dark-800 p-5">
        <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos por Severidade (24h)</h3>
        <div className="h-40 flex items-center justify-center">
          <p className="text-xs text-dark-700">Nenhum evento nas últimas 24h</p>
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Eventos por Severidade (24h)</h3>
      <div className="flex flex-col md:flex-row items-center gap-4">
        <div className="relative w-40 h-40">
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
                  <Cell key={entry.severity} fill={colorMap[entry.severity] || "#475569"} />
                ))}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <p className="text-lg font-bold text-white">{total.toLocaleString("pt-BR")}</p>
              <p className="text-[10px] text-dark-700">total</p>
            </div>
          </div>
        </div>
        <div className="flex-1 space-y-2 w-full">
          {data.map((item) => (
            <div key={item.severity} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: colorMap[item.severity] || "#475569" }} />
                <span className="text-dark-700 font-medium">{labelMap[item.severity] || item.severity}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-gray-300 font-medium">{item.count.toLocaleString("pt-BR")}</span>
                <span className="text-dark-700 w-10 text-right">{item.percentage.toFixed(1).replace(".", ",")}%</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
