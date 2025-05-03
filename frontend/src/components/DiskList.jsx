import React, { useState, useEffect } from 'react';
import axios from '../utils/axiosConfig';
import './DiskList.css';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function DiskList() {
  const [disks, setDisks] = useState([]);
  const [selectedDisk, setSelectedDisk] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDisks();
  }, []);

  const fetchDisks = async () => {
    try {
      const response = await axios.get('/disk/list');
      setDisks(response.data);
    } catch (err) {
      setError('获取磁盘信息失败');
    }
  };

  const handleDiskSelect = (device) => {
    setSelectedDisk(device);
  };

  return (
    <div className="disk-layout">
      <div className="disk-list-panel compact">
        <div className="panel-header">
          <h3 className="compact-title">Disks</h3>
        </div>
        {disks.map(disk => (
          <div 
            key={disk.device}
            className={`disk-card ${selectedDisk === disk.device ? 'selected' : ''}`}
            onClick={() => handleDiskSelect(disk.device)}
          >
            <div className="disk-header">
              <span className="device-name">{disk.device}</span>
              <span className={`status-tag ${disk.status.toLowerCase()}`}>
                {disk.status}
              </span>
            </div>
            <div className="disk-details">
              <span className="model">{disk.model}</span>
              <div className="temperature-indicator">
                <div 
                  className="temperature-bar"
                  style={{ width: `${Math.min(disk.temperature, 60)}%` }}
                />
                <span>{disk.temperature}°C</span>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      <div className="disk-detail-panel">
        {selectedDisk ? (
          <DiskDetails device={selectedDisk} />
        ) : (
          <div className="empty-state">请选择磁盘查看详情</div>
        )}
      </div>
    </div>
  );
}

const DiskDetails = ({ device }) => {
  const [details, setDetails] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDetails = async () => {
      try {
        const response = await axios.get(`/disk/detail/${device.replace('/dev/', '')}`);
        setDetails(response.data);
      } catch (err) {
        console.error('获取磁盘详情失败:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchDetails();
  }, [device]);

  if (loading) return <div className="loading-indicator">加载中...</div>;

  return (
    <div className="detail-container">
      <div className="basic-info-grid">
        <div className="info-item">
          <label>型号：</label>
          <span>{details.model}</span>
        </div>
        <div className="info-item">
          <label>容量：</label>
          <span>{details.size}</span>
        </div>
        <div className="info-item">
          <label>运行时间：</label>
          <span>{details.powerOnHours} 小时</span>
        </div>
        <div className="info-item">
          <label>健康状态：</label>
          <span className={`health-status ${details.health.toLowerCase()}`}>
            {details.health}
          </span>
        </div>
      </div>

      <div className="smart-table-container">
        <h4>SMART属性</h4>
        <table className="smart-table">
          <thead>
            <tr>
              <th>属性</th>
              <th>当前值</th>
              <th>阈值</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>坏道</td>
              <td>{details.reallocatedSectorCount}</td>
              <td>0</td>
              <td>{details.reallocatedSectorCount > 0 ? '⚠️' : '✅'}</td>
            </tr>
            <tr>
              <td>寻道错误率</td>
              <td>{details.seekErrorRate}</td>
              <td>0</td>
              <td>{details.seekErrorRate > 0 ? '⚠️' : '✅'}</td>
            </tr>
            <tr>
              <td>旋转重试计数</td>
              <td>{details.spinRetryCount}</td>
              <td>0</td>
              <td>{details.spinRetryCount > 0 ? '⚠️' : '✅'}</td>
            </tr>
            <tr>
              <td>UDMA CRC 错误计数</td>
              <td>{details.udmaCrcErrorCount}</td>
              <td>0</td>
              <td>{details.udmaCrcErrorCount > 0 ? '⚠️' : '✅'}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <TemperatureChart data={details?.historyTemperature || []} />
    </div>
  );
};

const TemperatureChart = ({ data = [] }) => {
  if (data.length === 0) {
    return (
      <div className="empty-chart">
        <div className="empty-text">暂无温度历史数据</div>
      </div>
    );
  }
  const formatTime = (index) => {
    const minutesAgo = (data.length - 1 - index) * 10;
    const time = new Date(Date.now() - minutesAgo * 60000);
    return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  };

  const chartData = data.map((value, index) => ({
    value,
    index
  }));

  console.log('TemperatureChart Data:', {
    rawData: data,
    chartData: chartData,
    dataLength: data.length
  });

  return (
    <div className="chart-container">
      <h4>温度历史曲线</h4>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="index"
            tickFormatter={formatTime}
            label={{ value: '', position: 'bottom' }}
          />
          <YAxis label={{ value: '', angle: -90, position: 'left' }} />
          <Tooltip
            labelFormatter={(value) => formatTime(value)}
            formatter={(value) => [`${value} ℃`, '温度']}
          />
          <Line
            type="monotone"
            dataKey="value"
            stroke="#ff7300"
            strokeWidth={2}
            dot={{ fill: '#ff7300', strokeWidth: 1 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};