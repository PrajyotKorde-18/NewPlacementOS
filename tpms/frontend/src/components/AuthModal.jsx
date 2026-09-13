import React, { useState } from 'react';
import { X, Lock, Mail, User, Shield, AlertCircle, Sparkles, GraduationCap, Building2, CheckCircle2, ArrowRight } from 'lucide-react';
import { api, setAuthToken, setCurrentUser } from '../api';

export default function AuthModal({ isOpen, onClose, onAuthSuccess }) {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [collegeId, setCollegeId] = useState('');
  const [role, setRole] = useState('STUDENT');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.toLowerCase().endsWith('@rknec.edu')) {
      setError('Campus Domain Restriction: Email must end with @rknec.edu');
      return;
    }

    setLoading(true);
    try {
      if (isRegister) {
        const payload = {
          name,
          email,
          password,
          collegeId: collegeId || (role === 'STUDENT' ? '2022CS099' : 'TPO-OFFICER-01'),
          role,
        };
        const response = await api.register(payload);
        setAuthToken(response.token);
        setCurrentUser(response);
        onAuthSuccess(response);
        onClose();
      } else {
        const response = await api.login(email, password);
        setAuthToken(response.token);
        setCurrentUser(response);
        onAuthSuccess(response);
        onClose();
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setError('');
    setLoading(true);
    try {
      const response = await api.login(demoEmail, demoPassword);
      setAuthToken(response.token);
      setCurrentUser(response);
      onAuthSuccess(response);
      onClose();
    } catch (err) {
      setError('Demo login failed: ' + (err.message || err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/70 backdrop-blur-md p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-100 relative">
        {/* Modal Decorative Top Header */}
        <div className="bg-gradient-to-r from-[#0E6E76] via-[#0D6269] to-[#0A4E53] p-6 text-white relative overflow-hidden">
          <div className="absolute right-0 top-0 translate-x-1/4 -translate-y-1/4 w-40 h-40 bg-white/10 rounded-full blur-2xl pointer-events-none" />
          <button
            onClick={onClose}
            className="absolute top-4 right-4 p-1.5 text-white/70 hover:text-white rounded-full bg-white/10 hover:bg-white/20 transition-all"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-2xl bg-white/15 backdrop-blur-md text-white font-bold text-2xl flex items-center justify-center shadow-inner border border-white/20">
              TP
            </div>
            <div>
              <div className="inline-flex items-center space-x-1.5 bg-emerald-400/20 text-teal-200 text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full backdrop-blur-md">
                <Shield className="w-3 h-3" />
                <span>RKNEC Verified Portal</span>
              </div>
              <h2 className="text-xl font-bold font-heading mt-0.5">
                Placement<span className="text-teal-200">OS</span> Authentication
              </h2>
            </div>
          </div>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          {/* Mode Switcher Tabs */}
          <div className="flex bg-slate-100 p-1 rounded-2xl">
            <button
              type="button"
              onClick={() => { setIsRegister(false); setError(''); }}
              className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                !isRegister ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => { setIsRegister(true); setError(''); }}
              className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                isRegister ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              Create Account
            </button>
          </div>

          {error && (
            <div className="p-3.5 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-2.5 text-red-700 text-xs animate-in fade-in">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Form Controls */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Full Name</label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type="text"
                      required
                      placeholder="e.g. John Doe"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full pl-10 pr-4 py-2.5 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      {role === 'STUDENT' ? 'College Roll Number' : 'Staff / Officer ID'}
                    </label>
                    <input
                      type="text"
                      required={role === 'STUDENT'}
                      placeholder={role === 'STUDENT' ? 'e.g. 2022CS099' : 'e.g. TPO-OFFICER-01 (Optional)'}
                      value={collegeId}
                      onChange={(e) => setCollegeId(e.target.value)}
                      className="w-full px-3.5 py-2.5 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Role Type</label>
                    <select
                      value={role}
                      onChange={(e) => setRole(e.target.value)}
                      className="w-full px-3.5 py-2.5 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-white font-medium"
                    >
                      <option value="STUDENT">Student Candidate</option>
                      <option value="TPO">TPO / Coordinator</option>
                    </select>
                  </div>
                </div>
              </>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                College Domain Email (<span className="text-[#0E6E76] font-mono">@rknec.edu</span>)
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                <input
                  type="email"
                  required
                  id="auth-email-input"
                  placeholder="candidate.student@rknec.edu"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Account Password</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                <input
                  type="password"
                  required
                  id="auth-password-input"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              id="auth-submit-btn"
              className="w-full py-3.5 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-2xl transition-all shadow-md flex items-center justify-center space-x-2 disabled:opacity-50 mt-2"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>{isRegister ? 'Complete Registration' : 'Sign In to Portal'}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Credentials Selector */}
          {!isRegister && (
            <div className="pt-4 border-t border-slate-100 space-y-2">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider text-center">
                Instant 1-Click Demo Login
              </div>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleDemoLogin('candidate.student@rknec.edu', 'StudentPassword123')}
                  className="p-2.5 bg-slate-50 hover:bg-teal-50 hover:border-teal-200 border border-slate-200 rounded-xl text-left transition-all"
                >
                  <div className="text-xs font-bold text-slate-800 flex items-center space-x-1">
                    <GraduationCap className="w-3.5 h-3.5 text-[#0E6E76]" />
                    <span>Student Login</span>
                  </div>
                  <div className="text-[10px] text-slate-500 font-mono truncate">candidate.student</div>
                </button>

                <button
                  type="button"
                  onClick={() => handleDemoLogin('admin@rknec.edu', 'AdminPassword123')}
                  className="p-2.5 bg-slate-50 hover:bg-teal-50 hover:border-teal-200 border border-slate-200 rounded-xl text-left transition-all"
                >
                  <div className="text-xs font-bold text-slate-800 flex items-center space-x-1">
                    <Building2 className="w-3.5 h-3.5 text-[#0E6E76]" />
                    <span>TPO / Admin</span>
                  </div>
                  <div className="text-[10px] text-slate-500 font-mono truncate">admin@rknec.edu</div>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
