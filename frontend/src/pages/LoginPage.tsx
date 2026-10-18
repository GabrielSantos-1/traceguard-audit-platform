import { useState } from "react"
import { Shield, Eye, EyeOff, Clock } from "lucide-react"
import { useAuth } from "../contexts/AuthContext"

export default function LoginPage() {
  const { login, sessionNotice, clearSessionNotice } = useAuth()
  const [email, setEmail] = useState("admin@traceguard.local")
  const [password, setPassword] = useState("admin123")
  const [showPass, setShowPass] = useState(false)
  const [error, setError] = useState("")
  const [busy, setBusy] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    clearSessionNotice()
    setBusy(true)
    try {
      await login(email, password)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Erro ao autenticar")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="inline-flex p-3 rounded-2xl bg-teal-500/10 mb-4">
            <Shield className="w-8 h-8 text-teal-500" />
          </div>
          <h1 className="text-xl font-bold text-white tracking-tight">TRACEGUARD</h1>
          <p className="text-xs text-dark-700 mt-1">Audit & Security Platform</p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-xl bg-dark-900 border border-dark-800 p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-200 text-center">Acessar Dashboard</h2>

          {sessionNotice && !error && (
            <div className="text-xs text-amber-300 bg-amber-500/10 border border-amber-500/20 rounded-lg px-3 py-2 flex items-start gap-2">
              <Clock className="w-3.5 h-3.5 mt-0.5 shrink-0" aria-hidden="true" />
              <span>{sessionNotice}</span>
            </div>
          )}

          {error && (
            <div className="text-xs text-severity-critical bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
              {error}
            </div>
          )}

          <div>
            <label className="text-[11px] font-medium text-dark-700 block mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 rounded-lg bg-dark-800 border border-dark-700 text-sm text-gray-200 placeholder-dark-700 focus:outline-none focus:border-teal-500/50 transition-colors"
              placeholder="admin@traceguard.local"
              required
            />
          </div>

          <div>
            <label className="text-[11px] font-medium text-dark-700 block mb-1">Senha</label>
            <div className="relative">
              <input
                type={showPass ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3 py-2 pr-9 rounded-lg bg-dark-800 border border-dark-700 text-sm text-gray-200 placeholder-dark-700 focus:outline-none focus:border-teal-500/50 transition-colors"
                placeholder="••••••••"
                required
              />
              <button type="button" onClick={() => setShowPass(!showPass)}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-dark-700 hover:text-gray-300 transition-colors">
                {showPass ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={busy}
            className="w-full py-2 rounded-lg bg-teal-500 hover:bg-teal-600 disabled:opacity-50 text-sm font-semibold text-white transition-colors"
          >
            {busy ? "Entrando..." : "Entrar"}
          </button>
        </form>

        <p className="text-[10px] text-dark-700 text-center mt-4">
          Ambiente de desenvolvimento — v1.0.0
        </p>
      </div>
    </div>
  )
}
