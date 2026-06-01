import axios from 'axios';

export const UPLOADS_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? ''
  : 'https://document-processing-system-ccuk.onrender.com';

const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? '/api'
  : 'https://document-processing-system-ccuk.onrender.com/api';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle auth errors (401/403)
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // Clear expired or invalid credentials from local storage
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      
      // Force redirect to login page if we aren't already there
      if (!window.location.pathname.endsWith('/login') && !window.location.pathname.endsWith('/register')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  register: (userData) => api.post('/auth/register', userData),
  login: (credentials) => api.post('/auth/login', credentials),
};

// Document API
export const documentAPI = {
  upload: (formData) => {
    return api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  getAll: () => api.get('/documents'),
  getById: (id) => api.get(`/documents/${id}`),
  delete: (id) => api.delete(`/documents/${id}`),
};

// Job API
export const jobAPI = {
  getStatus: (jobId) => api.get(`/jobs/${jobId}/status`),
  getAll: () => api.get('/jobs'),
};

// Search API
export const searchAPI = {
  search: (searchTerm) => api.get(`/search?query=${encodeURIComponent(searchTerm)}`),
};

// Processing Result API
export const processingResultAPI = {
  getByJobId: (jobId) => api.get(`/jobs/${jobId}/result`),
};

export default api;
