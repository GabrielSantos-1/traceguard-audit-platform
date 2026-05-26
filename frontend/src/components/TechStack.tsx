import { Coffee, Globe, Database, Search, Radio, Box } from "lucide-react"

const techs = [
  { icon: Coffee, label: "Java 21 + Spring Boot", color: "text-red-400" },
  { icon: Globe, label: "React 18 + TypeScript", color: "text-blue-400" },
  { icon: Database, label: "PostgreSQL", color: "text-blue-400" },
  { icon: Search, label: "Elasticsearch", color: "text-orange-400" },
  { icon: Radio, label: "RabbitMQ", color: "text-orange-400" },
  { icon: Box, label: "Docker + Kubernetes", color: "text-blue-400" },
]

export default function TechStack() {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 transition-all duration-200 hover:border-dark-700">
      <h3 className="text-sm font-semibold text-gray-200 mb-4">Tecnologias</h3>
      <div className="grid grid-cols-1 gap-1.5">
        {techs.map((tech) => (
          <div key={tech.label} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-dark-800/50 transition-colors">
            <tech.icon className={`w-3.5 h-3.5 ${tech.color} shrink-0`} />
            <span className="text-[11px] text-gray-300">{tech.label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
