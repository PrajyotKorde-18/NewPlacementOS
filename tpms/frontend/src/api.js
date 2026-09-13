const API_BASE_URL = '/api';

export const getAuthToken = () => localStorage.getItem('tpms_token');
export const setAuthToken = (token) => localStorage.setItem('tpms_token', token);
export const removeAuthToken = () => localStorage.removeItem('tpms_token');

export const getCurrentUser = () => {
  const user = localStorage.getItem('tpms_user');
  return user ? JSON.parse(user) : null;
};
export const setCurrentUser = (user) => localStorage.setItem('tpms_user', JSON.stringify(user));
export const removeCurrentUser = () => localStorage.removeItem('tpms_user');

async function request(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = { ...options.headers };

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  let data;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    let errorMsg = 'API Request Failed';
    if (data && typeof data === 'object') {
      if (data.details && Array.isArray(data.details) && data.details.length > 0) {
        errorMsg = `${data.message || 'Validation failed'}: ${data.details.join('; ')}`;
      } else if (data.message) {
        errorMsg = data.message;
      }
    } else if (typeof data === 'string') {
      errorMsg = data;
    }
    const err = new Error(errorMsg);
    if (data && data.details) err.details = data.details;
    throw err;
  }

  return data;
}

export const api = {
  // Auth
  login: (email, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (payload) => request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),

  // Resumes & Profile
  uploadResume: (file, studentId) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('studentId', studentId);
    return request('/resumes/upload', { method: 'POST', body: formData });
  },
  getResumeStatus: (versionId) => request(`/resumes/${versionId}/status`),
  getStudentProfile: (studentId) => request(`/students/${studentId}/profile`),
  updateStudentProfile: (studentId, profileData) => request(`/students/${studentId}/profile`, { method: 'PUT', body: JSON.stringify(profileData) }),

  // Drives & JD Extraction
  getDrives: () => request('/drives'),
  extractJdText: (jdText) => request('/drives/extract-jd', { method: 'POST', body: JSON.stringify({ jdText }) }),
  extractJdFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request('/drives/extract-jd-file', { method: 'POST', body: formData });
  },
  createDrive: (driveData) => request('/drives', { method: 'POST', body: JSON.stringify(driveData) }),
  updateDrive: (driveId, driveData) => request(`/drives/${driveId}`, { method: 'PUT', body: JSON.stringify(driveData) }),
  deleteDrive: (driveId) => request(`/drives/${driveId}`, { method: 'DELETE' }),
  getDrive: (driveId) => request(`/drives/${driveId}`),
  getDriveApplications: (driveId) => request(`/drives/${driveId}/applications`),
  rankDriveApplications: (driveId) => request(`/drives/${driveId}/rank`, { method: 'POST' }),
  applyToDrive: (driveId, studentId) => request(`/drives/${driveId}/apply?studentId=${studentId}`, { method: 'POST' }),

  // Applications & Overrides
  getStudentApplications: (studentId) => request(`/applications/student/${studentId}`),
  overrideApplicationStatus: (applicationId, status, overrideReason) =>
    request(`/applications/${applicationId}/override`, {
      method: 'POST',
      body: JSON.stringify({ status, overrideReason }),
    }),
  getApplicationFeedback: (applicationId) => request(`/applications/${applicationId}/feedback`),

  // Admin Cleanup
  cleanupDatabase: () => request('/admin/data/cleanup', { method: 'POST' }),
};
