import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Violation } from '../types';

export function ViolationsPage() {
  const [eventType, setEventType] = useState('ORDER_CREATED');
  const [violations, setViolations] = useState<Violation[]>([]);
  const [summary, setSummary] = useState<Record<string, number>>({});
  const [error, setError] = useState<string | null>(null);

  const refresh = () => {
    setError(null);
    api.violations(eventType).then(setViolations).catch(e => setError(String(e)));
    api.violationSummary(eventType).then(setSummary).catch(() => undefined);
  };

  useEffect(refresh, [eventType]);

  return (
    <>
      <h1>Violations</h1>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input value={eventType} onChange={e => setEventType(e.target.value)} />
        <button onClick={refresh}>Refresh</button>
      </div>

      <div className="cards" style={{ marginBottom: 24 }}>
        {Object.entries(summary).map(([k, v]) => (
          <div className="card" key={k}>
            <h3>{k.replace(/_/g, ' ')}</h3>
            <div className="value">{v}</div>
          </div>
        ))}
      </div>

      {error && <p style={{ color: '#ff8080' }}>{error}</p>}

      <table>
        <thead>
          <tr><th>Event ID</th><th>Type</th><th>Field</th><th>Message</th><th>Timestamp</th></tr>
        </thead>
        <tbody>
          {violations.map(v => (
            <tr key={v.id}>
              <td>{v.eventId}</td>
              <td>{v.violationType}</td>
              <td>{v.field}</td>
              <td>{v.message}</td>
              <td>{v.timestamp}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}