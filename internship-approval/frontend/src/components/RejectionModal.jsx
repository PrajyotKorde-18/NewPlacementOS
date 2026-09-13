import React, { useState } from 'react';
import { AlertCircle, X } from 'lucide-react';

export function RejectionModal({ isOpen, onClose, onConfirm, requestId, companyName }) {
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) {
      setError('A valid rejection reason is mandatory before finalizing rejection.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await onConfirm(requestId, reason.trim());
      setReason('');
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to reject request');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-lg w-full overflow-hidden border border-slate-200 animate-in fade-in zoom-in duration-200">
        <div className="flex justify-between items-center px-6 py-4 border-b border-slate-100 bg-slate-50">
          <div className="flex items-center space-x-2 text-[var(--danger)]">
            <AlertCircle className="w-5 h-5" />
            <h3 className="font-bold font-heading text-lg">Reject Internship Request</h3>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 rounded-lg p-1 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <p className="text-sm text-slate-600">
            Rejecting submission for request <strong className="text-slate-800">#{requestId}</strong> ({companyName}).
            Please provide a mandatory, clear reason for the student and CDPC audit logs.
          </p>

          {error && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm font-medium">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
              Rejection Reason <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={4}
              required
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. Stipend below mandatory university criteria threshold, or unverified HR contact domain..."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !reason.trim()}
              className="px-4 py-2 text-sm font-semibold text-white bg-[var(--danger)] hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-sm transition-colors flex items-center space-x-2"
            >
              {isSubmitting ? 'Finalizing Rejection...' : 'Confirm Rejection'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
