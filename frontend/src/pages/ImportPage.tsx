import { useState, useRef } from 'react';
import { useHouseholdId } from '../hooks/useFetch';
import { importApi } from '../api/client';
import { Section, LoadingSpinner } from '../components/Shared';

/**
 * Import Page — UX for CSV import with preview (FR-8.2, FR-8.4).
 * Flow: Upload → Preview → Commit.
 */
export default function ImportPage() {
  const householdId = useHouseholdId();
  const fileRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;

    setLoading(true);
    setMessage('');
    try {
      const result = await importApi.previewCsv(file, 'TRANSACTION', householdId);
      setPreview(result);
    } catch (e: any) {
      setMessage(`Error: ${e.message}`);
    }
    setLoading(false);
  };

  return (
    <div className="page">
      <h1>Data Import</h1>

      <Section title="CSV Import">
        <p>Upload a CSV file to import transactions. <a href="/api/import/csv/template/transaction">Download template</a></p>
        <div className="import-form">
          <input type="file" ref={fileRef} accept=".csv" />
          <button className="btn btn-primary" onClick={handleUpload} disabled={loading}>
            {loading ? 'Processing...' : 'Upload & Preview'}
          </button>
        </div>
      </Section>

      {message && <div className="import-message">{message}</div>}
      {loading && <LoadingSpinner />}

      {preview && (
        <>
          <Section title="Import Preview">
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-label">Total Rows</div>
                <div className="stat-value">{preview.totalRows}</div>
              </div>
              <div className="stat-card green">
                <div className="stat-label">Valid</div>
                <div className="stat-value">{preview.validRows}</div>
              </div>
              <div className="stat-card red">
                <div className="stat-label">Errors</div>
                <div className="stat-value">{preview.errorRows}</div>
              </div>
              <div className="stat-card yellow">
                <div className="stat-label">Duplicates</div>
                <div className="stat-value">{preview.duplicateRows}</div>
              </div>
            </div>
          </Section>

          {preview.validationErrors?.length > 0 && (
            <Section title="Validation Errors">
              <ul className="error-list">
                {preview.validationErrors.map((e: string, i: number) => (
                  <li key={i}>{e}</li>
                ))}
              </ul>
            </Section>
          )}

          {preview.duplicates?.length > 0 && (
            <Section title="Potential Duplicates">
              {preview.duplicates.map((d: any, i: number) => (
                <div key={i} className="duplicate-flag">
                  <strong>Row {d.rowNumber}:</strong> {d.message}
                </div>
              ))}
            </Section>
          )}
        </>
      )}
    </div>
  );
}
