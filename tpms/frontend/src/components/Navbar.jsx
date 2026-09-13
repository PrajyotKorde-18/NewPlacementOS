import React, { useState } from 'react';
import { User, LogOut, Briefcase, FileText, CheckSquare, Layers, GraduationCap, Upload, Trash2, AlertTriangle, X, CheckCircle2 } from 'lucide-react';
import { api } from '../api';

export default function Navbar({ currentUser, currentTab, setCurrentTab, onLogout, onOpenAuth }) {
  const [showPurgeConfirm, setShowPurgeConfirm] = useState(false);
  const [purging, setPurging] = useState(false);
  const [purgeResult, setPurgeResult] = useState(null);
  const [error, setError] = useState('');

  const handlePurgeDatabase = async () => {
    setError('');
    setPurging(true);
    try {
      const res = await api.cleanupDatabase();
      setPurgeResult(res);
      setTimeout(() => {
        setShowPurgeConfirm(false);
        setPurgeResult(null);
        setCurrentTab('drives');
        window.location.reload();
      }, 2000);
    } catch (err) {
      setError(err.message || 'Failed to purge database');
    } finally {
      setPurging(false);
    }
  };

  const userRole = (currentUser?.role || '').toUpperCase();
  const isStudent = userRole === 'STUDENT';
  const isStaff = userRole === 'TPO' || userRole === 'COORDINATOR' || userRole === 'ADMIN';

  return (
    <>
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            {/* Logo & System Title */}
            <div className="flex items-center space-x-3 cursor-pointer shrink-0" onClick={() => setCurrentTab('drives')}>
              <div className="w-10 h-10 rounded-lg bg-[#0E6E76] flex items-center justify-center text-white font-bold text-xl shadow-md">
                TP
              </div>
              <div className="hidden lg:block">
                <span className="font-heading font-bold text-xl text-slate-900 tracking-tight">Placement<span className="text-[#0E6E76]">OS</span></span>
                <span className="text-xs block text-slate-500 font-medium">Training & Placement Management System</span>
              </div>
            </div>

            {/* Navigation Links */}
            {currentUser && (
              <nav className="flex space-x-1 sm:space-x-1.5 overflow-x-auto py-1 items-center">
                <button
                  id="nav-drives"
                  onClick={() => setCurrentTab('drives')}
                  className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                    currentTab === 'drives'
                      ? 'bg-[#E6F4F5] text-[#0E6E76]'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <Briefcase className="w-4 h-4" />
                  <span>Placement Drives</span>
                </button>

                {isStudent && (
                  <>
                    <button
                      id="nav-academic-setup"
                      onClick={() => setCurrentTab('academic-setup')}
                      className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                        currentTab === 'academic-setup'
                          ? 'bg-[#E6F4F5] text-[#0E6E76]'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <GraduationCap className="w-4 h-4" />
                      <span>Academic Setup</span>
                    </button>

                    <button
                      id="nav-resume-upload"
                      onClick={() => setCurrentTab('resume-upload')}
                      className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                        currentTab === 'resume-upload'
                          ? 'bg-[#E6F4F5] text-[#0E6E76]'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <Upload className="w-4 h-4" />
                      <span>Upload Resume</span>
                    </button>

                    <button
                      id="nav-profile"
                      onClick={() => setCurrentTab('profile')}
                      className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                        currentTab === 'profile'
                          ? 'bg-[#E6F4F5] text-[#0E6E76]'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <FileText className="w-4 h-4" />
                      <span>My Profile</span>
                    </button>

                    <button
                      id="nav-my-applications"
                      onClick={() => setCurrentTab('my-applications')}
                      className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                        currentTab === 'my-applications'
                          ? 'bg-[#E6F4F5] text-[#0E6E76]'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <CheckSquare className="w-4 h-4" />
                      <span>My Applications</span>
                    </button>
                  </>
                )}

                {isStaff && (
                  <>
                    <button
                      id="nav-create-drive"
                      onClick={() => setCurrentTab('create-drive')}
                      className={`px-3 py-2 rounded-lg font-semibold text-xs sm:text-sm transition-colors flex items-center space-x-1.5 whitespace-nowrap ${
                        currentTab === 'create-drive'
                          ? 'bg-[#E6F4F5] text-[#0E6E76]'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <Layers className="w-4 h-4" />
                      <span>Create Drive & Extract JD</span>
                    </button>

                    <button
                      id="nav-purge-db"
                      onClick={() => setShowPurgeConfirm(true)}
                      className="px-3 py-1.5 rounded-lg font-bold text-xs bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 transition-all flex items-center space-x-1.5 whitespace-nowrap shadow-2xs"
                      title="Coordinator DB Purge"
                    >
                      <Trash2 className="w-3.5 h-3.5 text-rose-600" />
                      <span>Purge Student DB</span>
                    </button>
                  </>
                )}
              </nav>
            )}

            {/* User Profile / Auth Button */}
            <div className="flex items-center space-x-3 shrink-0">
              {currentUser ? (
                <div className="flex items-center space-x-3">
                  <div className="text-right hidden md:block">
                    <div className="text-sm font-semibold text-slate-800">{currentUser.name}</div>
                    <div className="flex items-center space-x-1 justify-end">
                      <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${
                        currentUser.role === 'STUDENT' ? 'bg-blue-100 text-blue-700' : 'bg-teal-100 text-teal-800'
                      }`}>
                        {currentUser.role}
                      </span>
                      <span className="text-xs text-slate-500">{currentUser.email}</span>
                    </div>
                  </div>
                  <button
                    id="logout-btn"
                    onClick={onLogout}
                    title="Logout"
                    className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <LogOut className="w-5 h-5" />
                  </button>
                </div>
              ) : (
                <button
                  id="login-btn"
                  onClick={onOpenAuth}
                  className="px-4 py-2 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-medium text-sm rounded-lg transition-colors shadow-sm flex items-center space-x-2"
                >
                  <User className="w-4 h-4" />
                  <span>Sign In / Register</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Coordinator DB Purge Confirmation Modal */}
      {showPurgeConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/70 backdrop-blur-md p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-100 relative p-6 space-y-6">
            <button
              onClick={() => setShowPurgeConfirm(false)}
              className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center space-y-3">
              <div className="w-14 h-14 rounded-2xl bg-rose-100 text-rose-600 flex items-center justify-center mx-auto shadow-inner">
                <AlertTriangle className="w-7 h-7" />
              </div>
              <h3 className="text-xl font-bold font-heading text-slate-900">
                Purge Student & Drive Database?
              </h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                This action will <strong className="text-rose-700">permanently delete</strong> all student profiles, academic eligibility records, semester SGPAs, backlogs, uploaded resumes, and company placement drives from the database.
              </p>
            </div>

            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-semibold text-center">
                {error}
              </div>
            )}

            {purgeResult ? (
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl space-y-2 text-center text-emerald-800 animate-in fade-in">
                <CheckCircle2 className="w-6 h-6 text-emerald-600 mx-auto" />
                <div className="font-bold text-sm">{purgeResult.message}</div>
                <div className="text-[11px] text-emerald-700">
                  Purged {purgeResult.deletedStudentRecords} students & {purgeResult.deletedCompanyDrives} drives. Reloading portal...
                </div>
              </div>
            ) : (
              <div className="flex items-center space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowPurgeConfirm(false)}
                  className="flex-1 py-3 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-2xl transition-all"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  id="confirm-purge-db-btn"
                  onClick={handlePurgeDatabase}
                  disabled={purging}
                  className="flex-1 py-3 px-4 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-2xl transition-all shadow-md flex items-center justify-center space-x-2 disabled:opacity-50"
                >
                  {purging ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <>
                      <Trash2 className="w-4 h-4" />
                      <span>Confirm DB Purge</span>
                    </>
                  )}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
