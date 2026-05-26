import { useState, useEffect } from "react"
import { User, Shield, Bell, Key, FileText, Users, CheckCircle, XCircle, Plus, Trash2, Eye, EyeOff } from "lucide-react"
import { useAuth } from "../contexts/AuthContext"
import { changePassword, fetchApiKeys, createApiKey, revokeApiKey, type ApiKeyItem } from "../services/api"

type ConfigTab = "perfil" | "seguranca" | "notificacoes" | "apikeys" | "auditoria" | "times"

const tabs: { id: ConfigTab; label: string; icon: typeof User }[] = [
  { id: "perfil", label: "Perfil da Conta", icon: User },
  { id: "seguranca", label: "Segurança", icon: Shield },
  { id: "notificacoes", label: "Notificações", icon: Bell },
  { id: "apikeys", label: "API Keys", icon: Key },
  { id: "auditoria", label: "Auditoria", icon: FileText },
  { id: "times", label: "Times", icon: Users },
]

export default function ConfiguracoesPage() {
  const [tab, setTab] = useState<ConfigTab>("perfil")
  const { user } = useAuth()

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Configurações</h2>
        <p className="text-xs text-dark-700 mt-1">Personalize a plataforma de acordo com suas necessidades</p>
      </div>

      <div className="flex gap-1 flex-wrap">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              tab === t.id ? "bg-teal-500/10 text-teal-400" : "text-dark-700 hover:text-gray-300 hover:bg-dark-800"
            }`}
          >
            <t.icon className="w-3.5 h-3.5" />
            {t.label}
          </button>
        ))}
      </div>

      {tab === "perfil" && <PerfilTab user={user} />}
      {tab === "seguranca" && <SegurancaTab />}
      {tab === "notificacoes" && <NotificacoesTab />}
      {tab === "apikeys" && <ApiKeysTab />}
      {tab === "auditoria" && <AuditoriaTab />}
      {tab === "times" && <TimesTab />}
    </div>
  )
}

function PerfilTab({ user }: { user: { id: string; tenantId: string; role: string } | null }) {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-200">Informações da Conta</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {[
          { label: "ID do Usuário", value: user?.id || "—" },
          { label: "Tenant ID", value: user?.tenantId || "—" },
          { label: "Papel (RBAC)", value: user?.role || "—" },
        ].map((f) => (
          <div key={f.label}>
            <p className="text-[11px] font-medium text-dark-700 mb-1">{f.label}</p>
            <p className="text-xs text-gray-300 font-mono bg-dark-800 rounded-lg px-3 py-2 truncate">{f.value}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

function SegurancaTab() {
  const [currentPassword, setCurrent] = useState("")
  const [newPassword, setNew] = useState("")
  const [confirm, setConfirm] = useState("")
  const [show, setShow] = useState(false)
  const [msg, setMsg] = useState<{ type: "ok" | "err"; text: string } | null>(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setMsg(null)
    if (newPassword !== confirm) { setMsg({ type: "err", text: "Senhas não conferem" }); return }
    if (newPassword.length < 8) { setMsg({ type: "err", text: "Mínimo 8 caracteres" }); return }
    setBusy(true)
    try {
      await changePassword(currentPassword, newPassword)
      setMsg({ type: "ok", text: "Senha alterada com sucesso" })
      setCurrent(""); setNew(""); setConfirm("")
    } catch (err: unknown) {
      setMsg({ type: "err", text: err instanceof Error ? err.message : "Erro ao alterar senha" })
    } finally { setBusy(false) }
  }

  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-200">Alterar Senha</h3>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-dark-700">
        <div className="flex items-center gap-2"><Shield className="w-3.5 h-3.5 text-green-400" /> Mínimo 8 caracteres</div>
        <div className="flex items-center gap-2"><Shield className="w-3.5 h-3.5 text-green-400" /> BCrypt hash</div>
        <div className="flex items-center gap-2"><Shield className="w-3.5 h-3.5 text-green-400" /> JWT com refresh token rotation</div>
        <div className="flex items-center gap-2"><Shield className="w-3.5 h-3.5 text-green-400" /> Rate limiting por endpoint</div>
      </div>

      {msg && (
        <div className={`text-xs rounded-lg px-3 py-2 ${msg.type === "ok" ? "bg-green-500/10 text-green-400" : "bg-red-500/10 text-red-400"}`}>
          {msg.text}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3 max-w-sm">
        <PasswordField label="Senha Atual" value={currentPassword} onChange={setCurrent} show={show} setShow={setShow} />
        <PasswordField label="Nova Senha" value={newPassword} onChange={setNew} show={show} setShow={setShow} />
        <PasswordField label="Confirmar Nova Senha" value={confirm} onChange={setConfirm} show={show} setShow={setShow} />
        <button type="submit" disabled={busy}
          className="px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-600 disabled:opacity-50 text-xs font-semibold text-white transition-colors">
          {busy ? "Salvando..." : "Salvar Senha"}
        </button>
      </form>
    </div>
  )
}

function PasswordField({ label, value, onChange, show, setShow }: {
  label: string; value: string; onChange: (v: string) => void; show: boolean; setShow: (v: boolean) => void
}) {
  return (
    <div>
      <label className="text-[11px] font-medium text-dark-700 block mb-1">{label}</label>
      <div className="relative">
        <input type={show ? "text" : "password"} value={value} onChange={e => onChange(e.target.value)}
          className="w-full px-3 py-2 pr-9 rounded-lg bg-dark-800 border border-dark-700 text-sm text-gray-200 focus:outline-none focus:border-teal-500/50 transition-colors" />
        <button type="button" onClick={() => setShow(!show)}
          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-dark-700 hover:text-gray-300">
          {show ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
        </button>
      </div>
    </div>
  )
}

function NotificacoesTab() {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-200">Canais de Notificação</h3>
      <p className="text-xs text-dark-700">Configure os canais de alerta na página de Integrações.</p>
      <div className="flex gap-3">
        {["Email", "Slack", "Webhook"].map((c) => (
          <div key={c} className="flex items-center gap-2 px-3 py-2 rounded-lg bg-dark-800 border border-dark-700">
            <span className="text-xs text-gray-300">{c}</span>
            <span className="text-[10px] text-dark-700">Indisponível</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function ApiKeysTab() {
  const [keys, setKeys] = useState<ApiKeyItem[]>([])
  const [loading, setLoading] = useState(true)
  const [showNew, setShowNew] = useState(false)
  const [name, setName] = useState("")
  const [scopes, setScopes] = useState<string[]>(["AUDIT_READ"])
  const [createdKey, setCreatedKey] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    fetchApiKeys().then(setKeys).catch(console.error).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res = await createApiKey(name, scopes)
      setCreatedKey(res.apiKey)
      setName(""); setScopes(["AUDIT_READ"]); setShowNew(false)
      load()
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Erro")
    }
  }

  const handleRevoke = async (id: string) => {
    if (!confirm("Revogar esta API Key? A ação é irreversível.")) return
    try { await revokeApiKey(id); load() }
    catch (err: unknown) { alert(err instanceof Error ? err.message : "Erro") }
  }

  const allScopes = ["AUDIT_READ", "AUDIT_WRITE"]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-200">Chaves de API</h3>
        <button onClick={() => { setShowNew(!showNew); setCreatedKey(null) }}
          className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-teal-500 hover:bg-teal-600 text-xs font-semibold text-white transition-colors">
          <Plus className="w-3 h-3" /> Nova Chave
        </button>
      </div>

      {createdKey && (
        <div className="rounded-xl bg-yellow-500/10 border border-yellow-500/30 p-4 space-y-2">
          <p className="text-xs font-semibold text-yellow-400">⚠️ Chave criada — copie agora, não será exibida novamente</p>
          <p className="text-xs font-mono bg-dark-800 rounded-lg px-3 py-2 text-gray-200 break-all select-all">{createdKey}</p>
          <button onClick={() => { navigator.clipboard.writeText(createdKey); setCreatedKey(null) }}
            className="text-[10px] font-medium text-teal-500 hover:text-teal-400">Copiar e fechar</button>
        </div>
      )}

      {showNew && (
        <form onSubmit={handleCreate} className="rounded-xl bg-dark-900 border border-dark-800 p-4 space-y-3">
          <div>
            <label className="text-[11px] font-medium text-dark-700 block mb-1">Nome</label>
            <input value={name} onChange={e => setName(e.target.value)} required
              className="w-full px-3 py-2 rounded-lg bg-dark-800 border border-dark-700 text-sm text-gray-200 focus:outline-none focus:border-teal-500/50" />
          </div>
          <div>
            <label className="text-[11px] font-medium text-dark-700 block mb-1">Scopes</label>
            <div className="flex gap-2">
              {allScopes.map(s => (
                <label key={s} className="flex items-center gap-1.5 cursor-pointer">
                  <input type="checkbox" checked={scopes.includes(s)}
                    onChange={() => setScopes(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s])}
                    className="accent-teal-500" />
                  <span className="text-xs text-gray-300">{s}</span>
                </label>
              ))}
            </div>
          </div>
          <button type="submit" className="px-4 py-1.5 rounded-lg bg-teal-500 hover:bg-teal-600 text-xs font-semibold text-white transition-colors">
            Criar
          </button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-20">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : (
        <div className="space-y-2">
          {keys.map(k => (
            <div key={k.id} className="rounded-xl bg-dark-900 border border-dark-800 p-4 flex items-center justify-between hover:border-dark-700 transition-colors">
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-gray-200">{k.name}</span>
                  {k.revoked ? (
                    <span className="flex items-center gap-1 text-[10px] text-red-400"><XCircle className="w-3 h-3" />Revoked</span>
                  ) : (
                    <span className="flex items-center gap-1 text-[10px] text-green-400"><CheckCircle className="w-3 h-3" />Active</span>
                  )}
                </div>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-[10px] text-dark-700 font-mono">{k.scopes.join(", ")}</span>
                  <span className="text-[10px] text-dark-700">|</span>
                  <span className="text-[10px] text-dark-700">{new Date(k.createdAt).toLocaleDateString("pt-BR")}</span>
                </div>
              </div>
              {!k.revoked && (
                <button onClick={() => handleRevoke(k.id)}
                  className="p-2 rounded-lg hover:bg-red-500/10 text-dark-700 hover:text-red-400 transition-colors">
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          ))}
          {keys.length === 0 && <p className="text-xs text-dark-700 text-center py-6">Nenhuma API Key cadastrada</p>}
        </div>
      )}
    </div>
  )
}

function AuditoriaTab() {
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-200">Configurações de Auditoria</h3>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { label: "Retenção de Eventos", value: "90 dias (configurável)" },
          { label: "Armazenamento", value: "PostgreSQL + Elasticsearch" },
          { label: "Imutabilidade", value: "Audit trail com append-only" },
        ].map((i) => (
          <div key={i.label} className="p-3 rounded-lg bg-dark-800/50">
            <p className="text-[11px] font-medium text-dark-700">{i.label}</p>
            <p className="text-xs text-gray-300 mt-1">{i.value}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

function TimesTab() {
  const roles = [
    { name: "SUPER_ADMIN", desc: "Acesso total", count: 1 },
    { name: "TENANT_ADMIN", desc: "Admin por tenant", count: 0 },
    { name: "AUDITOR", desc: "Consultas e relatórios", count: 0 },
    { name: "OPERATOR", desc: "Operações", count: 0 },
  ]
  return (
    <div className="rounded-xl bg-dark-900 border border-dark-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-200">Papéis RBAC</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {roles.map(r => (
          <div key={r.name} className="p-3 rounded-lg bg-dark-800/50 border border-dark-700">
            <p className="text-xs font-semibold text-gray-200">{r.name}</p>
            <p className="text-[10px] text-dark-700">{r.desc}</p>
            <p className="text-lg font-bold text-white mt-1">{r.count}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
