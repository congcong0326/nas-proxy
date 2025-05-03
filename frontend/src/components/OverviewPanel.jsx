import React, { useState, useEffect } from "react";
import "./OverviewPanel.css";
import "./DiskList.css";

import StatusIndicator from './StatusIndicator';
import { formatMemory, formatFlow } from '../utils/format';
import axios from '../utils/axiosConfig';
import UserDetailModal from './UserDetailModal';
import DiskList from './DiskList';

export default function OverviewPanel({ data }) {
  const [dailyData, setDailyData] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedDate, setSelectedDate] = useState(''); 
  const [showUserDetail, setShowUserDetail] = useState(false);
  const [dailyLoading, setDailyLoading] = useState(true);
  const [dailyError, setDailyError] = useState('');

  useEffect(() => {
    const fetchDailyData = async () => {
      try {
        const currentDate = new Date().toISOString().split('T')[0];
        const response = await axios.get(`/daily?date=${currentDate}`);
        setDailyData(response.data);
        setDailyLoading(false);
      } catch (err) {
        setDailyError('获取当日流量数据失败');
        setDailyLoading(false);
      }
    };
    fetchDailyData();
  }, []);
  // 内存使用率解析函数
  const parseMemoryUsage = (str) => {
    const match = str.match(/(\d+\.\d+)%[^\d]+(\d+)[^\d]+(\d+|NaN)MB/);
    const result = match ? {
      percent: parseFloat(match[1]),
      used: parseInt(match[2]),
      total: parseInt(match[3])
    } : { percent: 0, used: 0, total: 0 };
    
    if (isNaN(result.total)) result.total = 0;
    return result;
  };

  return (
    <div className="overview-container">
      <div className="system-overview">
        <div className="system-header">
          <h3 className="system-title">系统概览</h3>
        </div>
        {/* 第一行四列 */}
        <div className="summary-grid">
        <div className="metric-card">
          <h3><i className="icon-cpu"></i> CPU使用率</h3>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${data.cpuUsage}%` }} />
          </div>
          <div className="metric-value">{data.cpuUsage}%</div>
        </div>

        {['memoryUsage', 'jvmMemoryUsage'].map((key) => {
          const mem = parseMemoryUsage(data[key]);
          return (
            <div className="metric-card" key={key}>
              <h3><i className="icon-memory"></i> {key.includes('jvm') ? 'JVM内存' : '内存'}使用率</h3>
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${mem.percent}%` }} />
              </div>
              <div className="metric-value">
                {mem.percent}% [{mem.used}MB/{mem.total > 0 ? mem.total + 'MB' : '未知'}]
              </div>
            </div>
          );
        })}

        <div className="metric-card">
          <h3><i className="icon-connection"></i> TCP连接数</h3>
          <div className="metric-value">{data.tcpConnections}</div>
        </div>
      </div>

      {/* 第二行三列 */}
      <div className="summary-grid second-row">
        <div className="metric-card">
          <h3><i className="icon-success"></i> 成功率</h3>
          <div className="metric-value">
            {isNaN(data.successRate) ? '-' : `${(parseFloat(data.successRate) * 100).toFixed(1)}%`}
          </div>
        </div>

        {['monthlyUp', 'monthlyDown'].map((key) => (
          <div className="metric-card" key={key}>
            <h3><i className="icon-traffic"></i> 本月{key.includes('Up') ? '上传' : '下载'}流量</h3>
            <div className="metric-value">
              {formatFlow(data[key])}
            </div>
          </div>
        ))}
        </div>
      </div>

      {/* 设备表格区 */}
      <div className="device-table">
        <DiskList />
      </div>

      {/* 设备表格区 */}
      <div className="device-table">
        <h3>设备列表</h3>
        <table>
          <thead>
            <tr>
              <th>设备名称</th>
              <th>IP地址</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.pcs?.length > 0 ? data.pcs.map((device) => (
              <tr key={device.ip}>
                <td>{device.name}</td>
                <td>{device.ip}</td>
                <td>
                  <StatusIndicator online={device.online} />
                </td>
                <td>
                  {!device.online && (
                    <button 
                      className="wake-button"
                      onClick={() => wakeDevice(device.ip)}
                      disabled={!device.ip}
                    >
                      唤醒设备
                    </button>
                  )}
                </td>
              </tr>
            )) : (
              <tr>
                <td colSpan="4" className="no-devices">
                  暂无设备信息
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 当日流量详情区块 */}
      <div className="daily-traffic-container">
        <h3>当日访问详情</h3>
        {dailyLoading ? (
          <div className="loading-indicator">加载中...</div>
        ) : dailyError ? (
          <div className="error-message">{dailyError}</div>
        ) : (
          <table className="daily-traffic-table">
            <tbody>
              <tr>
                <td>上行流量</td>
                <td>{formatFlow(dailyData.up)}</td>
              </tr>
              <tr>
                <td>下行流量</td>
                <td>{formatFlow(dailyData.down)}</td>
              </tr>
              <tr>
                <td colSpan="2">
                  <div className="top-users-title">TOP10用户</div>
                  {Object.entries(dailyData.topUsers)
                    .map(([user, bytes]) => (
                      <div key={user} className="user-row">
                        <span 
                          onClick={() => {
                            setSelectedUser(user);
                            setSelectedDate(new Date().toISOString().split('T')[0]);
                            setShowUserDetail(true);
                          }}
                          style={{ cursor: 'pointer', textDecoration: 'underline' }}
                        >
                          {user}
                        </span>
                        <span className="traffic-value">{formatBytes(bytes)}</span>
                      </div>
                    ))}
                
                
                  {showUserDetail && (
                    <UserDetailModal
                      user={selectedUser}
                      date={selectedDate}
                      onClose={() => setShowUserDetail(false)}
                    />
                  )}
                </td>
              </tr>
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 MB';
  const k = 1024;
  const sizes = ['MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i-2];
};

// 新增唤醒功能处理函数
const wakeDevice = async (ip) => {
  try {
    await axios.get(`/wol?ip=${ip}`);
    alert('唤醒指令已发送');
  } catch (error) {
    alert('唤醒失败: ' + error.message);
  }
};