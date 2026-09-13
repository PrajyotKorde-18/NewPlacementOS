import React, { useState } from 'react';
import { Sparkles, Upload, Plus, Trash2, CheckCircle2, AlertCircle, Layers, Sliders } from 'lucide-react';
import { api } from '../api';

export default function CreateDriveView({ currentUser, onDriveCreated }) {
  const [companyName, setCompanyName] = useState('');
  const [roleTitle, setRoleTitle] = useState('');
  const [vacancies, setVacancies] = useState(5);
  const [ctc, setCtc] = useState('12 LPA');

  const [jdText, setJdText] = useState('');
  const [jdFile, setJdFile] = useState(null);

  const [extracting, setExtracting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Editable Role Profile Preview
  const [roleProfile, setRoleProfile] = useState(null);

  const [newSkillInput, setNewSkillInput] = useState('');
  const [targetCategory, setTargetCategory] = useState('MUST_HAVE');

  const handleExtractJd = async () => {
    setError('');
    setExtracting(true);
    try {
      let preview;
      if (jdFile) {
        preview = await api.extractJdFile(jdFile);
      } else if (jdText.trim()) {
        preview = await api.extractJdText(jdText);
      } else {
        setError('Please enter Job Description text or attach a PDF file first.');
        setExtracting(false);
        return;
      }
      setRoleProfile(preview);
      if (preview && preview.vacancies && preview.vacancies >= 1) {
        setVacancies(preview.vacancies);
      }
    } catch (err) {
      setError(err.message || 'Failed to extract skills from Job Description');
    } finally {
      setExtracting(false);
    }
  };

  const handleAddSkill = (e) => {
    e.preventDefault();
    if (!newSkillInput.trim() || !roleProfile) return;

    const updatedSkills = { ...roleProfile.requiredSkills };
    const currentList = updatedSkills[targetCategory] || [];
    updatedSkills[targetCategory] = [...currentList, newSkillInput.trim()];

    setRoleProfile({ ...roleProfile, requiredSkills: updatedSkills });
    setNewSkillInput('');
  };

  const handleRemoveSkill = (category, skillToRemove) => {
    if (!roleProfile) return;
    const updatedSkills = { ...roleProfile.requiredSkills };
    updatedSkills[category] = (updatedSkills[category] || []).filter((s) => s !== skillToRemove);
    setRoleProfile({ ...roleProfile, requiredSkills: updatedSkills });
  };

  const handleWeightChange = (key, val) => {
    if (!roleProfile) return;
    const num = parseFloat(val) || 0.0;
    const updatedWeights = { ...roleProfile.weights, [key]: num };
    setRoleProfile({ ...roleProfile, weights: updatedWeights });
  };

  const handleSubmitDrive = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!companyName.trim() || !roleTitle.trim()) {
      setError('Company name and role title are mandatory');
      return;
    }

    setSaving(true);
    try {
      const drivePayload = {
        collegeId: 'RKNECO1',
        companyName,
        roleTitle,
        vacancies: parseInt(vacancies, 10) || 1,
        ctc: ctc || 'Not Specified',
        roleProfile: roleProfile || {
          requiredSkills: { MUST_HAVE: [], NICE_TO_HAVE: [], BONUS: [] },
          weights: { skillMatch: 0.4, projectRelevance: 0.25, codingActivity: 0.15, certifications: 0.1, experience: 0.1 },
        },
      };

      const created = await api.createDrive(drivePayload);
      setSuccess(`Placement Drive for ${created.companyName} (${created.roleTitle}) created successfully!`);
      if (onDriveCreated) onDriveCreated(created);
    } catch (err) {
      setError(err.message || 'Failed to create placement drive');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div>
        <div className="inline-flex items-center space-x-2 bg-teal-100 text-[#0E6E76] px-3 py-1 rounded-full text-xs font-bold mb-2">
          <Layers className="w-3.5 h-3.5" />
          <span>Coordinator Portal</span>
        </div>
        <h1 className="text-3xl font-bold font-heading text-slate-900">Create Placement Drive & Extract JD</h1>
        <p className="text-sm text-slate-500 mt-1">Configure campus drives, auto-classify JD skills via Groq LLM, and customize candidate evaluation weights.</p>
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

      <form onSubmit={handleSubmitDrive} className="space-y-8">
        {/* Basic Drive Details */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
          <h3 className="text-lg font-bold font-heading text-slate-900 pb-3 border-b border-slate-100">1. Basic Drive Details</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Company Name</label>
              <input
                type="text"
                required
                placeholder="Google / Amazon"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Role Title</label>
              <input
                type="text"
                required
                placeholder="Software Engineer"
                value={roleTitle}
                onChange={(e) => setRoleTitle(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">CTC / Package</label>
              <input
                type="text"
                placeholder="e.g. 12 LPA"
                value={ctc}
                onChange={(e) => setCtc(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none font-semibold text-[#0E6E76]"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Available Vacancies</label>
              <input
                type="number"
                min="1"
                required
                value={vacancies}
                onChange={(e) => setVacancies(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#0E6E76] outline-none"
              />
            </div>
          </div>
        </div>

        {/* JD Upload & AI Extraction Trigger */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
          <div className="flex justify-between items-center pb-3 border-b border-slate-100">
            <h3 className="text-lg font-bold font-heading text-slate-900 flex items-center space-x-2">
              <Sparkles className="w-5 h-5 text-[#0E6E76]" />
              <span>2. Job Description & Groq Skill Extraction</span>
            </h3>
            <button
              type="button"
              onClick={handleExtractJd}
              disabled={extracting}
              className="px-4 py-2 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-xs rounded-xl transition-all shadow-sm flex items-center space-x-2 disabled:opacity-50"
            >
              {extracting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Extracting Skills...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Extract Skills via Groq LLM</span>
                </>
              )}
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Upload JD Document (PDF)</label>
              <div className="border-2 border-dashed border-slate-300 rounded-xl p-4 text-center hover:border-[#0E6E76] transition-colors">
                <Upload className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                <input
                  type="file"
                  accept=".pdf"
                  onChange={(e) => setJdFile(e.target.files?.[0] || null)}
                  className="text-xs text-slate-600"
                />
                {jdFile && <div className="text-xs font-semibold text-[#0E6E76] mt-2">Attached: {jdFile.name}</div>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Or Paste Job Description Text</label>
              <textarea
                rows={5}
                placeholder="Paste complete job description requirements here..."
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                className="w-full p-3 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-[#0E6E76] outline-none font-mono"
              />
            </div>
          </div>
        </div>

        {/* Editable Extraction Preview Step */}
        {roleProfile && (
          <div className="bg-gradient-to-b from-teal-50/50 to-white border-2 border-[#0E6E76] rounded-2xl p-6 shadow-md space-y-6 animate-in fade-in duration-300">
            <div className="flex justify-between items-center pb-3 border-b border-teal-200">
              <div>
                <span className="text-xs font-bold uppercase text-[#0E6E76] tracking-wider">Preview Step</span>
                <h3 className="text-xl font-bold font-heading text-slate-900">3. Classified Skills & Scoring Weights</h3>
              </div>
              <span className="text-xs bg-teal-100 text-teal-800 font-semibold px-3 py-1 rounded-full">
                Interactive Preview
              </span>
            </div>

            {/* Skill Bucket Chips */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {['MUST_HAVE', 'NICE_TO_HAVE', 'BONUS'].map((category) => (
                <div key={category} className="bg-white p-4 rounded-xl border border-slate-200 space-y-3">
                  <div className="flex justify-between items-center">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded uppercase ${
                      category === 'MUST_HAVE' ? 'bg-red-100 text-red-700' :
                      category === 'NICE_TO_HAVE' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-700'
                    }`}>
                      {category.replace('_', ' ')}
                    </span>
                    <span className="text-xs text-slate-400 font-mono">
                      {(roleProfile.requiredSkills?.[category] || []).length} skills
                    </span>
                  </div>

                  <div className="flex flex-wrap gap-1.5 min-h-[60px]">
                    {(roleProfile.requiredSkills?.[category] || []).map((skill, idx) => (
                      <span
                        key={idx}
                        className="inline-flex items-center space-x-1 px-2.5 py-1 bg-slate-100 text-slate-800 font-medium text-xs rounded-lg border border-slate-200"
                      >
                        <span>{skill}</span>
                        <button
                          type="button"
                          onClick={() => handleRemoveSkill(category, skill)}
                          className="text-slate-400 hover:text-red-600 ml-1"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            {/* Add Custom Skill Tool */}
            <div className="flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-3 bg-white p-3 rounded-xl border border-slate-200">
              <input
                type="text"
                placeholder="Add custom skill..."
                value={newSkillInput}
                onChange={(e) => setNewSkillInput(e.target.value)}
                className="flex-1 px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none"
              />
              <select
                value={targetCategory}
                onChange={(e) => setTargetCategory(e.target.value)}
                className="px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none"
              >
                <option value="MUST_HAVE">MUST_HAVE</option>
                <option value="NICE_TO_HAVE">NICE_TO_HAVE</option>
                <option value="BONUS">BONUS</option>
              </select>
              <button
                type="button"
                onClick={handleAddSkill}
                className="px-3 py-1.5 bg-[#0E6E76] text-white font-semibold text-xs rounded-lg hover:bg-[#0B575D] flex items-center space-x-1"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Skill</span>
              </button>
            </div>

            {/* Extracted Academic Criteria Section */}
            <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-4">
              <div className="flex justify-between items-center">
                <h4 className="text-xs font-bold uppercase text-slate-500 tracking-wider flex items-center space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-[#0E6E76]" />
                  <span>Extracted Academic Eligibility Constraints</span>
                </h4>
                <span className="text-[11px] text-slate-400 font-mono">
                  {roleProfile.minCgpa || roleProfile.maxActiveBacklogs !== null || roleProfile.minTenthPercentage || roleProfile.minTwelfthPercentage ? 'Constraints Applied' : 'No Academic Restriction'}
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Min CGPA Requirement</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    placeholder="e.g. 7.5 (Blank if none)"
                    value={roleProfile.minCgpa ?? ''}
                    onChange={(e) => setRoleProfile({ ...roleProfile, minCgpa: e.target.value ? parseFloat(e.target.value) : null })}
                    className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none font-mono focus:ring-2 focus:ring-[#0E6E76]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Min 10th Standard %</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    max="100"
                    placeholder="e.g. 70.0 (Blank if none)"
                    value={roleProfile.minTenthPercentage ?? ''}
                    onChange={(e) => setRoleProfile({ ...roleProfile, minTenthPercentage: e.target.value ? parseFloat(e.target.value) : null })}
                    className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none font-mono focus:ring-2 focus:ring-[#0E6E76]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Min 12th Standard %</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    max="100"
                    placeholder="e.g. 70.0 (Blank if none)"
                    value={roleProfile.minTwelfthPercentage ?? ''}
                    onChange={(e) => setRoleProfile({ ...roleProfile, minTwelfthPercentage: e.target.value ? parseFloat(e.target.value) : null })}
                    className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none font-mono focus:ring-2 focus:ring-[#0E6E76]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Max Active Backlogs</label>
                  <input
                    type="number"
                    min="0"
                    max="10"
                    placeholder="e.g. 1 (Blank if none)"
                    value={roleProfile.maxActiveBacklogs ?? ''}
                    onChange={(e) => setRoleProfile({ ...roleProfile, maxActiveBacklogs: e.target.value !== '' ? parseInt(e.target.value, 10) : null })}
                    className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none font-mono focus:ring-2 focus:ring-[#0E6E76]"
                  />
                </div>
              </div>
            </div>

            {/* Scoring Weights */}
            <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-4">
              <h4 className="text-xs font-bold uppercase text-slate-500 tracking-wider flex items-center space-x-2">
                <Sliders className="w-4 h-4 text-[#0E6E76]" />
                <span>Custom Category Scoring Weights</span>
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-5 gap-4">
                {['skillMatch', 'projectRelevance', 'codingActivity', 'certifications', 'experience'].map((key) => (
                  <div key={key}>
                    <label className="block text-[11px] font-semibold text-slate-700 capitalize mb-1">
                      {key.replace(/([A-Z])/g, ' $1')}
                    </label>
                    <input
                      type="number"
                      step="0.05"
                      min="0"
                      max="1"
                      value={roleProfile.weights?.[key] ?? 0.2}
                      onChange={(e) => handleWeightChange(key, e.target.value)}
                      className="w-full px-2 py-1 text-xs border border-slate-300 rounded-lg text-center font-mono focus:ring-2 focus:ring-[#0E6E76]"
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Submit */}
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-8 py-3 bg-[#0E6E76] hover:bg-[#0B575D] text-white font-bold text-sm rounded-xl transition-all shadow-md flex items-center space-x-2 disabled:opacity-50"
          >
            {saving ? (
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <span>Finalize & Create Placement Drive</span>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
