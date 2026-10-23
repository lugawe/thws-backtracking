import { useEffect, useState } from 'react'
import './App.css'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'

const CANDLES_URL = 'http://localhost:8080/api/stocks/AAPL/candles?interval=1day&limit=30&currency=USD'
const INDICATORS_URL = 'http://localhost:8080/api/indicators/evaluate'
const RSI_REQUEST = {
  symbol: 'AAPL',
  interval: 'DAY_1',
  limit: 44,
  currency: 'USD',
  indicators: [{ name: 'rsi14', type: 'rsi', arguments: ['close', '14'] }],
}
const dateFormatter = new Intl.DateTimeFormat('de-DE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

function formatTimestamp(timestamp) {
  const numericTimestamp = Number(timestamp)
  const date = Number.isFinite(numericTimestamp)
    ? new Date(Math.abs(numericTimestamp) < 1e12 ? numericTimestamp * 1000 : numericTimestamp)
    : new Date(timestamp)

  return Number.isNaN(date.getTime()) ? String(timestamp) : dateFormatter.format(date)
}

function getRsiRows(result) {
  if (Array.isArray(result.candles)) {
    return result.candles.flatMap((candle) => {
      const value = candle.indicators?.rsi14
      return value == null ? [] : [{ timestamp: candle.timestamp, rsi: Number(value) }]
    }).filter((row) => Number.isFinite(row.rsi))
  }

  const indicatorList = [
    ...(Array.isArray(result.indicators) ? result.indicators : []),
    ...(Array.isArray(result.results) ? result.results : []),
  ]
  const indicatorResult = indicatorList.find((indicator) => indicator.name === 'rsi14')
    ?? result.indicators?.rsi14
    ?? result.results?.rsi14
  const rows = Array.isArray(result)
    ? result
    : result.rsi14 ?? indicatorResult?.values ?? indicatorResult?.data ?? indicatorResult ?? result.values?.rsi14 ?? result.data ?? []

  if (!Array.isArray(rows)) return []

  return rows.map((row) => {
    if (typeof row === 'number') return { rsi: row }

    const value = row.value ?? row.rsi ?? row.rsi14

    return {
      timestamp: row.timestamp ?? row.date ?? row.time,
      rsi: value == null ? Number.NaN : Number(value),
    }
  }).filter((row) => Number.isFinite(row.rsi))
}

function App() {
  const [candles, setCandles] = useState([])
  const [rsiValues, setRsiValues] = useState([])
  const [error, setError] = useState('')
  const [rsiError, setRsiError] = useState('')
  const [rsiLoaded, setRsiLoaded] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    async function loadCandles() {
      try {
        const response = await fetch(CANDLES_URL, { signal: controller.signal })
        if (!response.ok) throw new Error(`Request fehlgeschlagen (${response.status})`)

        const result = await response.json()
        const rows = Array.isArray(result) ? result : result.candles ?? result.data ?? []
        const sortedCandles = rows.map((candle) => {
          const timestamp = candle.timestamp ?? candle.date ?? candle.time
          const numericTimestamp = Number(timestamp)
          const parsedTimestamp = Number.isFinite(numericTimestamp)
            ? (Math.abs(numericTimestamp) < 1e12 ? numericTimestamp * 1000 : numericTimestamp)
            : new Date(timestamp).getTime()

          return {
            date: formatTimestamp(timestamp),
            timestamp: parsedTimestamp,
            close: Number(candle.close),
          }
        })
          .filter((candle) => Number.isFinite(candle.timestamp) && Number.isFinite(candle.close))
          .sort((a, b) => a.timestamp - b.timestamp)

        setCandles(sortedCandles)
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setError(requestError.message)
      }
    }

    async function loadRsi() {
      try {
        const response = await fetch(INDICATORS_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(RSI_REQUEST),
          signal: controller.signal,
        })
        if (!response.ok) throw new Error(`Request fehlgeschlagen (${response.status})`)

        const result = await response.json()
        const rows = getRsiRows(result)
        setRsiValues(rows)
        if (rows.length === 0) setRsiError('Die Antwort enthält keine erkennbaren RSI-Werte.')
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setRsiError(requestError.message)
      } finally {
        if (!controller.signal.aborted) setRsiLoaded(true)
      }
    }

    loadCandles()
    loadRsi()
    return () => controller.abort()
  }, [])

  return (
    <div style={{ width: '100%', padding: 20 }}>
      <h2>AAPL Schlusskurs (USD)</h2>
      {error && <p role="alert">Kursdaten konnten nicht geladen werden: {error}</p>}
      {!error && candles.length === 0 && <p>Lade Kursdaten…</p>}
      {candles.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={candles}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis domain={['auto', 'auto']} />
            <Tooltip />
            <Line type="monotone" dataKey="close" name="Schlusskurs" stroke="#6366f1" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 7 }} />
          </LineChart>
        </ResponsiveContainer>
      )}
      <h2>RSI (14)</h2>
      {rsiError && <p role="alert">RSI konnte nicht geladen werden: {rsiError}</p>}
      {!rsiLoaded && !rsiError && <p>Lade RSI…</p>}
      {rsiValues.length > 0 && (
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={rsiValues}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="timestamp" tickFormatter={formatTimestamp} />
            <YAxis domain={[0, 100]} />
            <Tooltip labelFormatter={formatTimestamp} />
            <Line type="monotone" dataKey="rsi" name="RSI 14" stroke="#f97316" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}

export default App
