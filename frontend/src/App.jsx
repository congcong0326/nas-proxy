// 删除组件导入
import { useState, useEffect } from 'react';
import axios from './utils/axiosConfig.jsx';
// 此处已移除TrafficChart导入
import OverviewPanel from './components/OverviewPanel';
import './App.css';

const App = () => {
  const [systemData, setSystemData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 新增API数据获取逻辑
  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await axios.get('/overview');
        setSystemData(response.data);
        setLoading(false);
      } catch (err) {
        const errorMessage = err.response?.data?.message || '网络连接异常';
        setError(`数据加载失败: ${errorMessage}`);
        setLoading(false);
        setSystemData({});
      }
    };
    fetchData();
  }, []);
  const [trafficData] = useState([
    { month: '1月', value: 65 },
    { month: '2月', value: 59 },
    // ...其他月份数据
  ]);


  // 在JSX渲染部分移除图表组件
  return (
    <div className="dashboard-layout">
      {loading ? (
        <div className="loading-indicator">加载中...</div>
      ) : error ? (
        <div className="error-message">{error}</div>
      ) : systemData?.cpuUsage === undefined ? (
        <div className="empty-state">
          <h3>暂无系统数据</h3>
          <button onClick={() => window.location.reload()}>重新加载</button>
        </div>
      ) : (
        <OverviewPanel data={systemData} />
      )}
      {/* 此处已移除流量图表渲染代码 */}
    </div>
  );
};

export default App;
