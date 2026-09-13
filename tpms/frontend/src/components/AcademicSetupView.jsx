import React, { useState, useEffect } from 'react';
import {
  GraduationCap,
  BookOpen,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Plus,
  Trash2,
  Hash,
  Phone,
  Linkedin,
  Github,
  Globe,
  RefreshCw,
  Sparkles,
  Check,
  Building2,
  FileCheck,
  Upload
} from 'lucide-react';
import { api } from '../api';

export default function AcademicSetupView({ currentUser, onProceedToResume }) {
  const [activeStep, setActiveStep] = useState(1); // Step 1: Identity, Step 2: Schooling, Step 3: SGPAs & Backlogs
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const [form, setForm] = useState({
    collegeRollNumber: '',
    phoneNumber: '',
    placementStatus: 'NOT_PLACED',
    linkedinUrl: '',
    githubUrl: '',
    portfolioUrl: '',
    academicProfile: {
      tenthPercentage: '',
      tenthCgpa: '',
      tenthBoard: 'CBSE',
      tenthYearOfPassing: 2020,
      twelfthPercentage: '',
      twelfthCgpa: '',
      twelfthBoard: 'State Board',
      twelfthStream: 'Science (PCM)',
      twelfthYearOfPassing: 2022,
      branch: 'Computer Science & Engineering',
      admissionYear: 2022,
      expectedGraduationYear: 2026,
      semesterSgpas: [
        { semesterNumber: 1, sgpa: 8.0, isFinalized: true },
        { semesterNumber: 2, sgpa: 8.5, isFinalized: true }
      ],
      backlogRecords: [],
    },
    structuredCertifications: [],
  });

  const studentId = currentUser?.id || 1;

  useEffect(() => {
    fetchProfile();
  }, [studentId]);

  const fetchProfile = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getStudentProfile(studentId);
      if (data) {
        const ac = data.academicProfile || {};
        setForm({
          collegeRollNumber: data.collegeRollNumber || '',
          phoneNumber: data.phoneNumber || '',
          placementStatus: data.placementStatus || 'NOT_PLACED',
          linkedinUrl: data.linkedinUrl || '',
          githubUrl: data.githubUrl || '',
          portfolioUrl: data.portfolioUrl || '',
          academicProfile: {
            tenthPercentage: ac.tenthPercentage !== null && ac.tenthPercentage !== undefined ? ac.tenthPercentage : '',
            tenthCgpa: ac.tenthCgpa !== null && ac.tenthCgpa !== undefined ? ac.tenthCgpa : '',
            tenthBoard: ac.tenthBoard || 'CBSE',
            tenthYearOfPassing: ac.tenthYearOfPassing || 2020,
            twelfthPercentage: ac.twelfthPercentage !== null && ac.twelfthPercentage !== undefined ? ac.twelfthPercentage : '',
            twelfthCgpa: ac.twelfthCgpa !== null && ac.twelfthCgpa !== undefined ? ac.twelfthCgpa : '',
            twelfthBoard: ac.twelfthBoard || 'State Board',
            twelfthStream: ac.twelfthStream || 'Science (PCM)',
            twelfthYearOfPassing: ac.twelfthYearOfPassing || 2022,
            branch: ac.branch || 'Computer Science & Engineering',
            admissionYear: ac.admissionYear || 2022,
            expectedGraduationYear: ac.expectedGraduationYear || 2026,
            semesterSgpas: ac.semesterSgpas && ac.semesterSgpas.length > 0 ? ac.semesterSgpas.map(s => ({ ...s })) : [
              { semesterNumber: 1, sgpa: 8.0, isFinalized: true },
              { semesterNumber: 2, sgpa: 8.5, isFinalized: true }
            ],
            backlogRecords: (ac.backlogRecords || []).map(b => ({ ...b })),
          },
          structuredCertifications: (data.certifications || []).map(c => ({
            name: c.name || '',
            issuingBody: c.issuingBody || c.issuer || '',
            issueDate: c.issueDate || '',
            credentialUrl: c.credentialUrl || '',
          })),
        });
      }
    } catch (err) {
      console.error('Failed to load student profile for setup:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveAll = async () => {
    setSaving(true);
    setError('');
    setSuccessMsg('');

    try {
      const ac = form.academicProfile;
      const payload = {
        collegeRollNumber: form.collegeRollNumber && form.collegeRollNumber.trim() !== '' ? form.collegeRollNumber.trim() : null,
        phoneNumber: form.phoneNumber && form.phoneNumber.trim() !== '' ? form.phoneNumber.trim() : null,
        placementStatus: form.placementStatus,
        linkedinUrl: form.linkedinUrl && form.linkedinUrl.trim() !== '' ? form.linkedinUrl.trim() : null,
        githubUrl: form.githubUrl && form.githubUrl.trim() !== '' ? form.githubUrl.trim() : null,
        portfolioUrl: form.portfolioUrl && form.portfolioUrl.trim() !== '' ? form.portfolioUrl.trim() : null,
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
        structuredCertifications: (form.structuredCertifications || [])
          .filter(c => c.name && c.name.trim() !== '' && c.issuingBody && c.issuingBody.trim() !== '')
          .map(c => ({
            name: c.name.trim(),
            issuingBody: c.issuingBody.trim(),
            issueDate: c.issueDate && c.issueDate.trim() !== '' ? c.issueDate.trim() : null,
            credentialUrl: c.credentialUrl && c.credentialUrl.trim() !== '' ? c.credentialUrl.trim() : null,
          })),
      };

      await api.updateStudentProfile(studentId, payload);
      setSuccessMsg('Academic Profile saved successfully! Redirecting to Resume Upload...');
      
      setTimeout(() => {
        if (onProceedToResume) {
          onProceedToResume();
        }
      }, 1000);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to save academic profile details');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-10 space-y-8 animate-in fade-in duration-300">
      {/* Page Header & Step Tracker */}
      <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-[#0E6E76] bg-teal-50 px-3.5 py-1 rounded-full border border-teal-100">
              Page {activeStep} of 3 — Academic Onboarding
            </span>
            <h1 className="text-2xl font-bold font-heading text-slate-900 mt-2">
              {activeStep === 1 && "1. Identity & Contact Information"}
              {activeStep === 2 && "2. Schooling & Degree Qualifications"}
              {activeStep === 3 && "3. Semester SGPAs & Backlogs"}
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              {activeStep === 1 && "Enter your college roll number, contact info, and professional links."}
              {activeStep === 2 && "Enter your 10th, 12th percentage and engineering branch details."}
              {activeStep === 3 && "Provide semester-wise SGPAs to compute your drive eligibility CGPA."}
            </p>
          </div>
        </div>

        {/* Step Indicator Navigation Pills */}
        <div className="grid grid-cols-3 gap-3 pt-2 border-t border-slate-100">
          <button
            onClick={() => setActiveStep(1)}
            className={`p-3 rounded-2xl text-left transition-all border flex items-center space-x-3 ${
              activeStep === 1
                ? 'bg-[#E6F4F5] border-[#0E6E76] text-[#0E6E76] font-bold shadow-xs'
                : activeStep > 1
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800 font-semibold'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
              activeStep === 1 ? 'bg-[#0E6E76] text-white' : activeStep > 1 ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-600'
            }`}>
              {activeStep > 1 ? <Check className="w-4 h-4" /> : '1'}
            </div>
            <div className="hidden sm:block">
              <div className="text-xs font-heading">Step 1</div>
              <div className="text-[11px] opacity-80">Identity & Social</div>
            </div>
          </button>

          <button
            onClick={() => setActiveStep(2)}
            className={`p-3 rounded-2xl text-left transition-all border flex items-center space-x-3 ${
              activeStep === 2
                ? 'bg-[#E6F4F5] border-[#0E6E76] text-[#0E6E76] font-bold shadow-xs'
                : activeStep > 2
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800 font-semibold'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
              activeStep === 2 ? 'bg-[#0E6E76] text-white' : activeStep > 2 ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-600'
            }`}>
              {activeStep > 2 ? <Check className="w-4 h-4" /> : '2'}
            </div>
            <div className="hidden sm:block">
              <div className="text-xs font-heading">Step 2</div>
              <div className="text-[11px] opacity-80">School & Degree</div>
            </div>
          </button>

          <button
            onClick={() => setActiveStep(3)}
            className={`p-3 rounded-2xl text-left transition-all border flex items-center space-x-3 ${
              activeStep === 3
                ? 'bg-[#E6F4F5] border-[#0E6E76] text-[#0E6E76] font-bold shadow-xs'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
              activeStep === 3 ? 'bg-[#0E6E76] text-white' : 'bg-slate-200 text-slate-600'
            }`}>
              3
            </div>
            <div className="hidden sm:block">
              <div className="text-xs font-heading">Step 3</div>
              <div className="text-[11px] opacity-80">SGPAs & Backlogs</div>
            </div>
          </button>
        </div>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-3 text-emerald-800 text-sm font-medium animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">Error Saving Details</div>
            <div className="text-xs mt-0.5">{error}</div>
          </div>
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center">
          <div className="w-10 h-10 border-4 border-[#0E6E76] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-sm text-slate-500 font-medium">Loading candidate details...</p>
        </div>
      ) : (
        <div className="space-y-8">
          {/* PAGE 1: Identity & Contact */}
          {activeStep === 1 && (
            <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
              <div className="border-b pb-4">
                <h2 className="text-lg font-bold font-heading text-slate-900">
                  College Identity & Social Signals
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Provide your official college ID roll number and professional profile handles.
                </p>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    College Roll Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    id="academic-roll-input"
                    value={form.collegeRollNumber}
                    onChange={(e) => setForm({ ...form, collegeRollNumber: e.target.value })}
                    placeholder="e.g. 2022CS099"
                    className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
                    <input
                      type="text"
                      id="academic-phone-input"
                      value={form.phoneNumber}
                      onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
                      placeholder="e.g. 9876543210"
                      className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Placement Status</label>
                    <select
                      value={form.placementStatus}
                      onChange={(e) => setForm({ ...form, placementStatus: e.target.value })}
                      className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-white font-medium text-slate-800"
                    >
                      <option value="NOT_PLACED">NOT_PLACED (Eligible for all drives)</option>
                      <option value="PLACED_OPEN_TO_BETTER">PLACED_OPEN_TO_BETTER (Open to Dream drives)</option>
                      <option value="PLACED_FINAL">PLACED_FINAL (Opted out / Placed)</option>
                    </select>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-100 space-y-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Professional Links</h3>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1 flex items-center space-x-1.5">
                      <Linkedin className="w-4 h-4 text-blue-600" />
                      <span>LinkedIn Profile URL</span>
                    </label>
                    <input
                      type="text"
                      value={form.linkedinUrl}
                      onChange={(e) => setForm({ ...form, linkedinUrl: e.target.value })}
                      placeholder="https://linkedin.com/in/username"
                      className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1 flex items-center space-x-1.5">
                        <Github className="w-4 h-4 text-slate-800" />
                        <span>GitHub Profile URL</span>
                      </label>
                      <input
                        type="text"
                        value={form.githubUrl}
                        onChange={(e) => setForm({ ...form, githubUrl: e.target.value })}
                        placeholder="https://github.com/username"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1 flex items-center space-x-1.5">
                        <Globe className="w-4 h-4 text-teal-600" />
                        <span>Personal Portfolio URL</span>
                      </label>
                      <input
                        type="text"
                        value={form.portfolioUrl}
                        onChange={(e) => setForm({ ...form, portfolioUrl: e.target.value })}
                        placeholder="https://myportfolio.com"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl focus:ring-2 focus:ring-[#0E6E76] outline-none bg-slate-50/50"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-6 flex justify-end">
                <button
                  type="button"
                  onClick={() => setActiveStep(2)}
                  className="px-8 py-3.5 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-2xl shadow-md transition-all flex items-center space-x-2"
                >
                  <span>Continue to School & Degree →</span>
                </button>
              </div>
            </div>
          )}

          {/* PAGE 2: Schooling & Degree */}
          {activeStep === 2 && (
            <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
              <div className="border-b pb-4">
                <h2 className="text-lg font-bold font-heading text-slate-900">
                  Schooling Marks & Degree Program
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Companies shortlist candidates by 10th %, 12th % and Branch requirements.
                </p>
              </div>

              <div className="space-y-6">
                {/* 10th Details Card */}
                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200/80 space-y-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-[#0E6E76] flex items-center space-x-2">
                    <BookOpen className="w-4 h-4" />
                    <span>Class 10th (Secondary School)</span>
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">10th Percentage %</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="100"
                        id="academic-tenth-input"
                        value={form.academicProfile.tenthPercentage}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, tenthPercentage: e.target.value }
                        })}
                        placeholder="e.g. 92.5"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none font-mono bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">10th Board</label>
                      <input
                        type="text"
                        value={form.academicProfile.tenthBoard}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, tenthBoard: e.target.value }
                        })}
                        placeholder="CBSE / ICSE / State Board"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none bg-white"
                      />
                    </div>
                  </div>
                </div>

                {/* 12th Details Card */}
                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200/80 space-y-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-[#0E6E76] flex items-center space-x-2">
                    <GraduationCap className="w-4 h-4" />
                    <span>Class 12th (Senior Secondary / Diploma)</span>
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">12th Percentage %</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="100"
                        id="academic-twelfth-input"
                        value={form.academicProfile.twelfthPercentage}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, twelfthPercentage: e.target.value }
                        })}
                        placeholder="e.g. 88.0"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none font-mono bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">12th Stream</label>
                      <input
                        type="text"
                        value={form.academicProfile.twelfthStream}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, twelfthStream: e.target.value }
                        })}
                        placeholder="Science (PCM)"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none bg-white"
                      />
                    </div>
                  </div>
                </div>

                {/* Degree Program */}
                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200/80 space-y-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-[#0E6E76] flex items-center space-x-2">
                    <Building2 className="w-4 h-4" />
                    <span>B.Tech / B.E. Degree Information</span>
                  </h3>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Engineering Branch</label>
                    <input
                      type="text"
                      id="academic-branch-input"
                      value={form.academicProfile.branch}
                      onChange={(e) => setForm({
                        ...form,
                        academicProfile: { ...form.academicProfile, branch: e.target.value }
                      })}
                      placeholder="Computer Science & Engineering"
                      className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none bg-white"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Admission Year</label>
                      <input
                        type="number"
                        value={form.academicProfile.admissionYear}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, admissionYear: e.target.value }
                        })}
                        placeholder="2022"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none font-mono bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Expected Graduation</label>
                      <input
                        type="number"
                        value={form.academicProfile.expectedGraduationYear}
                        onChange={(e) => setForm({
                          ...form,
                          academicProfile: { ...form.academicProfile, expectedGraduationYear: e.target.value }
                        })}
                        placeholder="2026"
                        className="w-full px-4 py-3 text-sm border border-slate-200 rounded-2xl outline-none font-mono bg-white"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-6 flex justify-between">
                <button
                  type="button"
                  onClick={() => setActiveStep(1)}
                  className="px-6 py-3.5 text-slate-600 hover:bg-slate-100 font-bold text-xs rounded-2xl transition-all"
                >
                  ← Back to Step 1
                </button>
                <button
                  type="button"
                  onClick={() => setActiveStep(3)}
                  className="px-8 py-3.5 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-2xl shadow-md transition-all flex items-center space-x-2"
                >
                  <span>Continue to SGPAs & Backlogs →</span>
                </button>
              </div>
            </div>
          )}

          {/* PAGE 3: Semester SGPAs & Backlogs */}
          {activeStep === 3 && (
            <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
              <div className="border-b pb-4">
                <h2 className="text-lg font-bold font-heading text-slate-900">
                  Semester SGPAs & Backlog History
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Your overall CGPA is computed dynamically from your finalized semester SGPAs.
                </p>
              </div>

              <div className="space-y-6">
                {/* Semester SGPAs */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-[#0E6E76]">Semester SGPAs (1 to 8)</h3>
                    <button
                      type="button"
                      onClick={() => {
                        const list = form.academicProfile.semesterSgpas;
                        const nextSem = list.length + 1;
                        if (nextSem <= 10) {
                          setForm({
                            ...form,
                            academicProfile: {
                              ...form.academicProfile,
                              semesterSgpas: [...list, { semesterNumber: nextSem, sgpa: 8.0, isFinalized: true }]
                            }
                          });
                        }
                      }}
                      className="text-xs bg-teal-50 hover:bg-teal-100 text-[#0E6E76] px-3.5 py-1.5 rounded-xl font-bold flex items-center space-x-1 transition-colors"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Add Semester</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {form.academicProfile.semesterSgpas.map((sem, idx) => (
                      <div key={idx} className="flex items-center justify-between p-3.5 bg-slate-50 rounded-2xl border border-slate-200 text-xs">
                        <span className="font-bold text-slate-800 font-heading">Semester {sem.semesterNumber}:</span>
                        <div className="flex items-center space-x-2">
                          <input
                            type="number"
                            step="0.01"
                            min="0"
                            max="10"
                            value={sem.sgpa}
                            onChange={(e) => {
                              const list = [...form.academicProfile.semesterSgpas];
                              list[idx].sgpa = e.target.value;
                              setForm({
                                ...form,
                                academicProfile: { ...form.academicProfile, semesterSgpas: list }
                              });
                            }}
                            placeholder="SGPA"
                            className="w-20 px-2.5 py-1.5 border border-slate-300 rounded-xl outline-none font-mono text-center font-bold bg-white text-slate-900"
                          />
                          <label className="flex items-center space-x-1 cursor-pointer text-[11px] text-slate-600 font-medium">
                            <input
                              type="checkbox"
                              checked={sem.isFinalized}
                              onChange={(e) => {
                                const list = [...form.academicProfile.semesterSgpas];
                                list[idx].isFinalized = e.target.checked;
                                setForm({
                                  ...form,
                                  academicProfile: { ...form.academicProfile, semesterSgpas: list }
                                });
                              }}
                              className="rounded text-[#0E6E76]"
                            />
                            <span>Finalized</span>
                          </label>
                          <button
                            type="button"
                            onClick={() => {
                              const list = form.academicProfile.semesterSgpas.filter((_, i) => i !== idx);
                              setForm({
                                ...form,
                                academicProfile: { ...form.academicProfile, semesterSgpas: list }
                              });
                            }}
                            className="text-slate-400 hover:text-red-600 p-1 transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Backlog Records */}
                <div className="pt-4 border-t border-slate-100 space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-amber-700 flex items-center space-x-1.5">
                      <AlertTriangle className="w-4 h-4 text-amber-600" />
                      <span>Backlog History Records</span>
                    </h3>
                    <button
                      type="button"
                      onClick={() => {
                        const list = form.academicProfile.backlogRecords;
                        setForm({
                          ...form,
                          academicProfile: {
                            ...form.academicProfile,
                            backlogRecords: [...list, { subjectName: '', semester: 1, cleared: false, clearedOn: '' }]
                          }
                        });
                      }}
                      className="text-xs bg-amber-50 hover:bg-amber-100 text-amber-800 px-3.5 py-1.5 rounded-xl font-bold flex items-center space-x-1 transition-colors"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Add Backlog Subject</span>
                    </button>
                  </div>

                  {form.academicProfile.backlogRecords.length > 0 ? (
                    <div className="space-y-3">
                      {form.academicProfile.backlogRecords.map((b, idx) => (
                        <div key={idx} className="flex flex-wrap items-center gap-3 bg-amber-50/50 p-4 rounded-2xl border border-amber-200 text-xs">
                          <input
                            type="text"
                            value={b.subjectName}
                            onChange={(e) => {
                              const list = [...form.academicProfile.backlogRecords];
                              list[idx].subjectName = e.target.value;
                              setForm({
                                ...form,
                                academicProfile: { ...form.academicProfile, backlogRecords: list }
                              });
                            }}
                            placeholder="Subject Name (e.g. Applied Mathematics)"
                            className="flex-1 min-w-[200px] px-3.5 py-2 border border-slate-300 rounded-xl outline-none bg-white font-medium"
                          />
                          <select
                            value={b.semester}
                            onChange={(e) => {
                              const list = [...form.academicProfile.backlogRecords];
                              list[idx].semester = e.target.value;
                              setForm({
                                ...form,
                                academicProfile: { ...form.academicProfile, backlogRecords: list }
                              });
                            }}
                            className="px-3 py-2 border border-slate-300 rounded-xl outline-none bg-white font-medium"
                          >
                            {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                              <option key={s} value={s}>Semester {s}</option>
                            ))}
                          </select>
                          <label className="flex items-center space-x-1.5 cursor-pointer text-slate-800 font-bold bg-white px-3 py-2 rounded-xl border border-slate-200">
                            <input
                              type="checkbox"
                              checked={b.cleared}
                              onChange={(e) => {
                                const list = [...form.academicProfile.backlogRecords];
                                list[idx].cleared = e.target.checked;
                                setForm({
                                  ...form,
                                  academicProfile: { ...form.academicProfile, backlogRecords: list }
                                });
                              }}
                              className="rounded text-emerald-600"
                            />
                            <span>Cleared</span>
                          </label>
                          <button
                            type="button"
                            onClick={() => {
                              const list = form.academicProfile.backlogRecords.filter((_, i) => i !== idx);
                              setForm({
                                ...form,
                                academicProfile: { ...form.academicProfile, backlogRecords: list }
                              });
                            }}
                            className="text-red-500 hover:text-red-700 p-2"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="p-5 bg-slate-50 rounded-2xl text-xs text-slate-500 italic text-center border border-slate-100">
                      Clean Record: No active or past subject backlogs reported.
                    </div>
                  )}
                </div>
              </div>

              <div className="pt-6 flex justify-between items-center">
                <button
                  type="button"
                  onClick={() => setActiveStep(2)}
                  className="px-6 py-3.5 text-slate-600 hover:bg-slate-100 font-bold text-xs rounded-2xl transition-all"
                >
                  ← Back to Step 2
                </button>

                <button
                  type="button"
                  onClick={handleSaveAll}
                  disabled={saving}
                  id="save-academic-btn"
                  className="px-8 py-4 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold rounded-2xl shadow-lg flex items-center space-x-3 transition-all text-sm disabled:opacity-50"
                >
                  {saving ? (
                    <>
                      <RefreshCw className="w-5 h-5 animate-spin" />
                      <span>Saving Academic Details...</span>
                    </>
                  ) : (
                    <>
                      <span>Save Academic Profile & Upload Resume →</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
