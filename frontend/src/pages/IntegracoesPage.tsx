import { useEffect, useState } from "react"
import { Globe, Mail, MessageSquare, Database, Bell, CheckCircle, XCircle } from "lucide-react"
import { fetchIntegrations, saveIntegration, deleteIntegration, type IntegrationItem } from "../services/api"

const providerMeta: Record<string, { name: string; desc: string; icon: typeof Globe; color: string; fields: { key: string; label: string; placeholder: string }[] }> = {
  slack: {
    name: "Slack", desc: "Notificações de alertas no Slack",
    icon: MessageSquare, color: "text-purple-400",
    fields: [
      { key: "webhookUrl", label: "Webhook URL", placeholder: "https://hooks.slack.com/services/..." },
      { key: "channel", label: "Canal", placeholder: "#alertas" },
    ],
  },
  email: {
    name: "Email SMTP", desc: "Relatórios periódicos por email",
    icon: Mail, color: "text-blue-400",
    fields: [
      { key: "host", label: "SMTP Host", placeholder: "smtp.gmail.com" },
      { key: "port", label: "Porta", placeholder: "587" },
      { key: "username", label: "Usuário", placeholder: "user@email.com" },
      { key: "password", label: "Senha", placeholder: "••••••••" },
      { key: "to", label: "Destinatário", placeholder: "admin@traceguard.local" },
    ],
  },
  webhook: {
    name: "Webhook", desc: "Integração com sistemas externos via HTTP",
    icon: Globe, color: "text-green-400",
    fields: [
      { key: "url", label: "URL do Webhook", placeholder: "https://meu-sistema.com/webhook" },
      { key: "secret", label: "Secret (opcional)", placeholder: "hmac-secret" },
    ],
  },
  elasticsearch: {
    name: "Elasticsearch", desc: "Exportação de logs para busca e analytics",
    icon: Database, color: "text-orange-400",
    fields: [
      { key: "host", label: "Host", placeholder: "http://localhost:9200" },
      { key: "index", label: "Índice", placeholder: "traceguard-audit" },
      { key: "apiKey", label: "API Key", placeholder: "••••••••" },
    ],
  },
  pagerduty: {
    name: "PagerDuty", desc: "Alertas críticos via PagerDuty",
    icon: Bell, color: "text-red-400",
    fields: [
      { key: "routingKey", label: "Routing Key", placeholder: "..." },
      { key: "severity", label: "Severidade mínima", placeholder: "critical" },
    ],
  },
}

const providers = Object.keys(providerMeta)

