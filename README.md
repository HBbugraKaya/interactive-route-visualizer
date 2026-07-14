# Interactive Route Visualizer

Desktop route storytelling tool for remote meetings: **Java Swing + FlatLaf** chrome, **React** canvas embedded via **JCEF** Chromium, with Excalidraw-style annotations and per-stop weather from **Open-Meteo**.

## Features

- Place / drag / connect route stops (nodes) and edges
- Meeting tools: Select, Connect, Pen, Text, Arrow, Sticky (+ Undo)
- FlatLaf date picker → weather badge on each stop that has lat/lon
- JSON open/save (nodes, edges, annotations, date)
- Demo route: Istanbul → Ankara → Kayseri

## Requirements

- JDK 17+
- Node.js 18+ (to build the React canvas)
- Internet (first JCEF native download + Open-Meteo)

## Quick start (Windows)

```bat
scripts\build-web.bat
.tools\apache-maven-3.9.9\bin\mvn.cmd -pl desktop exec:java
```

If Maven is not under `.tools` yet, download it once:

```powershell
New-Item -ItemType Directory -Force -Path .tools | Out-Null
Invoke-WebRequest https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip -OutFile $env:TEMP\maven.zip
Expand-Archive $env:TEMP\maven.zip .tools -Force
```

Or with system Maven:

```bat
scripts\build-web.bat
mvn -pl desktop exec:java
```

### Dev mode (hot reload React)

1. `cd web && npm install && npm run dev`
2. `set ROUTEVIZ_WEB_URL=http://localhost:5173`
3. `mvn -pl desktop exec:java`

## Project layout

```
desktop/   Swing + FlatLaf + JCEF host + JS bridge
  src/main/java/com/routeviz/
    App.java                 # entry point
    config/AppContext.java   # composition root (DI wiring)
    service/                 # interfaces
    service/impl/            # implementations + SLF4J logs
    model/ ui/ bridge/ web/
web/       Vite React TypeScript canvas
scripts/   build-web.bat / build-web.sh  (build + copy into resources)
```

Services are interface-based (`CurrencyService`, `GeocodingService`, `WeatherService`,
`RouteDocumentService`, `WebAssetServer`) with `*Impl` under `service.impl`, wired in `AppContext`.
Logging uses **SLF4J** + Logback (`logback.xml`).

## Tests

```bat
.\mvnw.cmd -pl desktop test
```

## Usage tips

- **Double-click** canvas → add stop
- **Connect** tool → drag from one stop to another
- Select a stop in the canvas → edit label / lat / lon in the right panel
- Change **Route date** in the toolbar → weather badges refresh
- **Pen / Sticky / Arrow / Text** for meeting markup; Ctrl+Z undo

## Sample JSON

See `samples/demo-route.json`.

## Notes

- First launch downloads Chromium natives into `jcef-bundle/` (can take a few minutes).
- Packaged UI is served from an embedded localhost server so Open-Meteo CORS works.
