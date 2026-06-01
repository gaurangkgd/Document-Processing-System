import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { documentAPI, jobAPI, UPLOADS_BASE_URL } from '../services/api';
import { useNavigate } from 'react-router-dom';

const renderFileIconMini = (mimeType) => {
  if (mimeType === 'application/pdf') {
    return (
      <div className="w-10 h-10 bg-red-50 border border-red-100 rounded-lg flex items-center justify-center text-red-500 font-bold shadow-sm">
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
      </div>
    );
  }
  if (mimeType && mimeType.startsWith('image/')) {
    return (
      <div className="w-10 h-10 bg-blue-50 border border-blue-100 rounded-lg flex items-center justify-center text-blue-500 font-bold shadow-sm">
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </div>
    );
  }
  return (
    <div className="w-10 h-10 bg-slate-50 border border-slate-100 rounded-lg flex items-center justify-center text-slate-500 font-bold shadow-sm">
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    </div>
  );
};

const Dashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalDocuments: 0,
    pendingJobs: 0,
    completedJobs: 0
  });
  const [recentDocuments, setRecentDocuments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    fetchDashboardData();
  }, [user, navigate]);

  const fetchDashboardData = async () => {
    try {
      const [docsResponse, jobsResponse] = await Promise.all([
        documentAPI.getAll(),
        jobAPI.getAll()
      ]);

      const documents = docsResponse.data;
      const jobs = jobsResponse.data;

      setStats({
        totalDocuments: documents.length,
        pendingJobs: jobs.filter(j => j.status === 'PENDING' || j.status === 'PROCESSING').length,
        completedJobs: jobs.filter(j => j.status === 'COMPLETED').length
      });

      setRecentDocuments(documents.slice(0, 5));
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[85vh] flex flex-col justify-center items-center">
        <div className="relative w-14 h-14 animate-spin rounded-full border-4 border-slate-100 border-t-indigo-600 shadow-md"></div>
        <p className="mt-4 text-sm font-semibold text-slate-500 animate-pulse">Orchestrating services...</p>
      </div>
    );
  }

  const username = user?.username || user?.user?.username || 'User';

  return (
    <div className="min-h-screen bg-slate-50/50 py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header Greeting Banner */}
        <div className="mb-10 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-8 text-white shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl -mr-20 -mt-20"></div>
          <div className="absolute bottom-0 left-0 w-80 h-80 bg-violet-500/5 rounded-full blur-3xl -ml-20 -mb-20"></div>
          <div className="relative">
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Welcome back, <span className="bg-gradient-to-r from-indigo-200 to-violet-100 bg-clip-text text-transparent">{username}</span>!
            </h1>
            <p className="mt-2 text-slate-300 font-medium max-w-xl text-sm sm:text-base leading-relaxed">
              Your intelligent asynchronous workspace is running smoothly. Upload files or search extracted content in real-time.
            </p>
          </div>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
          {/* Card 1 */}
          <div className="bg-white rounded-xl border border-slate-100 p-6 shadow-sm hover:shadow-md transition-shadow duration-150 flex items-center justify-between">
            <div>
              <div className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Total Documents</div>
              <div className="mt-2 text-3xl font-extrabold text-slate-900">{stats.totalDocuments}</div>
            </div>
            <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center shadow-sm shadow-indigo-50">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7v8a2 2 0 002 2h6M8 7V5a2 2 0 012-2h4.586a1 1 0 01.707.293l4.414 4.414a1 1 0 01.293.707V15a2 2 0 01-2 2h-2M8 7H6a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2v-2" />
              </svg>
            </div>
          </div>

          {/* Card 2 */}
          <div className="bg-white rounded-xl border border-slate-100 p-6 shadow-sm hover:shadow-md transition-shadow duration-150 flex items-center justify-between">
            <div>
              <div className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Active Queue Jobs</div>
              <div className="mt-2 text-3xl font-extrabold text-amber-500">{stats.pendingJobs}</div>
            </div>
            <div className="w-12 h-12 bg-amber-50 text-amber-600 rounded-xl flex items-center justify-center shadow-sm shadow-amber-50 relative">
              {stats.pendingJobs > 0 && (
                <span className="absolute top-0.5 right-0.5 flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-amber-500"></span>
                </span>
              )}
              <svg className="w-6 h-6 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 8H18.2" />
              </svg>
            </div>
          </div>

          {/* Card 3 */}
          <div className="bg-white rounded-xl border border-slate-100 p-6 shadow-sm hover:shadow-md transition-shadow duration-150 flex items-center justify-between">
            <div>
              <div className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Completed Processings</div>
              <div className="mt-2 text-3xl font-extrabold text-emerald-500">{stats.completedJobs}</div>
            </div>
            <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-xl flex items-center justify-center shadow-sm shadow-emerald-50">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        {/* Recent Documents */}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden mb-8">
          <div className="px-6 py-5 border-b border-slate-100 flex justify-between items-center">
            <h2 className="text-xl font-bold text-slate-900">Recent Documents</h2>
            <button
              onClick={() => navigate('/documents')}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 uppercase tracking-wider"
            >
              Manage all
            </button>
          </div>
          <div className="p-6">
            {recentDocuments.length > 0 ? (
              <div className="space-y-3.5">
                {recentDocuments.map((doc) => (
                  <div key={doc.id} className="flex justify-between items-center p-4 bg-slate-50/55 border border-slate-100 rounded-xl hover:bg-slate-50 transition-colors duration-100">
                    <div className="flex items-center space-x-3.5">
                      {doc.thumbnailUrl ? (
                        <img 
                          src={doc.thumbnailUrl.startsWith('http') ? doc.thumbnailUrl : `${UPLOADS_BASE_URL}${doc.thumbnailUrl}`} 
                          alt={doc.originalFilename} 
                          className="w-10 h-10 object-cover rounded-lg border border-slate-200 shadow-sm"
                          onError={(e) => {
                            e.target.style.display = 'none';
                            e.target.nextSibling.style.display = 'flex';
                          }}
                        />
                      ) : null}
                      <div style={{ display: doc.thumbnailUrl ? 'none' : 'flex' }}>
                        {renderFileIconMini(doc.mimeType)}
                      </div>
                      <div>
                        <div className="font-bold text-slate-900 leading-snug">{doc.originalFilename}</div>
                        <div className="text-xs text-slate-400 mt-0.5">
                          Size: {(doc.fileSize / 1024).toFixed(2)} KB • Uploaded: {new Date(doc.uploadDate).toLocaleDateString()}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => navigate(`/documents`)}
                      className="bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 px-3.5 py-1.5 rounded-lg text-xs font-bold shadow-sm transition-colors cursor-pointer"
                    >
                      View Details
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-12">
                <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <p className="text-slate-400 font-semibold">No documents uploaded yet.</p>
                <p className="text-xs text-slate-400 mt-1">Get started by uploading your first document to process.</p>
              </div>
            )}
          </div>
        </div>

        {/* Quick Action Button Hub */}
        <div className="flex justify-center mt-6">
          <button
            onClick={() => navigate('/documents')}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-7 py-3.5 rounded-xl text-base shadow-lg shadow-indigo-100 hover:shadow-xl hover:scale-[1.01] transition-all cursor-pointer flex items-center space-x-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 4v16m8-8H4" />
            </svg>
            <span>Upload New Document</span>
          </button>
        </div>

      </div>
    </div>
  );
};

export default Dashboard;
