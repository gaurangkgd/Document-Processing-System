import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const username = user?.username || user?.user?.username || 'User';

  const navLinkClass = ({ isActive }) =>
    `inline-flex items-center px-3 pt-1 border-b-2 text-sm font-semibold transition-all duration-150 ${
      isActive
        ? 'border-indigo-600 text-indigo-600'
        : 'border-transparent text-gray-500 hover:text-gray-800 hover:border-gray-200'
    }`;

  return (
    <nav className="bg-white/85 backdrop-blur-md border-b border-slate-100 sticky top-0 z-50 shadow-sm transition-all">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex">
            {/* Logo */}
            <div className="flex-shrink-0 flex items-center">
              <Link to="/" className="flex items-center space-x-2.5 group">
                <div className="w-9 h-9 bg-gradient-to-tr from-indigo-600 to-violet-500 rounded-lg flex items-center justify-center shadow-md shadow-indigo-100 group-hover:scale-105 transition-transform duration-150">
                  <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-indigo-950 via-slate-900 to-indigo-900 bg-clip-text text-transparent">
                  DocProcessor
                </span>
              </Link>
            </div>

            {/* Navigation links */}
            {user && (
              <div className="hidden sm:ml-8 sm:flex sm:space-x-4">
                <NavLink to="/dashboard" className={navLinkClass}>
                  Dashboard
                </NavLink>
                <NavLink to="/documents" className={navLinkClass}>
                  Documents
                </NavLink>
                <NavLink to="/search" className={navLinkClass}>
                  Search
                </NavLink>
              </div>
            )}
          </div>

          {/* User profile & action buttons */}
          <div className="flex items-center">
            {user ? (
              <div className="flex items-center space-x-5">
                <div className="flex items-center space-x-2 bg-slate-50 border border-slate-100 rounded-full py-1 px-3.5 pr-2.5 shadow-sm">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">User:</span>
                  <span className="text-sm font-bold text-slate-800">{username}</span>
                  <div className="w-6.5 h-6.5 bg-indigo-50 border border-indigo-100 text-indigo-600 rounded-full flex items-center justify-center font-bold text-xs uppercase shadow-sm">
                    {username.charAt(0)}
                  </div>
                </div>
                <button
                  onClick={handleLogout}
                  className="bg-slate-900 hover:bg-red-600 hover:shadow-red-50 text-white font-semibold px-4 py-2 rounded-lg text-sm transition-all duration-150 hover:shadow-md cursor-pointer"
                >
                  Logout
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-3">
                <Link
                  to="/login"
                  className="text-slate-600 hover:text-slate-900 font-semibold px-3 py-2 rounded-lg text-sm transition-colors duration-150"
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-4.5 py-2 rounded-lg text-sm shadow-md shadow-indigo-100 transition-all duration-150 hover:shadow-lg cursor-pointer"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
