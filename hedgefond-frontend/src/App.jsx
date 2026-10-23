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
const STRATEGIES_URL = 'http://localhost:8080/api/strategies/evaluate'
const MEAN_REVERSION_STRATEGY = `strategy "Mean Reversion" {
  bands = bollinger(close, 20, 2)
  rsi = rsi(close, 14)
  buy when crossesBelow(close, bands.lower) and rsi < 30
  sell when crossesAbove(close, bands.middle)
  on buy do marketBuy()
  on sell do marketSell()
}`
const INDICATOR_REQUEST = {
  symbol: 'AAPL',
  interval: 'DAY_1',
  limit: 49,
  currency: 'USD',
  indicators: [
    { name: 'rsi14', type: 'rsi', arguments: ['close', '14'] },
    { name: 'bollinger20', type: 'bollinger', arguments: ['close', '20', '2'] },
  ],
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

function timestampToMilliseconds(timestamp) {
  const numericTimestamp = Number(timestamp)
  if (Number.isFinite(numericTimestamp)) {
    return Math.abs(numericTimestamp) < 1e12 ? numericTimestamp * 1000 : numericTimestamp
  }
  return new Date(timestamp).getTime()
}

function getStrategySignals(result) {
  if (!Array.isArray(result.signals)) return []

  return result.signals.map((signal) => {
    const timestamp = timestampToMilliseconds(signal.timestamp)
    return {
      dateKey: Number.isFinite(timestamp) ? new Date(timestamp).toISOString().slice(0, 10) : '',
      action: String(signal.action).toUpperCase(),
      price: Number(signal.price),
    }
  }).filter((signal) => signal.dateKey && Number.isFinite(signal.price)
    && ['BUY', 'SELL'].includes(signal.action))
}

async function fetchStrategySignals(strategy, candles, signal) {
  if (candles.length === 0) return []

  const firstVisibleTimestamp = candles[0].timestamp
  const lastVisibleTimestamp = candles[candles.length - 1].timestamp
  const start = new Date(firstVisibleTimestamp - 45 * 24 * 60 * 60 * 1000)
    .toISOString().slice(0, 10)
  const end = new Date(lastVisibleTimestamp).toISOString().slice(0, 10)
  const response = await fetch(STRATEGIES_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      symbol: 'AAPL',
      interval: 'DAY_1',
      start,
      end,
      limit: 60,
      currency: 'USD',
      strategy,
    }),
    signal,
  })
  if (!response.ok) throw new Error(`Request fehlgeschlagen (${response.status})`)

  return getStrategySignals(await response.json())
}

function SignalDot({ cx, cy, value, fill, label }) {
  if (value == null || !Number.isFinite(cx) || !Number.isFinite(cy)) return null

  return (
    <g>
      <circle cx={cx} cy={cy} r={9} fill={fill} stroke="#fff" strokeWidth={2} />
      <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central" fill="#fff" fontSize={10} fontWeight="bold">
        {label}
      </text>
    </g>
  )
}

function getRsiRows(result) {
  if (Array.isArray(result.candles)) {
    return result.candles.flatMap((candle) => {
      const value = candle.indicators?.rsi14
      return value == null ? [] : [{ timestamp: candle.timestamp, rsi: Number(value) }]
    }).filter((row) => Number.isFinite(row.rsi)).slice(-30)
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
  }).filter((row) => Number.isFinite(row.rsi)).slice(-30)
}

function getBollingerRows(result) {
  if (!Array.isArray(result.candles)) return []

  return result.candles.map((candle) => {
    const bands = candle.indicators?.bollinger20
    return {
      timestamp: candle.timestamp,
      close: Number(candle.close),
      middle: bands?.middle == null ? null : Number(bands.middle),
      upper: bands?.upper == null ? null : Number(bands.upper),
      lower: bands?.lower == null ? null : Number(bands.lower),
    }
  }).filter((row) => Number.isFinite(row.close)).slice(-30)
}

