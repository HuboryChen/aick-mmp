import axios from 'axios';

const API_BASE = '/api/v1/ai';

export interface PassengerStats {
  id: number;
  cameraId: number;
  startTime: string;
  endTime: string;
  enterCount: number;
  exitCount: number;
  insideCount: number;
  maxInsideCount: number;
}

export interface BehaviorEvent {
  id: number;
  cameraId: number;
  eventType: 'LOITERING' | 'INTRUSION' | 'GATHERING' | 'FALL';
  level: 'INFO' | 'WARNING' | 'CRITICAL';
  positionData?: string;
  snapshotUrl?: string;
  description?: string;
  eventTime: string;
  status: 'UNRESOLVED' | 'ACKNOWLEDGED' | 'RESOLVED';
}

export interface VehicleRecord {
  id: number;
  cameraId: number;
  plateNumber: string;
  plateColor?: string;
  confidence?: number;
  snapshotUrl?: string;
  isWhitelisted: boolean;
  detectTime: string;
}

export interface WhitelistEntry {
  id?: number;
  plateNumber: string;
  plateColor?: string;
  ownerName?: string;
  description?: string;
  enabled: boolean;
}

export const aiApi = {
  // Passenger
  getPassengerStats: (cameraId: number, startTime: string, endTime: string) =>
    axios.get<PassengerStats[]>(`${API_BASE}/stats/passenger`, { params: { cameraId, startTime, endTime } }),

  getRealtimePassenger: (cameraId: number) =>
    axios.get<string>(`${API_BASE}/stats/passenger/realtime/${cameraId}`),

  // Behavior
  getBehaviorEvents: (cameraId: number, eventType?: string, status?: string) =>
    axios.get<BehaviorEvent[]>(`${API_BASE}/alerts/behavior`, { params: { cameraId, eventType, status } }),

  updateBehaviorStatus: (id: number, status: string) =>
    axios.put<BehaviorEvent>(`${API_BASE}/alerts/behavior/${id}/status`, null, { params: { status } }),

  // Vehicle
  getVehicleRecords: (plateNumber?: string, cameraId?: number) =>
    axios.get<VehicleRecord[]>(`${API_BASE}/vehicles/records`, { params: { plateNumber, cameraId } }),

  // Whitelist
  getWhitelist: () =>
    axios.get<WhitelistEntry[]>(`${API_BASE}/vehicles/whitelist`),

  addWhitelist: (entry: WhitelistEntry) =>
    axios.post<WhitelistEntry>(`${API_BASE}/vehicles/whitelist`, entry),

  updateWhitelist: (id: number, entry: WhitelistEntry) =>
    axios.put<WhitelistEntry>(`${API_BASE}/vehicles/whitelist/${id}`, entry),

  deleteWhitelist: (id: number) =>
    axios.delete(`${API_BASE}/vehicles/whitelist/${id}`),
};
