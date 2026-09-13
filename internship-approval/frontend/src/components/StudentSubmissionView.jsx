import React, { useState, useEffect } from 'react';
import { Upload, FileCheck, AlertTriangle, ShieldCheck, Download, Loader2, LogIn } from 'lucide-react';
import { api } from '../services/api';

export function StudentSubmissionView({ currentUser, onOpenAuth }) {
  const [formData, setFormData] = useState({
    companyName: '',
    roleTitle: '',
    durationWeeks: '12',
    stipend: '15000',
    startDate: '2026-06-01',
    endDate: '2026-08-31',
    mode: 'ON_SITE',
    hrContactEmail: '',
  });

  const [file, setFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);
  const [error, setError] = useState(null);

  const [submissionsList, setSubmissionsList] = useState([]);

  useEffect(() => {
    if (currentUser?.id) {
      loadMySubmissions();
    }
  }, [currentUser]);

  const loadMySubmissions = async () => {
    try {
      if (!currentUser?.id) return;
      const res = await fetch(`/api/internships/student/${currentUser.id}`, {
        headers: localStorage.getItem('internship_token')
          ? { 'Authorization': `Bearer ${localStorage.getItem('internship_token')}` }
          : {}
      });
      if (res.ok) {
        const list = await res.json();
        setSubmissionsList(list);
      }
    } catch (e) {
      console.error('Failed to load student submissions', e);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!currentUser) {
      if (onOpenAuth) onOpenAuth();
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSubmissionResult(null);

    try {
      const data = new FormData();
      const dtoBlob = new Blob([JSON.stringify({
        studentId: currentUser.id,
        companyName: formData.companyName,
        roleTitle: formData.roleTitle,
        durationWeeks: parseInt(formData.durationWeeks, 10),
        stipend: parseFloat(formData.stipend),
        startDate: formData.startDate,
        endDate: formData.endDate,
        mode: formData.mode,
        hrContactEmail: formData.hrContactEmail,
      })], { type: 'application/json' });

      data.append('dto', dtoBlob);
      if (file) {
        data.append('file', file);
      }

      const res = await api.submitOffer(data);
      setSubmissionResult(res);
      setSubmissionsList((prev) => [res, ...prev]);

      // Reset form
      setFormData((prev) => ({
        ...prev,
        companyName: '',
        roleTitle: '',
        hrContactEmail: '',
      }));
      setFile(null);
    } catch (err) {
      setError(err.message || 'Submission failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'MANUALLY_APPROVED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-[var(--success-bg)] text-[var(--success)]">MANUALLY APPROVED</span>;
      case 'MANUALLY_REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-[var(--danger-bg)] text-[var(--danger)]">REJECTED BY CDPC</span>;
      case 'FLAGGED_FOR_REVIEW':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800">FLAGGED FOR CDPC REVIEW</span>;
      case 'RULE_APPROVED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">RULE PASSED (PENDING CDPC ACTION)</span>;
      case 'RULE_REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800">RULE FAILED (PENDING CDPC ACTION)</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Title & Introduction */}
      <div>
        <h2 className="text-2xl font-bold font-heading text-[var(--text-primary)]">
          Submit Internship Offer Letter
        </h2>
        <p className="text-slate-600 text-sm mt-1">
          Upload your internship offer letter for mandatory CDPC verification, policy compliance check, and NOC issuance.
        </p>
      </div>

      {/* Submission Form */}
      <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 bg-slate-50 border-b border-slate-200">
          <h3 className="font-bold font-heading text-slate-800">Offer Letter Details</h3>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm font-medium flex items-center space-x-2">
              <AlertTriangle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Company Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="companyName"
                required
                value={formData.companyName}
                onChange={handleChange}
                placeholder="e.g. Acme Technologies"
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Role Title <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="roleTitle"
                required
                value={formData.roleTitle}
                onChange={handleChange}
                placeholder="e.g. Software Engineer Intern"
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Duration (Weeks) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="durationWeeks"
                required
                min="1"
                value={formData.durationWeeks}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Monthly Stipend (₹) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="stipend"
                required
                min="0"
                value={formData.stipend}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                HR Contact Email <span className="text-red-500">*</span>
              </label>
              <input
                type="email"
                name="hrContactEmail"
                required
                value={formData.hrContactEmail}
                onChange={handleChange}
                placeholder="e.g. hr@acme.com"
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Internship Mode <span className="text-red-500">*</span>
              </label>
              <select
                name="mode"
                value={formData.mode}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none bg-white"
              >
                <option value="ON_SITE">On-Site</option>
                <option value="HYBRID">Hybrid</option>
                <option value="REMOTE">Remote</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                Start Date
              </label>
              <input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                End Date
              </label>
              <input
                type="date"
                name="endDate"
                value={formData.endDate}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] focus:border-[var(--brand)] outline-none"
              />
            </div>
          </div>

          {/* Offer Letter File Upload */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
              Upload Offer Letter PDF (SHA-256 Verified) <span className="text-red-500">*</span>
            </label>
            <div className="border-2 border-dashed border-slate-300 hover:border-[var(--brand)] rounded-xl p-6 text-center bg-slate-50 hover:bg-slate-100/50 transition-colors cursor-pointer relative">
              <input
                type="file"
                accept=".pdf"
                required
                onChange={handleFileChange}
                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
              <div className="flex flex-col items-center justify-center space-y-2">
                <Upload className="w-8 h-8 text-[var(--brand)]" />
                <p className="text-sm font-medium text-slate-700">
                  {file ? file.name : 'Click or drag PDF offer letter here'}
                </p>
                <p className="text-xs text-slate-500">PDF up to 10MB</p>
              </div>
            </div>
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={isSubmitting || !file}
              className="px-6 py-3 bg-[var(--brand)] hover:bg-[var(--brand-hover)] text-white font-semibold rounded-lg shadow-md disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center space-x-2"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  <span>Evaluating Offer & Scam Signals...</span>
                </>
              ) : (
                <>
                  <FileCheck className="w-5 h-5" />
                  <span>Submit Offer for Approval</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* Submission Result Feedback */}
      {submissionResult && (
        <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6 space-y-4 animate-in fade-in duration-300">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <ShieldCheck className="w-6 h-6 text-[var(--brand)]" />
              <h3 className="text-lg font-bold font-heading text-slate-800">
                Offer Submission #{submissionResult.id} Created
              </h3>
            </div>
            {renderStatusBadge(submissionResult.status)}
          </div>

          {/* Rule Evaluations Summary */}
          {submissionResult.ruleEvaluations && submissionResult.ruleEvaluations.length > 0 && (
            <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600">Policy Criteria Checks</h4>
              <div className="space-y-1">
                {submissionResult.ruleEvaluations.map((rule, idx) => (
                  <div key={idx} className="flex items-center justify-between text-xs py-1">
                    <span className="font-medium text-slate-700">{rule.ruleName}</span>
                    <span className={rule.passed ? 'text-[var(--success)] font-bold' : 'text-[var(--danger)] font-bold'}>
                      {rule.passed ? 'PASSED' : `FAILED (${rule.failureReason})`}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Fraud Flags Summary */}
          {submissionResult.scamFlags && submissionResult.scamFlags.length > 0 && (
            <div className="bg-amber-50 p-4 rounded-lg border border-amber-200 text-amber-900 space-y-2">
              <div className="flex items-center space-x-2 text-amber-800 font-bold text-sm">
                <AlertTriangle className="w-4 h-4" />
                <span>Scam Pattern Flagged for CDPC Review</span>
              </div>
              <ul className="list-disc list-inside text-xs space-y-1 text-amber-800">
                {submissionResult.scamFlags.map((flag, idx) => (
                  <li key={idx}>
                    <strong>[{flag.flagType}]</strong>: {flag.detail}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Student Submissions List */}
      {submissionsList.length > 0 && (
        <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 bg-slate-50 border-b border-slate-200">
            <h3 className="font-bold font-heading text-slate-800">My Submitted Applications</h3>
          </div>
          <div className="divide-y divide-slate-200">
            {submissionsList.map((item) => (
              <div key={item.id} className="p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-3">
                    <h4 className="font-bold text-base text-slate-900">{item.companyName}</h4>
                    {renderStatusBadge(item.status)}
                  </div>
                  <p className="text-sm text-slate-600">
                    {item.roleTitle} • {item.durationWeeks} Weeks • ₹{item.stipend}/mo
                  </p>
                  <p className="text-xs text-slate-400">HR: {item.hrContactEmail}</p>
                </div>

                {item.status === 'MANUALLY_APPROVED' && (
                  <a
                    href={`http://localhost:8081/api/internships/${item.id}/noc`}
                    target="_blank"
                    rel="noreferrer"
                    className="px-4 py-2 bg-[var(--success)] hover:bg-emerald-700 text-white font-semibold text-xs rounded-lg shadow-sm transition-colors flex items-center space-x-2 self-start md:self-auto"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Official NOC PDF</span>
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
