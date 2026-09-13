import React, { useState, useEffect } from 'react';
import { Award, CheckCircle2, XCircle, ShieldAlert, Sparkles, RefreshCw, Eye, Edit3, X, AlertCircle } from 'lucide-react';
import { api } from '../api';

export default function RankedShortlistView({ driveId, currentUser, onViewFeedback }) {
  const isCoordinator = ['TPO', 'COORDINATOR', 'ADMIN'].includes(currentUser?.role?.toUpperCase());

  const [drive, setDrive] = useState(null);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [ranking, setRanking] = useState(false);
  const [error, setError] = useState('');

  // Override Modal state
  const [overrideApp, setOverrideApp] = useState(null);
  const [overrideStatus, setOverrideStatus] = useState('OVERRIDDEN_INCLUDED');
  const [overrideReason, setOverrideReason] = useState('');
  const [savingOverride, setSavingOverride] = useState(false);
  const [overrideError, setOverrideError] = useState('');

  // Edit Vacancies state
  const [isEditingVacancies, setIsEditingVacancies] = useState(false);
  const [vacanciesInput, setVacanciesInput] = useState(1);
  const [savingVacancies, setSavingVacancies] = useState(false);

  const handleSaveVacancies = async (e) => {
    e.preventDefault();
    const num = parseInt(vacanciesInput, 10);
    if (isNaN(num) || num < 1) return;
    setSavingVacancies(true);
    try {
      const updated = await api.updateDrive(driveId, { vacancies: num });
      setDrive(updated);
      setIsEditingVacancies(false);
    } catch (err) {
      setError(err.message || 'Failed to update vacancies count');
    } finally {
      setSavingVacancies(false);
    }
  };

  useEffect(() => {
    loadDriveAndApps();
  }, [driveId]);

  const loadDriveAndApps = async () => {
    setLoading(true);
    setError('');
    try {
      const [driveData, appsData] = await Promise.all([
        api.getDrive(driveId),
        api.getDriveApplications(driveId),
      ]);
      setDrive(driveData);
      setApplications(appsData);
    } catch (err) {
      setError(err.message || 'Failed to load drive shortlist');
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerRanking = async () => {
    setRanking(true);
    setError('');
    try {
      const rankedApps = await api.rankDriveApplications(driveId);
      setApplications(rankedApps);
    } catch (err) {
      setError(err.message || 'Failed to trigger candidate ranking algorithm');
    } finally {
      setRanking(false);
    }
  };

  const handleOpenOverride = (app) => {
    setOverrideApp(app);
    setOverrideStatus(app.status === 'OVERRIDDEN_EXCLUDED' ? 'OVERRIDDEN_EXCLUDED' : 'OVERRIDDEN_INCLUDED');
    setOverrideReason(app.overrideReason || '');
    setOverrideError('');
  };

  const handleSaveOverride = async (e) => {
    e.preventDefault();
    if (!overrideReason.trim()) {
      setOverrideError('Mandatory override reason is required.');
      return;
    }

    setSavingOverride(true);
    setOverrideError('');
    try {
      const updatedApp = await api.overrideApplicationStatus(overrideApp.id, overrideStatus, overrideReason.trim());
      setApplications((prev) => prev.map((a) => (a.id === updatedApp.id ? updatedApp : a)));
      setOverrideApp(null);
    } catch (err) {
      setOverrideError(err.message || 'Failed to submit status override');
    } finally {
      setSavingOverride(false);
    }
  };

  if (loading) {
    return (
      <div className="py-20 text-center space-y-3">
        <div className="w-10 h-10 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium text-slate-500">Loading ranked shortlist & applications...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8 animate-in fade-in duration-300">
      {/* Header Banner */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
        <div>
          <div className="flex items-center space-x-2 text-xs text-[#0E6E76] font-bold font-mono">
            <span>DRIVE #{drive?.id}</span>
            <span>•</span>
            <span>{drive?.collegeId}</span>
            {drive?.ctc && (
              <>
                <span>•</span>
                <span className="text-teal-700 bg-teal-50 px-2 py-0.5 rounded-full border border-teal-200">Package: {drive?.ctc}</span>
              </>
            )}
          </div>
          <h1 className="text-2xl font-bold font-heading text-slate-900 mt-1">{drive?.companyName} — {drive?.roleTitle}</h1>
          <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 mt-2">
            <span className="inline-flex items-center space-x-1.5">
              <span>Vacancies: <strong className="text-slate-800">{drive?.vacancies}</strong></span>
              {isCoordinator && (
                <button
                  onClick={() => {
                    setVacanciesInput(drive?.vacancies || 1);
                    setIsEditingVacancies(true);
                  }}
                  className="p-1 text-slate-400 hover:text-[#0E6E76] rounded hover:bg-slate-100 transition-colors"
                  title="Edit Vacancy Count"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                </button>
              )}
            </span>
            <span>Total Applicants: <strong className="text-slate-800">{applications.length}</strong></span>
          </div>

          {drive?.roleProfile && (
            <div className="mt-3 pt-3 border-t border-slate-100 space-y-2">
              {/* Academic Constraints */}
              <div className="flex flex-wrap gap-1.5 items-center text-xs">
                <span className="font-bold text-slate-600 text-[11px]">Academic Criteria:</span>
                {drive.roleProfile.minCgpa ? (
                  <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                    CGPA ≥ {drive.roleProfile.minCgpa}
                  </span>
                ) : null}
                {drive.roleProfile.minTenthPercentage ? (
                  <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                    10th ≥ {drive.roleProfile.minTenthPercentage}%
                  </span>
                ) : null}
                {drive.roleProfile.minTwelfthPercentage ? (
                  <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                    12th ≥ {drive.roleProfile.minTwelfthPercentage}%
                  </span>
                ) : null}
                {drive.roleProfile.maxActiveBacklogs !== null && drive.roleProfile.maxActiveBacklogs !== undefined ? (
                  <span className="text-[10px] bg-slate-100 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 font-mono">
                    Max Backlogs: {drive.roleProfile.maxActiveBacklogs}
                  </span>
                ) : null}
                {!drive.roleProfile.minCgpa && !drive.roleProfile.minTenthPercentage && !drive.roleProfile.minTwelfthPercentage && drive.roleProfile.maxActiveBacklogs === null && (
                  <span className="text-[10px] text-slate-400 italic">No Academic Restrictions</span>
                )}
              </div>

              {/* Required Must-Have Skills */}
              {drive.roleProfile.requiredSkills?.MUST_HAVE?.length > 0 && (
                <div className="flex flex-wrap gap-1.5 items-center text-xs">
                  <span className="font-bold text-slate-600 text-[11px]">Extracted Must-Have Skills:</span>
                  {drive.roleProfile.requiredSkills.MUST_HAVE.map((skill, idx) => (
                    <span key={idx} className="text-[10px] bg-teal-50 text-teal-800 font-semibold px-2 py-0.5 rounded border border-teal-200 font-medium">
                      {skill}
                    </span>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {isCoordinator && (
          <button
            onClick={handleTriggerRanking}
            disabled={ranking || applications.length === 0}
            className="px-6 py-3 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-xl transition-all shadow-md flex items-center space-x-2 disabled:opacity-50"
          >
            {ranking ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Calculating & Ranking Candidates...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                <span>Run Candidate Scoring & Auto-Shortlist</span>
              </>
            )}
          </button>
        )}
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>{error}</div>
        </div>
      )}

      {/* Applications Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="p-4 bg-slate-50 border-b border-slate-200 flex justify-between items-center">
          <h3 className="font-bold font-heading text-sm text-slate-800">Candidate Ranking Matrix</h3>
          <span className="text-xs text-slate-500">Sorted by Total Weighted Score</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-100/70 border-b border-slate-200 text-[11px] font-bold uppercase text-slate-500 tracking-wider">
                <th className="py-3.5 px-4">Rank</th>
                <th className="py-3.5 px-4">Student Info</th>
                <th className="py-3.5 px-4">Total Score</th>
                <th className="py-3.5 px-4">Sub-Scores Breakdown</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-xs">
              {applications.length > 0 ? (
                applications.map((app) => {
                  const bd = app.scoreBreakdown;
                  return (
                    <tr key={app.id} className="hover:bg-slate-50/80 transition-colors">
                      {/* Rank Position */}
                      <td className="py-4 px-4 font-mono font-bold text-slate-700">
                        {app.rankPosition ? `#${app.rankPosition}` : '-'}
                      </td>

                      {/* Student Info */}
                      <td className="py-4 px-4">
                        <div className="font-bold text-slate-900">{app.studentName || 'Student'}</div>
                        <div className="text-[11px] text-slate-500 font-mono">{app.studentEmail}</div>
                        <div className="text-[10px] text-teal-700 font-semibold bg-teal-50 px-1.5 py-0.5 rounded inline-block mt-1">
                          ID: {app.collegeId}
                        </div>
                      </td>

                      {/* Total Weighted Score */}
                      <td className="py-4 px-4 font-mono font-bold text-base text-[#0E6E76]">
                        {app.score != null ? app.score.toFixed(1) : 'N/A'}
                      </td>

                      {/* Score Breakdown Chips */}
                      <td className="py-4 px-4">
                        {bd ? (
                          <div className="grid grid-cols-2 gap-1 max-w-xs text-[10px] font-mono">
                            <span className="bg-slate-100 px-1.5 py-0.5 rounded">Skill: {bd.skillMatchScore?.toFixed(0)}</span>
                            <span className="bg-slate-100 px-1.5 py-0.5 rounded">Proj: {bd.projectRelevanceScore?.toFixed(0)}</span>
                            <span className="bg-slate-100 px-1.5 py-0.5 rounded">Code: {bd.codingActivityScore?.toFixed(0)}</span>
                            <span className="bg-slate-100 px-1.5 py-0.5 rounded">Cert: {bd.certificationsScore?.toFixed(0)}</span>
                          </div>
                        ) : (
                          <span className="text-slate-400 italic">Not evaluated</span>
                        )}
                      </td>

                      {/* Application Status */}
                      <td className="py-4 px-4">
                        <span
                          className={`inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-full font-bold text-[11px] ${
                            app.status === 'SHORTLISTED'
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                              : app.status === 'OVERRIDDEN_INCLUDED'
                              ? 'bg-teal-100 text-teal-900 border border-teal-300'
                              : app.status === 'OVERRIDDEN_EXCLUDED'
                              ? 'bg-red-100 text-red-900 border border-red-300'
                              : app.status === 'NOT_SHORTLISTED'
                              ? 'bg-slate-100 text-slate-600 border border-slate-200'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {app.status === 'SHORTLISTED' && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />}
                          {app.status === 'OVERRIDDEN_INCLUDED' && <Sparkles className="w-3.5 h-3.5 text-teal-700" />}
                          {app.status === 'OVERRIDDEN_EXCLUDED' && <XCircle className="w-3.5 h-3.5 text-red-600" />}
                          <span>{app.status}</span>
                        </span>
                        {app.overrideReason && (
                          <div className="text-[10px] text-amber-800 italic mt-1 max-w-xs">
                            Reason: {app.overrideReason}
                          </div>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-4 px-4 text-right space-x-2">
                        {isCoordinator && (
                          <button
                            onClick={() => handleOpenOverride(app)}
                            className="px-2.5 py-1 bg-slate-100 hover:bg-teal-50 text-slate-700 hover:text-[#0E6E76] rounded-lg font-semibold transition-colors inline-flex items-center space-x-1"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>Override</span>
                          </button>
                        )}
                        <button
                          onClick={() => onViewFeedback(app.id)}
                          className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-semibold transition-colors inline-flex items-center space-x-1"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Feedback</span>
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400 italic">
                    No candidates have applied to this drive yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Override Status Modal */}
      {overrideApp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 relative border border-slate-100 animate-in fade-in zoom-in duration-200">
            <button
              onClick={() => setOverrideApp(null)}
              className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-xl font-bold font-heading text-slate-900 mb-1">Coordinator Status Override</h3>
            <p className="text-xs text-slate-500 mb-4">Candidate: <strong className="text-slate-800">{overrideApp.studentName}</strong></p>

            {overrideError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-xs">
                {overrideError}
              </div>
            )}

            <form onSubmit={handleSaveOverride} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Override Status Direction</label>
                <select
                  value={overrideStatus}
                  onChange={(e) => setOverrideStatus(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                >
                  <option value="OVERRIDDEN_INCLUDED">OVERRIDDEN_INCLUDED (Force Include Candidate)</option>
                  <option value="OVERRIDDEN_EXCLUDED">OVERRIDDEN_EXCLUDED (Force Exclude Candidate)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Mandatory Override Reason</label>
                <textarea
                  rows={3}
                  required
                  placeholder="State clear justification for override..."
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  className="w-full p-2.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setOverrideApp(null)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingOverride}
                  className="px-4 py-2 bg-[#0E6E76] text-white font-semibold text-xs rounded-lg hover:bg-[#0B575D] disabled:opacity-50"
                >
                  {savingOverride ? 'Saving...' : 'Apply Status Override'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Vacancies Modal */}
      {isEditingVacancies && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-sm w-full p-6 relative border border-slate-100 animate-in fade-in zoom-in duration-200">
            <button
              onClick={() => setIsEditingVacancies(false)}
              className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold font-heading text-slate-900 mb-1">Update Drive Vacancies</h3>
            <p className="text-xs text-slate-500 mb-4">Company: <strong className="text-slate-800">{drive?.companyName}</strong></p>

            <form onSubmit={handleSaveVacancies} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Number of Vacancies</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={vacanciesInput}
                  onChange={(e) => setVacanciesInput(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none font-bold text-slate-900"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsEditingVacancies(false)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingVacancies}
                  className="px-4 py-2 bg-[#0E6E76] text-white font-semibold text-xs rounded-lg hover:bg-[#0B575D] disabled:opacity-50"
                >
                  {savingVacancies ? 'Updating...' : 'Save Vacancies'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
