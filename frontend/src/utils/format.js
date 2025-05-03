export const formatMemory = (mb) => {
  if (mb >= 1024) return `${(mb/1024).toFixed(1)} GB`;
  return `${mb} MB`;
};

export const formatFlow = (bytes) => {
  const GB = 1024 * 1024 * 1024;
  if (bytes >= GB) {
    return `${(bytes / GB).toFixed(1)} GB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};