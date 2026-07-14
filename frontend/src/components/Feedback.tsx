interface ErrorNoticeProps {
  message: string;
}

export function ErrorNotice({ message }: ErrorNoticeProps) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </div>
  );
}

interface EmptyStateProps {
  title: string;
  description?: string;
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-6 py-12 text-center">
      <h3 className="text-base font-medium text-slate-700">{title}</h3>
      {description && (
        <p className="mt-2 text-sm text-slate-500">{description}</p>
      )}
    </div>
  );
}
