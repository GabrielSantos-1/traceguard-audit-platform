const API_BASE = ""

interface ApiResponse<T> {
  success: boolean
  data: T
  meta: {
    correlationId: string
    page?: number
    size?: number
    totalElements?: number
    totalPages?: number
    hasNext?: boolean
    hasPrevious?: boolean
  }
}

class ApiError extends Error {
  code: string
  status: number

  constructor(code: string, message: string, status: number) {
    super(message)
    this.code = code
    this.status = status
  }
}

function getToken(): string | null {
  return localStorage.getItem("tg_token")
}

function setToken(token: string) {
  localStorage.setItem("tg_token", token)
}

function clearToken() {
  localStorage.removeItem("tg_token")
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }
  const token = getToken()
  if (token) headers["Authorization"] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const json = await res.json()

  if (!res.ok) {
    const msg = json?.error?.message || json?.message || "Erro desconhecido"
    const code = json?.error?.code || "ERROR"
    throw new ApiError(code, msg, res.status)
  }

  return json as ApiResponse<T>
}

export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  del: <T>(path: string) => request<T>("DELETE", path),
}

export interface LoginPayload {
  email: string
  password: string
}

export interface LoginData {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  refreshExpiresIn: number
  user: {
    id: string
    tenantId: string
    role: string
  }
}

export interface DashboardKpi {
  totalEvents24h: number
  activeAlerts: number
  activeUsers: number
  totalTenants: number
  eventsChangePercent: number
  alertsChangePercent: number
  usersChangePercent: number
}

export interface SeverityItem {
  severity: string
  count: number
  percentage: number
}

export interface TimelineItem {
  hour: string
  count: number
}

export interface RecentEvent {
  id: string
  action: string
  description: string
  severity: string
  ip: string
  occurredAt: string
}

export interface CriticalAlert {
  id: string
  title: string
  description: string
  severity: string
  ip: string
  occurredAt: string
}

export async function login(email: string, password: string): Promise<LoginData> {
  const res = await api.post<LoginData>("/api/v1/auth/login", { email, password })
  setToken(res.data.accessToken)
  return res.data
}

export async function fetchKpi(): Promise<DashboardKpi> {
  const res = await api.get<DashboardKpi>("/api/v1/admin/dashboard/kpi")
  return res.data
}

export async function fetchSeverityDistribution(): Promise<SeverityItem[]> {
  const res = await api.get<SeverityItem[]>("/api/v1/admin/dashboard/severity-distribution")
  return res.data
}

export async function fetchTimeline(): Promise<TimelineItem[]> {
  const res = await api.get<TimelineItem[]>("/api/v1/admin/dashboard/timeline")
  return res.data
}

export async function fetchRecentEvents(): Promise<RecentEvent[]> {
  const res = await api.get<RecentEvent[]>("/api/v1/admin/dashboard/recent-events")
  return res.data
}

export async function fetchCriticalAlerts(): Promise<CriticalAlert[]> {
  const res = await api.get<CriticalAlert[]>("/api/v1/admin/dashboard/critical-alerts")
  return res.data
}

export interface IntegrationItem {
  provider: string
  name: string
  configJson: string | null
  enabled: boolean
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.patch("/api/v1/admin/account/password", { currentPassword, newPassword })
}

export async function fetchIntegrations(): Promise<IntegrationItem[]> {
  const res = await api.get<IntegrationItem[]>("/api/v1/admin/integrations")
  return res.data
}

export async function saveIntegration(provider: string, name: string, configJson: string, enabled: boolean): Promise<IntegrationItem> {
  const res = await api.put<IntegrationItem>(`/api/v1/admin/integrations/${provider}`, { name, configJson, enabled })
  return res.data
}

export async function deleteIntegration(provider: string): Promise<void> {
  await api.del(`/api/v1/admin/integrations/${provider}`)
}

export async function fetchApiKeys(): Promise<ApiKeyItem[]> {
  const res = await api.get<{ content: ApiKeyItem[] }>("/api/v1/admin/api-keys?size=50")
  return res.data.content || []
}

export interface ApiKeyItem {
  id: string
  name: string
  scopes: string[]
  enabled: boolean
  revoked: boolean
  expiresAt: string | null
  lastUsedAt: string | null
  createdAt: string
}

export async function createApiKey(name: string, scopes: string[]): Promise<{ id: string; apiKey: string }> {
  const res = await api.post<{ id: string; apiKey: string }>("/api/v1/admin/api-keys", {
    tenantId: null, name, scopes,
  })
  return res.data
}

export async function revokeApiKey(id: string): Promise<void> {
  await api.patch(`/api/v1/admin/api-keys/${id}/revoke`)
}

export { getToken, clearToken }
