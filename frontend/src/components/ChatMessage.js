import React, { useRef, useState, useContext, useEffect } from 'react';
import axiosInstance from "../api/axiosInstance";
import { AuthContext } from '../context/AuthContext';
import { FaTrash, FaPencilAlt } from 'react-icons/fa';

/**
 * @file 단일 채팅 메시지와 그 안에 포함된 다양한 콘텐츠(텍스트, 이미지, 비디오, 파일, 링크 미리보기, YouTube)를 렌더링하는 컴포넌트들을 정의합니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * YouTube 비디오를 렌더링하는 컴포넌트입니다.
 * 처음에는 썸네일을 보여주고, 클릭 시 YouTube 임베드 플레이어로 전환됩니다.
 * @param {object} props
 * @param {string} props.videoId - 재생할 YouTube 비디오의 ID.
 * @param {string} props.initialUrl - 원본 YouTube URL.
 * @returns {JSX.Element} YouTubePlayer 컴포넌트의 JSX.
 */
const YouTubePlayer = ({ videoId, initialUrl }) => {
  /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 비디오 플레이어 표시 여부 상태 */
  const [showVideo, setShowVideo] = useState(false);
  
  /** 썸네일 클릭 시 비디오를 표시하도록 상태를 변경하는 핸들러. */
  const handleThumbnailClick = () => {
    setShowVideo(true);
  };
  
  const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`;
  
  if (!showVideo) {
    return (
        <>
          <a href={initialUrl} target="_blank" rel="noopener noreferrer">{initialUrl}</a>
          <div className="youtube-thumbnail" onClick={handleThumbnailClick} style={{ backgroundImage: `url(${thumbnailUrl})` }}>
            <div className="play-button"></div>
          </div>
        </>
    );
  }
  
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


/**
 * 채팅방 내의 단일 메시지를 렌더링하는 컴포넌트.
 * 메시지 내용, 작성자 정보, 시간, 수정/삭제 기능 등을 포함합니다.
 * @param {object} props
 * @param {import('../context/ChatContext').Message} props.message - 렌더링할 메시지 객체.
 * @param {boolean} props.isFirstInGroup - 같은 작성자의 연속된 메시지 그룹 중 첫 번째 메시지인지 여부.
 * @param {'ADMIN'|'MEMBER'|null} props.myRole - 현재 사용자의 방 내 역할.
 * @param {(messageId: number) => void} props.handleDeleteMessage - 메시지 삭제 처리 함수.
 * @param {(messageId: number, newContent: string) => void} props.handleEditMessage - 메시지 수정 처리 함수.
 * @returns {JSX.Element} ChatMessage 컴포넌트의 JSX.
 */
const ChatMessage = ({ message, isFirstInGroup, myRole, handleDeleteMessage, handleEditMessage }) => {
  const { user } = useContext(AuthContext);
  const messageBodyRef = useRef(null);
  /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 마우스가 메시지 위에 올라와 있는지 여부 */
  const [isHovered, setIsHovered] = useState(false);
  /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 메시지 수정 모드 활성화 여부 */
  const [isEditing, setIsEditing] = useState(false);
  /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 수정 중인 메시지 내용 */
  const [editedContent, setEditedContent] = useState(message.content);

  /**
   * 외부로부터 메시지 내용이 변경되었을 때, 수정 중이 아니라면 내부 상태를 동기화하는 Effect.
   */
  useEffect(() => {
    if (!isEditing) {
      setEditedContent(message.content);
    }
  }, [message.content, isEditing]);

  /** 메시지 수정을 시작하는 함수. */
  const handleStartEdit = () => {
    setIsEditing(true);
  };

  /** 메시지 수정을 취소하고 원래 내용으로 복구하는 함수. */
  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditedContent(message.content);
  };

  /**
   * 수정된 내용을 저장하는 함수.
   * 변경 사항이 있을 경우에만 부모 컴포넌트의 `handleEditMessage`를 호출합니다.
   */
  const handleSaveEdit = () => {
    if (editedContent.trim() === message.content) {
        setIsEditing(false);
        return;
    }
    handleEditMessage(message.messageId, editedContent.trim());
    setIsEditing(false);
  };

  /**
   * 메시지 텍스트를 파싱하여 URL, 이미지, 비디오, YouTube, 멘션 등을 각각에 맞는 컴포넌트로 렌더링합니다.
   * @param {string} text - 원본 메시지 텍스트.
   * @returns {React.ReactNodeArray} 렌더링할 React 노드 배열.
   */
  const renderAdvancedContent = (text) => {
    if (!text) return '';
    
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
    const videoExtensions = ['mp4', 'webm', 'ogg'];
    const youtubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w\-]+)/;
    const mentionRegex = /@([^\s]+)/g;

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
      
      const mentionParts = part.split(mentionRegex);
      return mentionParts.map((mPart, j) => {
        if (j % 2 === 1) {
          const mentionedNickname = mPart;
          const isMe = user && user.nickname === mentionedNickname;
          return <span key={`${i}-${j}`} className={`mention-highlight ${isMe ? 'mention-me' : ''}`}>@{mentionedNickname}</span>;
        }
        return mPart.split('\n').map((line, k) => <React.Fragment key={`${i}-${j}-${k}`}>{line}{k < mPart.split('\n').length - 1 && <br />}</React.Fragment>);
      });
    });
  };  

  /**
   * 메시지 타입에 따라 적절한 콘텐츠를 렌더링합니다. (예: 파일, 텍스트)
   * @returns {JSX.Element} 렌더링된 메시지 내용.
   */
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
  
  /**
   * 메시지에 링크 미리보기 정보가 있을 경우, 이를 렌더링합니다.
   * @returns {JSX.Element|null} 링크 미리보기 카드 JSX 또는 null.
   */
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