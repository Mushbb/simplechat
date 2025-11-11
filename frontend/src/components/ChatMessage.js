import React, { useRef, useState, useContext, useEffect } from 'react';
import axiosInstance from "../api/axiosInstance";
import { AuthContext } from '../context/AuthContext';
import { FaTrash, FaPencilAlt } from 'react-icons/fa';

const SERVER_URL = axiosInstance.getUri();

// ... (YouTubePlayer 컴포넌트는 변경 없음)
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


const ChatMessage = ({ message, isFirstInGroup, myRole, handleDeleteMessage, handleEditMessage }) => {
  const { user } = useContext(AuthContext);
  const messageBodyRef = useRef(null);
  const [isHovered, setIsHovered] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editedContent, setEditedContent] = useState(message.content);

  useEffect(() => {
    // message.content가 외부(WebSocket)로부터 변경되었을 때,
    // 수정 중이 아니라면 editedContent를 업데이트합니다.
    if (!isEditing) {
      setEditedContent(message.content);
    }
  }, [message.content, isEditing]);

  const handleStartEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditedContent(message.content); // 원래 내용으로 복구
  };

  const handleSaveEdit = () => {
    if (editedContent.trim() === message.content) {
        setIsEditing(false);
        return;
    }
    handleEditMessage(message.messageId, editedContent.trim());
    setIsEditing(false);
  };

  // ... (renderAdvancedContent, renderMessageContent, renderLinkPreview 함수는 변경 없음)
  // 텍스트를 파싱하여 링크, 이미지, 비디오, 유튜브 등을 렌더링하는 함수
      const renderAdvancedContent = (text) => {
      if (!text) return '';
      
      const urlRegex = /(https?:\/\/[^\s]+)/g;
      const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
      const videoExtensions = ['mp4', 'webm', 'ogg'];
      const youtubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w\-]+)/;
      const mentionRegex = /@([^\s]+)/g; // @닉네임 패턴
  
      const parts = text.split(urlRegex);
      return parts.map((part, i) => {
        if (part.match(urlRegex)) {
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
        
        // 멘션 처리
        const mentionParts = part.split(mentionRegex);
        return mentionParts.map((mPart, j) => {
          if (j % 2 === 1) { // 멘션된 닉네임 부분
            const mentionedNickname = mPart;
            const isMe = user && user.nickname === mentionedNickname;
            return <span key={`${i}-${j}`} className={`mention-highlight ${isMe ? 'mention-me' : ''}`}>@{mentionedNickname}</span>;
          }
          // 일반 텍스트 또는 멘션이 아닌 부분
          return mPart.split('\n').map((line, k) => <React.Fragment key={`${i}-${j}-${k}`}>{line}{k < mPart.split('\n').length - 1 && <br />}</React.Fragment>);
        });
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
      <div 
        className={`chat-message-item ${isFirstInGroup ? 'is-first' : ''}`}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        {isFirstInGroup && (
          <img src={`${SERVER_URL}${message.authorProfileImageUrl}`} alt={message.authorName} className="chat-profile-img" />
        )}
        <div className="chat-message-content">
          {isFirstInGroup && (
            <div className="chat-message-header">
              <span className="chat-author-name">{message.authorName}</span>
              <span className="chat-timestamp">
                {(() => {
                  const messageDate = new Date(message.createdAt.replace(' ', 'T') + 'Z');
                  const today = new Date();
                  const isToday = messageDate.getFullYear() === today.getFullYear() &&
                                  messageDate.getMonth() === today.getMonth() &&
                                  messageDate.getDate() === today.getDate();

                  return isToday 
                    ? messageDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                    : messageDate.toLocaleString([], { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
                })()}
              </span>
            </div>
          )}
          <div className="chat-message-body" ref={messageBodyRef}>
            {isEditing ? (
              <div className="message-edit-form">
                <textarea
                  value={editedContent}
                  onChange={(e) => setEditedContent(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSaveEdit();
                    }
                    if (e.key === 'Escape') {
                      handleCancelEdit();
                    }
                  }}
                  autoFocus
                />
                <div className="edit-buttons">
                  <button onClick={handleCancelEdit}>취소</button>
                  <button onClick={handleSaveEdit}>저장</button>
                </div>
              </div>
            ) : (
              <>
                {renderMessageContent()}
                {renderLinkPreview()}
              </>
            )}
          </div>
        </div>
        {isHovered && !isEditing && (
          <div className="message-toolbar">
            <span className="toolbar-timestamp">{new Date(message.createdAt.replace(' ', 'T') + 'Z').toLocaleString()}</span>
            {(myRole === 'ADMIN' || message.authorId === user.userId) && (
              <>
                <button className="toolbar-button" onClick={handleStartEdit}>
                  <FaPencilAlt />
                </button>
                <button className="toolbar-button" onClick={() => handleDeleteMessage(message.messageId)}>
                  <FaTrash />
                </button>
              </>
            )}
          </div>
        )}
      </div>
  );
};

export default ChatMessage;