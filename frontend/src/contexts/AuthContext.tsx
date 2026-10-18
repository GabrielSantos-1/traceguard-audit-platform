import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react"
import {
  clearTokens,
  getToken,
  login as apiLogin,
  logout as apiLogout,
  onSessionExpired,
} from "../services/api"

interface User {
  id: string
  tenantId: string
  role: string
}

interface AuthContextType {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  loading: boolean
  /** Set when the session ended on the API side, so the login screen can explain why. */
  sessionNotice: string | null
  clearSessionNotice: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

/** Reads the identity out of the signed token instead of trusting a value kept in the UI. */
function decodeUser(token: string): User | null {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]))
    if (!payload?.sub) return null
    return { id: payload.sub, tenantId: payload.tenantId, role: payload.role }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getToken())
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [sessionNotice, setSessionNotice] = useState<string | null>(null)

  // The transport layer decides when a session is over (expired or revoked token); the context
  // follows it so the UI clears instead of staying as a shell full of empty panels.
  useEffect(() => {
    return onSessionExpired((reason) => {
      setToken(null)
      setUser(null)
      setSessionNotice(reason)
    })
  }, [])

  useEffect(() => {
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }
    const decoded = decodeUser(token)
    if (!decoded) {
      // An unreadable token cannot authorise anything: drop it instead of rendering a broken shell.
      clearTokens()
      setToken(null)
      setUser(null)
    } else {
      setUser(decoded)
    }
    setLoading(false)
  }, [token])

  const login = async (email: string, password: string) => {
    const data = await apiLogin(email, password)
    setToken(data.accessToken)
    setUser(data.user)
    setSessionNotice(null)
  }

  const logout = useCallback(async () => {
    await apiLogout()
    setToken(null)
    setUser(null)
    setSessionNotice(null)
  }, [])

  const clearSessionNotice = useCallback(() => setSessionNotice(null), [])

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isAuthenticated: !!token,
        login,
        logout,
        loading,
        sessionNotice,
        clearSessionNotice,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be inside AuthProvider")
  return ctx
}
