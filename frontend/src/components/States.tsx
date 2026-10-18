import { AlertCircle, Inbox, RefreshCw } from "lucide-react"

/**
 * Shared panel states.
 *
 * These exist because every panel used to answer `.catch(console.error)`: when the API was down the
 * dashboard showed "no events", which on a security tool reads as "nothing is happening". A failure
 * has to be visible and recoverable.
 */

export function LoadingBlock({ label = "Carregando..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center h-40" role="status" aria-live="polite">
      <div className="flex flex-col items-center gap-2">
        <div className="animate-spin w-5 h-5 border-2 border-teal-500 border-t-transparent rounded-full" />
        <p className="text-[11px] text-slate-400">{label}</p>
      </div>
    </div>
  )
}

export function ErrorBlock({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex items-center justify-center h-40" role="alert">
      <div className="flex flex-col items-center gap-2 text-center px-4">
        <AlertCircle className="w-5 h-5 text-red-400" aria-hidden="true" />
        <p className="text-xs text-slate-300">{message}</p>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="flex items-center gap-1.5 text-[11px] font-medium text-teal-400 hover:text-teal-300 transition-colors"
          >
            <RefreshCw className="w-3 h-3" aria-hidden="true" />
            Tentar novamente
          </button>
        )}
      </div>
    </div>
  )
}

export function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="flex items-center justify-center h-40">
      <div className="flex flex-col items-center gap-2">
        <Inbox className="w-5 h-5 text-slate-500" aria-hidden="true" />
        <p className="text-xs text-slate-400">{message}</p>
      </div>
    </div>
  )
}
