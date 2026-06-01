import { useState, useEffect } from 'react';
import { documentAPI, UPLOADS_BASE_URL } from '../services/api';

const renderFileIcon = (mimeType) => {
  if (mimeType === 'application/pdf') {
    return (
      <div className="w-16 h-16 bg-red-50 border border-red-200 rounded flex flex-col items-center justify-center text-red-600 font-bold shadow-sm">
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
        <span className="text-[10px] mt-1 tracking-wider uppercase">PDF</span>
      </div>
    );
  }
  if (mimeType && mimeType.startsWith('image/')) {
    return (
      <div className="w-16 h-16 bg-blue-50 border border-blue-200 rounded flex flex-col items-center justify-center text-blue-600 font-bold shadow-sm">
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span className="text-[10px] mt-1 tracking-wider uppercase">IMG</span>
      </div>
    );
  }
  return (
    <div className="w-16 h-16 bg-gray-50 border border-gray-200 rounded flex flex-col items-center justify-center text-gray-500 font-bold shadow-sm">
      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <span className="text-[10px] mt-1 tracking-wider uppercase">DOC</span>
    </div>
  );
};

const Documents = () => {
  const [documents, setDocuments] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      const response = await documentAPI.getAll();
      setDocuments(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch documents:', error);
      setLoading(false);
    }
  };

  const handleFileSelect = (e) => {
    setSelectedFile(e.target.files[0]);
    setMessage('');
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('Please select a file');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);
    setUploading(true);
    setMessage('');

    try {
      await documentAPI.upload(formData);
      setMessage('File uploaded and processing started!');
      setSelectedFile(null);
      document.querySelector('input[type="file"]').value = '';
      setTimeout(() => fetchDocuments(), 2000);
    } catch (error) {
      setMessage(error.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!globalThis.confirm('Are you sure?')) return;
    try {
      await documentAPI.delete(id);
      setMessage('Document deleted');
      fetchDocuments();
    } catch (error) {
      console.error('Delete failed:', error);
      setMessage('Failed to delete');
    }
  };

  if (loading) {
    return (
      <div className="min-h-[85vh] flex flex-col justify-center items-center">
        <div className="relative w-14 h-14 animate-spin rounded-full border-4 border-slate-100 border-t-indigo-600 shadow-md"></div>
        <p className="mt-4 text-sm font-semibold text-slate-500 animate-pulse">Loading documents catalog...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50/50 py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Page Title */}
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Workspace Documents</h1>
          <p className="mt-2 text-sm text-slate-500 leading-relaxed">
            Upload new files, view automatically rendered first-page thumbnails, and open previews instantly.
          </p>
        </div>

        {/* Upload Document Panel */}
        <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm mb-8">
          <h2 className="text-lg font-bold text-slate-900 mb-4">Upload New Document</h2>
          
          <div className="flex flex-col md:flex-row items-stretch gap-4">
            {/* Dashed Drag/Click Zone */}
            <label className="flex-1 border-2 border-dashed border-slate-200 hover:border-indigo-500 rounded-2xl p-6 flex flex-col items-center justify-center cursor-pointer transition-all bg-slate-50/45 hover:bg-indigo-50/15 group relative">
              <input 
                type="file" 
                onChange={handleFileSelect} 
                accept=".pdf,.doc,.docx,.txt" 
                className="sr-only"
              />
              <div className="w-11 h-11 bg-white border border-slate-150 rounded-xl flex items-center justify-center text-slate-400 group-hover:text-indigo-600 shadow-sm transition-colors duration-150">
                <svg className="w-5.5 h-5.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 4v16m8-8H4" />
                </svg>
              </div>
              <span className="mt-3.5 text-sm font-bold text-slate-800 leading-none">
                {selectedFile ? selectedFile.name : 'Choose File'}
              </span>
              <span className="mt-1.5 text-xs text-slate-400 font-medium">
                {selectedFile ? `File size: ${(selectedFile.size / 1024).toFixed(2)} KB` : 'Accepts PDF, Word, and Text documents up to 10MB'}
              </span>
            </label>

            {/* Upload Action Button */}
            <div className="flex items-center">
              <button 
                onClick={handleUpload} 
                disabled={!selectedFile || uploading} 
                className="w-full md:w-auto h-full md:h-auto bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-8 py-4.5 rounded-2xl shadow-md shadow-indigo-100 hover:shadow-lg transition-all disabled:opacity-50 flex items-center justify-center space-x-1.5 cursor-pointer"
              >
                {uploading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                    <span>Uploading...</span>
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                    </svg>
                    <span>Upload</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {message && (
            <div className={`mt-4 p-3.5 rounded-xl text-sm font-semibold flex items-center space-x-2 border ${
              message.toLowerCase().includes('failed') || message.toLowerCase().includes('error') 
                ? 'bg-red-50 border-red-150 text-red-700' 
                : 'bg-emerald-50 border-emerald-150 text-emerald-700'
            }`}>
              <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {message.toLowerCase().includes('failed') || message.toLowerCase().includes('error') ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                )}
              </svg>
              <span>{message}</span>
            </div>
          )}
        </div>

        {/* Your Documents Panel */}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="px-6 py-4.5 border-b border-slate-100 bg-slate-50/50">
            <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Your Documents</h2>
          </div>
          
          <div className="divide-y divide-slate-100">
            {documents.length > 0 ? documents.map((doc) => (
              <div key={doc.id} className="p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between hover:bg-slate-50/55 transition-colors duration-150 gap-4">
                <div className="flex items-center space-x-4">
                  {/* Thumbnail / SVG Icon Wrapper */}
                  <div className="flex-shrink-0 relative">
                    {doc.thumbnailUrl ? (
                      <img 
                        src={doc.thumbnailUrl.startsWith('http') ? doc.thumbnailUrl : `${UPLOADS_BASE_URL}${doc.thumbnailUrl}`} 
                        alt={doc.originalFilename} 
                        className="w-16 h-16 object-cover rounded-xl border border-slate-200 shadow-sm hover:scale-105 transition-transform duration-150" 
                        onError={(e) => {
                          e.target.style.display = 'none';
                          e.target.nextSibling.style.display = 'flex';
                        }}
                      />
                    ) : null}
                    <div style={{ display: doc.thumbnailUrl ? 'none' : 'flex' }}>
                      {renderFileIcon(doc.mimeType)}
                    </div>
                  </div>
                  
                  {/* Document metadata */}
                  <div>
                    <h3 className="text-lg font-bold text-slate-900 leading-tight">{doc.originalFilename}</h3>
                    <div className="mt-1.5 flex items-center space-x-2 text-xs text-slate-400">
                      <span>Size: {(doc.fileSize / 1024).toFixed(2)} KB</span>
                      <span className="text-slate-200 font-bold">•</span>
                      <span>Type: {doc.mimeType?.split('/')[1]?.toUpperCase() || 'UNKNOWN'}</span>
                      <span className="text-slate-200 font-bold">•</span>
                      <span>Uploaded: {new Date(doc.uploadDate).toLocaleDateString()}</span>
                    </div>
                  </div>
                </div>
                
                {/* Actions */}
                <div className="flex items-center space-x-2 w-full sm:w-auto">
                  <button 
                    onClick={() => window.open(`${UPLOADS_BASE_URL}/uploads/${doc.storedFilename}`, '_blank')} 
                    className="flex-1 sm:flex-none bg-indigo-50 hover:bg-indigo-100 text-indigo-600 font-bold px-4 py-2 rounded-xl text-xs shadow-sm transition-colors cursor-pointer flex items-center justify-center space-x-1"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <span>View / Download</span>
                  </button>
                  <button 
                    onClick={() => handleDelete(doc.id)} 
                    className="flex-1 sm:flex-none bg-red-50 hover:bg-red-100 text-red-600 font-bold px-4 py-2 rounded-xl text-xs shadow-sm transition-colors cursor-pointer flex items-center justify-center space-x-1"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    <span>Delete</span>
                  </button>
                </div>
              </div>
            )) : (
              <div className="py-20 text-center">
                <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <p className="text-slate-500 font-bold">No documents uploaded yet</p>
                <p className="text-xs text-slate-400 mt-1">Get started by choosing a file in the upload zone above.</p>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};

export default Documents;
