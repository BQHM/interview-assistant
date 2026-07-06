import { Link, Outlet, useLocation } from 'react-router-dom';
import { FileStack, Sparkles, Users } from 'lucide-react';

interface NavItem {
  id: string;
  path: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  description?: string;
}

export default function Layout() {
  const location = useLocation();
  const currentPath = location.pathname;

  // 导航项
  const navItems: NavItem[] = [
    { id: 'resumes', path: '/resumes', label: '简历管理', icon: FileStack, description: '管理简历，AI 分析' },
    { id: 'interview', path: '/interview', label: '模拟面试', icon: Sparkles, description: '文字面试练习' },
    { id: 'interviews', path: '/interviews', label: '面试记录', icon: Users, description: '查看面试历史' },
  ];

  // 判断当前页面是否激活
  const isActive = (path: string) => {
    if (path === '/resumes') {
      return currentPath === '/resumes' || currentPath === '/' || currentPath.startsWith('/resumes/') || currentPath === '/upload';
    }
    return currentPath.startsWith(path);
  };

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50">
      {/* 左侧边栏 */}
      <aside className="w-64 bg-white border-r border-slate-100 fixed h-screen left-0 top-0 z-50 flex flex-col">
        {/* Logo */}
        <div className="p-6 border-b border-slate-100 flex items-center gap-3">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center text-white shadow-lg">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <span className="text-lg font-bold text-slate-800 block">AI Interview</span>
            <span className="text-xs text-slate-400">智能面试助手</span>
          </div>
        </div>

        {/* 导航菜单 */}
        <nav className="flex-1 p-4 overflow-y-auto">
          <div className="space-y-2">
            {navItems.map((item) => {
              const active = isActive(item.path);

              return (
                <Link
                  key={item.id}
                  to={item.path}
                  className={`group flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200
                    ${active
                      ? 'bg-blue-50 text-blue-600'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                    }`}
                >
                  <div className={`w-9 h-9 rounded-lg flex items-center justify-center transition-colors
                    ${active
                      ? 'bg-blue-100 text-blue-600'
                      : 'bg-slate-100 text-slate-500 group-hover:bg-slate-200 group-hover:text-slate-700'
                    }`}
                  >
                    <item.icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <span className={`text-sm block ${active ? 'font-semibold' : 'font-medium'}`}>
                      {item.label}
                    </span>
                    {item.description && (
                      <span className="text-xs text-slate-400 truncate block">
                        {item.description}
                      </span>
                    )}
                  </div>
                </Link>
              );
            })}
          </div>
        </nav>

        {/* 底部信息 */}
        <div className="p-4 border-t border-slate-100">
          <div className="px-3 py-2 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl">
            <p className="text-xs text-blue-600 font-medium">AI 面试助手 v1.0</p>
            <p className="text-xs text-slate-400 mt-0.5">Powered by AI</p>
          </div>
        </div>
      </aside>

      {/* 主内容区 */}
      <main className="flex-1 ml-64 p-10 min-h-screen overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
