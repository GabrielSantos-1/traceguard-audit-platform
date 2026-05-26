import { Shield } from "lucide-react"

const badges = [
  { label: "OWASP TOP 10", color: "text-green-400 border-green-500/30" },
  { label: "LGPD COMPLIANT", color: "text-blue-400 border-blue-500/30" },
  { label: "ISO 27001 READY", color: "text-purple-400 border-purple-500/30" },
  { label: "SOC 2 READY", color: "text-teal-400 border-teal-500/30" },
]

export default function Footer() {
  return (
    <footer className="border-t border-dark-800 mt-6">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-5 px-1">
        <div className="flex items-center gap-3 flex-wrap justify-center">
          {badges.map((badge) => (
            <span
              key={badge.label}
              className={`text-[10px] font-bold px-2.5 py-1 rounded-full border ${badge.color} bg-dark-900/50`}
            >
              {badge.label}
            </span>
          ))}
        </div>
        <p className="text-[11px] text-dark-700 flex items-center gap-1.5">
          <Shield className="w-3 h-3 text-teal-500" />
          Segurança não é um recurso. É o nosso fundamento.
        </p>
      </div>
    </footer>
  )
}
