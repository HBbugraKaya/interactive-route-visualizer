import { useEffect, useState } from 'react';
import { GraphCanvas } from './components/GraphCanvas';
import { ToolPalette } from './tools/ToolPalette';
import { useGraphStore } from './store/graphStore';
import { initJavaBridge, notifyStatus, onHostCommand } from './bridge/javaBridge';
import { fetchWeatherForStops } from './services/weatherApi';
import type { CurrencyCode, GraphDocument, GraphMeta, RouteNode, Tool, WeatherDay } from './types';
import './App.css';

function App() {
  const store = useGraphStore();
  const [weatherByNode, setWeatherByNode] = useState<Record<string, WeatherDay>>({});

  useEffect(() => {
    initJavaBridge();
    notifyStatus('React route canvas booted');

    return onHostCommand((msg) => {
      switch (msg.type) {
        case 'setTool':
          store.setTool(msg.payload as Tool);
          break;
        case 'setDate':
          store.setDate(String(msg.payload));
          break;
        case 'setCurrencies':
          store.setCurrencies(msg.payload as CurrencyCode[]);
          break;
        case 'setMeta':
          store.setMeta(msg.payload as Partial<GraphMeta>);
          break;
        case 'addNode':
          store.addNode();
          break;
        case 'deleteSelection':
          store.deleteSelection();
          break;
        case 'fitView':
          store.fitView();
          break;
        case 'loadGraph':
          store.loadGraph(msg.payload as GraphDocument);
          break;
        case 'updateNode':
          store.updateNode(msg.payload as RouteNode);
          break;
        case 'exportGraph':
          store.exportGraph();
          break;
        default:
          break;
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const weatherKey = store.doc.nodes
    .map(
      (n) =>
        `${n.id}:${n.lat ?? ''},${n.lon ?? ''},${n.stayDate ?? ''},${n.useStayDate !== false}`,
    )
    .join(';');

  useEffect(() => {
    let cancelled = false;
    const fallback = new Date().toISOString().slice(0, 10);
    notifyStatus('Loading weather for stop dates…');
    fetchWeatherForStops(store.doc.nodes, fallback)
      .then((map) => {
        if (!cancelled) {
          setWeatherByNode(map);
          const n = Object.keys(map).length;
          notifyStatus(n ? `Weather loaded for ${n} stop(s)` : 'No coordinates for weather');
        }
      })
      .catch(() => {
        if (!cancelled) notifyStatus('Weather fetch failed');
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [weatherKey]);

  return (
    <div className="app-shell">
      <ToolPalette
        tool={store.tool}
        onChange={store.setTool}
        onUndo={store.undo}
        onClearAnnotations={store.clearAnnotations}
      />
      <GraphCanvas store={store} weatherByNode={weatherByNode} />
      <div className="hint-bar">
        Double-click to add stop · City search in right panel · Stay date drives weather · Currencies
        in the top bar
      </div>
    </div>
  );
}

export default App;
