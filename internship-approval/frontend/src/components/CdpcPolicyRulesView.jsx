import React, { useState, useEffect } from 'react';
import { Settings, Plus, Edit2, Trash2, Shield, AlertCircle, Loader2 } from 'lucide-react';
import { api } from '../services/api';

export function CdpcPolicyRulesView() {
  const [rules, setRules] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal State for Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleForm, setRuleForm] = useState({
    ruleName: '',
    label: '',
    ruleType: 'STIPEND',
    ruleValue: '',
    isActive: true,
    description: '',
  });
  const [isSaving, setIsSaving] = useState(false);

  const fetchRules = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.getPolicyRules();
      setRules(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch policy rules');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleOpenAdd = () => {
    setEditingRule(null);
    setRuleForm({
      ruleName: '',
      label: '',
      ruleType: 'STIPEND',
      ruleValue: '5000',
      isActive: true,
      description: '',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (rule) => {
    setEditingRule(rule);
    setRuleForm({
      ruleName: rule.ruleName, // System key - disabled in form!
      label: rule.label,
      ruleType: rule.ruleType,
      ruleValue: rule.ruleValue,
      isActive: rule.isActive,
      description: rule.description || '',
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this policy rule criteria?')) return;
    try {
      await api.deletePolicyRule(id);
      await fetchRules();
    } catch (err) {
      alert(err.message || 'Failed to delete rule');
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      if (editingRule) {
        // On update, ruleName is immutable and ignored by backend
        await api.updatePolicyRule(editingRule.id, ruleForm);
      } else {
        await api.createPolicyRule(ruleForm);
      }
      setIsModalOpen(false);
      await fetchRules();
    } catch (err) {
      alert(err.message || 'Failed to save policy rule');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Title Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold font-heading text-[var(--text-primary)] flex items-center space-x-3">
            <Settings className="w-7 h-7 text-[var(--brand)]" />
            <span>Policy Criteria Rules Settings</span>
          </h2>
          <p className="text-slate-600 text-sm mt-1">
            Configure automated policy thresholds (e.g. stipend cutoff). Note: <strong className="text-slate-800">rule_name</strong> is an immutable system key.
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          className="px-4 py-2.5 bg-[var(--brand)] hover:bg-[var(--brand-hover)] text-white text-xs font-bold rounded-lg shadow-md transition-colors flex items-center space-x-2 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Policy Rule</span>
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm font-medium">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="py-16 text-center text-slate-500 flex flex-col items-center space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-[var(--brand)]" />
          <p className="text-sm font-medium">Loading Policy Criteria Rules...</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <th className="py-3.5 px-6">System Key (Immutable)</th>
                <th className="py-3.5 px-6">Display Label</th>
                <th className="py-3.5 px-6">Rule Type</th>
                <th className="py-3.5 px-6">Value / Threshold</th>
                <th className="py-3.5 px-6">Status</th>
                <th className="py-3.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-sm">
              {rules.map((rule) => (
                <tr key={rule.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-4 px-6 font-mono font-bold text-xs text-slate-800">
                    <span className="inline-flex items-center space-x-1.5 px-2.5 py-1 bg-slate-100 border border-slate-200 rounded text-slate-700">
                      <Shield className="w-3 h-3 text-[var(--brand)]" />
                      <span>{rule.ruleName}</span>
                    </span>
                  </td>
                  <td className="py-4 px-6 font-semibold text-slate-900">{rule.label}</td>
                  <td className="py-4 px-6 text-slate-600 font-medium">{rule.ruleType}</td>
                  <td className="py-4 px-6 font-mono font-semibold text-[var(--brand)]">{rule.ruleValue}</td>
                  <td className="py-4 px-6">
                    {rule.isActive ? (
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">Active</span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-600">Inactive</span>
                    )}
                  </td>
                  <td className="py-4 px-6 text-right space-x-2">
                    <button
                      onClick={() => handleOpenEdit(rule)}
                      className="p-1.5 text-slate-600 hover:text-[var(--brand)] hover:bg-slate-100 rounded-lg transition-colors"
                      title="Edit Rule"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(rule.id)}
                      className="p-1.5 text-slate-600 hover:text-[var(--danger)] hover:bg-red-50 rounded-lg transition-colors"
                      title="Delete Rule"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Policy Rule Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full overflow-hidden border border-slate-200 animate-in fade-in duration-200">
            <div className="flex justify-between items-center px-6 py-4 border-b border-slate-100 bg-slate-50">
              <h3 className="font-bold font-heading text-lg text-slate-800">
                {editingRule ? 'Edit Criteria Policy Rule' : 'Create Policy Rule'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 rounded-lg p-1"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Rule System Key <span className="text-xs text-slate-400 font-normal">(Immutable)</span>
                </label>
                <input
                  type="text"
                  disabled={!!editingRule}
                  required
                  value={ruleForm.ruleName}
                  onChange={(e) => setRuleForm((prev) => ({ ...prev, ruleName: e.target.value }))}
                  placeholder="MINIMUM_STIPEND"
                  className={`w-full px-3.5 py-2 border rounded-lg text-sm font-mono outline-none ${
                    editingRule
                      ? 'bg-slate-100 border-slate-200 text-slate-500 cursor-not-allowed'
                      : 'border-slate-300 focus:ring-2 focus:ring-[var(--brand)]'
                  }`}
                />
                {editingRule && (
                  <p className="text-[11px] text-amber-600 mt-1 flex items-center space-x-1">
                    <AlertCircle className="w-3 h-3" />
                    <span>System keys are immutable and bound to evaluator logic.</span>
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Display Label (Editable) <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={ruleForm.label}
                  onChange={(e) => setRuleForm((prev) => ({ ...prev, label: e.target.value }))}
                  placeholder="e.g. Minimum Monthly Stipend (₹)"
                  className="w-full px-3.5 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Rule Type
                  </label>
                  <select
                    value={ruleForm.ruleType}
                    onChange={(e) => setRuleForm((prev) => ({ ...prev, ruleType: e.target.value }))}
                    className="w-full px-3.5 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] outline-none bg-white"
                  >
                    <option value="STIPEND">STIPEND</option>
                    <option value="DURATION">DURATION</option>
                    <option value="LOCATION">LOCATION</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Rule Value / Cutoff <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={ruleForm.ruleValue}
                    onChange={(e) => setRuleForm((prev) => ({ ...prev, ruleValue: e.target.value }))}
                    placeholder="5000"
                    className="w-full px-3.5 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] outline-none font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={ruleForm.description}
                  onChange={(e) => setRuleForm((prev) => ({ ...prev, description: e.target.value }))}
                  placeholder="Mandatory criteria rule description..."
                  className="w-full px-3.5 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-[var(--brand)] outline-none"
                />
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="isActiveCheck"
                  checked={ruleForm.isActive}
                  onChange={(e) => setRuleForm((prev) => ({ ...prev, isActive: e.target.checked }))}
                  className="w-4 h-4 text-[var(--brand)] focus:ring-[var(--brand)] border-slate-300 rounded"
                />
                <label htmlFor="isActiveCheck" className="text-sm font-medium text-slate-700">
                  Rule Active for Automatic Evaluation
                </label>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-sm font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 text-sm font-semibold text-white bg-[var(--brand)] hover:bg-[var(--brand-hover)] rounded-lg shadow-sm transition-colors flex items-center space-x-2"
                >
                  {isSaving ? 'Saving...' : 'Save Policy Rule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
