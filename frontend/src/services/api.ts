const API_BASE = ""

const ACCESS_KEY = "tg_token"
const REFRESH_KEY = "tg_refresh"
const REQUEST_TIMEOUT_MS = 15000

export interface ApiMeta {
  correlationId: string
  page?: number
  size?: number
  totalElements?: number
  totalPages?: number
  hasNext?: boolean
  hasPrevious?: boolean
}

interface ApiResponse<T> {
  success: boolean
  data: T
  meta?: ApiMeta
  error?: { code?: string; message?: string }
}

export class ApiError extends Error {
  code: string
  status: number

  constructor(code: string, message: string, status: number) {
    super(message)
    this.code = code
    this.status = status
  }
}

export function getToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

/**
 * The backend rotates refresh tokens on every use, so both values have to be replaced together —
 * keeping the old refresh token would make the next renewal fail.
 */
export function setTokens(accessToken: string, refreshToken?: string | null) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

// --- session expiry notification -------------------------------------------------------------
// The UI has to know the difference between "the API returned no rows" and "your session ended".

type SessionExpiredHandler = (reason: string) => void
const sessionExpiredHandlers = new Set<SessionExpiredHandler>()

export function onSessionExpired(handler: SessionExpiredHandler): () => void {
  sessionExpiredHandlers.add(handler)
  return () => sessionExpiredHandlers.delete(handler)
}

function expireSession(reason: string) {
  clearTokens()
  sessionExpiredHandlers.forEach((handler) => handler(reason))
}

// --- token renewal ---------------------------------------------------------------------------

let refreshInFlight: Promise<boolean> | null = null

/**
 * Renews the access token. Every panel on the dashboard can hit 401 in the same instant, so the
 * renewal is single-flight: concurrent callers await the same request instead of racing each other
 * and burning through the rotating refresh token.
 */
function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = doRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function doRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return false
    const json = await res.json()
    const data = json?.data
    if (!data?.accessToken) return false
    setTokens(data.accessToken, data.refreshToken)
    return true
  } catch {
    return false
  }
}

/** Reads a body that is not guaranteed to be JSON (a proxy error page, an empty 500...). */
async function readJson(res: Response): Promise<any> {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  retryOn401 = true,
): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }
  const token = getToken()
  if (token) headers["Authorization"] = `Bearer ${token}`

  // Without a timeout a hung request left the panel spinning forever.
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)

  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })

    const json = await readJson(res)

    // The access token expires mid-session: renew it once and replay the request instead of
    // showing the user a dashboard that is silently empty.
    if (res.status === 401 && retryOn401 && !path.startsWith("/api/v1/auth/")) {
      const renewed = await refreshSession()
      if (renewed) {
        return request<T>(method, path, body, false)
      }
      expireSession("Sua sessão expirou. Entre novamente.")
      throw new ApiError("SESSION_EXPIRED", "Sessão expirada", 401)
    }

    if (!res.ok) {
      const msg = json?.error?.message || json?.message || `Erro HTTP ${res.status}`
      const code = json?.error?.code || "ERROR"
      throw new ApiError(code, msg, res.status)
    }

    return (json ?? { success: true, data: undefined as T }) as ApiResponse<T>
  } catch (err) {
    if (err instanceof ApiError) throw err
    if ((err as Error)?.name === "AbortError") {
      throw new ApiError("TIMEOUT", "O servidor demorou demais para responder", 0)
    }
    throw new ApiError("NETWORK", "Não foi possível falar com o servidor", 0)
  } finally {
    clearTimeout(timer)
  }
}

/** Raw request for the endpoints that answer a bare value instead of the {success, data} envelope. */
async function requestRaw(path: string, retryOn401 = true): Promise<Response> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  try {
    const token = getToken()
    const res = await fetch(`${API_BASE}${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      signal: controller.signal,
    })
    if (res.status === 401 && retryOn401) {
      const renewed = await refreshSession()
      if (renewed) {
        clearTimeout(timer)
        return requestRaw(path, false)
      }
      expireSession("Sua sessão expirou. Entre novamente.")
    }
    return res
  } finally {
    clearTimeout(timer)
  }
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
  /** null when the previous window was empty: there is no baseline to compute a change from. */
  eventsChangePercent: number | null
  alertsChangePercent: number | null
  usersChangePercent: number | null
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
  // Keep the refresh token as well: without it the session cannot be renewed and dies at the first 401.
  setTokens(res.data.accessToken, res.data.refreshToken)
  return res.data
}

/**
 * Ends the session on the server too. The backend revokes the refresh token, so a copy of it cannot
 * be replayed after the user logs out.
 */
export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearTokens()
    return
  }
  try {
    await api.post("/api/v1/auth/logout", { refreshToken })
  } catch (err) {
    // The local session has to end even when the server cannot be reached.
    console.error("logout", err)
  } finally {
    clearTokens()
  }
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

/**
 * Active alert count for the header badge.
 *
 * This endpoint answers a bare number instead of the usual {success, data, meta} envelope, so it
 * cannot go through the shared request helper.
 */
export async function fetchActiveAlertCount(): Promise<number> {
  const res = await requestRaw("/api/v1/admin/detection/alerts/count")
  if (!res.ok) throw new ApiError("ERROR", `Erro HTTP ${res.status}`, res.status)
  return Number(await res.text()) || 0
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
