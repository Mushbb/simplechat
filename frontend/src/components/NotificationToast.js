import React from 'react';

/**
 * @file react-toastify 내에서 사용될 사용자 정의 토스트 컴포넌트입니다.
 * 친구 요청이나 방 초대와 같이 사용자의 상호작용(수락/거절)이 필요한 알림을 표시합니다.
 */

/**
 * 상호작용이 가능한 알림 토스트 컴포넌트.
 * @param {object} props
 * @param {import('../context/NotificationContext').Notification} props.notification - 표시할 알림 데이터 객체.
 * @param {Function} props.onAccept - '수락' 버튼 클릭 시 호출될 콜백 함수.
 * @param {Function} props.onReject - '거절' 버튼 클릭 시 호출될 콜백 함수.
 * @param {Function} props.closeToast - 토스트를 닫기 위해 호출될 함수 (react-toastify에서 제공).
 * @returns {JSX.Element} NotificationToast 컴포넌트의 JSX.
 */
function NotificationToast({ notification, onAccept, onReject, closeToast }) {
	
	/**
	 * '수락' 버튼 클릭 시 실행될 핸들러.
	 * 부모로부터 받은 onAccept 콜백을 실행하고 토스트를 닫습니다.
	 */
	const handleAccept = () => {
		onAccept(notification);
		closeToast();
	};
	
	/**
	 * '거절' 버튼 클릭 시 실행될 핸들러.
	 * 부모로부터 받은 onReject 콜백을 실행하고 토스트를 닫습니다.
	 */
	const handleReject = () => {
		onReject(notification.notificationId);
		closeToast();
	};
	
	return (
		<div className="notification-item">
			<span>{notification.content}</span>
			<div className="notification-actions">
				<button onClick={handleAccept}>수락</button>
				<button className="danger-button" onClick={handleReject}>거절</button>
			</div>
		</div>
	);
}

export default NotificationToast;