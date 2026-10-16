import React, { useState, useEffect } from 'react';
import { ShieldCheck, CheckCircle2, XCircle, AlertTriangle, Building2, Briefcase, User, Loader2 } from 'lucide-react';
import { api } from '../services/api';

export function HrConfirmationPage({ onBackToLogin }) {
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [info, setInfo] = useState(null);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const urlToken = searchParams.get('token');

    if (urlToken && urlToken.trim()) {
      const cleanToken = urlToken.trim();
      setToken(cleanToken);
      fetchDetails(cleanToken);
    } else {
      setLoading(false);
      setInfo({
        valid: false,
        message: 'No verification token provided in the link. Please open the exact link sent to your HR email address.',
      });
    }
  }, []);

  const fetchDetails = async (tokenToVerify) => {
    setLoading(true);
    try {
      const data = await api.getHrConfirmationInfo(tokenToVerify);
      setInfo(data);
    } catch (err) {
      setInfo({
        valid: false,
        message: err.message || 'Unable to connect to the verification server. Please try again later.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async (answer) => {
    if (!token) return;
    setSubmitting(true);
    try {
      const res = await api.submitHrConfirmation(token, answer);
      setResult(res);
    } catch (err) {
      alert('Error submitting confirmation: ' + (err.message || 'Server error'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans antialiased">
      {/* Top Banner */}
      <header className="bg-slate-950/80 backdrop-blur-md border-b border-slate-800 sticky top-0 z-30">
        <div className="max-w-4xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-teal-600 flex items-center justify-center text-white shadow-md">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-sm font-bold tracking-tight text-white font-heading">PlacementOS</span>
                <span className="text-[10px] font-mono uppercase bg-teal-950 text-teal-400 border border-teal-800/80 px-1.5 py-0.5 rounded">
                  HR Verification Authority
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Ramdeobaba University • CDPC Gatekeeping System</p>
            </div>
          </div>
          {onBackToLogin && (
            <button
              onClick={onBackToLogin}
              className="text-xs font-semibold text-slate-400 hover:text-white transition-colors"
            >
              Portal Home
            </button>
          )}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-2xl w-full mx-auto px-4 py-12 flex flex-col justify-center">
        {loading ? (
          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-10 text-center space-y-4 shadow-xl backdrop-blur-sm">
            <Loader2 className="w-8 h-8 animate-spin text-teal-400 mx-auto" />
            <p className="text-sm text-slate-300">Retrieving offer confirmation details...</p>
          </div>
        ) : result ? (
          /* Post-submission Response Card */
          <div
            className={`border rounded-2xl p-8 sm:p-10 shadow-2xl space-y-6 ${
              result.status === 'CONFIRMED'
                ? 'bg-emerald-950/30 border-emerald-500/40'
                : 'bg-rose-950/30 border-rose-500/40'
            }`}
          >
            <div className="flex items-center space-x-4">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 ${
                  result.status === 'CONFIRMED'
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                }`}
              >
                {result.status === 'CONFIRMED' ? (
                  <CheckCircle2 className="w-8 h-8" />
                ) : (
                  <XCircle className="w-8 h-8" />
                )}
              </div>
              <div>
                <span
                  className={`text-xs font-mono uppercase tracking-wider font-bold ${
                    result.status === 'CONFIRMED' ? 'text-emerald-400' : 'text-rose-400'
                  }`}
                >
                  {result.status === 'CONFIRMED' ? 'Offer Verified' : 'Offer Disowned / Denied'}
                </span>
                <h2 className="text-xl font-bold text-white mt-0.5">
                  {result.status === 'CONFIRMED' ? 'Thank You for Confirming' : 'Response Successfully Logged'}
                </h2>
              </div>
            </div>

            <p className="text-sm text-slate-300 leading-relaxed">{result.message}</p>

            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 text-xs text-slate-400 space-y-1">
              <p>Your digital verification response has been securely archived in the institutional CDPC audit trail.</p>
              <p>You may now safely close this browser window.</p>
            </div>
          </div>
        ) : !info?.valid ? (
          /* Invalid / Expired / Used Link */
          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-8 sm:p-10 shadow-xl space-y-6">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs font-mono uppercase text-amber-400 font-bold tracking-wider">
                  Link Invalid or Expired
                </span>
                <h2 className="text-lg font-bold text-white">Confirmation Link Inactive</h2>
              </div>
            </div>

            <p className="text-sm text-slate-300 leading-relaxed">
              {info?.message ||
                'This confirmation link is invalid, has expired, or has already been used. Each link is single-use for security.'}
            </p>

            <p className="text-xs text-slate-400">
              If your organization already submitted a response, no further action is required. For inquiries, please contact
              the Career Development &amp; Placement Cell at Ramdeobaba University.
            </p>
          </div>
        ) : (
          /* Active Confirmation Card */
          <div className="bg-slate-800/90 border border-slate-700/90 rounded-2xl p-8 sm:p-10 shadow-2xl space-y-6">
            <div>
              <span className="text-xs font-mono uppercase text-teal-400 font-bold tracking-wider">
                Official Verification Request
              </span>
              <h2 className="text-xl font-bold text-white mt-1">Please Confirm This Internship Offer</h2>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                A student has submitted an internship offer letter claiming employment with your company. To protect students
                and ensure authentic placements, please verify if this offer was issued by your organization.
              </p>
            </div>

            {/* Offer Metadata Summary */}
            <div className="bg-slate-900/90 border border-slate-700/80 rounded-xl p-5 space-y-3">
              <div className="flex items-center space-x-3 text-sm">
                <User className="w-4 h-4 text-teal-400 shrink-0" />
                <span className="text-slate-400 text-xs w-28">Candidate:</span>
                <span className="font-semibold text-slate-100 font-mono">{info.studentMaskedName}</span>
              </div>
              <div className="flex items-center space-x-3 text-sm">
                <Building2 className="w-4 h-4 text-teal-400 shrink-0" />
                <span className="text-slate-400 text-xs w-28">Company:</span>
                <span className="font-semibold text-slate-100">{info.companyName}</span>
              </div>
              <div className="flex items-center space-x-3 text-sm">
                <Briefcase className="w-4 h-4 text-teal-400 shrink-0" />
                <span className="text-slate-400 text-xs w-28">Designation:</span>
                <span className="font-semibold text-slate-100">{info.roleTitle}</span>
              </div>
            </div>

            {/* Decision Buttons */}
            <div className="pt-2 space-y-3">
              <p className="text-xs font-semibold text-slate-300">Did your organization issue this internship offer?</p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => handleAction('YES')}
                  className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-lg transition-all flex items-center justify-center space-x-2 cursor-pointer"
                >
                  {submitting ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="w-4 h-4" />
                  )}
                  <span>Yes, we issued this offer</span>
                </button>

                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => handleAction('NO')}
                  className="w-full py-3 px-4 bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-lg transition-all flex items-center justify-center space-x-2 cursor-pointer"
                >
                  {submitting ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <XCircle className="w-4 h-4" />
                  )}
                  <span>No, this is not ours</span>
                </button>
              </div>
              <p className="text-[11px] text-slate-500 text-center">
                Clicking either button registers your response immediately and invalidates the link from further changes.
              </p>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800 py-4 text-center text-xs text-slate-500">
        PlacementOS — System 2: CDPC Gatekeeping &amp; Anti-Scam Authority • Ramdeobaba University
      </footer>
    </div>
  );
}
