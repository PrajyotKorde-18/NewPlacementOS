import React, { useState, useEffect } from 'react';
import { Briefcase, CheckCircle2, Clock, Eye, Sparkles, AlertCircle, ArrowRight } from 'lucide-react';
import { api } from '../api';

export default function StudentApplicationsView({ currentUser, onViewFeedback }) {
  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [applyingId, setApplyingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const studentId = currentUser?.id || 1;

  useEffect(() => {
    loadDrivesAndApplications();
  }, [studentId]);

  const loadDrivesAndApplications = async () => {
    setLoading(true);
    setError('');
    try {
      const [validDrives, myApps] = await Promise.all([
        api.getDrives(),
        api.getStudentApplications(studentId),
      ]);
      setDrives(validDrives);
      setApplications(myApps);
    } catch (err) {
      setError(err.message || 'Failed to load placement drives');
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async (driveId) => {
    setApplyingId(driveId);
    setError('');
    setSuccess('');
    try {
      const response = await api.applyToDrive(driveId, studentId);
      setSuccess(`Applied to ${response.companyName} (${response.roleTitle}) successfully!`);
      loadDrivesAndApplications();
    } catch (err) {
      setError(err.message || 'Failed to apply to drive');
    } finally {
      setApplyingId(null);
    }
  };

  if (loading) {
    return (
      <div className="py-20 text-center space-y-3">
        <div className="w-10 h-10 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium text-slate-500">Loading placement drives & my applications...</p>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold font-heading text-slate-900">Placement Drives & My Applications</h1>
        <p className="text-sm text-slate-500 mt-1">Browse active campus drives, view real-time candidate scores, and check status feedback.</p>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>{error}</div>
        </div>
      )}

      {success && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start space-x-3 text-emerald-800 text-sm">
          <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          <div>{success}</div>
        </div>
      )}

      {/* Applied Drives Section */}
      <div className="space-y-4">
        <h3 className="text-lg font-bold font-heading text-slate-900 flex items-center space-x-2">
          <CheckCircle2 className="w-5 h-5 text-[#0E6E76]" />
          <span>My Submitted Applications</span>
        </h3>

        {applications.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {applications.map((app) => (
              <div key={app.id} className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4 hover:border-teal-200 transition-colors">
                <div className="flex justify-between items-start">
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase font-mono">Drive #{app.driveId}</span>
                    <h4 className="text-lg font-bold font-heading text-slate-900">{app.companyName}</h4>
                    <div className="text-xs text-slate-600 font-medium">{app.roleTitle}</div>
                  </div>
                  <span
                    className={`px-3 py-1 rounded-full text-xs font-bold ${
                      app.status === 'SHORTLISTED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : app.status === 'OVERRIDDEN_INCLUDED'
                        ? 'bg-teal-100 text-teal-900'
                        : app.status === 'OVERRIDDEN_EXCLUDED'
                        ? 'bg-red-100 text-red-800'
                        : app.status === 'NOT_SHORTLISTED'
                        ? 'bg-slate-100 text-slate-600'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {app.status}
                  </span>
                </div>

                {/* Candidate Score & Rank */}
                <div className="bg-slate-50 p-3 rounded-xl flex items-center justify-between text-xs font-mono">
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase">Rank Position</span>
                    <span className="font-bold text-slate-900 text-sm">{app.rankPosition ? `#${app.rankPosition}` : 'Pending Rank'}</span>
                  </div>
                  <div className="text-right">
                    <span className="text-slate-400 block text-[10px] uppercase">Relevance Score</span>
                    <span className="font-bold text-[#0E6E76] text-sm">{app.score != null ? `${app.score.toFixed(1)} / 100` : 'Evaluating'}</span>
                  </div>
                </div>

                <div className="flex justify-between items-center pt-2">
                  <span className="text-[11px] text-slate-400">Applied on {app.appliedAt ? new Date(app.appliedAt).toLocaleDateString() : 'Today'}</span>
                  <button
                    onClick={() => onViewFeedback(app.id)}
                    className="px-3 py-1.5 bg-[#E6F4F5] hover:bg-teal-100 text-[#0E6E76] font-bold text-xs rounded-lg transition-colors flex items-center space-x-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>View Rejection & Growth Feedback</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 bg-slate-50 border border-slate-200 rounded-2xl text-center text-slate-500 text-xs italic">
            You have not applied to any placement drives yet. Browse open drives below to apply.
          </div>
        )}
      </div>

      {/* Available Drives Section */}
      <div className="space-y-4 pt-4 border-t border-slate-200">
        <h3 className="text-lg font-bold font-heading text-slate-900 flex items-center space-x-2">
          <Briefcase className="w-5 h-5 text-[#0E6E76]" />
          <span>Active Placement Drives</span>
        </h3>

        {drives.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {drives.map((drive) => {
              const alreadyApplied = applications.some((a) => a.driveId === drive.id);
              return (
                <div key={drive.id} className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4 flex flex-col justify-between">
                  <div>
                    <div className="flex justify-between items-center text-xs text-slate-400 font-mono mb-1">
                      <span>Drive #{drive.id}</span>
                      <span>Vacancies: {drive.vacancies}</span>
                    </div>
                    <h4 className="text-lg font-bold font-heading text-slate-900">{drive.companyName}</h4>
                    <div className="text-xs text-slate-600 font-medium">{drive.roleTitle}</div>
                    {drive.ctc && (
                      <span className="inline-block mt-1.5 text-xs font-bold text-[#0E6E76] bg-teal-50 px-2.5 py-0.5 rounded-full border border-teal-200 font-mono">
                        Package: {drive.ctc}
                      </span>
                    )}

                    {drive.roleProfile && (
                      <div className="flex flex-wrap gap-1 mt-2">
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
                        {drive.roleProfile.minTenthPercentage && (
                          <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                            10th ≥ {drive.roleProfile.minTenthPercentage}%
                          </span>
                        )}
                        {drive.roleProfile.minTwelfthPercentage && (
                          <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                            12th ≥ {drive.roleProfile.minTwelfthPercentage}%
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  <button
                    onClick={() => handleApply(drive.id)}
                    disabled={alreadyApplied || applyingId === drive.id}
                    className={`w-full py-2.5 rounded-xl font-bold text-xs transition-all flex items-center justify-center space-x-2 ${
                      alreadyApplied
                        ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                        : 'bg-[#0E6E76] hover:bg-[#0B575D] text-white shadow-sm'
                    }`}
                  >
                    {applyingId === drive.id ? (
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : alreadyApplied ? (
                      <span>Applied</span>
                    ) : (
                      <>
                        <span>Apply to Drive</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="p-8 bg-slate-50 border border-slate-200 rounded-2xl text-center text-slate-500 text-xs italic">
            No active placement drives available.
          </div>
        )}
      </div>
    </div>
  );
}
