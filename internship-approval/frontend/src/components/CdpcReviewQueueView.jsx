import React, { useState, useEffect } from 'react';
import { ShieldAlert, CheckCircle, XCircle, AlertTriangle, FileText, Loader2, RefreshCw, LogIn } from 'lucide-react';
import { api } from '../services/api';
import { RejectionModal } from './RejectionModal';

export function CdpcReviewQueueView({ currentUser, onOpenAuth }) {
  const [queue, setQueue] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('ALL');

  // Rejection Modal State
  const [rejectionModalState, setRejectionModalState] = useState({
    isOpen: false,
    requestId: null,
    companyName: '',
  });

  const [actionInProgress, setActionInProgress] = useState(null);

  const fetchQueue = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.getReviewQueue();
      setQueue(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch CDPC review queue');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, [currentUser]);

  const handleApprove = async (requestId) => {
    setActionInProgress(requestId);
    try {
      await api.approveRequest(requestId);
      await fetchQueue();
    } catch (err) {
      alert(err.message || 'Approval failed');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleRejectConfirm = async (requestId, reason) => {
    try {
      await api.rejectRequest(requestId, reason);
      await fetchQueue();
    } catch (err) {
      throw err;
    }
  };

  const openRejectModal = (requestId, companyName) => {
    setRejectionModalState({
      isOpen: true,
      requestId,
      companyName,
    });
  };

  const filteredQueue = queue.filter((item) => {
    if (filter === 'FLAGGED') return item.scamFlags && item.scamFlags.length > 0;
    if (filter === 'RULE_APPROVED') return item.status === 'RULE_APPROVED';
    if (filter === 'RULE_REJECTED') return item.status === 'RULE_REJECTED';
    return true;
  });

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'MANUALLY_APPROVED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-[var(--success-bg)] text-[var(--success)]">MANUALLY APPROVED</span>;
      case 'MANUALLY_REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-[var(--danger-bg)] text-[var(--danger)]">REJECTED BY CDPC</span>;
      case 'FLAGGED_FOR_REVIEW':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300">FLAGGED FOR CDPC REVIEW</span>;
      case 'RULE_APPROVED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-900 border border-emerald-300">RULE PASSED (UNFLAGGED)</span>;
      case 'RULE_REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-900 border border-rose-300">RULE FAILED</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Title Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold font-heading text-[var(--text-primary)] flex items-center space-x-3">
            <ShieldAlert className="w-7 h-7 text-[var(--brand)]" />
            <span>CDPC Gatekeeping Review Queue</span>
          </h2>
          <p className="text-slate-600 text-sm mt-1">
            Mandatory human verification dashboard. All non-terminal student submissions require explicit CDPC approval.
          </p>
        </div>

        <button
          onClick={fetchQueue}
          disabled={isLoading}
          className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg border border-slate-300 transition-colors flex items-center space-x-2 self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          <span>Refresh Queue</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setFilter('ALL')}
          className={`px-3 py-1.5 rounded-md text-xs font-bold transition-colors ${
            filter === 'ALL'
              ? 'bg-[var(--brand)] text-white'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          All Queue ({queue.length})
        </button>
        <button
          onClick={() => setFilter('FLAGGED')}
          className={`px-3 py-1.5 rounded-md text-xs font-bold transition-colors ${
            filter === 'FLAGGED'
              ? 'bg-amber-600 text-white'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Fraud Flagged ({queue.filter((i) => i.scamFlags && i.scamFlags.length > 0).length})
        </button>
        <button
          onClick={() => setFilter('RULE_APPROVED')}
          className={`px-3 py-1.5 rounded-md text-xs font-bold transition-colors ${
            filter === 'RULE_APPROVED'
              ? 'bg-emerald-600 text-white'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Rule Passed ({queue.filter((i) => i.status === 'RULE_APPROVED').length})
        </button>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm font-medium">
          {error}
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="py-16 text-center text-slate-500 flex flex-col items-center space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-[var(--brand)]" />
          <p className="text-sm font-medium">Fetching CDPC Review Queue...</p>
        </div>
      ) : filteredQueue.length === 0 ? (
        <div className="py-16 text-center bg-white rounded-xl border border-slate-200 p-8 text-slate-500">
          <FileText className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-bold text-slate-700 text-base">Review Queue Empty</h3>
          <p className="text-xs text-slate-500 mt-1">No pending student internship requests require action under this filter.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredQueue.map((item) => (
            <div
              key={item.id}
              className={`bg-white rounded-xl shadow-sm border p-6 transition-all ${
                item.scamFlags && item.scamFlags.length > 0
                  ? 'border-amber-300 ring-1 ring-amber-200'
                  : 'border-slate-200'
              }`}
            >
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                {/* Request Main Details */}
                <div className="space-y-3 flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="text-xs font-mono font-bold text-slate-400">REQ #{item.id}</span>
                    <h3 className="font-bold font-heading text-lg text-slate-900">{item.companyName}</h3>
                    {renderStatusBadge(item.status)}
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs bg-slate-50 p-3 rounded-lg border border-slate-100">
                    <div>
                      <span className="text-slate-400 block font-medium">Student</span>
                      <strong className="text-slate-800">{item.studentName || 'Student'} ({item.collegeId || 'N/A'})</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">Role</span>
                      <strong className="text-slate-800">{item.roleTitle}</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">Stipend & Duration</span>
                      <strong className="text-slate-800">₹{item.stipend}/mo • {item.durationWeeks} Wks</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">HR Email</span>
                      <strong className="text-slate-800">{item.hrContactEmail}</strong>
                    </div>
                  </div>

                  {/* Fraud Flags Visibly Surfaced */}
                  {item.scamFlags && item.scamFlags.length > 0 && (
                    <div className="bg-amber-50 border border-amber-300 rounded-lg p-4 space-y-2">
                      <div className="flex items-center space-x-2 text-amber-900 font-bold text-xs uppercase tracking-wider">
                        <AlertTriangle className="w-4 h-4 text-amber-600" />
                        <span>Scam Pattern Flag Detected ({item.scamFlags.length} Signals)</span>
                      </div>
                      <div className="space-y-2">
                        {item.scamFlags.map((flag, idx) => (
                          <div key={idx} className="text-xs text-amber-900 bg-white p-2.5 rounded border border-amber-200 shadow-2xs">
                            <div className="flex justify-between items-center font-bold text-amber-900 mb-0.5">
                              <span>SIGNAL: {flag.flagType}</span>
                              {flag.totalRelatedRequests > 1 && (
                                <span className="px-2 py-0.5 bg-amber-100 text-amber-800 rounded font-semibold text-[10px]">
                                  {flag.totalRelatedRequests} Related Requests Linked
                                </span>
                              )}
                            </div>
                            <p className="text-amber-800">{flag.detail}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                {/* Explicit Human Action Buttons */}
                <div className="flex flex-row lg:flex-col gap-2 min-w-[160px] justify-end">
                  <button
                    onClick={() => handleApprove(item.id)}
                    disabled={actionInProgress === item.id}
                    className="flex-1 lg:flex-none px-4 py-2.5 bg-[var(--brand)] hover:bg-[var(--brand-hover)] text-white text-xs font-bold rounded-lg shadow-sm transition-colors flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    {actionInProgress === item.id ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <CheckCircle className="w-4 h-4" />
                        <span>Approve & Generate NOC</span>
                      </>
                    )}
                  </button>

                  <button
                    onClick={() => openRejectModal(item.id, item.companyName)}
                    disabled={actionInProgress === item.id}
                    className="flex-1 lg:flex-none px-4 py-2.5 bg-[var(--danger-bg)] hover:bg-red-200 text-[var(--danger)] text-xs font-bold rounded-lg border border-red-200 transition-colors flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Reject Offer</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Rejection Modal */}
      <RejectionModal
        isOpen={rejectionModalState.isOpen}
        onClose={() => setRejectionModalState((prev) => ({ ...prev, isOpen: false }))}
        onConfirm={handleRejectConfirm}
        requestId={rejectionModalState.requestId}
        companyName={rejectionModalState.companyName}
      />
    </div>
  );
}
