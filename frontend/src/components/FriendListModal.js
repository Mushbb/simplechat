import React, { createContext, useState, useEffect, useContext, useRef } from 'react';
import { FriendContext } from '../context/FriendContext';
import { ModalContext } from '../context/ModalContext';
import '../styles/Friends.css';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = axiosInstance.getUri();

function FriendListModal() {
	const { friends, setFriends, removeFriend } = useContext(FriendContext);
	const { friendModalConfig, closeFriendListModal, openUserProfileModal } = useContext(ModalContext);
	const [loading, setLoading] = useState(true);
	const modalRef = useRef(null);
	// ✅ 1. 보정된 위치를 저장할 새로운 state 추가
	const [correctedPosition, setCorrectedPosition] = useState(null);

	// ✅ 2. 모달 위치를 화면 경계에 맞게 보정하는 useEffect 추가
	useEffect(() => {
		if (friendModalConfig.isOpen && friendModalConfig.position && modalRef.current) {
			const modal = modalRef.current;
			const { left, top, bottom } = friendModalConfig.position;
			let newLeft = left;

			// 화면 왼쪽 경계 체크
			if (newLeft < 0) {
				newLeft = 8; // 화면 왼쪽에 너무 붙지 않도록 약간의 여백(8px)을 줍니다.
			}

			// 화면 오른쪽 경계 체크
			if (newLeft + modal.offsetWidth > window.innerWidth) {
				newLeft = window.innerWidth - modal.offsetWidth - 8; // 화면 오른쪽에도 여백(8px)을 줍니다.
			}

			setCorrectedPosition({ left: newLeft, top, bottom, mode: friendModalConfig.position.mode });
		} else {
			setCorrectedPosition(null); // 모달이 닫히면 위치 정보 초기화
		}
	}, [friendModalConfig.isOpen, friendModalConfig.position]); // 모달이 열리거나 위치가 바뀔 때마다 실행

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
	
	// ✅ 3. 보정된 위치(correctedPosition)를 사용하여 스타일 객체 생성
	const modalStyle = friendModalConfig.isOpen && correctedPosition ? {
		position: correctedPosition.mode || 'absolute',
		top: correctedPosition.top ? `${correctedPosition.top}px` : 'auto',
		bottom: correctedPosition.bottom ? `${correctedPosition.bottom}px` : 'auto',
		left: `${correctedPosition.left}px`,
		// 초기 렌더링 시 위치 계산 전까지는 보이지 않도록 처리
		visibility: correctedPosition ? 'visible' : 'hidden',
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