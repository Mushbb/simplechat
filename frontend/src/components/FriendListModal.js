import React, { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';
import UserProfileModal from './UserProfileModal';

const SERVER_URL = 'http://10.50.131.25:8080';

function FriendListModal() {
	const { closeFriendListModal, friends, setFriends, removeFriend } = useContext(AuthContext);
	const [loading, setLoading] = useState(true);
	const modalRef = useRef(null);
	
	// --- 프로필 모달 관련 로컬 상태 ---
	const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
	const [selectedProfile, setSelectedProfile] = useState(null);
	const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });
	
	// --- 바깥 클릭 시 닫기 기능 ---
	useEffect(() => {
		const handleClickOutside = (event) => {
			// 모달이 존재하고, 클릭한 곳이 모달 내부가 아닐 때
			if (modalRef.current && !modalRef.current.contains(event.target)) {
				// 프로필 모달이 열려있을 때는 친구 목록 모달이 닫히지 않도록 함
				if (!isProfileModalOpen) {
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
	
	// --- 핸들러 함수 ---
	const handleProfileClick = async (friendId, event) => {
		// li 요소의 위치를 기준으로 모달 위치 계산
		const liRect = event.currentTarget.getBoundingClientRect();
		setModalPosition({ top: liRect.top, left: liRect.left + 110 });
		
		try {
			const response = await axiosInstance.get(`/user/${friendId}/profile`);
			setSelectedProfile(response.data); // 서버에서 받은 전체 프로필로 state 설정
			setIsProfileModalOpen(true);
		} catch (error) {
			console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
			alert('프로필 정보를 가져오는 데 실패했습니다.');
		}
	};
	
	const handleRemoveClick = (e, friendId) => {
		e.stopPropagation(); // 이벤트 버블링 방지
		removeFriend(friendId);
	};
	
	return (
		<>
		<div className="friend-list-modal" ref={modalRef}>
			<div className="modal-header">
				<h2>친구 목록</h2>
				<button onClick={closeFriendListModal} className="close-btn">&times;</button>
			</div>
			<div className="modal-body">
				{loading ? (
					<p>친구 목록을 불러오는 중입니다...</p>
				) : friends.length === 0 ? (
					<p>아직 친구가 없습니다.</p>
				) : (
					<ul className="friend-list">
						{friends.map(friend => (
							<li key={friend.userId} className="friend-item" onClick={(e) => handleProfileClick(friend.userId, e)}>
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
			{/* 프로필 모달 렌더링 */}
			{isProfileModalOpen && (
				<UserProfileModal
					profile={selectedProfile}
					onClose={() => setIsProfileModalOpen(false)}
					position={modalPosition}
				/>
			)}
		</>
	);
}

export default FriendListModal;