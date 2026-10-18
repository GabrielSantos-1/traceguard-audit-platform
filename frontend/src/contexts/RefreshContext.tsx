import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react"

/**
 * A security dashboard that loads once and never updates is a snapshot the user will trust long
 * after it went stale. This provider drives a refresh cycle: panels depend on `signal` to re-fetch,
 * and they report back through `markUpdated` so the timestamp reflects real data, not a timer.
 */
const AUTO_REFRESH_MS = 60000

interface RefreshContextType {
  /** Bumps on every cycle; panels include it in their effect deps to re-fetch. */
  signal: number
  lastUpdated: Date | null
  refreshing: boolean
  refresh: () => void
  markUpdated: () => void
  autoRefresh: boolean
  toggleAutoRefresh: () => void
}

const RefreshContext = createContext<RefreshContextType | null>(null)

export function RefreshProvider({ children }: { children: ReactNode }) {
  const [signal, setSignal] = useState(0)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const releaseTimer = useRef<number | null>(null)

  const refresh = useCallback(() => {
    setSignal((n) => n + 1)
    setRefreshing(true)
    // The panels fetch independently; this only releases the button so it cannot hang forever.
    if (releaseTimer.current) window.clearTimeout(releaseTimer.current)
    releaseTimer.current = window.setTimeout(() => setRefreshing(false), 1200)
  }, [])

  const markUpdated = useCallback(() => setLastUpdated(new Date()), [])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = window.setInterval(refresh, AUTO_REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [autoRefresh, refresh])

  useEffect(() => {
    return () => {
      if (releaseTimer.current) window.clearTimeout(releaseTimer.current)
    }
  }, [])

  const toggleAutoRefresh = useCallback(() => setAutoRefresh((on) => !on), [])

  return (
    <RefreshContext.Provider
      value={{ signal, lastUpdated, refreshing, refresh, markUpdated, autoRefresh, toggleAutoRefresh }}
    >
      {children}
    </RefreshContext.Provider>
  )
}

export function useRefresh() {
  const ctx = useContext(RefreshContext)
  if (!ctx) throw new Error("useRefresh must be inside RefreshProvider")
  return ctx
}
