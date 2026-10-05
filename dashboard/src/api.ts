import type {
  Contract, SchemaAnalysis, Violation, MigrationRecommendation,
} from './types';

const BASE = '/api';

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

export const api = {
  allContracts: () => get<Contract[]>('/contracts'),
  contractVersions: (eventType: string) => get<Contract[]>(`/contracts/${eventType}`),
  compare: (eventType: string, from: number, to: number) =>
    get<SchemaAnalysis>(`/contracts/${eventType}/compare?from=${from}&to=${to}`),
  violations: (eventType: string, limit = 100) =>
    get<Violation[]>(`/violations?eventType=${eventType}&limit=${limit}`),
  violationSummary: (eventType: string) =>
    get<Record<string, number>>(`/violations/summary?eventType=${eventType}`),
  qualityScore: (eventType: string, total = 0) =>
    get<{ eventType: string; totalEvents: number; violations: number; qualityScore: number }>(
      `/quality/${eventType}?processedTotal=${total}`),
  recommend: (analysis: SchemaAnalysis) =>
    post<MigrationRecommendation>('/recommendations', analysis),
};