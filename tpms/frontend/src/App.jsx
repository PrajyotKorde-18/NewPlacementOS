import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import StudentProfileView from './components/StudentProfileView';
import AcademicSetupView from './components/AcademicSetupView';
import ResumeUploadView from './components/ResumeUploadView';
import CreateDriveView from './components/CreateDriveView';
import RankedShortlistView from './components/RankedShortlistView';
import StudentApplicationsView from './components/StudentApplicationsView';
import FeedbackModal from './components/FeedbackModal';
import { api, getCurrentUser, removeAuthToken, removeCurrentUser } from './api';
import { Briefcase, ArrowRight, Sparkles, Shield, User, FileText, AlertCircle, Trash2, X } from 'lucide-react';

export default function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [isAuthOpen, setIsAuthOpen] = useState(false);

  const [currentTab, setCurrentTab] = useState('drives');
  const [selectedDriveId, setSelectedDriveId] = useState(null);
  const [drives, setDrives] = useState([]);
  const [loadingDrives, setLoadingDrives] = useState(false);
  const [deletingDrive, setDeletingDrive] = useState(null);

  const [feedbackAppId, setFeedbackAppId] = useState(null);

  useEffect(() => {
    const user = getCurrentUser();
    if (user) {
      setCurrentUser(user);
    }
  }, []);

  useEffect(() => {
    if (currentUser && currentTab === 'drives' && !selectedDriveId) {
      loadDrives();
    }
  }, [currentUser, currentTab, selectedDriveId]);

  const loadDrives = async () => {
    setLoadingDrives(true);
    try {
      const data = await api.getDrives();
      setDrives(data);
    } catch (err) {
      console.error('Failed to fetch placement drives', err);
    } finally {
      setLoadingDrives(false);
    }
  };

  const handleDeleteDrive = async (e, driveId, companyName) => {
    e.stopPropagation();
    if (window.confirm(`Are you sure you want to delete Placement Drive #${driveId} for '${companyName}'? This will remove the drive and its application records from the database.`)) {
      try {
        await api.deleteDrive(driveId);
        alert(`Placement Drive #${driveId} (${companyName}) deleted successfully.`);
        loadDrives();
      } catch (err) {
        alert('Failed to delete drive: ' + (err.message || err));
      }
    }
  };

  const handleLogout = () => {
    removeAuthToken();
    removeCurrentUser();
    setCurrentUser(null);
    setCurrentTab('drives');
  };

  const handleOpenFeedback = (appId) => {
    setFeedbackAppId(appId);
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-slate-900 flex flex-col font-body">
      <Navbar
        currentUser={currentUser}
        currentTab={currentTab}
        setCurrentTab={(tab) => {
          setSelectedDriveId(null);
          setCurrentTab(tab);
        }}
        onLogout={handleLogout}
        onOpenAuth={() => setIsAuthOpen(true)}
      />

      <main className="flex-1">
        {!currentUser && (
          <div className="max-w-6xl mx-auto px-4 py-12 lg:py-20 animate-in fade-in duration-300">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
              {/* Left Column: Official System Identity & Key Capabilities */}
              <div className="lg:col-span-7 space-y-8">
                <div className="inline-flex items-center space-x-2 bg-[#E6F4F5] border border-teal-200/80 px-4 py-1.5 rounded-full text-xs font-bold text-[#0E6E76]">
                  <Shield className="w-4 h-4 text-[#0E6E76]" />
                  <span>Official Campus Placement & Candidate Match OS</span>
                </div>

                <div className="space-y-3">
                  <h1 className="text-4xl sm:text-5xl font-extrabold font-heading text-slate-900 tracking-tight leading-tight">
                    Training & Placement <br />
                    <span className="text-[#0E6E76] bg-gradient-to-r from-[#0E6E76] to-[#0D6269] bg-clip-text text-transparent">
                       Management System (TPMS)
                    </span>
                  </h1>
                  <p className="text-slate-600 text-base leading-relaxed max-w-xl">
                    Automated technical candidate extraction, LLM JD skill classification, strategy-pattern candidate scoring engine, and deterministic rejection transparency.
                  </p>
                </div>

                {/* Capability Badges */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="p-4 bg-white border border-slate-200/80 rounded-2xl shadow-xs space-y-1.5">
                    <div className="w-8 h-8 rounded-xl bg-teal-50 text-[#0E6E76] flex items-center justify-center font-bold">
                      <Sparkles className="w-4 h-4" />
                    </div>
                    <h3 className="font-bold text-sm text-slate-900">Groq LLM Resume Parser</h3>
                    <p className="text-xs text-slate-500">Automatically extracts technical skills, projects, and handles from PDF resumes.</p>
                  </div>

                  <div className="p-4 bg-white border border-slate-200/80 rounded-2xl shadow-xs space-y-1.5">
                    <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center font-bold">
                      <Shield className="w-4 h-4" />
                    </div>
                    <h3 className="font-bold text-sm text-slate-900">Academic Eligibility Engine</h3>
                    <p className="text-xs text-slate-500">DB-level aggregate queries for CGPA cutoffs and active backlog filtering.</p>
                  </div>
                </div>

                {/* Campus Verification Footer */}
                <div className="pt-2 flex items-center space-x-6 text-xs text-slate-400 font-medium border-t border-slate-200/60">
                  <span className="flex items-center space-x-1.5 text-slate-600">
                    <span className="w-2 h-2 rounded-full bg-emerald-500" />
                    <span>Restricted to @rknec.edu Domain</span>
                  </span>
                  <span>•</span>
                  <span>Dual Microservice Architecture</span>
                </div>
              </div>

              {/* Right Column: Embedded Formal Quick Sign-In Card */}
              <div className="lg:col-span-5">
                <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-xl space-y-6 relative overflow-hidden">
                  <div className="bg-gradient-to-r from-[#0E6E76] to-[#0A4E53] -mx-8 -mt-8 p-6 text-white mb-2">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-xl bg-white/20 text-white font-bold text-xl flex items-center justify-center backdrop-blur-md">
                        TP
                      </div>
                      <div>
                        <h2 className="font-bold text-lg font-heading">Portal Access</h2>
                        <p className="text-xs text-teal-100">Sign in with your campus credentials</p>
                      </div>
                    </div>
                  </div>

                  <div className="text-center space-y-4 pt-2">
                    <p className="text-xs text-slate-600">
                      Access placement drive shortlists, candidate rank matrices, academic onboarding, and rejection feedback logs.
                    </p>

                    <button
                      onClick={() => setIsAuthOpen(true)}
                      className="w-full py-4 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-2xl transition-all shadow-md flex items-center justify-center space-x-2"
                    >
                      <User className="w-4 h-4" />
                      <span>Sign In / Create Account</span>
                    </button>
                  </div>

                  {/* 1-Click Quick Demo Login Shortcuts */}
                  <div className="pt-4 border-t border-slate-100 space-y-2">
                    <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider text-center">
                      Instant 1-Click Demo Login
                    </div>
                    <div className="grid grid-cols-2 gap-2.5">
                      <button
                        onClick={async () => {
                          try {
                            const res = await api.login('candidate.student@rknec.edu', 'StudentPassword123');
                            setCurrentUser(res);
                            setCurrentTab('drives');
                          } catch (err) {
                            alert('Login failed: ' + err.message);
                          }
                        }}
                        className="p-3 bg-slate-50 hover:bg-teal-50 hover:border-teal-300 border border-slate-200 rounded-2xl text-left transition-all group"
                      >
                        <div className="text-xs font-bold text-slate-800 group-hover:text-[#0E6E76]">Student Candidate</div>
                        <div className="text-[10px] text-slate-400 font-mono truncate">candidate.student</div>
                      </button>

                      <button
                        onClick={async () => {
                          try {
                            const res = await api.login('admin@rknec.edu', 'AdminPassword123');
                            setCurrentUser(res);
                            setCurrentTab('drives');
                          } catch (err) {
                            alert('Login failed: ' + err.message);
                          }
                        }}
                        className="p-3 bg-slate-50 hover:bg-teal-50 hover:border-teal-300 border border-slate-200 rounded-2xl text-left transition-all group"
                      >
                        <div className="text-xs font-bold text-slate-800 group-hover:text-[#0E6E76]">TPO / Coordinator</div>
                        <div className="text-[10px] text-slate-400 font-mono truncate">admin@rknec.edu</div>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {currentUser && (
          <>
            {currentTab === 'drives' && (
              selectedDriveId ? (
                <div className="space-y-4">
                  <div className="max-w-7xl mx-auto px-4 pt-4">
                    <button
                      onClick={() => setSelectedDriveId(null)}
                      className="text-xs text-[#0E6E76] hover:underline font-bold"
                    >
                      ← Back to All Placement Drives
                    </button>
                  </div>
                  <RankedShortlistView
                    driveId={selectedDriveId}
                    currentUser={currentUser}
                    onViewFeedback={handleOpenFeedback}
                  />
                </div>
              ) : (
                <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
                  <div className="flex justify-between items-center">
                    <div>
                      <h1 className="text-3xl font-bold font-heading text-slate-900">Placement Drives Overview</h1>
                      <p className="text-sm text-slate-500 mt-1">Select a placement drive to view candidate rankings and score breakdowns.</p>
                    </div>
                    {(currentUser?.role?.toUpperCase() === 'TPO' || currentUser?.role?.toUpperCase() === 'COORDINATOR' || currentUser?.role?.toUpperCase() === 'ADMIN') && (
                      <button
                        onClick={() => setCurrentTab('create-drive')}
                        className="px-4 py-2 bg-[#0E6E76] text-white font-bold text-xs rounded-xl hover:bg-[#0B575D] shadow-sm flex items-center space-x-1.5"
                      >
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>Create New Drive</span>
                      </button>
                    )}
                  </div>

                  {loadingDrives ? (
                    <div className="py-16 text-center">
                      <div className="w-10 h-10 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
                      <p className="text-sm text-slate-500 font-medium">Fetching placement drives...</p>
                    </div>
                  ) : drives.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                      {drives.map((drive) => (
                        <div
                          key={drive.id}
                          className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm hover:border-[#0E6E76] transition-all space-y-4 cursor-pointer"
                          onClick={() => setSelectedDriveId(drive.id)}
                        >
                          <div className="flex justify-between items-center text-xs font-mono">
                            <span className="text-slate-400">Drive #{drive.id}</span>
                            <div className="flex items-center space-x-2">
                              <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-semibold">Active</span>
                              {(currentUser?.role?.toUpperCase() === 'TPO' || currentUser?.role?.toUpperCase() === 'COORDINATOR' || currentUser?.role?.toUpperCase() === 'ADMIN') && (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    setDeletingDrive(drive);
                                  }}
                                  title="Delete Drive from DB"
                                  className="p-1 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors z-10 relative"
                                >
                                  <Trash2 className="w-4 h-4 pointer-events-none" />
                                </button>
                              )}
                            </div>
                          </div>
                          <div>
                            <h3 className="text-xl font-bold font-heading text-slate-900">
                              {drive.companyName}
                            </h3>
                            <p className="text-xs text-slate-500 font-medium">
                              {drive.roleTitle}
                            </p>

                            {drive.ctc && (
                              <span className="inline-block mt-1 text-xs font-bold text-[#0E6E76] bg-teal-50 px-2 py-0.5 rounded-full border border-teal-200 font-mono">
                                Package: {drive.ctc}
                              </span>
                            )}

                            {drive.roleProfile && (
                              <div className="space-y-2 mt-3 pt-2 border-t border-slate-100">
                                {/* Academic Criteria Badges */}
                                <div className="flex flex-wrap gap-1">
                                  {drive.roleProfile.minCgpa && (
                                    <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                                      CGPA ≥ {drive.roleProfile.minCgpa}
                                    </span>
                                  )}
                                  {drive.roleProfile.maxActiveBacklogs !== null && drive.roleProfile.maxActiveBacklogs !== undefined && (
                                    <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                                      Max Backlogs: {drive.roleProfile.maxActiveBacklogs}
                                    </span>
                                  )}
                                </div>

                                {/* Skills Badges */}
                                {drive.roleProfile.requiredSkills?.MUST_HAVE?.length > 0 && (
                                  <div className="flex flex-wrap gap-1">
                                    {drive.roleProfile.requiredSkills.MUST_HAVE.map((skill, idx) => (
                                      <span key={idx} className="text-[10px] bg-teal-50 text-teal-800 font-semibold px-1.5 py-0.5 rounded border border-teal-200">
                                        {skill}
                                      </span>
                                    ))}
                                  </div>
                                )}
                              </div>
                            )}
                          </div>

                          <div className="pt-2 border-t border-slate-100 flex justify-between items-center text-xs text-[#0E6E76] font-bold">
                            <span>View Candidate Shortlist Matrix</span>
                            <ArrowRight className="w-4 h-4" />
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="p-12 bg-white border border-slate-200 rounded-2xl text-center text-slate-500 text-xs italic">
                      No active placement drives found. Click "Create New Drive" to set up a placement drive.
                    </div>
                  )}
                </div>
              )
            )}

            {currentTab === 'academic-setup' && (
              <AcademicSetupView
                currentUser={currentUser}
                onProceedToResume={() => setCurrentTab('resume-upload')}
              />
            )}

            {currentTab === 'resume-upload' && (
              <ResumeUploadView
                currentUser={currentUser}
                onBackToAcademic={() => setCurrentTab('academic-setup')}
                onViewProfile={() => setCurrentTab('profile')}
              />
            )}

            {currentTab === 'profile' && (
              <StudentProfileView
                currentUser={currentUser}
                onEditAcademic={() => setCurrentTab('academic-setup')}
                onUploadResume={() => setCurrentTab('resume-upload')}
              />
            )}

            {currentTab === 'create-drive' && <CreateDriveView currentUser={currentUser} onDriveCreated={() => setCurrentTab('drives')} />}
            {currentTab === 'my-applications' && <StudentApplicationsView currentUser={currentUser} onViewFeedback={handleOpenFeedback} />}
          </>
        )}
      </main>

      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onAuthSuccess={(user) => {
          setCurrentUser(user);
          setCurrentTab('drives');
        }}
      />

      <FeedbackModal
        applicationId={feedbackAppId}
        isOpen={!!feedbackAppId}
        onClose={() => setFeedbackAppId(null)}
      />

      {/* Delete Placement Drive Confirmation Modal */}
      {deletingDrive && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full p-6 space-y-6 relative border border-slate-100">
            <button
              onClick={() => setDeletingDrive(null)}
              className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
                <Trash2 className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold font-heading text-slate-900">
                Delete Placement Drive #{deletingDrive.id}?
              </h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                Are you sure you want to delete <strong className="text-slate-800">{deletingDrive.companyName} ({deletingDrive.roleTitle})</strong>? This action will permanently remove the drive and all associated student applications from the database.
              </p>
            </div>

            <div className="flex items-center space-x-3 pt-2">
              <button
                type="button"
                onClick={() => setDeletingDrive(null)}
                className="flex-1 py-3 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-2xl transition-all"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await api.deleteDrive(deletingDrive.id);
                    setDeletingDrive(null);
                    loadDrives();
                  } catch (err) {
                    alert('Failed to delete drive: ' + (err.message || err));
                  }
                }}
                className="flex-1 py-3 px-4 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-2xl transition-all shadow-md flex items-center justify-center space-x-2"
              >
                <Trash2 className="w-4 h-4" />
                <span>Delete Drive</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
