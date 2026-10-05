import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import type { Contract, SchemaAnalysis } from '../types';

export function ContractsPage() {
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [eventType, setEventType] = useState<string>('');
  const [from, setFrom] = useState<number | ''>('');
  const [to, setTo] = useState<number | ''>('');
  const [analysis, setAnalysis] = useState<SchemaAnalysis | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.allContracts().then(setContracts).catch(e => setError(String(e)));
  }, []);

  const eventTypes = useMemo(() => Array.from(new Set(contracts.map(c => c.eventType))), [contracts]);
  const versions = useMemo(() =>
    contracts.filter(c => c.eventType === eventType).map(c => c.version).sort((a, b) => a - b),
    [contracts, eventType]);

  const compare = async () => {
    if (!eventType || from === '' || to === '') return;
    setError(null);
    try {
      setAnalysis(await api.compare(eventType, from as number, to as number));
    } catch (e) {
      setError(String(e));
    }
  };

  return (
    <>
      <h1>Contracts</h1>
      <table>
        <thead>
          <tr><th>Event Type</th><th>Version</th><th>Fields</th><th>Rules</th><th>Created</th></tr>
        </thead>
        <tbody>
          {contracts.map(c => (
            <tr key={c.id}>
              <td>{c.eventType}</td>
              <td>v{c.version}</td>
              <td>{Object.keys(c.fields).length}</td>
              <td>{c.qualityRules?.length ?? 0}</td>
              <td>{c.createdAt}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 style={{ marginTop: 32 }}>Compare Versions</h2>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 16 }}>
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
        <button onClick={compare}>Compare</button>
      </div>

      {error && <p style={{ color: '#ff8080' }}>{error}</p>}

      {analysis && (
        <div className="card">
          <h3>
            {analysis.eventType} v{analysis.oldVersion} &rarr; v{analysis.newVersion}{' '}
            <span className={`badge ${badgeClass(analysis.compatibility)}`}>{analysis.compatibility}</span>
          </h3>
          <table>
            <thead><tr><th>Field</th><th>Change</th><th>Old</th><th>New</th></tr></thead>
            <tbody>
              {analysis.changes.map((c, i) => (
                <tr key={i}>
                  <td>{c.field}</td>
                  <td>{c.type}</td>
                  <td>{c.oldType ?? '--'} {c.oldRequired !== null ? (c.oldRequired ? '(req)' : '(opt)') : ''}</td>
                  <td>{c.newType ?? '--'} {c.newRequired !== null ? (c.newRequired ? '(req)' : '(opt)') : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function badgeClass(c: string) {
  if (c === 'COMPATIBLE') return 'compatible';
  if (c === 'BREAKING') return 'breaking';
  return 'potentially';
}