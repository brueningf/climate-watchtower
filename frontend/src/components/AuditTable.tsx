import { useEffect, useState } from 'react'

type AuditItem = {
  id: string | number
  receivedAt?: string
  channel?: string
  temperature?: number | string
  humidity?: number | string
  pressure?: number | string
}
  // keep any other fields that may be returned (classification/raw) available via index signature
  [key: string]: any
  items: AuditItem[]
  page: number
  totalPages: number
  totalElements: number
}

export default function AuditTable() {
  const [data, setData] = useState<AuditResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)

  useEffect(() => {
    async function load() {
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})
        setLoading(true)
        setError(null)
        const res = await fetch(`/api/audit?page=${page}&size=${size}`)
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const json = await res.json()
        setData(json)
      } catch (e: any) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [page, size])

  if (loading) return <div className="p-4 text-slate-600">Loading audit events...</div>
  if (error) return <div className="p-4 text-red-600">Error loading audit events: {error}</div>
  if (!data || !data.items || data.items.length === 0)
    return <div className="p-4 text-slate-600">No audit events found.</div>

  const goToPage = (p: number) => {
    if (!data) return
    const clamped = Math.max(0, Math.min(p, Math.max(0, data.totalPages - 1)))
    setPage(clamped)
  }

  const prevPage = () => goToPage(page - 1)
  const nextPage = () => goToPage(page + 1)

  const onChangeSize = (s: number) => {
    setSize(s)
    // reset to first page when page size changes
    setPage(0)
  }

  // render up to 7 page buttons around current page
  const renderPageButtons = () => {
    if (!data) return null
    const total = data.totalPages
  const toggleRow = (id: string | number) => {
    setExpanded((prev) => ({ ...prev, [String(id)]: !prev[String(id)] }))
  }

    let end = start + maxButtons
    if (end > total) {
      end = total
      start = Math.max(0, end - maxButtons)
    }

    const buttons = [] as JSX.Element[]
    for (let i = start; i < end; i++) {
      buttons.push(
        <button
          key={i}
          onClick={() => goToPage(i)}
          className={`px-3 py-1 rounded-md mr-1 text-sm ${i === page ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
        >
          {i + 1}
        </button>
      )
    }
    return buttons
  }

  return (
    <div className="p-6">
      <h2 className="text-2xl font-semibold mb-4 text-slate-800">Raw Events</h2>
      <div className="overflow-x-auto bg-white shadow rounded-lg">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Received At</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Module</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Temp</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Humidity</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Pressure</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-100">
            {data.items.map((item) => (
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Channel</th>
                <td className="px-4 py-3 text-sm text-slate-700">{item.receivedAt}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.module}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.temperature}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.humidity}</td>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-700">Details</th>
              </tr>
            ))}
          </tbody>
        </table>
              <>
                <tr key={item.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm text-slate-700">{item.receivedAt ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{item.channel ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{item.module ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{item.temperature ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{item.humidity ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{item.pressure ?? '-'}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">
                    <button
                      onClick={() => toggleRow(item.id)}
                      className="px-2 py-1 bg-slate-100 rounded-md text-sm text-slate-700 hover:bg-slate-200"
                    >
                      {expanded[String(item.id)] ? 'Hide' : 'Show'}
                    </button>
                  </td>
                </tr>

                {expanded[String(item.id)] && (
                  <tr key={`${item.id}-details`} className="bg-slate-50">
                    <td colSpan={7} className="px-4 py-3 text-sm text-slate-700 font-mono whitespace-pre-wrap">
                      {JSON.stringify(item, null, 2)}
                    </td>
                  </tr>
                )}
              </>
          <label className="text-sm text-slate-600">Show
            <select
              value={size}
              onChange={(e) => onChangeSize(Number(e.target.value))}
              className="ml-2 px-2 py-1 border rounded-md bg-white text-slate-700"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
        </div>
      </div>
    </div>
  )
}
