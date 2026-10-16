# thws-hedgefond

## Use Cases:

- Verrechnungskonto (Simulation) (Olia)
  - Kauf, Verkauf, Gebühren, Abheben, Dazubuchen
- Depot (Luis)
  - Aktueller Marktwert der gekauften Wertpapiere, Chart, Rendite, Kaufpreise, Orderbuch
  
- Orderabwicklung (Chris)
  - Market Order
  - Optional: Stop-Loss Order, Limit Order, Auf Fallende Kurse setzen (Short), Hebel, rechtzeitig automatisches Ausstoppen
  - Handelskosten Simulation
    - Spread, Ordergebühren für Börsen…, Konfigurierter
- Börsenanbindung (Luis)
  - Historische Aktienkurse
  - Live Aktienkurse
- Automatisierte Trading Algorithmen implementieren (Olia, Chris)
  - Modular, erweiterbar, Konfigurierbar


- Backtracking Simulation (Chris)
  - Algorithmen miteinander vergleichen
  - Simulierter Kontostand Anfang und Ende
  - Algorithmus in Kurszeitraum bei Aktie Y testen


### Nice to have

- Interner LLM/MCP Server
  - Der Nutzer wünscht im Chat: „Führe einen Backtest der RSI-Strategie für Apple über das Jahr 2022 durch und zeige mir danach meinen aktuellen Kontostand.“
- Export
- Detaillierte Analyseseite
  - Kennzahlen
     - Maximum Drawdown
     - Rendite (Kumulated, Anulated)
     - Benchmark Vergleich (MSCI World, S&P 500)
- ML Handelsstrategie
  - Sentiment-Analyse (FinBERT / NLP): Ein vortrainiertes Sprachmodell analysiert Finanz-Headlines oder News
  - Trend-Vorhersage (XGBoost / Random Forest / LSTM): Ihr trainiert ein Modell auf historischen technischen Indikatoren (RSI, MACD, Volatilität), um den Trend (Steigt/Fällt) für den Folgetag vorherzusagen.
