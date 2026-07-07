import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import InterviewPage from './pages/InterviewPage';
import ResumeDetailPage from './pages/ResumeDetailPage';
import ResumeListPage from './pages/ResumeListPage';
import ResumeUploadPage from './pages/ResumeUploadPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/resumes" replace />} />
          <Route path="resumes" element={<ResumeListPage />} />
          <Route path="upload" element={<ResumeUploadPage />} />
          <Route path="resumes/:id" element={<ResumeDetailPage />} />
          <Route path="interview/:sessionId" element={<InterviewPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;