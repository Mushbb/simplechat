import React, { createContext, useState, useEffect, useContext, useRef } from 'react';
import { FriendContext } from '../context/FriendContext';
import { ModalContext } from '../context/ModalContext';
import '../styles/Friends.css';
import axiosInstance from '../api/axiosInstance';

/**
 * @file 친구 목록을 표시하고 상호작용하는 재사용 가능한 모달 컴포넌트입니다.
 * 이 모달의 열림 상태, 제목, 위치 및 친구 클릭 시 동작은 `ModalContext`를 통해 제어됩니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * 친구 목록을 표시하는 모달 컴포넌트.
 * @returns {JSX.Element | null} `friendModalConfig.isOpen`이 true일 때만 렌더링됩니다.
 */
function FriendListModal() {
	const { friends, setFriends, removeFriend } = useContext(FriendContext);
	const { friendModalConfig, closeFriendListModal, openUserProfileModal } = useContext(ModalContext);
	/** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 친구 목록 로딩 상태 */
	const [loading, setLoading] = useState(true);
	const modalRef = useRef(null);
	/** @type {[{left: number, top: number, bottom: number, mode: string}|null, React.Dispatch<React.SetStateAction<{left: number, top: number, bottom: number, mode: string}>>]} 화면 경계를 고려하여 보정된 모달의 위치 상태 */
	const [correctedPosition, setCorrectedPosition] = useState(null);

	/**
	 * 모달이 열릴 때, 전달받은 위치 정보를 기반으로 화면을 벗어나지 않도록 위치를 보정하는 Effect.
	 */
	useEffect(() => {
		if (friendModalConfig.isOpen && friendModalConfig.position && modalRef.current) {
			const modal = modalRef.current;
			const { left, top, bottom } = friendModalConfig.position;
			let newLeft = left;

			// 화면 왼쪽 경계 체크
			if (newLeft < 0) {
				newLeft = 8;
			}

			// 화면 오른쪽 경계 체크
			if (newLeft + modal.offsetWidth > window.innerWidth) {
				newLeft = window.innerWidth - modal.offsetWidth - 8;
			}

			setCorrectedPosition({ left: newLeft, top, bottom, mode: friendModalConfig.position.mode });
		} else {
			setCorrectedPosition(null); // 모달이 닫히면 위치 정보 초기화
		}
	}, [friendModalConfig.isOpen, friendModalConfig.position]);

	/**
	 * 모달 외부를 클릭했을 때 모달을 닫기 위한 Effect.
	 */
	useEffect(() => {
		const handleClickOutside = (event) => {
			if (event.target.closest('[data-modal-toggle="friendlist"]')) {
				return;
			}
			if (modalRef.current && !modalRef.current.contains(event.target)) {
				closeFriendListModal();
			}
		};
		
		document.addEventListener('mousedown', handleClickOutside);
		return () => {
			document.removeEventListener('mousedown', handleClickOutside);
		};
	}, [closeFriendListModal]);
	
	/**
	 * 친구 목록이 비어있을 때 서버로부터 친구 목록을 가져오는 Effect.
	 */
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
	
	/**
	 * 친구 삭제 버튼 클릭 시 이벤트 버블링을 막고 친구 삭제 함수를 호출하는 핸들러.
	 * @param {React.MouseEvent} e - 마우스 클릭 이벤트.
	 * @param {number} friendId - 삭제할 친구의 ID.
	 */
	const handleRemoveClick = (e, friendId) => {
		e.stopPropagation();
		removeFriend(friendId);
	};
	
	/**
	 * 모달의 위치를 동적으로 설정하기 위한 스타일 객체.
	 * @type {React.CSSProperties}
	 */
	const modalStyle = friendModalConfig.isOpen && correctedPosition ? {
		position: correctedPosition.mode || 'absolute',
		top: correctedPosition.top ? `${correctedPosition.top}px` : 'auto',
		bottom: correctedPosition.bottom ? `${correctedPosition.bottom}px` : 'auto',
		left: `${correctedPosition.left}px`,
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