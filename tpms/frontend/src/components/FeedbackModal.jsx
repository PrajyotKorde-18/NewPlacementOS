import React, { useState, useEffect } from 'react';
import { X, Sparkles, AlertCircle, CheckCircle2, Award, Info, FileText } from 'lucide-react';
import { api } from '../api';

export default function FeedbackModal({ applicationId, isOpen, onClose }) {
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen && applicationId) {
      loadFeedback();
    }
  }, [isOpen, applicationId]);

  const loadFeedback = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getApplicationFeedback(applicationId);
      setFeedback(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch candidate feedback');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full p-6 relative border border-slate-100 animate-in fade-in zoom-in duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center space-x-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-teal-100 text-[#0E6E76] flex items-center justify-center font-bold">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-xl font-bold font-heading text-slate-900">Deterministic Candidate Feedback</h3>
            <p className="text-xs text-slate-500">Per-role relevance & growth recommendations</p>
          </div>
        </div>

        {loading && (
          <div className="py-12 text-center">
            <div className="w-8 h-8 border-3 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            <p className="text-xs text-slate-500">Generating candidate feedback...</p>
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs">
            {error}
          </div>
        )}

        {!loading && feedback && (
          <div className="space-y-4 text-xs">
            {/* Header info */}
            <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 flex justify-between items-center">
              <div>
                <div className="font-bold text-slate-900 text-sm">{feedback.companyName} — {feedback.roleTitle}</div>
                <div className="text-slate-500 font-mono text-[11px]">Candidate: {feedback.studentName}</div>
              </div>
              <span className={`px-2.5 py-1 rounded-full font-bold text-[10px] ${
                feedback.status === 'SHORTLISTED' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-700'
              }`}>
                {feedback.status}
              </span>
            </div>

            {/* Relatively Weakest Category Callout */}
            {feedback.relativelyWeakestCategory && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start space-x-2 text-amber-900">
                <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold">Primary Growth Focus (Relatively Weakest Area)</div>
                  <div className="font-mono text-amber-800 font-semibold mt-0.5">
                    {feedback.relativelyWeakestCategory}
                  </div>
                </div>
              </div>
            )}

            {/* Missing MUST_HAVE skills */}
            {feedback.missingMustHaveSkills && feedback.missingMustHaveSkills.length > 0 && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl space-y-1 text-red-800">
                <div className="font-bold flex items-center space-x-1">
                  <AlertCircle className="w-4 h-4 text-red-600" />
                  <span>Missing Essential MUST_HAVE Skills</span>
                </div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {feedback.missingMustHaveSkills.map((skill, idx) => (
                    <span key={idx} className="px-2 py-0.5 bg-red-100 font-mono text-[10px] font-semibold text-red-700 rounded">
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Deterministic Summary text */}
            <div className="bg-white p-3 border border-slate-200 rounded-xl space-y-1">
              <div className="font-bold text-slate-700 text-[11px] uppercase font-mono tracking-wider">Structured Feedback Summary</div>
              <pre className="text-slate-600 font-sans text-xs whitespace-pre-wrap leading-relaxed">
                {feedback.feedbackSummary}
              </pre>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={onClose}
                className="px-4 py-2 bg-[#0E6E76] text-white font-semibold text-xs rounded-xl hover:bg-[#0B575D]"
              >
                Close Feedback
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
