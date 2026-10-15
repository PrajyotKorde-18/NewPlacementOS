import React, { useState, useEffect } from 'react';
import {
  Upload,
  FileText,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Sparkles,
  ArrowLeft,
  ArrowRight,
  Code,
  Briefcase,
  Award,
  Check
} from 'lucide-react';
import { api } from '../api';

export default function ResumeUploadView({ currentUser, onBackToAcademic, onViewProfile }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [extractionStatus, setExtractionStatus] = useState(null);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [versionId, setVersionId] = useState(null);

  const studentId = currentUser?.id || 1;

  useEffect(() => {
    fetchProfile();
  }, [studentId]);

  // Poll status if versionId is active and PENDING or PROCESSING
  useEffect(() => {
    let interval = null;
    if (versionId && (extractionStatus === 'PENDING' || extractionStatus === 'PROCESSING')) {
      interval = setInterval(async () => {
        try {
          const statusRes = await api.getResumeStatus(versionId);
          const currentStatus = statusRes.extractionStatus;
          setExtractionStatus(currentStatus);
          if (currentStatus === 'COMPLETED' || currentStatus === 'PROCESSED') {
            clearInterval(interval);
            setUploading(false);
            setSuccessMsg('Resume PDF parsed successfully via Groq LLM!');
            fetchProfile();
          } else if (currentStatus === 'FLAGGED_FOR_REVIEW') {
            clearInterval(interval);
            setUploading(false);
            setError('Extraction Flagged for Review: We couldn\'t extract sufficient structured data from this PDF.');
            fetchProfile();
          } else if (currentStatus === 'FAILED') {
            clearInterval(interval);
            setUploading(false);
            setError(`Resume extraction failed: ${statusRes.errorLog || 'Unknown error'}`);
          }
        } catch (err) {
          console.error('Error fetching resume status:', err);
        }
      }, 1500);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [versionId, extractionStatus]);

  const fetchProfile = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getStudentProfile(studentId);
      setProfile(data);
    } catch (err) {
      console.error('Failed to load profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      setError('Please select a valid PDF file');
      return;
    }

    setUploading(true);
    setError('');
    setSuccessMsg('');
    setExtractionStatus('PENDING');

    try {
      const response = await api.uploadResume(file, studentId);
      setVersionId(response.versionId || response.resumeId);
    } catch (err) {
      setUploading(false);
      setExtractionStatus('FAILED');
      setError(err.message || 'Failed to upload resume');
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-8 animate-in fade-in duration-300">
      {/* Wizard Progress Header */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-100">
              Step 2 of 2
            </span>
            <h1 className="text-2xl font-bold font-heading text-slate-900 mt-2">
              Upload PDF Resume & AI Skill Extraction
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Upload your official PDF resume to automatically extract skills, projects, and coding handles.
            </p>
          </div>

          <div className="flex items-center space-x-2 text-xs font-semibold text-slate-400 shrink-0">
            <span className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
              <Check className="w-4 h-4" />
            </span>
            <span className="text-slate-500 line-through">Academic Details</span>
            <span className="text-slate-300">──────</span>
            <span className="w-8 h-8 rounded-full bg-[#0E6E76] text-white flex items-center justify-center font-bold">2</span>
            <span className="text-slate-700">Resume Upload</span>
          </div>
        </div>

        {/* Progress bar line */}
        <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
          <div className="bg-[#0E6E76] h-full w-full rounded-full transition-all duration-500" />
        </div>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center space-x-3 text-emerald-800 text-sm font-medium animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">Extraction Error</div>
            <div className="text-xs mt-0.5">{error}</div>
          </div>
        </div>
      )}

      {/* Main Upload Drop Zone Card */}
      <div className="bg-gradient-to-br from-white to-teal-50/30 border-2 border-dashed border-[#0E6E76]/40 hover:border-[#0E6E76] rounded-2xl p-8 text-center space-y-4 shadow-sm transition-all">
        <div className="w-16 h-16 rounded-2xl bg-teal-100/80 text-[#0E6E76] flex items-center justify-center mx-auto shadow-sm">
          <Upload className="w-8 h-8" />
        </div>
        <div>
          <h2 className="text-xl font-bold font-heading text-slate-900">
            Select or Drag Your PDF Resume
          </h2>
          <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
            Groq Llama-3 AI will analyze your PDF to automatically classify your technical skills, projects, and coding handles.
          </p>
        </div>

        <div>
          <label className="cursor-pointer bg-[#0E6E76] hover:bg-[#0B575D] text-white px-6 py-3 rounded-xl font-bold text-sm shadow-md transition-all inline-flex items-center space-x-2">
            <FileText className="w-4 h-4" />
            <span>Select PDF File to Upload</span>
            <input type="file" id="resume-upload-input" accept=".pdf" onChange={handleFileUpload} className="hidden" />
          </label>
        </div>
      </div>

      {/* Asynchronous Extraction Worker Indicator */}
      {uploading && (
        <div className="bg-teal-50 border-2 border-dashed border-[#0E6E76] rounded-2xl p-6 text-center space-y-3">
          <div className="w-12 h-12 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto" />
          <div>
            <h3 className="font-bold text-[#0E6E76] text-lg font-heading">Groq LLM Resume Parsing in Progress...</h3>
            <p className="text-xs text-slate-600 mt-1">Analyzing document text and building structured technical candidate profile.</p>
          </div>
          <div className="inline-flex items-center space-x-2 text-xs font-medium text-amber-700 bg-amber-100 px-3 py-1 rounded-full">
            <RefreshCw className="w-3.5 h-3.5 animate-spin" />
            <span>Status: {extractionStatus || 'PENDING'}</span>
          </div>
        </div>
      )}

      {/* Extracted Profile Preview Highlights */}
      {!loading && profile && (
        <div className="space-y-6">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold font-heading text-slate-900 uppercase tracking-wider flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-[#0E6E76]" />
                <span>Extracted Technical Skills</span>
              </h3>
              {profile.extractionSource === 'MOCK' ? (
                <span className="text-xs bg-amber-100 border border-amber-300 text-amber-800 font-bold px-2.5 py-0.5 rounded-full flex items-center gap-1">
                  ⚠️ Mock data
                </span>
              ) : (
                <span className="text-xs bg-emerald-100 border border-emerald-300 text-emerald-800 font-medium px-2.5 py-0.5 rounded-full">
                  LLM Extracted
                </span>
              )}
            </div>
            {profile.skills && profile.skills.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {profile.skills.map((skill, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1.5 bg-[#E6F4F5] text-[#0E6E76] font-semibold text-xs rounded-lg border border-teal-100"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            ) : (
              <div className="text-xs text-slate-400 italic">No skills extracted yet. Upload a PDF resume above.</div>
            )}
          </div>

          {/* Extracted Projects */}
          {profile.projects && profile.projects.length > 0 && (
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
              <h3 className="text-sm font-bold font-heading text-slate-900 uppercase tracking-wider flex items-center space-x-2">
                <Briefcase className="w-4 h-4 text-[#0E6E76]" />
                <span>Extracted Projects</span>
              </h3>
              <div className="space-y-3">
                {profile.projects.map((project, idx) => (
                  <div key={idx} className="p-4 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                    <div className="font-bold text-sm text-slate-900">{project.title || 'Project'}</div>
                    <div className="text-slate-600">{project.description}</div>
                    {project.techStack && (
                      <div className="text-[11px] text-[#0E6E76] font-mono mt-1">Tech Stack: {project.techStack}</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Navigation Buttons Footer */}
      <div className="pt-4 flex items-center justify-between">
        <button
          onClick={onBackToAcademic}
          id="back-to-academic-btn"
          className="px-5 py-2.5 text-slate-600 hover:bg-slate-100 font-bold rounded-xl text-xs flex items-center space-x-2 transition-all"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>← Back to Academic Setup</span>
        </button>

        <button
          onClick={onViewProfile}
          id="view-full-profile-btn"
          className="px-6 py-3 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold rounded-xl text-xs shadow-md flex items-center space-x-2 transition-all"
        >
          <span>View Complete Profile Dashboard</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
