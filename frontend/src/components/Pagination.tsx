import { ChevronLeft, ChevronRight } from "lucide-react"
import type { ApiMeta } from "../services/api"

interface PaginationProps {
  meta: ApiMeta | undefined
  onPageChange: (page: number) => void
  /** Rows currently on screen, used as a fallback when the endpoint sends no meta. */
  count: number
}

/**
 * The list endpoints have always answered with `meta.totalElements`/`hasNext`; the UI simply ignored
 * it, so anything past the first page stayed invisible. This is the control that consumes it.
 */
export default function Pagination({ meta, onPageChange, count }: PaginationProps) {
  const page = meta?.page ?? 0
  const size = meta?.size ?? count
  const totalElements = meta?.totalElements ?? count
  const totalPages = meta?.totalPages ?? 1

  if (totalElements === 0 || (totalPages <= 1 && count < size)) return null

  const from = totalElements === 0 ? 0 : page * size + 1
  const to = Math.min((page + 1) * size, totalElements)
  const canPrev = meta?.hasPrevious ?? page > 0
  const canNext = meta?.hasNext ?? false

  const button =
    "flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-dark-800 text-[11px] font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed enabled:hover:bg-dark-800 enabled:text-slate-300 text-slate-400"

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-dark-800">
      <p className="text-[11px] text-slate-400 tabular-nums">
        {from}–{to} de {totalElements.toLocaleString("pt-BR")}
      </p>
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={!canPrev}
          onClick={() => onPageChange(page - 1)}
          className={button}
          aria-label="Página anterior"
        >
          <ChevronLeft className="w-3.5 h-3.5" aria-hidden="true" />
          Anterior
        </button>
        <span className="text-[11px] text-slate-400 tabular-nums">
          Página {page + 1} de {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          disabled={!canNext}
          onClick={() => onPageChange(page + 1)}
          className={button}
          aria-label="Próxima página"
        >
          Próxima
          <ChevronRight className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
      </div>
    </div>
  )
}
