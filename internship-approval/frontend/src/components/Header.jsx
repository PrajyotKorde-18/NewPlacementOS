import React from 'react';
import { ShieldCheck, FileText, Settings, ShieldAlert, LogOut, User, QrCode } from 'lucide-react';

export function Header({
  activeTab,
  setActiveTab,
  currentUser,
  onLogout,
}) {
  const isCdpc = currentUser?.role === 'CDPC_ADMIN';

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm font-sans">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo & System Brand */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-[var(--brand)] flex items-center justify-center text-white shadow-md">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-900 font-heading leading-tight">
                PlacementOS
              </h1>
              <p className="text-xs text-[var(--brand)] font-medium">
                {isCdpc ? 'CDPC Gatekeeping Portal' : 'Student Internship Portal'}
              </p>
            </div>
          </div>

          {/* Navigation Links based on Role */}
          <nav className="flex space-x-1 sm:space-x-2">
            {!isCdpc ? (
              <button
                onClick={() => setActiveTab('student-submit')}
                className={`flex items-center space-x-2 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                  activeTab === 'student-submit'
                    ? 'bg-teal-50 text-[#0E6E76] font-bold border border-teal-200'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <FileText className="w-4 h-4 text-[#0E6E76]" />
                <span>Submit Offer & My Applications</span>
              </button>
            ) : (
              <>
                <button
                  onClick={() => setActiveTab('cdpc-review')}
                  className={`flex items-center space-x-2 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                    activeTab === 'cdpc-review'
                      ? 'bg-amber-50 text-amber-900 font-bold border border-amber-200'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  <ShieldAlert className="w-4 h-4 text-amber-600" />
                  <span>Review Queue</span>
                </button>
                <button
                  onClick={() => setActiveTab('cdpc-rules')}
                  className={`flex items-center space-x-2 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                    activeTab === 'cdpc-rules'
                      ? 'bg-amber-50 text-amber-900 font-bold border border-amber-200'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  <Settings className="w-4 h-4 text-amber-600" />
                  <span>Policy Criteria Rules</span>
                </button>
              </>
            )}

            <a
              href="/verify-noc"
              className="flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 border border-transparent hover:border-emerald-200 transition-all"
              title="Verify Official Institutional NOC Certificate"
            >
              <QrCode className="w-4 h-4 text-emerald-600" />
              <span>Verify NOC</span>
            </a>
          </nav>

          {/* Logged In User Badge & Logout */}
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2.5 bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-xl">
              <div className="w-7 h-7 rounded-lg bg-teal-100 flex items-center justify-center text-[#0E6E76]">
                <User className="w-4 h-4" />
              </div>
              <div className="text-left hidden sm:block">
                <div className="text-xs font-bold text-slate-900 leading-none">
                  {currentUser?.name || currentUser?.email || 'User'}
                </div>
                <div className="text-[10px] text-[#0E6E76] font-mono leading-tight uppercase font-semibold mt-0.5">
                  {currentUser?.role === 'CDPC_ADMIN' ? 'CDPC Admin' : 'Student'}
                </div>
              </div>
              <button
                onClick={onLogout}
                title="Sign Out to Auth Page"
                className="ml-2 p-1 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
