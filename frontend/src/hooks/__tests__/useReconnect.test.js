import { calculateDelay, calculateAdaptiveQuality } from '../useReconnect';

describe('calculateDelay', () => {
  const config = {
    initialDelayMs: 1000,
    maxDelayMs: 30000,
    jitterFactor: 0.3,
  };

  it('returns initial delay for retryCount 0', () => {
    const delay = calculateDelay(0, config);
    // 1s base ± 30% jitter = 700-1300
    expect(delay).toBeGreaterThanOrEqual(700);
    expect(delay).toBeLessThanOrEqual(1300);
  });

  it('exponential backoff works correctly', () => {
    const delay1 = calculateDelay(1, config);
    const delay2 = calculateDelay(2, config);
    expect(delay2).toBeGreaterThan(delay1);
  });

  it('caps at maxDelayMs', () => {
    const delay = calculateDelay(10, config);
    expect(delay).toBeLessThanOrEqual(30000);
  });

  it('uses default config when not provided', () => {
    const delay = calculateDelay(0);
    expect(delay).toBeGreaterThanOrEqual(700);
    expect(delay).toBeLessThanOrEqual(1300);
  });
});

describe('calculateAdaptiveQuality', () => {
  it('returns current quality when network is excellent', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0, roundTripTime: 50 },
      '720p'
    );
    expect(result).toBe('720p');
  });

  it('returns current quality when packetLoss < 1%', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0.5, roundTripTime: 50 },
      '1080p'
    );
    expect(result).toBe('1080p');
  });

  it('degrades one level when RTT is moderate (100-300ms)', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0, roundTripTime: 200 },
      '720p'
    );
    expect(result).toBe('480p');
  });

  it('degrades one level when packetLoss is moderate (1-3%)', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 2, roundTripTime: 50 },
      '720p'
    );
    expect(result).toBe('480p');
  });

  it('degrades two levels when network is poor (high RTT)', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0, roundTripTime: 400 },
      '1080p'
    );
    expect(result).toBe('480p');
  });

  it('degrades two levels when packetLoss is high', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 5, roundTripTime: 50 },
      '720p'
    );
    expect(result).toBe('360p');
  });

  it('caps at minimum quality (360p)', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 10, roundTripTime: 500 },
      '480p'
    );
    expect(result).toBe('360p');
  });

  it('handles 360p as minimum gracefully', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 5, roundTripTime: 400 },
      '360p'
    );
    expect(result).toBe('360p');
  });

  it('handles 480p at minimum gracefully', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 5, roundTripTime: 400 },
      '480p'
    );
    expect(result).toBe('360p');
  });

  it('returns current quality for 1080p when only slight degradation needed', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0.5, roundTripTime: 80 },
      '1080p'
    );
    expect(result).toBe('1080p');
  });
});
