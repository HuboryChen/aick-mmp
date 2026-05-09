import React, { createContext, useContext, useReducer, useCallback, useMemo } from 'react';

// Connection states
export const ConnectionState = {
  IDLE: 'idle',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  DISCONNECTED: 'disconnected',
  FAILED: 'failed',
  CLOSED: 'closed',
};

// Action types
const ActionTypes = {
  UPDATE_STATE: 'UPDATE_STATE',
  INCREMENT_RETRY: 'INCREMENT_RETRY',
  RESET_RETRY: 'RESET_RETRY',
  SET_ERROR: 'SET_ERROR',
  CLEAR_ERROR: 'CLEAR_ERROR',
  UPDATE_HEALTH_METRICS: 'UPDATE_HEALTH_METRICS',
  RESET: 'RESET',
};

const initialState = {
  connectionState: ConnectionState.IDLE,
  retryCount: 0,
  error: null,
  healthMetrics: {
    packetLoss: 0,
    roundTripTime: 0,
    jitter: 0,
  },
};

function reducer(state, action) {
  switch (action.type) {
    case ActionTypes.UPDATE_STATE:
      return { ...state, connectionState: action.payload };
    case ActionTypes.INCREMENT_RETRY:
      return { ...state, retryCount: state.retryCount + 1 };
    case ActionTypes.RESET_RETRY:
      return { ...state, retryCount: 0 };
    case ActionTypes.SET_ERROR:
      return { ...state, error: action.payload };
    case ActionTypes.CLEAR_ERROR:
      return { ...state, error: null };
    case ActionTypes.UPDATE_HEALTH_METRICS:
      return { ...state, healthMetrics: { ...state.healthMetrics, ...action.payload } };
    case ActionTypes.RESET:
      return initialState;
    default:
      return state;
  }
}

const StreamHealthContext = createContext(null);

export function StreamHealthProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const updateConnectionState = useCallback((newState) => {
    dispatch({ type: ActionTypes.UPDATE_STATE, payload: newState });
  }, []);

  const incrementRetry = useCallback(() => {
    dispatch({ type: ActionTypes.INCREMENT_RETRY });
  }, []);

  const resetRetry = useCallback(() => {
    dispatch({ type: ActionTypes.RESET_RETRY });
  }, []);

  const setError = useCallback((error) => {
    dispatch({ type: ActionTypes.SET_ERROR, payload: error });
  }, []);

  const clearError = useCallback(() => {
    dispatch({ type: ActionTypes.CLEAR_ERROR });
  }, []);

  const updateHealthMetrics = useCallback((metrics) => {
    dispatch({ type: ActionTypes.UPDATE_HEALTH_METRICS, payload: metrics });
  }, []);

  const reset = useCallback(() => {
    dispatch({ type: ActionTypes.RESET });
  }, []);

  const value = useMemo(() => ({
    ...state,
    updateConnectionState,
    incrementRetry,
    resetRetry,
    setError,
    clearError,
    updateHealthMetrics,
    reset,
  }), [state, updateConnectionState, incrementRetry, resetRetry, setError, clearError, updateHealthMetrics, reset]);

  return (
    <StreamHealthContext.Provider value={value}>
      {children}
    </StreamHealthContext.Provider>
  );
}

export function useStreamHealth() {
  const context = useContext(StreamHealthContext);
  if (!context) {
    throw new Error('useStreamHealth must be used within StreamHealthProvider');
  }
  return context;
}

export default StreamHealthContext;
