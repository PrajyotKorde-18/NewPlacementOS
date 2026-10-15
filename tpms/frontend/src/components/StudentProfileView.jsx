import React, { useState, useEffect } from 'react';
import {
  Upload,
  FileText,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Code,
  Award,
  Briefcase,
  Sparkles,
  GraduationCap,
  BookOpen,
  AlertTriangle,
  Edit3,
  ExternalLink,
  Plus,
  Trash2,
  X,
  Save,
  Phone,
  Hash,
  Globe,
  Linkedin,
  Github,
  Calendar,
  Check
} from 'lucide-react';
import { api } from '../api';

export default function StudentProfileView({ currentUser, onEditAcademic, onUploadResume }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [extractionStatus, setExtractionStatus] = useState(null);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [versionId, setVersionId] = useState(null);

  // Edit Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editForm, setEditForm] = useState({
    collegeRollNumber: '',
    phoneNumber: '',
    placementStatus: 'NOT_PLACED',
    linkedinUrl: '',
    githubUrl: '',
    portfolioUrl: '',
    academicProfile: {
      tenthPercentage: '',
      tenthCgpa: '',
      tenthBoard: '',
      tenthYearOfPassing: '',
      twelfthPercentage: '',
      twelfthCgpa: '',
      twelfthBoard: '',
      twelfthStream: '',
      twelfthYearOfPassing: '',
      branch: '',
      admissionYear: '',
      expectedGraduationYear: '',
      semesterSgpas: [],
      backlogRecords: [],
    },
    structuredCertifications: [],
  });

  const studentId = currentUser?.id || 1;

  useEffect(() => {
    fetchProfile();
  }, [studentId]);

  // Poll resume extraction status
  useEffect(() => {
    let interval = null;
    if (versionId && extractionStatus === 'PENDING') {
      interval = setInterval(async () => {
        try {
          const statusRes = await api.getResumeStatus(versionId);
          setExtractionStatus(statusRes.extractionStatus);
          if (statusRes.extractionStatus === 'PROCESSED') {
            clearInterval(interval);
            setUploading(false);
            setSuccessMsg('Resume parsed successfully! Extracted data updated.');
            fetchProfile();
          } else if (statusRes.extractionStatus === 'FLAGGED_FOR_REVIEW') {
            clearInterval(interval);
            setUploading(false);
            setError('Extraction Flagged for Review: Could not extract sufficient structured data.');
            fetchProfile();
          } else if (statusRes.extractionStatus === 'FAILED') {
            clearInterval(interval);
            setUploading(false);
            setError(`Resume extraction failed: ${statusRes.errorLog || 'Unknown error'}`);
          }
        } catch (err) {
          console.error(err);
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
      populateEditForm(data);
    } catch (err) {
      console.error('Failed to load profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const populateEditForm = (data) => {
    if (!data) return;
    const ac = data.academicProfile || {};
    setEditForm({
      collegeRollNumber: data.collegeRollNumber || '',
      phoneNumber: data.phoneNumber || '',
      placementStatus: data.placementStatus || 'NOT_PLACED',
      linkedinUrl: data.linkedinUrl || '',
      githubUrl: data.githubUrl || '',
      portfolioUrl: data.portfolioUrl || '',
      academicProfile: {
        tenthPercentage: ac.tenthPercentage !== null && ac.tenthPercentage !== undefined ? ac.tenthPercentage : '',
        tenthCgpa: ac.tenthCgpa !== null && ac.tenthCgpa !== undefined ? ac.tenthCgpa : '',
        tenthBoard: ac.tenthBoard || '',
        tenthYearOfPassing: ac.tenthYearOfPassing || '',
        twelfthPercentage: ac.twelfthPercentage !== null && ac.twelfthPercentage !== undefined ? ac.twelfthPercentage : '',
        twelfthCgpa: ac.twelfthCgpa !== null && ac.twelfthCgpa !== undefined ? ac.twelfthCgpa : '',
        twelfthBoard: ac.twelfthBoard || '',
        twelfthStream: ac.twelfthStream || '',
        twelfthYearOfPassing: ac.twelfthYearOfPassing || '',
        branch: ac.branch || '',
        admissionYear: ac.admissionYear || '',
        expectedGraduationYear: ac.expectedGraduationYear || '',
        semesterSgpas: (ac.semesterSgpas || []).map(s => ({ ...s })),
        backlogRecords: (ac.backlogRecords || []).map(b => ({ ...b })),
      },
      structuredCertifications: (data.certifications || []).map(c => ({
        name: c.name || '',
        issuingBody: c.issuingBody || c.issuer || '',
        issueDate: c.issueDate || '',
        credentialUrl: c.credentialUrl || '',
      })),
    });
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

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccessMsg('');

    try {
      // Build payload cleanly converting strings to numbers where appropriate
      const ac = editForm.academicProfile;
      const payload = {
        collegeRollNumber: editForm.collegeRollNumber && editForm.collegeRollNumber.trim() !== '' ? editForm.collegeRollNumber.trim() : null,
        phoneNumber: editForm.phoneNumber && editForm.phoneNumber.trim() !== '' ? editForm.phoneNumber.trim() : null,
        placementStatus: editForm.placementStatus,
        linkedinUrl: editForm.linkedinUrl && editForm.linkedinUrl.trim() !== '' ? editForm.linkedinUrl.trim() : null,
        githubUrl: editForm.githubUrl && editForm.githubUrl.trim() !== '' ? editForm.githubUrl.trim() : null,
        portfolioUrl: editForm.portfolioUrl && editForm.portfolioUrl.trim() !== '' ? editForm.portfolioUrl.trim() : null,
        academicProfile: {
          tenthPercentage: ac.tenthPercentage !== '' && ac.tenthPercentage !== null && !isNaN(ac.tenthPercentage) ? parseFloat(ac.tenthPercentage) : null,
          tenthCgpa: ac.tenthCgpa !== '' && ac.tenthCgpa !== null && !isNaN(ac.tenthCgpa) ? parseFloat(ac.tenthCgpa) : null,
          tenthBoard: ac.tenthBoard && ac.tenthBoard.trim() !== '' ? ac.tenthBoard.trim() : null,
          tenthYearOfPassing: ac.tenthYearOfPassing !== '' && ac.tenthYearOfPassing !== null && !isNaN(ac.tenthYearOfPassing) ? parseInt(ac.tenthYearOfPassing, 10) : null,
          twelfthPercentage: ac.twelfthPercentage !== '' && ac.twelfthPercentage !== null && !isNaN(ac.twelfthPercentage) ? parseFloat(ac.twelfthPercentage) : null,
          twelfthCgpa: ac.twelfthCgpa !== '' && ac.twelfthCgpa !== null && !isNaN(ac.twelfthCgpa) ? parseFloat(ac.twelfthCgpa) : null,
          twelfthBoard: ac.twelfthBoard && ac.twelfthBoard.trim() !== '' ? ac.twelfthBoard.trim() : null,
          twelfthStream: ac.twelfthStream && ac.twelfthStream.trim() !== '' ? ac.twelfthStream.trim() : null,
          twelfthYearOfPassing: ac.twelfthYearOfPassing !== '' && ac.twelfthYearOfPassing !== null && !isNaN(ac.twelfthYearOfPassing) ? parseInt(ac.twelfthYearOfPassing, 10) : null,
          branch: ac.branch && ac.branch.trim() !== '' ? ac.branch.trim() : null,
          admissionYear: ac.admissionYear !== '' && ac.admissionYear !== null && !isNaN(ac.admissionYear) ? parseInt(ac.admissionYear, 10) : null,
          expectedGraduationYear: ac.expectedGraduationYear !== '' && ac.expectedGraduationYear !== null && !isNaN(ac.expectedGraduationYear) ? parseInt(ac.expectedGraduationYear, 10) : null,
          semesterSgpas: (ac.semesterSgpas || [])
            .filter(s => s.sgpa !== '' && s.sgpa !== null && !isNaN(parseFloat(s.sgpa)))
            .map(s => ({
              semesterNumber: parseInt(s.semesterNumber, 10),
              sgpa: parseFloat(s.sgpa),
              isFinalized: s.isFinalized !== undefined ? Boolean(s.isFinalized) : true,
            })),
          backlogRecords: (ac.backlogRecords || [])
            .filter(b => b.subjectName && b.subjectName.trim() !== '')
            .map(b => ({
              subjectName: b.subjectName.trim(),
              semester: parseInt(b.semester, 10),
              cleared: Boolean(b.cleared),
              clearedOn: b.clearedOn && b.clearedOn.trim() !== '' ? b.clearedOn.trim() : null,
            })),
        },
        structuredCertifications: (editForm.structuredCertifications || [])
          .filter(c => c.name && c.name.trim() !== '' && c.issuingBody && c.issuingBody.trim() !== '')
          .map(c => ({
            name: c.name.trim(),
            issuingBody: c.issuingBody.trim(),
            issueDate: c.issueDate && c.issueDate.trim() !== '' ? c.issueDate.trim() : null,
            credentialUrl: c.credentialUrl && c.credentialUrl.trim() !== '' ? c.credentialUrl.trim() : null,
          })),
      };

      const updated = await api.updateStudentProfile(studentId, payload);
      setProfile(updated);
      populateEditForm(updated);
      setIsEditModalOpen(false);
      setSuccessMsg('Academic Profile & Details saved successfully!');
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to save student profile');
    } finally {
      setSaving(false);
    }
  };

  const getPlacementStatusBadge = (status) => {
    switch (status) {
      case 'PLACED_FINAL':
        return (
          <span className="inline-flex items-center space-x-1.5 bg-emerald-100 text-emerald-800 border border-emerald-300 text-xs font-bold px-3 py-1 rounded-full shadow-2xs">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Placed (Final)</span>
          </span>
        );
      case 'PLACED_OPEN_TO_BETTER':
        return (
          <span className="inline-flex items-center space-x-1.5 bg-blue-100 text-blue-800 border border-blue-300 text-xs font-bold px-3 py-1 rounded-full shadow-2xs">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Placed (Open to Better)</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center space-x-1.5 bg-amber-100 text-amber-800 border border-amber-300 text-xs font-bold px-3 py-1 rounded-full shadow-2xs">
            <GraduationCap className="w-3.5 h-3.5" />
            <span>Not Placed (Eligible)</span>
          </span>
        );
    }
  };

  const academic = profile?.academicProfile;

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8 animate-in fade-in duration-300">
      {/* Top Banner Header */}
      <div className="bg-gradient-to-r from-[#0E6E76] via-[#0D6269] to-[#0A4E53] rounded-2xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 translate-x-1/4 -translate-y-1/4 w-80 h-80 bg-white/10 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0 relative z-10">
          <div>
            <div className="inline-flex items-center space-x-2 bg-white/15 px-3 py-1 rounded-full text-xs font-semibold backdrop-blur-md mb-3">
              <Sparkles className="w-3.5 h-3.5 text-teal-200" />
              <span>TPMS Candidate Eligibility & Profile System</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold font-heading">
              {currentUser?.name || 'Student Profile'}
            </h1>
            <p className="text-teal-100 text-sm mt-1">
              Academic Eligibility Details, Sem-wise SGPAs, Backlogs & Extracted Signals
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => onEditAcademic ? onEditAcademic() : setIsEditModalOpen(true)}
              id="edit-profile-btn"
              className="bg-white/20 hover:bg-white/30 backdrop-blur-md text-white px-4 py-2.5 rounded-xl font-bold text-sm border border-white/30 shadow-md transition-all flex items-center space-x-2"
            >
              <Edit3 className="w-4 h-4" />
              <span>Edit Academic Details</span>
            </button>

            <button
              onClick={() => onUploadResume ? onUploadResume() : null}
              id="upload-resume-btn"
              className="bg-white text-[#0E6E76] hover:bg-teal-50 px-5 py-2.5 rounded-xl font-bold text-sm shadow-md transition-all flex items-center space-x-2 shrink-0"
            >
              <Upload className="w-4 h-4" />
              <span>Upload Resume PDF</span>
            </button>
          </div>
        </div>
      </div>

      {/* Success Banner */}
      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between text-emerald-800 text-sm font-medium animate-in fade-in">
          <div className="flex items-center space-x-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg('')} className="text-emerald-600 hover:text-emerald-800">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Uploading Status Indicator */}
      {uploading && (
        <div className="bg-teal-50 border-2 border-dashed border-[#0E6E76] rounded-2xl p-6 text-center space-y-3">
          <div className="w-12 h-12 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto" />
          <div>
            <h3 className="font-bold text-[#0E6E76] text-lg font-heading">Groq LLM Resume Extraction in Progress...</h3>
            <p className="text-xs text-slate-600 mt-1">Analyzing PDF resume and building structured candidate profile.</p>
          </div>
          <div className="inline-flex items-center space-x-2 text-xs font-medium text-amber-700 bg-amber-100 px-3 py-1 rounded-full">
            <RefreshCw className="w-3.5 h-3.5 animate-spin" />
            <span>Status: {extractionStatus || 'PENDING'}</span>
          </div>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">Notice</div>
            <div className="text-xs mt-0.5">{error}</div>
          </div>
        </div>
      )}

      {/* Loading state */}
      {loading && !uploading && (
        <div className="py-16 text-center">
          <div className="w-10 h-10 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-sm text-slate-500 font-medium">Fetching candidate profile details...</p>
        </div>
      )}

      {!loading && profile && (
        <div className="space-y-8">
          {/* Key Eligibility Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-5">
            {/* Current CGPA */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-2 relative overflow-hidden">
              <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
                <span>Current CGPA</span>
                <GraduationCap className="w-4 h-4 text-[#0E6E76]" />
              </div>
              <div className="text-3xl font-extrabold font-heading text-slate-900">
                {academic?.currentCgpa ? `${academic.currentCgpa} / 10.0` : 'N/A'}
              </div>
              <p className="text-xs text-slate-500">
                Based on {academic?.semesterSgpas?.filter(s => s.isFinalized).length || 0} finalized semesters
              </p>
            </div>

            {/* Active Backlogs */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-2 relative overflow-hidden">
              <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
                <span>Active Backlogs</span>
                <AlertTriangle className={`w-4 h-4 ${(academic?.activeBacklogCount || 0) > 0 ? 'text-amber-500' : 'text-emerald-500'}`} />
              </div>
              <div className="flex items-baseline space-x-2">
                <span className={`text-3xl font-extrabold font-heading ${(academic?.activeBacklogCount || 0) > 0 ? 'text-amber-600' : 'text-slate-900'}`}>
                  {academic?.activeBacklogCount || 0}
                </span>
                <span className="text-xs text-slate-400">
                  (Total ever: {academic?.totalEverBacklogCount || 0})
                </span>
              </div>
              <p className="text-xs text-slate-500">
                {(academic?.activeBacklogCount || 0) === 0 ? 'Eligible for 0-backlog drives' : 'Active backlogs need clearing'}
              </p>
            </div>

            {/* 10th & 12th Qualifications */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-2">
              <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
                <span>School Qualifications</span>
                <BookOpen className="w-4 h-4 text-[#0E6E76]" />
              </div>
              <div className="text-xs space-y-1 text-slate-700 font-medium">
                <div>10th: <span className="font-bold text-slate-900">{academic?.tenthPercentage ? `${academic.tenthPercentage}%` : academic?.tenthCgpa ? `${academic.tenthCgpa} CGPA` : 'N/A'}</span> ({academic?.tenthBoard || 'Board'})</div>
                <div>12th: <span className="font-bold text-slate-900">{academic?.twelfthPercentage ? `${academic.twelfthPercentage}%` : academic?.twelfthCgpa ? `${academic.twelfthCgpa} CGPA` : 'N/A'}</span> ({academic?.twelfthStream || 'Stream'})</div>
              </div>
            </div>

            {/* Roll & Branch Info */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-2">
              <div className="flex items-center justify-between text-slate-500 text-xs font-bold uppercase tracking-wider">
                <span>College Identity</span>
                <Hash className="w-4 h-4 text-[#0E6E76]" />
              </div>
              <div className="text-sm font-bold font-heading text-slate-900">
                {profile.collegeRollNumber || profile.collegeId || 'Roll N/A'}
              </div>
              <p className="text-xs text-slate-500">
                {academic?.branch || 'Computer Science'} ({academic?.admissionYear || 2022} - {academic?.expectedGraduationYear || 2026})
              </p>
            </div>
          </div>

          {/* Grid Layout for Profile Details */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Left Sidebar: Contact & Identity Card */}
            <div className="md:col-span-1 space-y-6">
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-5">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 rounded-2xl bg-teal-100 text-[#0E6E76] font-bold text-2xl flex items-center justify-center font-heading">
                    {currentUser?.name?.charAt(0) || 'S'}
                  </div>
                  <div>
                    <h3 className="font-bold font-heading text-lg text-slate-900">{currentUser?.name || 'Student Candidate'}</h3>
                    <div className="text-xs text-slate-500 font-mono mt-0.5">{profile.email || currentUser?.email}</div>
                    <div className="mt-2">{getPlacementStatusBadge(profile.placementStatus)}</div>
                  </div>
                </div>

                <hr className="border-slate-100" />

                {/* Identity & Contact Attributes */}
                <div className="space-y-3 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500 flex items-center space-x-1.5">
                      <Hash className="w-3.5 h-3.5 text-[#0E6E76]" />
                      <span>Roll Number:</span>
                    </span>
                    <span className="font-bold font-mono text-slate-800">{profile.collegeRollNumber || 'Not set'}</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-slate-500 flex items-center space-x-1.5">
                      <Phone className="w-3.5 h-3.5 text-[#0E6E76]" />
                      <span>Phone:</span>
                    </span>
                    <span className="font-bold text-slate-800">{profile.phoneNumber || 'Not set'}</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-slate-500 flex items-center space-x-1.5">
                      <GraduationCap className="w-3.5 h-3.5 text-[#0E6E76]" />
                      <span>Branch:</span>
                    </span>
                    <span className="font-bold text-slate-800">{academic?.branch || 'N/A'}</span>
                  </div>
                </div>

                <hr className="border-slate-100" />

                {/* Social Links */}
                <div>
                  <h4 className="text-xs font-bold uppercase text-slate-400 tracking-wider mb-3">
                    Social & Professional Links
                  </h4>
                  <div className="space-y-2">
                    {profile.linkedinUrl && (
                      <a
                        href={profile.linkedinUrl.startsWith('http') ? profile.linkedinUrl : `https://${profile.linkedinUrl}`}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between p-2.5 bg-blue-50/60 hover:bg-blue-100/60 rounded-xl text-xs font-semibold text-blue-700 transition-colors border border-blue-100"
                      >
                        <span className="flex items-center space-x-2">
                          <Linkedin className="w-4 h-4 text-blue-600" />
                          <span>LinkedIn Profile</span>
                        </span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    )}

                    {profile.githubUrl && (
                      <a
                        href={profile.githubUrl.startsWith('http') ? profile.githubUrl : `https://${profile.githubUrl}`}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between p-2.5 bg-slate-100 hover:bg-slate-200/80 rounded-xl text-xs font-semibold text-slate-800 transition-colors border border-slate-200"
                      >
                        <span className="flex items-center space-x-2">
                          <Github className="w-4 h-4 text-slate-800" />
                          <span>GitHub Profile</span>
                        </span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    )}

                    {profile.portfolioUrl && (
                      <a
                        href={profile.portfolioUrl.startsWith('http') ? profile.portfolioUrl : `https://${profile.portfolioUrl}`}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between p-2.5 bg-teal-50 hover:bg-teal-100/70 rounded-xl text-xs font-semibold text-[#0E6E76] transition-colors border border-teal-100"
                      >
                        <span className="flex items-center space-x-2">
                          <Globe className="w-4 h-4 text-[#0E6E76]" />
                          <span>Portfolio Website</span>
                        </span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    )}

                    {!profile.linkedinUrl && !profile.githubUrl && !profile.portfolioUrl && (
                      <div className="text-xs text-slate-400 italic">No social links added yet. Click "Edit Academic Details" to add links.</div>
                    )}
                  </div>
                </div>

                {/* Coding Profiles */}
                {profile.codingProfiles && Object.keys(profile.codingProfiles).length > 0 && (
                  <div>
                    <h4 className="text-xs font-bold uppercase text-slate-400 tracking-wider mb-3 flex items-center space-x-1.5">
                      <Code className="w-3.5 h-3.5 text-[#0E6E76]" />
                      <span>Extracted Coding Handles</span>
                    </h4>
                    <div className="space-y-1.5">
                      {Object.entries(profile.codingProfiles).map(([platform, link]) => (
                        <a
                          key={platform}
                          href={link}
                          target="_blank"
                          rel="noreferrer"
                          className="flex items-center justify-between p-2 bg-slate-50 hover:bg-teal-50 rounded-lg text-xs font-medium text-slate-700 transition-colors"
                        >
                          <span className="capitalize font-bold text-slate-900">{platform}</span>
                          <span className="text-[10px] text-slate-400 truncate max-w-[120px]">{link}</span>
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Right Column: Academic Breakdown & Extracted Signals */}
            <div className="md:col-span-2 space-y-6">
              {/* Semester-wise SGPA Breakdown */}
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold font-heading text-slate-900 flex items-center space-x-2">
                    <BookOpen className="w-4 h-4 text-[#0E6E76]" />
                    <span>Semester-wise SGPA Breakdown</span>
                  </h4>
                  <span className="text-xs text-slate-400 font-medium">
                    Cumulative: <strong className="text-slate-900">{academic?.currentCgpa || 'N/A'}</strong>
                  </span>
                </div>

                {academic?.semesterSgpas && academic.semesterSgpas.length > 0 ? (
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {academic.semesterSgpas.map((sem, idx) => (
                      <div
                        key={idx}
                        className={`p-3 rounded-xl border text-center space-y-1 transition-all ${
                          sem.isFinalized
                            ? 'bg-teal-50/50 border-teal-200/80 shadow-2xs'
                            : 'bg-slate-50 border-slate-200 opacity-75'
                        }`}
                      >
                        <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          Semester {sem.semesterNumber}
                        </div>
                        <div className="text-lg font-extrabold font-heading text-slate-900">
                          {sem.sgpa?.toFixed(2)}
                        </div>
                        <div className="text-[10px] font-semibold text-teal-700 bg-teal-100/60 rounded px-1.5 py-0.5 inline-block">
                          {sem.isFinalized ? 'Finalized' : 'Pending'}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="p-4 bg-slate-50 rounded-xl text-center text-xs text-slate-400 italic border border-slate-100">
                    No semester SGPA records entered yet. Click "Edit Academic Details" to add SGPAs.
                  </div>
                )}
              </div>

              {/* Backlog Records Tracking */}
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                <h4 className="text-sm font-bold font-heading text-slate-900 flex items-center space-x-2">
                  <AlertTriangle className="w-4 h-4 text-amber-600" />
                  <span>Backlog History & Status</span>
                </h4>

                {academic?.backlogRecords && academic.backlogRecords.length > 0 ? (
                  <div className="space-y-2">
                    {academic.backlogRecords.map((b, idx) => (
                      <div
                        key={idx}
                        className={`p-3.5 rounded-xl border flex items-center justify-between text-xs ${
                          b.cleared
                            ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900'
                            : 'bg-amber-50/60 border-amber-200 text-amber-900'
                        }`}
                      >
                        <div>
                          <span className="font-bold text-sm text-slate-900">{b.subjectName}</span>
                          <span className="ml-2 text-slate-500 text-[11px]">(Semester {b.semester})</span>
                        </div>

                        <div>
                          {b.cleared ? (
                            <span className="inline-flex items-center space-x-1 font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-md">
                              <Check className="w-3.5 h-3.5" />
                              <span>Cleared {b.clearedOn ? `on ${b.clearedOn}` : ''}</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center space-x-1 font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-md">
                              <AlertTriangle className="w-3.5 h-3.5" />
                              <span>Active Backlog</span>
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-xl text-xs text-emerald-700 font-medium flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Clean Academic Record: No active or historical backlogs reported!</span>
                  </div>
                )}
              </div>

              {/* Verified Structured Certifications */}
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                <h4 className="text-sm font-bold font-heading text-slate-900 flex items-center space-x-2">
                  <Award className="w-4 h-4 text-[#0E6E76]" />
                  <span>Certifications</span>
                </h4>

                {profile.certifications && profile.certifications.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {profile.certifications.map((cert, idx) => (
                      <div key={idx} className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl text-xs space-y-1">
                        <div className="font-bold text-slate-900 text-sm">{cert.name || cert.title}</div>
                        <div className="text-slate-500 font-medium">{cert.issuingBody || cert.issuer || 'Industry Certified'}</div>
                        {cert.credentialUrl && (
                          <a
                            href={cert.credentialUrl.startsWith('http') ? cert.credentialUrl : `https://${cert.credentialUrl}`}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center space-x-1 text-teal-700 hover:underline font-semibold mt-1"
                          >
                            <span>Verify Credential</span>
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-xs text-slate-400 italic">No certifications listed.</div>
                )}
              </div>

              {/* Extracted Technical Skills */}
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold font-heading text-slate-900 flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-[#0E6E76]" />
                    <span>Extracted Skills</span>
                  </h4>
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
                        className="px-3 py-1.5 bg-[#E6F4F5] text-[#0E6E76] font-semibold text-xs rounded-lg border border-teal-100 shadow-2xs"
                      >
                        {skill}
                      </span>
                    ))}
                  </div>
                ) : (
                  <div className="text-xs text-slate-400 italic">No skills extracted yet. Upload a resume PDF to auto-extract.</div>
                )}
              </div>

              {/* Extracted Projects */}
              {profile.projects && profile.projects.length > 0 && (
                <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
                  <h4 className="text-sm font-bold font-heading text-slate-900 flex items-center space-x-2">
                    <Briefcase className="w-4 h-4 text-[#0E6E76]" />
                    <span>Extracted Resume Projects</span>
                  </h4>
                  <div className="space-y-3">
                    {profile.projects.map((project, idx) => (
                      <div key={idx} className="p-4 bg-slate-50 rounded-xl border border-slate-100 space-y-1">
                        <div className="font-bold text-sm text-slate-900">{project.title || 'Project'}</div>
                        <div className="text-xs text-slate-600">{project.description}</div>
                        {project.techStack && (
                          <div className="text-[11px] text-[#0E6E76] font-mono mt-1">Tech Stack: {project.techStack}</div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modern Interactive Edit Profile Modal */}
      {isEditModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-slate-200 animate-in zoom-in-95 duration-200 my-8">
            {/* Modal Header */}
            <div className="p-6 bg-slate-900 text-white rounded-t-2xl flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Edit3 className="w-5 h-5 text-teal-400" />
                <h3 className="text-lg font-bold font-heading">Edit Academic & Signal Profile</h3>
              </div>
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Form Body */}
            <form onSubmit={handleSaveProfile} className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
              {/* Section 1: Basic Identity */}
              <div className="space-y-4">
                <h4 className="font-bold font-heading text-sm text-[#0E6E76] uppercase tracking-wider border-b pb-2">
                  Identity & Placement Signals
                </h4>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">College Roll Number</label>
                    <input
                      type="text"
                      value={editForm.collegeRollNumber}
                      onChange={(e) => setEditForm({ ...editForm, collegeRollNumber: e.target.value })}
                      placeholder="e.g. 2022CS045"
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Phone Number</label>
                    <input
                      type="text"
                      value={editForm.phoneNumber}
                      onChange={(e) => setEditForm({ ...editForm, phoneNumber: e.target.value })}
                      placeholder="e.g. 9876543210"
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Placement Status</label>
                    <select
                      value={editForm.placementStatus}
                      onChange={(e) => setEditForm({ ...editForm, placementStatus: e.target.value })}
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none bg-white"
                    >
                      <option value="NOT_PLACED">NOT_PLACED</option>
                      <option value="PLACED_OPEN_TO_BETTER">PLACED_OPEN_TO_BETTER</option>
                      <option value="PLACED_FINAL">PLACED_FINAL</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">LinkedIn URL</label>
                    <input
                      type="text"
                      value={editForm.linkedinUrl}
                      onChange={(e) => setEditForm({ ...editForm, linkedinUrl: e.target.value })}
                      placeholder="linkedin.com/in/username"
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">GitHub URL</label>
                    <input
                      type="text"
                      value={editForm.githubUrl}
                      onChange={(e) => setEditForm({ ...editForm, githubUrl: e.target.value })}
                      placeholder="github.com/username"
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Portfolio URL</label>
                    <input
                      type="text"
                      value={editForm.portfolioUrl}
                      onChange={(e) => setEditForm({ ...editForm, portfolioUrl: e.target.value })}
                      placeholder="myportfolio.com"
                      className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
                    />
                  </div>
                </div>
              </div>

              {/* Section 2: Schooling & Degree */}
              <div className="space-y-4">
                <h4 className="font-bold font-heading text-sm text-[#0E6E76] uppercase tracking-wider border-b pb-2">
                  10th, 12th & Graduation Details
                </h4>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">10th Percentage %</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      max="100"
                      value={editForm.academicProfile.tenthPercentage}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, tenthPercentage: e.target.value }
                      })}
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">10th Board</label>
                    <input
                      type="text"
                      value={editForm.academicProfile.tenthBoard}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, tenthBoard: e.target.value }
                      })}
                      placeholder="CBSE / ICSE / State"
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">12th Percentage %</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      max="100"
                      value={editForm.academicProfile.twelfthPercentage}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, twelfthPercentage: e.target.value }
                      })}
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">12th Stream</label>
                    <input
                      type="text"
                      value={editForm.academicProfile.twelfthStream}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, twelfthStream: e.target.value }
                      })}
                      placeholder="Science (PCM)"
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Engineering Branch</label>
                    <input
                      type="text"
                      value={editForm.academicProfile.branch}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, branch: e.target.value }
                      })}
                      placeholder="Computer Science Engineering"
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Admission Year</label>
                    <input
                      type="number"
                      value={editForm.academicProfile.admissionYear}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, admissionYear: e.target.value }
                      })}
                      placeholder="2022"
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Expected Graduation Year</label>
                    <input
                      type="number"
                      value={editForm.academicProfile.expectedGraduationYear}
                      onChange={(e) => setEditForm({
                        ...editForm,
                        academicProfile: { ...editForm.academicProfile, expectedGraduationYear: e.target.value }
                      })}
                      placeholder="2026"
                      className="w-full px-3 py-2 border rounded-lg outline-none"
                    />
                  </div>
                </div>
              </div>

              {/* Section 3: Semester SGPAs */}
              <div className="space-y-3">
                <div className="flex items-center justify-between border-b pb-2">
                  <h4 className="font-bold font-heading text-sm text-[#0E6E76] uppercase tracking-wider">
                    Semester-wise SGPAs (1 to 8)
                  </h4>
                  <button
                    type="button"
                    onClick={() => {
                      const list = editForm.academicProfile.semesterSgpas;
                      const nextSem = list.length + 1;
                      if (nextSem <= 10) {
                        setEditForm({
                          ...editForm,
                          academicProfile: {
                            ...editForm.academicProfile,
                            semesterSgpas: [...list, { semesterNumber: nextSem, sgpa: 8.0, isFinalized: true }]
                          }
                        });
                      }
                    }}
                    className="text-xs bg-teal-50 hover:bg-teal-100 text-[#0E6E76] px-3 py-1 rounded-lg font-bold flex items-center space-x-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Semester</span>
                  </button>
                </div>

                <div className="space-y-2">
                  {editForm.academicProfile.semesterSgpas.map((sem, idx) => (
                    <div key={idx} className="flex items-center space-x-3 bg-slate-50 p-2.5 rounded-xl border border-slate-200">
                      <span className="font-bold text-slate-700 w-24">Sem {sem.semesterNumber}:</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="10"
                        value={sem.sgpa}
                        onChange={(e) => {
                          const list = [...editForm.academicProfile.semesterSgpas];
                          list[idx].sgpa = e.target.value;
                          setEditForm({
                            ...editForm,
                            academicProfile: { ...editForm.academicProfile, semesterSgpas: list }
                          });
                        }}
                        placeholder="SGPA (e.g. 8.5)"
                        className="w-32 px-3 py-1.5 border rounded-lg outline-none bg-white font-mono"
                      />
                      <label className="flex items-center space-x-1.5 cursor-pointer text-slate-600">
                        <input
                          type="checkbox"
                          checked={sem.isFinalized}
                          onChange={(e) => {
                            const list = [...editForm.academicProfile.semesterSgpas];
                            list[idx].isFinalized = e.target.checked;
                            setEditForm({
                              ...editForm,
                              academicProfile: { ...editForm.academicProfile, semesterSgpas: list }
                            });
                          }}
                          className="rounded text-[#0E6E76] focus:ring-[#0E6E76]"
                        />
                        <span>Finalized Result</span>
                      </label>
                      <button
                        type="button"
                        onClick={() => {
                          const list = editForm.academicProfile.semesterSgpas.filter((_, i) => i !== idx);
                          setEditForm({
                            ...editForm,
                            academicProfile: { ...editForm.academicProfile, semesterSgpas: list }
                          });
                        }}
                        className="text-red-500 hover:text-red-700 ml-auto"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Section 4: Backlog Records */}
              <div className="space-y-3">
                <div className="flex items-center justify-between border-b pb-2">
                  <h4 className="font-bold font-heading text-sm text-[#0E6E76] uppercase tracking-wider">
                    Backlog Records
                  </h4>
                  <button
                    type="button"
                    onClick={() => {
                      const list = editForm.academicProfile.backlogRecords;
                      setEditForm({
                        ...editForm,
                        academicProfile: {
                          ...editForm.academicProfile,
                          backlogRecords: [...list, { subjectName: '', semester: 1, cleared: false, clearedOn: '' }]
                        }
                      });
                    }}
                    className="text-xs bg-amber-50 hover:bg-amber-100 text-amber-800 px-3 py-1 rounded-lg font-bold flex items-center space-x-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Backlog Record</span>
                  </button>
                </div>

                <div className="space-y-2">
                  {editForm.academicProfile.backlogRecords.map((b, idx) => (
                    <div key={idx} className="flex flex-wrap items-center gap-2 bg-amber-50/40 p-2.5 rounded-xl border border-amber-200">
                      <input
                        type="text"
                        value={b.subjectName}
                        onChange={(e) => {
                          const list = [...editForm.academicProfile.backlogRecords];
                          list[idx].subjectName = e.target.value;
                          setEditForm({
                            ...editForm,
                            academicProfile: { ...editForm.academicProfile, backlogRecords: list }
                          });
                        }}
                        placeholder="Subject Name (e.g. Mathematics)"
                        className="flex-1 min-w-[160px] px-3 py-1.5 border rounded-lg outline-none bg-white"
                      />
                      <select
                        value={b.semester}
                        onChange={(e) => {
                          const list = [...editForm.academicProfile.backlogRecords];
                          list[idx].semester = e.target.value;
                          setEditForm({
                            ...editForm,
                            academicProfile: { ...editForm.academicProfile, backlogRecords: list }
                          });
                        }}
                        className="px-2 py-1.5 border rounded-lg outline-none bg-white"
                      >
                        {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                          <option key={s} value={s}>Sem {s}</option>
                        ))}
                      </select>
                      <label className="flex items-center space-x-1 cursor-pointer text-slate-700 font-medium">
                        <input
                          type="checkbox"
                          checked={b.cleared}
                          onChange={(e) => {
                            const list = [...editForm.academicProfile.backlogRecords];
                            list[idx].cleared = e.target.checked;
                            setEditForm({
                              ...editForm,
                              academicProfile: { ...editForm.academicProfile, backlogRecords: list }
                            });
                          }}
                          className="rounded text-emerald-600"
                        />
                        <span>Cleared</span>
                      </label>
                      <button
                        type="button"
                        onClick={() => {
                          const list = editForm.academicProfile.backlogRecords.filter((_, i) => i !== idx);
                          setEditForm({
                            ...editForm,
                            academicProfile: { ...editForm.academicProfile, backlogRecords: list }
                          });
                        }}
                        className="text-red-500 hover:text-red-700 ml-auto"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Modal Footer */}
              <div className="pt-4 border-t flex items-center justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 text-slate-600 hover:bg-slate-100 font-bold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-6 py-2 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold rounded-xl shadow-md flex items-center space-x-2 transition-all disabled:opacity-50"
                >
                  {saving ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Saving Profile...</span>
                    </>
                  ) : (
                    <>
                      <Save className="w-4 h-4" />
                      <span>Save Profile Changes</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
