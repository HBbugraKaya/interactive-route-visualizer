type HostMessage = {
  type: string;
  payload?: unknown;
};

type Listener = (msg: HostMessage) => void;

declare global {
  interface Window {
    routeViz?: {
      command: (msg: HostMessage) => void;
    };
    cefQuery?: (req: {
      request: string;
      onSuccess?: (response: string) => void;
      onFailure?: (code: number, message: string) => void;
    }) => void;
  }
}

const listeners = new Set<Listener>();

export function initJavaBridge(): void {
  window.routeViz = {
    command(msg: HostMessage) {
      listeners.forEach((l) => l(msg));
    },
  };
  postToJava({ type: 'ready' });
}

export function onHostCommand(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function postToJava(payload: Record<string, unknown>): void {
  const request = JSON.stringify(payload);
  if (typeof window.cefQuery === 'function') {
    window.cefQuery({
      request,
      onSuccess: () => undefined,
      onFailure: () => undefined,
    });
    return;
  }

  console.debug('[bridge→java]', payload);
}

export function notifyStatus(message: string): void {
  postToJava({ type: 'status', message });
}
