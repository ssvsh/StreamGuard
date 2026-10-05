import { useEffect, useState } from 'react';
import { api } from '../api';

export function OverviewPage() {
  const [score, setScore] = useState<number | null>(null);
  const [totalViolations, setTotalViolations] = useState<number>(0);
  const [eventType, setEventType] = useState('ORDER_CREATED');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    api.qualityScore(eventType)
      .then(res => {
        setScore(res.qualityScore);
        setTotalViolations(res.violations);
      })
      .catch(err => setError(String(err)));
  }, [eventType]);

  return (
    <>
      <h1>StreamGuard Overview</h1>
      <div style={{ marginBottom: 16 }}>
        <label>Event type: </label>
        <input value={eventType} onChange={e => setEventType(e.target.value)} />
      </div>
      {error && <p style={{ color: '#ff8080' }}>{error}</p>}
      <div className="cards">
        <div className="card">
          <h3>Data Quality Score</h3>
          <div className="value">{score === null ? '--' : `${score}%`}</div>
        </div>
        <div className="card">
          <h3>Total Violations</h3>
          <div className="value">{totalViolations}</div>
        </div>
        <div className="card">
          <h3>Prometheus</h3>
          <div><a href="http://localhost:9090" target="_blank" rel="noreferrer">localhost:9090</a></div>
        </div>
        <div className="card">
          <h3>Grafana</h3>
          <div><a href="http://localhost:3000" target="_blank" rel="noreferrer">localhost:3000</a></div>
        </div>
      </div>
    </>
  );
}