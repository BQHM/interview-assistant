import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import InterviewCenterPage from './pages/InterviewCenterPage';
import InterviewHistoryPage from './pages/InterviewHistoryPage';
import InterviewPage from './pages/InterviewPage';
import InterviewReportPage from './pages/InterviewReportPage';
import ResumeDetailPage from './pages/ResumeDetailPage';
import ResumeListPage from './pages/ResumeListPage';
import ResumeUploadPage from './pages/ResumeUploadPage';

// App 是前端应用的根组件，主要负责配置页面路由。
// 可以把这里理解成“前端版 Controller 路由表”。
// 浏览器地址栏变了，React Router 就根据下面的 Route 决定显示哪个页面组件。
function App() {
  return (
    // BrowserRouter 让 React 可以根据浏览器地址栏路径切换页面。
    <BrowserRouter>
      <Routes>
        {/* Layout 是所有业务页面共用的外壳：左侧菜单 + 右侧内容区 */}
        <Route path="/" element={<Layout />}>
          {/* 访问根路径时，自动跳到简历列表页 */}
          <Route index element={<Navigate to="/resumes" replace />} />
          {/* 简历列表页 */}
          <Route path="resumes" element={<ResumeListPage />} />
          {/* 简历上传页 */}
          <Route path="upload" element={<ResumeUploadPage />} />
          {/* :id 是路径参数，例如 /resumes/11 中的 11 */}
          <Route path="resumes/:id" element={<ResumeDetailPage />} />
          {/* 面试中心页：选择简历和题目数量后创建面试 */}
          <Route path="interview" element={<InterviewCenterPage />} />
          {/* :sessionId 是面试会话编号，用来加载某一次面试 */}
          <Route path="interview/:sessionId" element={<InterviewPage />} />
          <Route path="interviews" element={<InterviewHistoryPage />} />
          {/* 面试报告页 */}
          <Route path="interviews/:sessionId/report" element={<InterviewReportPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