function App() {
  const [candles, setCandles] = useState([])
  const [strategySignals, setStrategySignals] = useState([])
  const [strategyText, setStrategyText] = useState(MEAN_REVERSION_STRATEGY)
  const [strategyLoading, setStrategyLoading] = useState(true)
  const [rsiValues, setRsiValues] = useState([])
  const [bollingerValues, setBollingerValues] = useState([])
  const [error, setError] = useState('')
  const [strategyError, setStrategyError] = useState('')
  const [rsiError, setRsiError] = useState('')
  const [bollingerError, setBollingerError] = useState('')
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
            dateKey: new Date(parsedTimestamp).toISOString().slice(0, 10),
            timestamp: parsedTimestamp,
            close: Number(candle.close),
          }
        })
          .filter((candle) => Number.isFinite(candle.timestamp) && Number.isFinite(candle.close))
          .sort((a, b) => a.timestamp - b.timestamp)

        setCandles(sortedCandles)

        if (sortedCandles.length > 0) {
          try {
            const signals = await fetchStrategySignals(
              MEAN_REVERSION_STRATEGY,
              sortedCandles,
              controller.signal,
            )
            if (!controller.signal.aborted) setStrategySignals(signals)
          } catch (requestError) {
            if (requestError.name !== 'AbortError') setStrategyError(requestError.message)
          } finally {
            if (!controller.signal.aborted) setStrategyLoading(false)
          }
        } else {
          setStrategyLoading(false)
        }
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setError(requestError.message)
          setStrategyLoading(false)
        }
      }
    }

    async function loadIndicators() {
      try {
        const response = await fetch(INDICATORS_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(INDICATOR_REQUEST),
          signal: controller.signal,
        })
        if (!response.ok) throw new Error(`Request fehlgeschlagen (${response.status})`)

        const result = await response.json()
        const rsiRows = getRsiRows(result)
        const bandRows = getBollingerRows(result)
        setRsiValues(rsiRows)
        setBollingerValues(bandRows)
        if (!bandRows.some((row) => row.middle != null)) {
          setBollingerError('Keine Bollinger-Band-Werte in der Antwort gefunden.')
        }
        if (rsiRows.length === 0) setRsiError('Keine RSI-Werte in der Antwort gefunden.')
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setRsiError(requestError.message)
          setBollingerError(requestError.message)
        }
      } finally {
        if (!controller.signal.aborted) setRsiLoaded(true)
      }
    }

    loadCandles()
    loadIndicators()
    return () => controller.abort()
  }, [])

  async function handleStrategySubmit(event) {
    event.preventDefault()
    if (candles.length === 0) return

    setStrategyLoading(true)
    setStrategyError('')
    setStrategySignals([])
    try {
      const signals = await fetchStrategySignals(strategyText, candles)
      setStrategySignals(signals)
    } catch (requestError) {
      setStrategyError(requestError.message)
    } finally {
      setStrategyLoading(false)
    }
  }

  const signalsByDate = new Map()
  strategySignals.forEach((signal) => {
    const daySignals = signalsByDate.get(signal.dateKey) ?? []
    daySignals.push(signal)
    signalsByDate.set(signal.dateKey, daySignals)
  })
  const chartCandles = candles.map((candle) => {
    const daySignals = signalsByDate.get(candle.dateKey) ?? []
    return {
      ...candle,
      buySignal: daySignals.find((signal) => signal.action === 'BUY')?.price ?? null,
      sellSignal: daySignals.find((signal) => signal.action === 'SELL')?.price ?? null,
    }
  })

  return (
    <div style={{ width: '100%', padding: 20 }}>
      <h2>AAPL Schlusskurs (USD)</h2>
      <p><strong style={{ color: '#16a34a' }}>K</strong> Kaufsignal &nbsp; <strong style={{ color: '#dc2626' }}>V</strong> Verkaufssignal</p>
      <form onSubmit={handleStrategySubmit} style={{ maxWidth: 800, margin: '16px auto 24px', textAlign: 'left' }}>
        <label htmlFor="strategy-text">Strategie</label>
        <textarea
          id="strategy-text"
          value={strategyText}
          onChange={(event) => setStrategyText(event.target.value)}
          rows={10}
          spellCheck={false}
          style={{ display: 'block', boxSizing: 'border-box', width: '100%', marginTop: 6, padding: 10, fontFamily: 'monospace' }}
        />
        <button type="submit" disabled={strategyLoading || candles.length === 0} style={{ marginTop: 10 }}>
          {strategyLoading ? 'Strategie wird ausgewertet…' : 'Strategie anwenden'}
        </button>
      </form>
      {error && <p role="alert">Kursdaten konnten nicht geladen werden: {error}</p>}
      {strategyError && <p role="alert">Strategie-Signale konnten nicht geladen werden: {strategyError}</p>}
      {!error && candles.length === 0 && <p>Lade Kursdaten…</p>}
      {candles.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartCandles}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis domain={['auto', 'auto']} />
            <Tooltip />
            <Line type="monotone" dataKey="close" name="Schlusskurs" stroke="#6366f1" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 7 }} />
            <Line type="monotone" dataKey="buySignal" name="Kaufsignal" stroke="transparent" dot={(props) => <SignalDot {...props} fill="#16a34a" label="K" />} activeDot={false} />
            <Line type="monotone" dataKey="sellSignal" name="Verkaufssignal" stroke="transparent" dot={(props) => <SignalDot {...props} fill="#dc2626" label="V" />} activeDot={false} />
          </LineChart>
        </ResponsiveContainer>
      )}
      <h2>Bollinger-Band (20, 2)</h2>
      {bollingerError && <p role="alert">Bollinger-Band konnte nicht geladen werden: {bollingerError}</p>}
      {!rsiLoaded && !bollingerError && <p>Lade Bollinger-Band…</p>}
      {bollingerValues.length > 0 && !bollingerError && (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={bollingerValues}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="timestamp" tickFormatter={formatTimestamp} />
            <YAxis domain={['auto', 'auto']} />
            <Tooltip labelFormatter={formatTimestamp} />
            <Line type="monotone" dataKey="close" name="Schlusskurs" stroke="#6366f1" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="middle" name="Mittleres Band" stroke="#f97316" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="upper" name="Oberes Band" stroke="#22c55e" strokeWidth={1.5} dot={false} />
            <Line type="monotone" dataKey="lower" name="Unteres Band" stroke="#22c55e" strokeWidth={1.5} dot={false} />
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
