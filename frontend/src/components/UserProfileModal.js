import React, { useState, useEffect, useContext } from 'react';
import axiosInstance from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';

/**
 * @file 다른 사용자의 프로필 정보를 표시하는 모달 컴포넌트입니다.
 * 현재 사용자와의 친구 관계 상태에 따라 '친구 추가' 등의 상호작용 버튼을 제공합니다.
 */

const SERVER_URL = axiosInstance.getUri();

// 인라인 스타일 객체들
const modalStyle = {
    position: 'absolute',
    width: '250px',
    backgroundColor: 'white',
    border: '1px solid #ccc',
    borderRadius: '8px',
    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    zIndex: 1100,
};
const headerStyle = {
    padding: '10px 15px',
    backgroundColor: '#f7f7f7',
    borderBottom: '1px solid #eee',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
};
const nicknameStyle = { fontWeight: 'bold', fontSize: '16px' };
const closeBtnStyle = { fontSize: '24px', color: '#aaa', cursor: 'pointer', border: 'none', background: 'none' };
const bodyStyle = { padding: '15px' };
const profilePicStyle = { width: '60px', height: '60px', borderRadius: '50%', marginRight: '15px' };
const statusMsgStyle = { marginTop: '10px', padding: '10px', backgroundColor: '#f9f9f9', borderRadius: '4px', minHeight: '40px', whiteSpace: 'pre-wrap', wordBreak: 'break-word' };
const footerStyle = { padding: '10px 15px', borderTop: '1px solid #eee', textAlign: 'right' };
const actionBtnStyle = { padding: '5px 10px', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer' };
const disabledBtnStyle = { ...actionBtnStyle, cursor: 'not-allowed', backgroundColor: '#eee', color: '#888' };


/**
 * 다른 사용자의 프로필을 표시하는 모달.
 * @param {object} props
 * @param {object} props.profile - 표시할 사용자의 프로필 정보.
 * @param {Function} props.onClose - 모달을 닫는 함수.
 * @param {{top: number, left: number}} props.position - 모달이 표시될 위치.
 * @returns {JSX.Element|null} 프로필 정보가 있을 경우 모달 JSX를, 없으면 null을 반환.
 */
function UserProfileModal({ profile, onClose, position }) {
    const { user: currentUser } = useContext(AuthContext);
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 현재 사용자와 프로필 사용자 간의 친구 관계 상태 */
    const [friendshipStatus, setFriendshipStatus] = useState('LOADING');
    
    /**
     * 프로필이 변경될 때마다 현재 사용자와의 친구 관계 상태를 서버에서 조회하는 Effect.
     */
    useEffect(() => {
        if (profile && currentUser && profile.userId !== currentUser.userId) {
            const fetchStatus = async () => {
                try {
                    const response = await axiosInstance.get(`/api/friends/status/${profile.userId}`);
                    setFriendshipStatus(response.data.status);
                } catch (error) {
                    console.error("Failed to fetch friendship status", error);
                    setFriendshipStatus('ERROR');
                }
            };
            fetchStatus();
        }
    }, [profile, currentUser]);
    
    /**
     * '친구 추가' 버튼 클릭 시 친구 요청을 보내는 핸들러.
     */
    const handleSendRequest = async () => {
        try {
            await axiosInstance.post('/api/friends/requests', { receiverId: profile.userId });
            setFriendshipStatus('PENDING_SENT');
        } catch (error) {
            alert(error.response?.data?.message || '친구 요청에 실패했습니다.');
        }
    };
    
    /**
     * 친구 관계 상태에 따라 다른 상호작용 버튼을 렌더링하는 함수.
     * @returns {JSX.Element|null} 상태에 맞는 버튼 JSX 또는 null.
     */
    const renderActionButtons = () => {
        if (!currentUser || !profile || currentUser.userId === profile.userId) {
            return null; // 내 프로필에는 버튼을 표시하지 않음
        }
        
        switch (friendshipStatus) {
            case 'NONE':
                return <button style={actionBtnStyle} onClick={handleSendRequest}>친구 추가</button>;
            case 'PENDING_SENT':
                return <button style={disabledBtnStyle} disabled>요청 보냄</button>;
            case 'PENDING_RECEIVED':
                return <button style={disabledBtnStyle} disabled>요청 받음</button>;
            case 'FRIENDS':
                return <button style={disabledBtnStyle} disabled>친구</button>;
            case 'LOADING':
                return <button style={disabledBtnStyle} disabled>Loading...</button>;
            default:
                return null;
        }
    };
    
    if (!profile) return null;
    
    return (
        <div style={{ ...modalStyle, top: position.top, left: position.left }}>
            <div style={headerStyle}>
                <span style={nicknameStyle}>{profile.nickname}</span>
                <button style={closeBtnStyle} onClick={onClose}>&times;</button>
            </div>
            <div style={bodyStyle}>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <img style={profilePicStyle} src={`${SERVER_URL}${profile.imageUrl}`} alt={profile.nickname} />
                    <div>
                        <p style={{ margin: 0, fontWeight: 'bold' }}>{profile.username}</p>
                    </div>
                </div>
                <div style={statusMsgStyle}>
                    {profile.status_msg || '(상태 메시지가 없습니다.)'}
                </div>
            </div>
            <div style={footerStyle}>
                {renderActionButtons()}
            </div>
        </div>
    );
}

export default UserProfileModal;