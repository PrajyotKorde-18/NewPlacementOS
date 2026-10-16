import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export function NocVerificationPage({ onBackToLogin }) {
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState(null);
  const [manualToken, setManualToken] = useState('');

  useEffect(() => {
    // Extract token from query param ?token=...
    const searchParams = new URLSearchParams(window.location.search);
    const urlToken = searchParams.get('token');

    if (urlToken && urlToken.trim()) {
      setToken(urlToken.trim());
      setManualToken(urlToken.trim());
      performVerification(urlToken.trim());
    } else {
      setLoading(false);
      setResult({
        valid: false,
        message: 'No verification token provided in the URL. Please enter a valid 32-character NOC token below.',
      });
    }
  }, []);

  const performVerification = async (tokenToVerify) => {
    setLoading(true);
    try {
      const data = await api.verifyNocToken(tokenToVerify);
      setResult(data);
    } catch (err) {
      setResult({
        valid: false,
        message: err.message || 'Unable to connect to the verification server. Please try again later.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleManualSubmit = (e) => {
    e.preventDefault();
    if (manualToken.trim()) {
      setToken(manualToken.trim());
      performVerification(manualToken.trim());
    }
  };

  const handleGoHome = () => {
    if (onBackToLogin) {
      onBackToLogin();
    } else {
      window.location.href = '/';
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans antialiased">
      {/* Top Banner */}
      <header className="border-b border-slate-800 bg-slate-950/70 backdrop-blur px-6 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-3 cursor-pointer" onClick={handleGoHome}>
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-emerald-500 flex items-center justify-center font-bold text-white shadow-lg shadow-indigo-500/20">
            OS
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
              PlacementOS
              <span className="text-[10px] uppercase tracking-wider font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                Official Verifier
              </span>
            </h1>
            <p className="text-xs text-slate-400">Ramdeobaba University • CDPC Gatekeeping System</p>
          </div>
        </div>

        <button
          onClick={handleGoHome}
          className="text-xs font-semibold px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
        >
          Sign In to Portal
        </button>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-12 flex flex-col items-center justify-center">
        {loading ? (
          <div className="w-full bg-slate-850 border border-slate-800 rounded-2xl p-10 text-center shadow-2xl flex flex-col items-center">
            <div className="w-12 h-12 border-4 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin mb-4" />
            <h2 className="text-xl font-bold text-white mb-2">Validating NOC Cryptographic Signature</h2>
            <p className="text-sm text-slate-400 max-w-md">
              Contacting institutional verification server to validate the HMAC-SHA256 signature and certificate records...
            </p>
          </div>
        ) : result?.valid ? (
          /* GREEN VERIFIED CARD */
          <div className="w-full bg-slate-850 border border-emerald-500/40 rounded-2xl p-8 sm:p-10 shadow-2xl shadow-emerald-950/30 relative overflow-hidden">
            {/* Top Green Accent Bar */}
            <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-600" />

            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-800/80">
              <div className="flex items-center space-x-4">
                <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <div>
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    Verified Official Document
                  </div>
                  <h2 className="text-2xl font-black text-white tracking-tight">NOC Certificate Confirmed</h2>
                </div>
              </div>

              <div className="text-right sm:text-right">
                <span className="text-[11px] font-mono text-slate-400 block uppercase">Verification Status</span>
                <span className="text-sm font-bold text-emerald-400">AUTHENTIC &amp; ACTIVE</span>
              </div>
            </div>

            {/* Verified Details Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 my-8">
              <div className="bg-slate-900/60 rounded-xl p-4 border border-slate-800">
                <span className="text-xs uppercase font-semibold text-slate-400 tracking-wider block mb-1">
                  Candidate Name (Masked)
                </span>
                <span className="text-lg font-bold text-white tracking-wide">
                  {result.studentName || 'A*** J***'}
                </span>
              </div>

              <div className="bg-slate-900/60 rounded-xl p-4 border border-slate-800">
                <span className="text-xs uppercase font-semibold text-slate-400 tracking-wider block mb-1">
                  Approved Organization
                </span>
                <span className="text-lg font-bold text-white">
                  {result.companyName}
                </span>
              </div>

              <div className="bg-slate-900/60 rounded-xl p-4 border border-slate-800">
                <span className="text-xs uppercase font-semibold text-slate-400 tracking-wider block mb-1">
                  Internship Role
                </span>
                <span className="text-base font-semibold text-slate-200">
                  {result.roleTitle}
                </span>
              </div>

              <div className="bg-slate-900/60 rounded-xl p-4 border border-slate-800">
                <span className="text-xs uppercase font-semibold text-slate-400 tracking-wider block mb-1">
                  Institutional Approval Date
                </span>
                <span className="text-base font-semibold text-slate-200">
                  {result.approvalDate}
                </span>
              </div>
            </div>

            {/* Cryptographic Assurance Footer */}
            <div className="bg-emerald-950/20 border border-emerald-500/20 rounded-xl p-4 text-xs text-emerald-300 flex items-start space-x-3">
              <svg className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              <div>
                <p className="font-semibold text-emerald-200">Cryptographic HMAC-SHA256 Assurance</p>
                <p className="text-emerald-400/90 mt-0.5">
                  This No Objection Certificate was verified directly against the Ramdeobaba University Career Development &amp; Placement Cell ledger.
                  Institutional privacy rules restrict the display of student registration IDs, emails, and internal reviewer signatures.
                </p>
              </div>
            </div>
          </div>
        ) : (
          /* RED CANNOT BE VERIFIED CARD */
          <div className="w-full bg-slate-850 border border-rose-500/40 rounded-2xl p-8 sm:p-10 shadow-2xl shadow-rose-950/30 relative overflow-hidden">
            {/* Top Red Accent Bar */}
            <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-rose-500 via-red-500 to-rose-600" />

            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-800/80">
              <div className="flex items-center space-x-4">
                <div className="w-14 h-14 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-400">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </div>
                <div>
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-rose-500/10 text-rose-400 border border-rose-500/20 mb-1">
                    Verification Failed
                  </div>
                  <h2 className="text-2xl font-black text-white tracking-tight">NOC Cannot Be Verified</h2>
                </div>
              </div>

              <div className="text-right sm:text-right">
                <span className="text-[11px] font-mono text-slate-400 block uppercase">Status</span>
                <span className="text-sm font-bold text-rose-400">UNRECOGNIZED / INVALID</span>
              </div>
            </div>

            <div className="my-6 bg-slate-900/60 rounded-xl p-6 border border-slate-800">
              <p className="text-base font-medium text-rose-200 mb-2">
                {result?.message || 'The specified NOC verification token does not match any official approved record in our institutional registry.'}
              </p>
              <p className="text-sm text-slate-400 leading-relaxed">
                If you are an employer or third party reviewing this document, the presented certificate may be unauthorized, expired, or tampered with. Please ask the applicant to furnish an authentic NOC with a valid cryptographic QR code.
              </p>
            </div>

            {/* Manual Token Lookup Box */}
            <form onSubmit={handleManualSubmit} className="mt-6 pt-6 border-t border-slate-800">
              <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                Have a different NOC Token?
              </label>
              <div className="flex flex-col sm:flex-row gap-3">
                <input
                  type="text"
                  placeholder="Enter 32-character hexadecimal token..."
                  value={manualToken}
                  onChange={(e) => setManualToken(e.target.value)}
                  className="flex-1 bg-slate-900 border border-slate-700 rounded-lg px-4 py-2.5 text-sm font-mono text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition"
                />
                <button
                  type="submit"
                  className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm rounded-lg transition shadow-lg shadow-indigo-600/20"
                >
                  Verify Token
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Back navigation */}
        <div className="mt-8 text-center">
          <button
            onClick={handleGoHome}
            className="text-sm text-slate-400 hover:text-white transition flex items-center gap-1.5 mx-auto"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Return to PlacementOS Portal
          </button>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-6 text-center text-xs text-slate-500">
        <p className="max-w-7xl mx-auto px-4">
          PlacementOS — System 2: CDPC Gatekeeping &amp; Internship Verification Authority • RKNEC
        </p>
      </footer>
    </div>
  );
}

export default NocVerificationPage;
