import { useCallback, useMemo, useRef, useState } from 'react';
import {
  createDemoGraph,
  type Annotation,
  type CurrencyCode,
  type GraphDocument,
  type GraphMeta,
  type RouteEdge,
  type RouteNode,
  type Tool,
  type Viewport,
  uid,
} from '../types';
import { postToJava } from '../bridge/javaBridge';

type HistorySnap = GraphDocument;

function cloneDoc(doc: GraphDocument): GraphDocument {
  return JSON.parse(JSON.stringify(doc)) as GraphDocument;
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function touchMeta(meta: GraphMeta): GraphMeta {
  return { ...meta, updatedAt: todayIso() };
}

const today = todayIso();

export function useGraphStore() {
  const [doc, setDoc] = useState<GraphDocument>(() => createDemoGraph(today));
  const [tool, setTool] = useState<Tool>('select');
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [selectedAnnotationId, setSelectedAnnotationId] = useState<string | null>(null);
  const [viewport, setViewport] = useState<Viewport>({ x: 40, y: 40, scale: 1 });
  const history = useRef<HistorySnap[]>([]);
  const syncTimer = useRef<number | null>(null);

  const pushHistory = useCallback((prev: GraphDocument) => {
    history.current.push(cloneDoc(prev));
    if (history.current.length > 50) history.current.shift();
  }, []);

  const scheduleSync = useCallback((next: GraphDocument) => {
    if (syncTimer.current) window.clearTimeout(syncTimer.current);
    syncTimer.current = window.setTimeout(() => {
      postToJava({ type: 'graphChanged', graph: next });
    }, 250);
  }, []);

  const commit = useCallback(
    (updater: (prev: GraphDocument) => GraphDocument, recordHistory = true) => {
      setDoc((prev) => {
        if (recordHistory) pushHistory(prev);
        const next = updater(prev);
        const withTouch = { ...next, meta: touchMeta(next.meta) };
        scheduleSync(withTouch);
        return withTouch;
      });
    },
    [pushHistory, scheduleSync],
  );

  const undo = useCallback(() => {
    const prev = history.current.pop();
    if (!prev) return;
    setDoc(prev);
    scheduleSync(prev);
  }, [scheduleSync]);

  const loadGraph = useCallback((graph: GraphDocument) => {
    history.current = [];
    const normalized = cloneDoc(graph);
    if (!normalized.meta.createdAt) normalized.meta.createdAt = todayIso();
    if (!normalized.meta.updatedAt) normalized.meta.updatedAt = normalized.meta.createdAt;
    if (!normalized.meta.currencies?.length) normalized.meta.currencies = ['EUR'];
    setDoc(normalized);
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
    setSelectedAnnotationId(null);
  }, []);

  const setDate = useCallback((_date: string) => {

  }, []);

  const setMeta = useCallback(
    (meta: Partial<GraphMeta>) => {
      commit((prev) => ({ ...prev, meta: { ...prev.meta, ...meta } }), false);
    },
    [commit],
  );

  const setCurrencies = useCallback(
    (currencies: CurrencyCode[]) => {
      const list = currencies?.length ? currencies : (['EUR'] as CurrencyCode[]);
      commit((prev) => ({ ...prev, meta: { ...prev.meta, currencies: list } }), false);
    },
    [commit],
  );

  const addNodeAt = useCallback(
    (x: number, y: number) => {
      const node: RouteNode = {
        id: uid('n'),
        x,
        y,
        label: 'New stop',
        type: 'stop',
        note: '',
        peopleCount: 1,
        stayDate: todayIso(),
        hotelNights: 1,
        pricePerNightEur: 120,
        usePeople: true,
        useStayDate: true,
        useHotelNights: true,
        usePrice: true,
      };
      commit((prev) => ({ ...prev, nodes: [...prev.nodes, node] }));
      setSelectedNodeId(node.id);
      setSelectedEdgeId(null);
      setSelectedAnnotationId(null);
      postToJava({ type: 'selectionChanged', node });
    },
    [commit],
  );

  const addNode = useCallback(() => {
    const x = (400 - viewport.x) / viewport.scale;
    const y = (280 - viewport.y) / viewport.scale;
    addNodeAt(x, y);
  }, [addNodeAt, viewport]);

  const updateNode = useCallback(
    (node: RouteNode) => {
      commit(
        (prev) => ({
          ...prev,
          nodes: prev.nodes.map((n) => (n.id === node.id ? { ...n, ...node } : n)),
        }),
        false,
      );
    },
    [commit],
  );

  const moveNode = useCallback(
    (id: string, x: number, y: number) => {
      setDoc((prev) => {
        const next = {
          ...prev,
          nodes: prev.nodes.map((n) => (n.id === id ? { ...n, x, y } : n)),
        };
        scheduleSync(next);
        return next;
      });
    },
    [scheduleSync],
  );

  const connectNodes = useCallback(
    (from: string, to: string) => {
      if (from === to) return;
      commit((prev) => {
        if (prev.edges.some((e) => e.from === from && e.to === to)) return prev;
        const edge: RouteEdge = { id: uid('e'), from, to };
        return { ...prev, edges: [...prev.edges, edge] };
      });
    },
    [commit],
  );

  const addAnnotation = useCallback(
    (anno: Annotation) => {
      commit((prev) => ({ ...prev, annotations: [...prev.annotations, anno] }));
      setSelectedAnnotationId(anno.id);
      setSelectedNodeId(null);
      setSelectedEdgeId(null);
    },
    [commit],
  );

  const updateAnnotation = useCallback(
    (anno: Annotation) => {
      commit(
        (prev) => ({
          ...prev,
          annotations: prev.annotations.map((a) => (a.id === anno.id ? anno : a)),
        }),
        false,
      );
    },
    [commit],
  );

  const deleteSelection = useCallback(() => {
    commit((prev) => {
      if (selectedNodeId) {
        return {
          ...prev,
          nodes: prev.nodes.filter((n) => n.id !== selectedNodeId),
          edges: prev.edges.filter((e) => e.from !== selectedNodeId && e.to !== selectedNodeId),
        };
      }
      if (selectedEdgeId) {
        return { ...prev, edges: prev.edges.filter((e) => e.id !== selectedEdgeId) };
      }
      if (selectedAnnotationId) {
        return {
          ...prev,
          annotations: prev.annotations.filter((a) => a.id !== selectedAnnotationId),
        };
      }
      return prev;
    });
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
    setSelectedAnnotationId(null);
    postToJava({ type: 'selectionChanged', node: null });
  }, [commit, selectedAnnotationId, selectedEdgeId, selectedNodeId]);

  const selectNode = useCallback(
    (id: string | null) => {
      setSelectedNodeId(id);
      setSelectedEdgeId(null);
      setSelectedAnnotationId(null);
      const node = id ? doc.nodes.find((n) => n.id === id) ?? null : null;
      postToJava({ type: 'selectionChanged', node });
    },
    [doc.nodes],
  );

  const selectEdge = useCallback((id: string | null) => {
    setSelectedEdgeId(id);
    setSelectedNodeId(null);
    setSelectedAnnotationId(null);
    postToJava({ type: 'selectionChanged', node: null });
  }, []);

  const selectAnnotation = useCallback((id: string | null) => {
    setSelectedAnnotationId(id);
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
    postToJava({ type: 'selectionChanged', node: null });
  }, []);

  const fitView = useCallback(() => {
    if (doc.nodes.length === 0) {
      setViewport({ x: 40, y: 40, scale: 1 });
      return;
    }
    const xs = doc.nodes.map((n) => n.x);
    const ys = doc.nodes.map((n) => n.y);
    const minX = Math.min(...xs) - 80;
    const minY = Math.min(...ys) - 80;
    setViewport({ x: -minX + 40, y: -minY + 40, scale: 1 });
  }, [doc.nodes]);

  const exportGraph = useCallback(() => {
    postToJava({ type: 'exportGraph', graph: doc });
  }, [doc]);

  const clearAnnotations = useCallback(() => {
    commit((prev) => ({ ...prev, annotations: [] }));
  }, [commit]);

  const selectedNode = useMemo(
    () => doc.nodes.find((n) => n.id === selectedNodeId) ?? null,
    [doc.nodes, selectedNodeId],
  );

  return {
    doc,
    tool,
    setTool,
    viewport,
    setViewport,
    selectedNodeId,
    selectedEdgeId,
    selectedAnnotationId,
    selectedNode,
    loadGraph,
    setDate,
    setMeta,
    setCurrencies,
    addNode,
    addNodeAt,
    updateNode,
    moveNode,
    connectNodes,
    addAnnotation,
    updateAnnotation,
    deleteSelection,
    selectNode,
    selectEdge,
    selectAnnotation,
    fitView,
    exportGraph,
    undo,
    clearAnnotations,
    pushHistoryBeforeDrag: () => pushHistory(doc),
  };
}

export type GraphStore = ReturnType<typeof useGraphStore>;
