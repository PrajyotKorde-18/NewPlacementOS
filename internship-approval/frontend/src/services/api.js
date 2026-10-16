const API_BASE_URL = '/api';

const getAuthHeaders = () => {
  const token = localStorage.getItem('internship_token');
  return token ? { 'Authorization': `Bearer ${token}` } : {};
};

export const api = {
  // Auth
  login: async (email, password) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || data.message || 'Login failed');
    }
    return data;
  },

  register: async (payload) => {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || data.message || 'Registration failed');
    }
    return data;
  },

  // Submit Internship Offer
  submitOffer: async (formData) => {
    const response = await fetch(`${API_BASE_URL}/internships/submit`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: formData,
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Failed to submit offer' }));
      throw new Error(err.message || err.error || 'Submission failed');
    }
    return response.json();
  },

  // Fetch CDPC Review Queue (Flagged + Unflagged)
  getReviewQueue: async () => {
    const response = await fetch(`${API_BASE_URL}/internships/review-queue`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to fetch review queue');
    }
    return response.json();
  },

  // Human Explicit Approval
  approveRequest: async (requestId) => {
    const response = await fetch(`${API_BASE_URL}/internships/${requestId}/approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Approval failed' }));
      throw new Error(err.message || 'Approval failed');
    }
    return response.json();
  },

  // Human Explicit Rejection (Requires Reason)
  rejectRequest: async (requestId, rejectionReason) => {
    const response = await fetch(`${API_BASE_URL}/internships/${requestId}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({ rejectionReason }),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Rejection failed' }));
      throw new Error(err.message || 'Rejection failed');
    }
    return response.json();
  },

  // Get NOC Record
  getNocRecord: async (requestId) => {
    const response = await fetch(`${API_BASE_URL}/internships/${requestId}/noc`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error('NOC record not found');
    }
    return response.json();
  },

  // Policy Rules CRUD
  getPolicyRules: async () => {
    const response = await fetch(`${API_BASE_URL}/policy-rules`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to fetch policy rules');
    }
    return response.json();
  },

  createPolicyRule: async (ruleData) => {
    const response = await fetch(`${API_BASE_URL}/policy-rules`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(ruleData),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Failed to create policy rule' }));
      throw new Error(err.message || 'Failed to create policy rule');
    }
    return response.json();
  },

  updatePolicyRule: async (id, ruleData) => {
    const response = await fetch(`${API_BASE_URL}/policy-rules/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(ruleData),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Failed to update policy rule' }));
      throw new Error(err.message || 'Failed to update policy rule');
    }
    return response.json();
  },

  deletePolicyRule: async (id) => {
    const response = await fetch(`${API_BASE_URL}/policy-rules/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to delete policy rule');
    }
    return true;
  },

  // Verify NOC Certificate (Public - No Auth Required)
  verifyNocToken: async (token) => {
    const response = await fetch(`${API_BASE_URL}/public/noc/verify/${encodeURIComponent(token)}`);
    const data = await response.json();
    return data;
  },
};
