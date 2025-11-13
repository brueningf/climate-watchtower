import { useEffect, useState } from 'react'

type AuditItem = {
  id: string | number
  receivedAt?: string
  module?: string
  temperature?: number | string
  humidity?: number | string
  pressure?: number | string
}

type AuditResponse = {
  items: AuditItem[]
  page: number
  totalPages: number
  totalElements: number
}

export default function AuditTable() {
  const [data, setData] = useState<AuditResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const res = await fetch('/api/audit?page=0&size=20')
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
  }, [])

  if (loading) return <div className="p-4 text-slate-600">Loading audit events...</div>
  if (error) return <div className="p-4 text-red-600">Error loading audit events: {error}</div>
  if (!data || !data.items || data.items.length === 0)
    return <div className="p-4 text-slate-600">No audit events found.</div>

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
              <tr key={item.id} className="hover:bg-slate-50">
                <td className="px-4 py-3 text-sm text-slate-700">{item.receivedAt}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.module}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.temperature}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.humidity}</td>
                <td className="px-4 py-3 text-sm text-slate-700">{item.pressure}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-2 text-sm text-slate-600">Page {data.page + 1} of {data.totalPages} — {data.totalElements} total</div>
    </div>
  )
}

