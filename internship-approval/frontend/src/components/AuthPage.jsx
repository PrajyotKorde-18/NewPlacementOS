import React, { useState } from 'react';
import { ShieldCheck, GraduationCap, ShieldAlert, Mail, Lock, User, CreditCard, AlertCircle, ArrowRight, Sparkles } from 'lucide-react';
import { api } from '../services/api';

export function AuthPage({ onAuthSuccess }) {
  const [portalType, setPortalType] = useState('STUDENT'); // 'STUDENT' | 'CDPC'
  const [isRegister, setIsRegister] = useState(false); // For student registration toggle

  // Form fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [collegeId, setCollegeId] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let data;
      if (portalType === 'STUDENT' && isRegister) {
        if (!email.toLowerCase().endsWith('@rknec.edu')) {
          setError('Registration restricted: Email domain must be @rknec.edu');
          setLoading(false);
          return;
        }
        data = await api.register({
          name,
          email,
          password,
          collegeId: collegeId || 'STU001',
          role: 'STUDENT',
        });
      } else {
        // Sign in for either Student or CDPC
        data = await api.login(email, password);
      }

      localStorage.setItem('internship_token', data.token);
      localStorage.setItem('internship_user', JSON.stringify(data));
      onAuthSuccess(data);
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = async (presetEmail, presetPassword) => {
    setError('');
    setLoading(true);
    try {
      const data = await api.login(presetEmail, presetPassword);
      localStorage.setItem('internship_token', data.token);
      localStorage.setItem('internship_user', JSON.stringify(data));
      onAuthSuccess(data);
    } catch (err) {
      setError(err.message || 'Demo login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-between relative overflow-hidden font-sans text-slate-100">
      {/* Background Decorative Gradients */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-teal-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl pointer-events-none" />

      {/* Top Brand Bar */}
      <header className="px-6 lg:px-12 py-6 flex items-center justify-between z-10">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-[#0E6E76] flex items-center justify-center text-white shadow-lg shadow-teal-900/50">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold font-heading text-white leading-tight">PlacementOS</h1>
            <p className="text-xs text-teal-400 font-medium">Internship Approval & Anti-Scam Portal</p>
          </div>
        </div>

        <div className="text-xs text-slate-400 hidden sm:block">
          Ramdeobaba University • CDPC Verification System
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex items-center justify-center px-4 py-8 z-10">
        <div className="max-w-md w-full bg-slate-800/90 backdrop-blur-md rounded-3xl p-8 border border-slate-700/80 shadow-2xl space-y-6">
          
          {/* Dual Portal Selection Segmented Control */}
          <div className="grid grid-cols-2 gap-2 bg-slate-900/80 p-1.5 rounded-2xl border border-slate-700">
            <button
              type="button"
              onClick={() => {
                setPortalType('STUDENT');
                setIsRegister(false);
                setError('');
              }}
              className={`py-2.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                portalType === 'STUDENT'
                  ? 'bg-[#0E6E76] text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <GraduationCap className="w-4 h-4" />
              <span>Student Portal</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setPortalType('CDPC');
                setIsRegister(false);
                setError('');
              }}
              className={`py-2.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                portalType === 'CDPC'
                  ? 'bg-amber-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <ShieldAlert className="w-4 h-4" />
              <span>CDPC Admin</span>
            </button>
          </div>

          {/* Title & Subtitle */}
          <div className="text-center space-y-1">
            <h2 className="text-xl font-bold font-heading text-white">
              {portalType === 'STUDENT'
                ? isRegister
                  ? 'Create Student Account'
                  : 'Student Sign In'
                : 'CDPC Coordinator Sign In'}
            </h2>
            <p className="text-xs text-slate-400">
              {portalType === 'STUDENT'
                ? 'Access offer letter submissions and track CDPC NOC approvals'
                : 'Manage gatekeeping review queue, fraud flags, and policy criteria'}
            </p>
          </div>

          {/* Quick Demo Credentials Widget */}
          <div className="bg-slate-900/60 p-3.5 rounded-2xl border border-slate-700/60 space-y-2">
            <div className="flex items-center justify-between text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              <span className="flex items-center space-x-1">
                <Sparkles className="w-3.5 h-3.5 text-teal-400" />
                <span>1-Click Demo Login</span>
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {portalType === 'STUDENT' ? (
                <button
                  type="button"
                  onClick={() => handleQuickDemo('student1@rknec.edu', 'Student@12345')}
                  className="py-2 px-3 bg-teal-950/80 hover:bg-teal-900 border border-teal-700/50 rounded-xl text-xs font-semibold text-teal-300 transition-colors flex items-center justify-between"
                >
                  <span>Rahul Sharma (Student)</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleQuickDemo('admin@rknec.edu', 'Admin@12345')}
                  className="col-span-2 py-2 px-3 bg-amber-950/80 hover:bg-amber-900 border border-amber-700/50 rounded-xl text-xs font-semibold text-amber-300 transition-colors flex items-center justify-between"
                >
                  <span>Sign In as CDPC Admin (admin@rknec.edu)</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>

          {error && (
            <div className="p-3 bg-red-950/80 border border-red-800 rounded-xl flex items-center space-x-2 text-red-200 text-xs font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleAuthSubmit} className="space-y-4">
            {portalType === 'STUDENT' && isRegister && (
              <>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Full Name</label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      required
                      placeholder="Rahul Sharma"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-slate-900 border border-slate-700 text-slate-100 rounded-xl focus:ring-2 focus:ring-teal-500 outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">College Roll No / ID</label>
                  <div className="relative">
                    <CreditCard className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      placeholder="STU001"
                      value={collegeId}
                      onChange={(e) => setCollegeId(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-slate-900 border border-slate-700 text-slate-100 rounded-xl focus:ring-2 focus:ring-teal-500 outline-none"
                    />
                  </div>
                </div>
              </>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                College Email address ({portalType === 'STUDENT' ? '@rknec.edu' : 'Admin Email'})
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                <input
                  type="email"
                  required
                  placeholder={portalType === 'STUDENT' ? 'student1@rknec.edu' : 'admin@rknec.edu'}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-xs bg-slate-900 border border-slate-700 text-slate-100 rounded-xl focus:ring-2 focus:ring-teal-500 outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Password</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-xs bg-slate-900 border border-slate-700 text-slate-100 rounded-xl focus:ring-2 focus:ring-teal-500 outline-none"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className={`w-full py-2.5 font-bold text-xs rounded-xl transition-all shadow-lg flex items-center justify-center space-x-2 disabled:opacity-50 ${
                portalType === 'STUDENT'
                  ? 'bg-[#0E6E76] hover:bg-[#0B575D] text-white'
                  : 'bg-amber-600 hover:bg-amber-700 text-white'
              }`}
            >
              {loading ? (
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <span>
                  {portalType === 'STUDENT'
                    ? isRegister
                      ? 'Register & Access Portal'
                      : 'Sign In to Student Portal'
                    : 'Sign In to CDPC Portal'}
                </span>
              )}
            </button>
          </form>

          {/* Toggle Register for Student */}
          {portalType === 'STUDENT' && (
            <div className="text-center pt-2 border-t border-slate-700/60">
              <button
                type="button"
                onClick={() => {
                  setIsRegister(!isRegister);
                  setError('');
                }}
                className="text-xs text-teal-400 hover:underline font-semibold"
              >
                {isRegister ? 'Already registered? Sign In here' : "New Student? Register with @rknec.edu email"}
              </button>
            </div>
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="py-4 text-center text-xs text-slate-500 z-10">
        PlacementOS • Department of Training & Placement • Ramdeobaba University
      </footer>
    </div>
  );
}
