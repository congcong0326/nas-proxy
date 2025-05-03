import React from 'react';
import './StatusIndicator.css';

export default function StatusIndicator({ online }) {
  return (
    <div className="status-indicator">
      <span className={`status-dot ${online ? 'online' : 'offline'}`} />
      <span>{online ? '在线' : '离线'}</span>
    </div>
  );
}