# 录像下载前端集成指南

## 概述

本文档描述如何在前端集成录像下载功能。

## API 端点

### 1. 下载单个录像
```
GET /api/recordings/{id}/download
```

**响应：**
- 200: 文件内容
- 503: 并发限制或文件被锁定

**示例代码（JavaScript）：**
```javascript
async function downloadRecording(recordingId, fileName) {
  const response = await fetch(`/api/recordings/${recordingId}/download`);
  
  if (response.ok) {
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove();
  } else {
    console.error('下载失败');
  }
}
```

### 2. 获取下载状态
```
GET /api/recordings/download/status
```

**响应：**
```json
{
  "activeCount": 1,
  "tasks": [
    {
      "recordingId": 1,
      "fileName": "test.mp4",
      "fileSize": 1024000
    }
  ]
}
```

### 3. 取消下载
```
POST /api/recordings/{id}/download/cancel
```

### 4. 批量下载准备
```
POST /api/recordings/batch-download
Content-Type: application/json

{
  "recordingIds": [1, 2, 3]
}
```

## 前端实现建议

### 1. 录像列表增强

在现有的录像列表页面添加下载功能：

```jsx
// 在 RecordingList 组件中添加
const [selectedRecordings, setSelectedRecordings] = useState([]);

const handleDownload = async () => {
  for (const recording of selectedRecordings) {
    await downloadRecording(recording.id, recording.name);
  }
  setSelectedRecordings([]);
};

return (
  <div>
    {/* 现有录像列表 */}
    <RecordingTable 
      data={recordings}
      onSelect={(selected) => setSelectedRecordings(selected)}
    />
    
    {/* 下载按钮 */}
    {selectedRecordings.length > 0 && (
      <Button onClick={handleDownload}>
        下载 ({selectedRecordings.length})
      </Button>
    )}
  </div>
);
```

### 2. 下载进度显示

可以使用下载状态API定期检查下载进度：

```javascript
const checkDownloadStatus = async () => {
  const response = await fetch('/api/recordings/download/status');
  const data = await response.json();
  setActiveDownloads(data.activeCount);
};
```

### 3. 并发下载控制

后端支持最多3个并发下载，前端可以同时发起多个下载请求。

## 注意事项

1. **大文件处理**：对于大文件录像，建议使用流式下载
2. **错误处理**：网络中断时需要显示错误信息
3. **用户提示**：下载开始和完成时需要向用户反馈
