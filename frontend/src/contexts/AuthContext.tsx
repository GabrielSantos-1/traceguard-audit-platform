import { createContext, useContext, useState, useEffect, type ReactNode } from "react"
import { getToken, clearToken, login as apiLogin } from "../services/api"

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
  logout: () => void
  loading: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getToken())
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]))
        setUser({
          id: payload.sub,
          tenantId: payload.tenantId,
          role: payload.role,
        })
      } catch {
        clearToken()
        setToken(null)
      }
    }
    setLoading(false)
  }, [token])

  const login = async (email: string, password: string) => {
    const data = await apiLogin(email, password)
    setToken(data.accessToken)
    setUser(data.user)
  }

  const logout = () => {
    clearToken()
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated: !!token, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be inside AuthProvider")
  return ctx
}
