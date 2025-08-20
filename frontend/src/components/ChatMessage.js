import React, { useRef, useState } from 'react';

const SERVER_URL = 'http://10.50.131.25:8080';

// ✅ NEW: 개별 유튜브 영상의 표시 상태(썸네일/플레이어)를 관리하는 컴포넌트
const YouTubePlayer = ({ videoId, initialUrl }) => {
  const [showVideo, setShowVideo] = useState(false);
  
  // 썸네일을 클릭하면 비디오를 보여주는 함수
  const handleThumbnailClick = () => {
    setShowVideo(true);
  };
  
  // 유튜브 썸네일 이미지 URL
  const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`;
  
  if (!showVideo) {
    // 썸네일 상태일 때의 JSX
    return (
        <>
          <a href={initialUrl} target="_blank" rel="noopener noreferrer">{initialUrl}</a>
          <div className="youtube-thumbnail" onClick={handleThumbnailClick} style={{ backgroundImage: `url(${thumbnailUrl})` }}>
            <div className="play-button"></div>
          </div>
        </>
    );
  }
  
  // 비디오 플레이어 상태일 때의 JSX
  return (
      <>
        <a href={initialUrl} target="_blank" rel="noopener noreferrer">{initialUrl}</a>
        <iframe
            style={{
              width: '100%',
              aspectRatio: '16 / 9',
              borderRadius: '8px',
              border: 'none',
              marginTop: '8px',
              display: 'block'
            }}
            src={`https://www.youtube.com/embed/${videoId}?autoplay=1`}
            title="YouTube video player"
            frameBorder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowFullScreen
        ></iframe>
      </>
  );
};

const ChatMessage = ({ message }) => {
  const messageBodyRef = useRef(null);
  
  // 텍스트를 파싱하여 링크, 이미지, 비디오, 유튜브 등을 렌더링하는 함수
  const renderAdvancedContent = (text) => {
    if (!text) return '';
    
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
    const videoExtensions = ['mp4', 'webm', 'ogg'];
    const youtubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w\-]+)/;
    
    return text.split(urlRegex).map((part, i) => {
      if (part.match(urlRegex)) {
        // ✅ MODIFIED: 유튜브 링크를 만나면 새로 만든 YouTubePlayer 컴포넌트를 사용합니다.
        const youtubeMatch = part.match(youtubeRegex);
        if (youtubeMatch) {
          const videoId = youtubeMatch[1];
          return <YouTubePlayer key={i} videoId={videoId} initialUrl={part} />;
        }
        
        try {
          const url = new URL(part);
          const extension = url.pathname.split('.').pop().toLowerCase();
          if (imageExtensions.includes(extension)) {
            return (
                <React.Fragment key={i}>
                  <a href={part} target="_blank" rel="noopener noreferrer">{part}</a><br />
                  <img src={part} alt="이미지 링크" />
                </React.Fragment>
            );
          }
          if (videoExtensions.includes(extension)) {
            return (
                <React.Fragment key={i}>
                  <a href={part} target="_blank" rel="noopener noreferrer">{part}</a><br />
                  <video src={part} controls />
                </React.Fragment>
            );
          }
        } catch (e) { /* 유효하지 않은 URL은 일반 링크로 처리 */ }
        
        return <a key={i} href={part} target="_blank" rel="noopener noreferrer">{part}</a>;
      }
      return part.split('\n').map((line, j) => <React.Fragment key={`${i}-${j}`}>{line}{j < part.split('\n').length - 1 && <br />}</React.Fragment>);
    });
  };
  
  const renderMessageContent = () => {
    const content = message.content || '';
    switch (message.messageType) {
      case 'FILE': {
        const parts = content.split(':');
        const displayName = parts[0];
        const storedFilename = parts.slice(1).join(':');
        const resourceUrl = `${SERVER_URL}/files/chat/${storedFilename}`;
        const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
        const videoExtensions = ['mp4', 'webm', 'ogg'];
        const fileExtension = displayName.split('.').pop().toLowerCase();
        if (imageExtensions.includes(fileExtension)) {
          return <img src={resourceUrl} alt={displayName} onClick={() => window.open(resourceUrl)} />;
        } else if (videoExtensions.includes(fileExtension)) {
          return <video src={resourceUrl} controls />;
        } else {
          return <a href={resourceUrl} download={displayName} target="_blank" rel="noopener noreferrer">{displayName} 다운로드</a>;
        }
      }
      case 'TEXT':
      default:
        return <p>{renderAdvancedContent(content)}</p>;
    }
  };
  
  const renderLinkPreview = () => {
    if (!message.linkPreview) return null;
    const { url, title, description, imageUrl } = message.linkPreview;
    const displayUrl = url.length > 50 ? url.slice(0, 47) + '...' : url;
    return (
        <a href={url} target="_blank" rel="noopener noreferrer" className="link-preview-card">
          {imageUrl && <img src={imageUrl} className="link-preview-image" alt="Preview" />}
          <div className="link-preview-info">
            <div className="link-preview-title">{title || ''}</div>
            <div className="link-preview-description">{description || ''}</div>
            <div className="link-preview-url">{displayUrl}</div>
          </div>
        </a>
    );
  };
  
  return (
      <div className="chat-message-item">
        <img src={`${SERVER_URL}${message.authorProfileImageUrl}`} alt={message.authorName} className="chat-profile-img" />
        <div className="chat-message-content">
          <div className="chat-message-header">
            <span className="chat-author-name">{message.authorName}</span>
            <span className="chat-timestamp">{message.createdAt}</span>
          </div>
          <div className="chat-message-body" ref={messageBodyRef}>
            {renderMessageContent()}
            {renderLinkPreview()}
          </div>
        </div>
      </div>
  );
};

export default ChatMessage;