export default function IntegracoesPage() {
  const [integrations, setIntegrations] = useState<IntegrationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    fetchIntegrations().then(setIntegrations).catch(console.error).finally(() => setLoading(false))
  }

  useEffect(load, [])

  const getStatus = (provider: string): IntegrationItem | undefined =>
    integrations.find(i => i.provider === provider)

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-bold text-white">Integrações</h2>
        <p className="text-xs text-dark-700 mt-1">Conecte o TraceGuard com seu ecossistema de segurança</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {providers.map((p) => {
            const meta = providerMeta[p]
            const status = getStatus(p)
            const Icon = meta.icon

            return (
              <div
                key={p}
                className="rounded-xl bg-dark-900 border border-dark-800 p-5 hover:border-dark-700 transition-all cursor-pointer group"
                onClick={() => setEditing(editing === p ? null : p)}
              >
                <div className="flex items-start justify-between mb-3">
                  <div className={`p-2 rounded-lg bg-dark-800 group-hover:scale-110 transition-transform`}>
                    <Icon className={`w-5 h-5 ${meta.color}`} />
                  </div>
                  {status ? (
                    status.enabled
                      ? <span className="flex items-center gap-1 text-[10px] text-green-400 font-medium"><CheckCircle className="w-3 h-3" /> Conectado</span>
                      : <span className="flex items-center gap-1 text-[10px] text-yellow-400 font-medium"><XCircle className="w-3 h-3" /> Desativado</span>
                  ) : (
                    <span className="text-[10px] text-dark-700 font-medium">Não configurado</span>
                  )}
                </div>
                <h3 className="text-sm font-semibold text-gray-200">{meta.name}</h3>
                <p className="text-xs text-dark-700 mt-1">{meta.desc}</p>

                {editing === p && (
                  <IntegrationForm
                    provider={p}
                    meta={meta}
                    existing={status}
                    onSave={() => { setEditing(null); load() }}
                    onDelete={() => { setEditing(null); load() }}
                  />
                )}

                {editing !== p && (
                  <span className="inline-block mt-3 text-[10px] font-medium text-teal-500 opacity-0 group-hover:opacity-100 transition-opacity">
                    {status ? "Reconfigurar →" : "Configurar →"}
                  </span>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function IntegrationForm({ provider, meta, existing, onSave, onDelete }: {
  provider: string
  meta: typeof providerMeta[string]
  existing: IntegrationItem | undefined
  onSave: () => void
  onDelete: () => void
}) {
  const [config, setConfig] = useState<Record<string, string>>(() => {
    if (existing?.configJson) {
      try { return JSON.parse(existing.configJson) } catch { return {} }
    }
    return {}
  })
  const [enabled, setEnabled] = useState(existing?.enabled ?? false)
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

  const handleSave = async () => {
    setBusy(true); setMsg(null)
    try {
      await saveIntegration(provider, meta.name, JSON.stringify(config), enabled)
      setMsg("Configuração salva com sucesso")
      setTimeout(onSave, 1000)
    } catch (err: unknown) {
      setMsg(err instanceof Error ? err.message : "Erro ao salvar")
    } finally { setBusy(false) }
  }

  const handleDelete = async () => {
    if (!confirm(`Remover integração ${meta.name}?`)) return
    setBusy(true)
    try {
      await deleteIntegration(provider)
      onDelete()
    } catch (err: unknown) {
      setMsg(err instanceof Error ? err.message : "Erro")
    } finally { setBusy(false) }
  }

  const handleTest = async () => {
    setMsg("Teste de conexão não disponível nesta versão")
  }

  return (
    <div className="mt-4 pt-4 border-t border-dark-800 space-y-3" onClick={e => e.stopPropagation()}>
      {meta.fields.map((f) => (
        <div key={f.key}>
          <label className="text-[10px] font-medium text-dark-700 block mb-0.5">{f.label}</label>
          <input
            type={f.key.includes("password") || f.key.includes("secret") || f.key.includes("apiKey") || f.key === "routingKey" ? "password" : "text"}
            value={config[f.key] || ""}
            onChange={e => setConfig({ ...config, [f.key]: e.target.value })}
            placeholder={f.placeholder}
            className="w-full px-2.5 py-1.5 rounded-lg bg-dark-800 border border-dark-700 text-xs text-gray-200 placeholder-dark-700 focus:outline-none focus:border-teal-500/50"
          />
        </div>
      ))}

      <label className="flex items-center gap-2 cursor-pointer">
        <input type="checkbox" checked={enabled} onChange={e => setEnabled(e.target.checked)} className="accent-teal-500" />
        <span className="text-xs text-gray-300">Habilitado</span>
      </label>

      {msg && <p className="text-[10px] text-teal-400">{msg}</p>}

      <div className="flex gap-2">
        <button onClick={handleSave} disabled={busy}
          className="px-3 py-1.5 rounded-lg bg-teal-500 hover:bg-teal-600 disabled:opacity-50 text-[10px] font-semibold text-white transition-colors">
          {busy ? "Salvando..." : "Salvar"}
        </button>
        <button onClick={handleTest} disabled={busy}
          className="px-3 py-1.5 rounded-lg bg-dark-800 hover:bg-dark-700 text-[10px] font-medium text-gray-300 transition-colors">
          Testar
        </button>
        {existing && (
          <button onClick={handleDelete} disabled={busy}
            className="px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-[10px] font-medium text-red-400 transition-colors ml-auto">
            Remover
          </button>
        )}
      </div>
    </div>
  )
}
