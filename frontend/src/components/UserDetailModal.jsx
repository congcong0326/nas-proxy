import React, { useEffect, useState } from 'react';
import './UserDetailModal.css';

export default function UserDetailModal({ user, date, onClose }) {
  const [loading, setLoading] = useState(true);
  const [accessRecords, setAccessRecords] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/user?user=${encodeURIComponent(user)}&date=${encodeURIComponent(date)}`);
        const data = await response.json();
        setAccessRecords(data.accessRecords);
      } catch (error) {
        console.error('获取用户详情失败:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user, date]);

  return (
    <div className="modal-mask">
      <div className="modal-container">
        <div className="modal-header">
          <h3>{user} - 访问详情</h3>
          <button onClick={onClose} className="close-btn">&times;</button>
        </div>
        <div className="modal-content">
          {loading ? (
            <div className="loading-indicator">加载中...</div>
          ) : (
            <table className="detail-table">
              <thead>
                <tr>
                  <th>访问URL</th>
                  <th>次数</th>
                  <th>上行流量</th>
                  <th>下行流量</th>
                  <th>最后访问</th>
                </tr>
              </thead>
              <tbody>
                {accessRecords.map((record, index) => (
                  <tr key={index}>
                    <td>{record.remoteUrl}</td>
                    <td>{record.accessCount}</td>
                    <td>{formatBytes(record.up)}</td>
                    <td>{formatBytes(record.down)}</td>
                    <td>{new Date(record.lastAccessTime).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function formatBytes(bytes) {
  return bytes >= 1073741824 
    ? (bytes / 1073741824).toFixed(2) + ' GB'
    : bytes >= 1048576 
    ? (bytes / 1048576).toFixed(2) + ' MB'
    : bytes >= 1024 
    ? (bytes / 1024).toFixed(2) + ' KB'
    : bytes + ' B';
}