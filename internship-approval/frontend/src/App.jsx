import React, { useState, useEffect } from 'react';
import { AuthPage } from './components/AuthPage';
import { Header } from './components/Header';
import { StudentSubmissionView } from './components/StudentSubmissionView';
import { CdpcReviewQueueView } from './components/CdpcReviewQueueView';
import { CdpcPolicyRulesView } from './components/CdpcPolicyRulesView';
import { NocVerificationPage } from './components/NocVerificationPage';

export function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [activeTab, setActiveTab] = useState('student-submit');
  const [currentPath, setCurrentPath] = useState(window.location.pathname);

  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handleLocationChange);
    return () => window.removeEventListener('popstate', handleLocationChange);
  }, []);

  useEffect(() => {
    const savedUser = localStorage.getItem('internship_user');
    if (savedUser) {
      try {
        const parsed = JSON.parse(savedUser);
        setCurrentUser(parsed);
        if (parsed.role === 'CDPC_ADMIN') {
          setActiveTab('cdpc-review');
        } else {
          setActiveTab('student-submit');
        }
      } catch (e) {
        console.error('Failed to parse cached user', e);
      }
    }
  }, []);

  const handleAuthSuccess = (user) => {
    setCurrentUser(user);
    if (user.role === 'CDPC_ADMIN') {
      setActiveTab('cdpc-review');
    } else {
      setActiveTab('student-submit');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('internship_token');
    localStorage.removeItem('internship_user');
    setCurrentUser(null);
  };

  const navigateTo = (path) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
  };

  // Public Verification Route (accessible logged in OR logged out)
  if (currentPath.startsWith('/verify-noc')) {
    return <NocVerificationPage onBackToLogin={() => navigateTo('/')} />;
  }

  // Entry Gatekeeper: If user is not logged in, render the Auth Landing Page exclusively!
  if (!currentUser) {
    return <AuthPage onAuthSuccess={handleAuthSuccess} />;
  }

  // Authenticated Application Dashboard
  const isCdpc = currentUser.role === 'CDPC_ADMIN';

  return (
    <div className="min-h-screen bg-[var(--bg-main)] text-[var(--text-primary)] font-sans antialiased flex flex-col">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        onLogout={handleLogout}
      />

      <main className="flex-1">
        {!isCdpc && <StudentSubmissionView currentUser={currentUser} />}
        {isCdpc && activeTab === 'cdpc-review' && <CdpcReviewQueueView currentUser={currentUser} />}
        {isCdpc && activeTab === 'cdpc-rules' && <CdpcPolicyRulesView currentUser={currentUser} />}
      </main>

      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500 font-sans">
        <div className="max-w-7xl mx-auto px-4">
          <p className="font-medium">
            PlacementOS — Internship Approval System • Ramdeobaba University CDPC Verification Engine
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;
