export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-';
  }

  const matched = value.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/);

  if (matched) {
    return `${matched[1]} ${matched[2]}`;
  }

  return value;
}

export function formatFileSize(bytes?: number | null): string {
  if (bytes === null || bytes === undefined) {
    return '-';
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}