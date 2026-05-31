import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchAPI } from '../services/api';

const Search = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) return;

    setLoading(true);
    setSearched(true);

    try {
      const response = await searchAPI.search(searchTerm);
      setResults(response.data);
    } catch (error) {
      console.error('Search failed:', error);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50/50 py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Page Title */}
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Search Documents</h1>
          <p className="mt-2 text-sm text-slate-500 leading-relaxed">
            Query the full text extracted from your PDF and text documents in real-time.
          </p>
        </div>

        {/* Search Console */}
        <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm mb-8">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search keywords, project names, phrases..."
                className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/25 focus:border-indigo-500 transition-all font-medium"
              />
            </div>
            <button
              type="submit"
              disabled={loading || !searchTerm.trim()}
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-7 py-3 rounded-xl shadow-md shadow-indigo-50 transition-all disabled:opacity-50 whitespace-nowrap cursor-pointer hover:shadow-lg flex items-center justify-center space-x-1.5"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>Searching...</span>
                </>
              ) : (
                <span>Search</span>
              )}
            </button>
          </form>
        </div>

        {/* Search Results Catalog */}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="px-6 py-4.5 border-b border-slate-100 bg-slate-50/50">
            <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
              {searched ? `Found ${results.length} Matching Documents` : 'Search Query Console'}
            </h2>
          </div>
          
          <div className="divide-y divide-slate-100">
            {loading ? (
              <div className="py-24 text-center">
                <div className="w-12 h-12 border-4 border-slate-100 border-t-indigo-600 rounded-full animate-spin mx-auto shadow-sm"></div>
                <p className="mt-4 text-xs font-semibold text-slate-400 animate-pulse">Running full-text indexes...</p>
              </div>
            ) : results.length > 0 ? (
              results.map((result) => (
                <div key={result.id} className="p-6 hover:bg-slate-50/55 transition-colors duration-150">
                  <div className="flex items-start space-x-3.5 mb-3">
                    <div className="w-8.5 h-8.5 bg-indigo-50 border border-indigo-100 text-indigo-600 rounded-lg flex items-center justify-center font-bold">
                      <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div className="flex-1">
                      <h3 className="text-base font-bold text-slate-900 leading-snug">
                        {result.documentName}
                      </h3>
                      <div className="text-xs text-slate-400 mt-0.5">
                        Uploaded: {new Date(result.uploadedAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                  
                  {/* Extracted Match Snippet */}
                  <div className="bg-slate-50 border border-slate-150 rounded-xl p-4 mb-4 text-sm text-slate-700 leading-relaxed relative">
                    <div className="absolute top-2.5 right-3 px-2 py-0.5 bg-indigo-100 text-indigo-700 font-bold text-[10px] uppercase rounded tracking-wider shadow-sm">
                      Text Match
                    </div>
                    <span className="font-medium text-slate-700 block pr-20 break-words">
                      {result.extractedText}
                    </span>
                  </div>

                  <div className="flex justify-end">
                    <button
                      onClick={() => navigate('/documents')}
                      className="bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-800 px-4 py-2 rounded-lg text-xs font-bold shadow-sm transition-all cursor-pointer flex items-center space-x-1"
                    >
                      <span>Manage Documents</span>
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))
            ) : searched ? (
              <div className="py-20 text-center">
                <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <p className="text-slate-500 font-bold">No results found for "{searchTerm}"</p>
                <p className="text-xs text-slate-400 mt-1">Try checking for spelling errors or searching a different keyword.</p>
              </div>
            ) : (
              <div className="py-20 text-center">
                <div className="w-16 h-16 bg-slate-50 text-slate-400 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <p className="text-slate-500 font-bold">Search index is empty</p>
                <p className="text-xs text-slate-400 mt-1">Enter a keyword or phrase above to query document contents.</p>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};

export default Search;
