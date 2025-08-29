import React, { createContext, useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = 'http://10.50.131.25:8080';

function FriendListModal() {
	const { closeFriendListModal, friends, setFriends, removeFriend, friendModalConfig,
		openUserProfileModal, closeUserProfileModal, selectedProfile, modalPosition } = useContext(AuthContext);
	const [loading, setLoading] = useState(true);
	const modalRef = useRef(null);
	
	// --- 바깥 클릭 시 닫기 기능 ---
	useEffect(() => {
		const handleClickOutside = (event) => {
			// 💡 클릭된 곳이 이름표를 가진 토글 버튼이면,
			if (event.target.closest('[data-modal-toggle="friendlist"]')) {
				return; // 그냥 무시하고 아무것도 하지 않음! (이중 처리 방지)
			}
			
			// 모달이 존재하고, 클릭한 곳이 모달 내부가 아닐 때
			if (modalRef.current && !modalRef.current.contains(event.target)) {
				// 프로필 모달이 열려있을 때는 친구 목록 모달이 닫히지 않도록 함
				if (modalRef.current && !modalRef.current.contains(event.target)) {
					closeFriendListModal();
				}
			}
		};
		
		// mousedown 이벤트 리스너 등록
		document.addEventListener('mousedown', handleClickOutside);
		// 컴포넌트가 사라질 때 이벤트 리스너 제거 (메모리 누수 방지)
		return () => {
			document.removeEventListener('mousedown', handleClickOutside);
		};
	}, [closeFriendListModal]);
	
	// --- 친구 목록 불러오기 기능 ---
	useEffect(() => {
		const fetchFriends = async () => {
			try {
				setLoading(true);
				const response = await axiosInstance.get('/api/friends');
				const fetchedFriends = response.data;
				
				fetchedFriends.sort((a, b) => {
					return (b.conn === 'CONNECT') - (a.conn === 'CONNECT')
						|| a.nickname.localeCompare(b.nickname);
				});
				
				setFriends(fetchedFriends);
			} catch (error) {
				console.error("Failed to fetch friends:", error);
			} finally {
				setLoading(false);
			}
		};
		if (!friends.length) { // 친구 목록이 비어있을 때만 호출
			fetchFriends();
		} else {
			setLoading(false);
		}
	}, [friends.length, setFriends]);
	
	const handleRemoveClick = (e, friendId) => {
		e.stopPropagation(); // 이벤트 버블링 방지
		removeFriend(friendId);
	};
	
	// ✨ 신규: 모달에 적용할 스타일 객체
	// modalPosition에 값이 있을 때만 top, left 스타일을 적용합니다.
	const modalStyle = friendModalConfig.isOpen && friendModalConfig.position ? {
		position: friendModalConfig.position.mode || 'absolute',
		top: friendModalConfig.position.top ? `${friendModalConfig.position.top}px` : 'auto',
		bottom: friendModalConfig.position.bottom ? `${friendModalConfig.position.bottom}px` : 'auto',
		left: `${friendModalConfig.position.left}px`,
	} : {
		display: 'none'
	};
	
	return (
		<>
		<div className="friend-list-modal" ref={modalRef} style={modalStyle}>
			<div className="modal-header">
				<h2>{friendModalConfig.title}</h2>
				<button onClick={closeFriendListModal} className="close-btn">&times;</button>
			</div>
			<div className="modal-body">
				{loading ? (
					<p>친구 목록을 불러오는 중입니다...</p>
				) : friends.length === 0 ? (
					<p className="no-friends">아직 친구가 없습니다. :(</p>
				) : (
					<ul className="friend-list">
						{friends.map(friend => (
							<li key={friend.userId} className="friend-item" onClick={(event) => friendModalConfig.onFriendClick(friend, event)}>
								<img src={`${SERVER_URL}${friend.profileImageUrl}`} alt={friend.nickname} className="friend-profile-img" />
								<span className={`friend-status ${friend.conn}`}>●</span>
								<span className="friend-nickname">{friend.nickname}</span>
								<button className="remove-friend-btn" onClick={(e) => handleRemoveClick(e, friend.userId)}>
									&times;
								</button>
							</li>
						))}
					</ul>
				)}
			</div>
		</div>
		</>
	);
}

export default FriendListModal;