import React, { useState } from 'react';
import { ShieldCheck, Mail, Lock, User, CreditCard, AlertCircle, X, CheckCircle2 } from 'lucide-react';
import { api } from '../services/api';

export function AuthModal({ isOpen, onClose, onAuthSuccess }) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [collegeId, setCollegeId] = useState('');
  const [role, setRole] = useState('STUDENT'); // 'STUDENT' | 'CDPC_ADMIN'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let data;
      if (isLogin) {
        data = await api.login(email, password);
      } else {
        if (!email.toLowerCase().endsWith('@rknec.edu')) {
          setError('Only @rknec.edu college emails are allowed for registration');
          setLoading(false);
          return;
        }
        data = await api.register({
          name,
          email,
          password,
          collegeId: collegeId || (role === 'CDPC_ADMIN' ? 'ADM001' : 'STU001'),
          role,
        });
      }

      // Save token and user info
      localStorage.setItem('internship_token', data.token);
      localStorage.setItem('internship_user', JSON.stringify(data));
      if (onAuthSuccess) onAuthSuccess(data);
      onClose();
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (presetEmail, presetPassword) => {
    setEmail(presetEmail);
    setPassword(presetPassword);
    setIsLogin(true);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100 relative space-y-6">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-xl bg-[var(--brand)] flex items-center justify-center text-white mx-auto shadow-md">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-bold font-heading text-slate-900">
            {isLogin ? 'Sign In to Internship Portal' : 'Register College Account'}
          </h2>
          <p className="text-xs text-slate-500">
            {isLogin
              ? 'Enter your @rknec.edu credentials to manage offers and NOCs'
              : 'Create a student or CDPC admin account with college email'}
          </p>
        </div>

        {/* Quick Demo Test Buttons */}
        <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-2">
          <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider text-center">
            Quick Demo Login Presets
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => handleQuickLogin('student1@rknec.edu', 'Student@12345')}
              className="py-1.5 px-2 bg-white hover:bg-teal-50 border border-slate-200 rounded-lg text-xs font-semibold text-[#0E6E76] transition-colors"
            >
              Student Demo
            </button>
            <button
              type="button"
              onClick={() => handleQuickLogin('admin@rknec.edu', 'Admin@12345')}
              className="py-1.5 px-2 bg-white hover:bg-teal-50 border border-slate-200 rounded-lg text-xs font-semibold text-[#0E6E76] transition-colors"
            >
              CDPC Admin Demo
            </button>
          </div>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl flex items-center space-x-2 text-red-700 text-xs font-medium">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLogin && (
            <>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="text"
                    required
                    placeholder="Rahul Sharma"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[var(--brand)] outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">College ID</label>
                  <div className="relative">
                    <CreditCard className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      placeholder="STU001"
                      value={collegeId}
                      onChange={(e) => setCollegeId(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[var(--brand)] outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Role</label>
                  <select
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[var(--brand)] outline-none"
                  >
                    <option value="STUDENT">Student</option>
                    <option value="CDPC_ADMIN">CDPC Admin</option>
                  </select>
                </div>
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">College Email (@rknec.edu)</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="email"
                required
                placeholder="student1@rknec.edu"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[var(--brand)] outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="password"
                required
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[var(--brand)] outline-none"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-[var(--brand)] hover:bg-[#0B575D] text-white font-bold text-xs rounded-xl transition-all shadow-md flex items-center justify-center space-x-2 disabled:opacity-50"
          >
            {loading ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <span>{isLogin ? 'Sign In' : 'Create Account'}</span>
            )}
          </button>
        </form>

        {/* Toggle Login/Signup */}
        <div className="text-center pt-2 border-t border-slate-100">
          <button
            type="button"
            onClick={() => {
              setIsLogin(!isLogin);
              setError('');
            }}
            className="text-xs text-[var(--brand)] hover:underline font-semibold"
          >
            {isLogin ? "Don't have an account? Register here" : 'Already registered? Sign In here'}
          </button>
        </div>
      </div>
    </div>
  );
}
