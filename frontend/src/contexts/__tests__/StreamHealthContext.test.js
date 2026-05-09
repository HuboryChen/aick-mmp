import React from 'react';
import { render, screen, act, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { StreamHealthProvider, useStreamHealth, ConnectionState } from '../StreamHealthContext';

// Test component that exposes context methods via ref or callback
const StateUpdater = ({ onReady }) => {
  const health = useStreamHealth();
  React.useEffect(() => {
    onReady(health);
  }, [health, onReady]);
  return <div data-testid="state">{JSON.stringify(health)}</div>;
};

const TestConsumer = () => {
  const health = useStreamHealth();
  return (
    <div data-testid="state">
      {JSON.stringify(health)}
    </div>
  );
};

describe('StreamHealthContext', () => {
  it('provides initial state', () => {
    const { getByTestId } = render(
      <StreamHealthProvider>
        <TestConsumer />
      </StreamHealthProvider>
    );
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.connectionState).toBe(ConnectionState.IDLE);
    expect(state.retryCount).toBe(0);
    expect(state.error).toBeNull();
  });

  it('updates connection state', async () => {
    let healthRef;
    const { getByTestId, rerender } = render(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    await act(async () => {
      healthRef.updateConnectionState(ConnectionState.CONNECTING);
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.connectionState).toBe(ConnectionState.CONNECTING);
  });

  it('increments retry count', async () => {
    let healthRef;
    const { getByTestId, rerender } = render(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    await act(async () => {
      healthRef.incrementRetry();
      healthRef.incrementRetry();
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.retryCount).toBe(2);
  });

  it('resets retry count', async () => {
    let healthRef;
    const { getByTestId, rerender } = render(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    await act(async () => {
      healthRef.incrementRetry();
      healthRef.incrementRetry();
      healthRef.resetRetry();
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.retryCount).toBe(0);
  });

  it('sets and clears error', async () => {
    let healthRef;
    const { getByTestId, rerender } = render(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    await act(async () => {
      healthRef.setError('Test error');
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    let state = JSON.parse(getByTestId('state').textContent);
    expect(state.error).toBe('Test error');
    
    await act(async () => {
      healthRef.clearError();
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    state = JSON.parse(getByTestId('state').textContent);
    expect(state.error).toBeNull();
  });

  it('updates health metrics', async () => {
    let healthRef;
    const { getByTestId, rerender } = render(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    await act(async () => {
      healthRef.updateHealthMetrics({ packetLoss: 5, roundTripTime: 200 });
    });
    
    rerender(
      <StreamHealthProvider>
        <StateUpdater onReady={(h) => { healthRef = h; }} />
      </StreamHealthProvider>
    );
    
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.healthMetrics.packetLoss).toBe(5);
    expect(state.healthMetrics.roundTripTime).toBe(200);
  });

  it('throws error when used outside provider', () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    expect(() => {
      render(<TestConsumer />);
    }).toThrow('useStreamHealth must be used within StreamHealthProvider');
    
    consoleError.mockRestore();
  });
});
