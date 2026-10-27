# Twelve Data API

Für unser Projekt (Börsenanbindung, historische und Live-Kurse, Backtesting) ist [Twelve Data](https://twelvedata.com) die Datenquelle.

## Credit-System

Jeder Request verbraucht API Credits. Das Limit gilt pro Minute und im Free-Plan zusätzlich pro Tag.

## Kostenlos: Basic-Plan

- 8 API Credits/Minute, 800/Tag
- Echtzeitdaten für US-Aktien und ETFs
- Historische Zeitreihen (Intraday bis Monat) für diese Märkte
- Technische Indikatoren (laufen ebenfalls über Credits)
- WebSocket nur als "Trial" (8 WS-Credits, wenige Trial-Symbole z.B. AAPL)

Für uns heißt das: Für Entwicklung und Backtests mit US-Aktien (z. B. AAPL, MSFT) reicht der Free-Plan, wenn wir historische Daten einmal laden und lokal cachen. Bei 8 Requests/Minute müssen wir Anfragen drosseln, und Live-Kurse für viele Symbole sind nicht drin. Wenn wir später Live testen möchten, könnte es Probleme geben.

**Nicht enthalten:** europäische Börsen (z. B. XETRA), Fundamentaldaten, echtes WebSocket-Streaming, Pre-/Post-Market-Daten.

## Datenumfang

### Wie viele (USA) Aktien gibt es?

An NASDAQ und NYSE sind es rund **5.800 Stammaktien**. Das deckt praktisch alle börsennotierten US-Aktien ab. Es fehlen allerdings delistete Firmen.

Im Free-Plan haben wir davon nur die US-Aktien in Echtzeit. Insgesamt gibt Twelve Data fast 1 Mio. Instrumente an.

Es gibt aber auch z.B. deutsche Aktien...

### Welche Zeitauflösungen gibt es?

`1min, 5min, 15min, 30min, 45min, 1h, 2h, 4h, 8h, 1day, 1week, 1month`

Pro Request gibt es maximal 5000 Kerzen, längere Zeiträume müssen in mehreren Requests geladen werden (`start_date`/`end_date`).

### Wie weit reicht die Historie zurück?

Das hängt vom Intervall ab. Beispiel AAPL:

| Intervall | Daten ab                |
| --------- | ----------------------- |
| 1min      | 24.03.2020              |
| 5min      | 08.01.2020              |
| 1h        | 07.01.2019              |
| 4h        | 20.06.2019              |
| 1day      | 12.12.1980 (Börsengang) |
| 1week     | 08.12.1980              |
| 1month    | 01.12.1980              |

- Intraday (1min–8h): nur etwa ab 2019/2020
- Täglich und länger: meist die komplette Historie seit dem Börsengang

Für Backtests über lange Zeiträume (z. B. 2000–2020) bleiben also nur Tages-, Wochen- oder Monatskerzen.
