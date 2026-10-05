import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Contract, MigrationRecommendation, SchemaAnalysis } from '../types';

export function RecommendationPage() {
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [eventType, setEventType] = useState<string>('');
  const [from, setFrom] = useState<number | ''>('');
  const [to, setTo] = useState<number | ''>('');
  const [analysis, setAnalysis] = useState<SchemaAnalysis | null>(null);
  const [recommendation, setRecommendation] = useState<MigrationRecommendation | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.allContracts().then(setContracts).catch(e => setError(String(e)));
  }, []);

  const eventTypes = Array.from(new Set(contracts.map(c => c.eventType)));
  const versions = contracts.filter(c => c.eventType === eventType).map(c => c.version).sort((a, b) => a - b);

  const run = async () => {
    if (!eventType || from === '' || to === '') return;
    setLoading(true); setError(null); setRecommendation(null);
    try {
      const a = await api.compare(eventType, from as number, to as number);
      setAnalysis(a);
      setRecommendation(await api.recommend(a));
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <h1>AI Migration Recommendation</h1>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <select value={eventType} onChange={e => setEventType(e.target.value)}>
          <option value="">Select event type</option>
          {eventTypes.map(e => <option key={e} value={e}>{e}</option>)}
        </select>
        <select value={from} onChange={e => setFrom(Number(e.target.value) || '')}>
          <option value="">From</option>
          {versions.map(v => <option key={v} value={v}>v{v}</option>)}
        </select>
        <select value={to} onChange={e => setTo(Number(e.target.value) || '')}>
          <option value="">To</option>
          {versions.map(v => <option key={v} value={v}>v{v}</option>)}
        </select>
        <button onClick={run} disabled={loading}>{loading ? 'Thinking...' : 'Recommend'}</button>
      </div>

      {error && <p style={{ color: '#ff8080' }}>{error}</p>}

      {analysis && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3>Deterministic verdict</h3>
          <p>{analysis.eventType} v{analysis.oldVersion} &rarr; v{analysis.newVersion}: <strong>{analysis.compatibility}</strong></p>
        </div>
      )}

      {recommendation && (
        <div className="card">
          <h3>Severity: {recommendation.severity}</h3>
          <p>{recommendation.summary}</p>
          <Section title="Migration Strategy" items={recommendation.migrationStrategy} />
          <Section title="Rollback Strategy" items={recommendation.rollbackStrategy} />
          <Section title="Risks" items={recommendation.risks} />
        </div>
      )}
    </>
  );
}

function Section({ title, items }: { title: string; items: string[] }) {
  if (!items?.length) return null;
  return (
    <>
      <h4>{title}</h4>
      <ol>{items.map((s, i) => <li key={i}>{s}</li>)}</ol>
    </>
  );
}