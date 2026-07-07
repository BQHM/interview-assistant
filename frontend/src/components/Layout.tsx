import { Link, Outlet, useLocation } from 'react-router-dom';
import { FileStack, Sparkles, Users } from 'lucide-react';

interface NavItem {
  // 菜单唯一标识，map 渲染时会用到。
  id: string;
  // 点击菜单后跳转的前端路径。
  path: string;
  // 菜单显示文字。
  label: string;
  // lucide-react 图标组件。
  icon: React.ComponentType<{ className?: string }>;
  // 菜单下方的小描述，可选。
  description?: string;
}

export default function Layout() {
  // useLocation 可以拿到当前浏览器路径，例如 /resumes/11。
  const location = useLocation();
  const currentPath = location.pathname;

  // 左侧导航菜单的数据源。
  // 以后要增减菜单时，优先改这里，而不是复制多段 JSX。
  const navItems: NavItem[] = [
    { id: 'resumes', path: '/resumes', label: '简历管理', icon: FileStack, description: '管理简历，AI 分析' },
    { id: 'interview', path: '/interview', label: '模拟面试', icon: Sparkles, description: '文字面试练习' },
    { id: 'interviews', path: '/interviews', label: '面试记录', icon: Users, description: '查看面试历史' },
  ];

  // 根据当前浏览器路径判断哪个菜单要高亮。
  // 简历详情页和上传页也归到“简历管理”菜单下面。
  const isActive = (path: string) => {
    if (path === '/resumes') {
      return currentPath === '/resumes' || currentPath === '/' || currentPath.startsWith('/resumes/') || currentPath === '/upload';
    }
    return currentPath.startsWith(path);
  };

  return (
    // Tailwind CSS 写法：className 里的一串类名就是样式。
    // 例如 flex 表示弹性布局，min-h-screen 表示最小高度为一屏。
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

        {/* 导航菜单：map 会把 navItems 数组转换成多个 Link */}
        <nav className="flex-1 p-4 overflow-y-auto">
          <div className="space-y-2">
            {navItems.map((item) => {
              const active = isActive(item.path);

              return (
                <Link
                  key={item.id}
                  to={item.path}
                  // 模板字符串里根据 active 动态切换样式，实现“当前菜单高亮”。
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
                    {/* item.icon 是一个组件变量，这里渲染对应图标 */}
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
        {/* Outlet 表示当前子路由页面会渲染到这里 */}
        {/* 例如访问 /resumes 时，这里显示 ResumeListPage */}
        <Outlet />
      </main>
    </div>
  );
}
