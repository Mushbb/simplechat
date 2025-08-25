import React from 'react';

// 알림 데이터(notification), 수락/거절 함수, 토스트 닫기 함수를 props로 받습니다.
function NotificationToast({ notification, onAccept, onReject, closeToast }) {
	
	// 수락 버튼 클릭 시 실행될 함수
	const handleAccept = () => {
		onAccept(notification); // AuthContext의 수락 함수 호출
		closeToast();           // 토스트 닫기
	};
	
	// 거절 버튼 클릭 시 실행될 함수
	const handleReject = () => {
		onReject(notification.notificationId); // AuthContext의 거절 함수 호출
		closeToast();                          // 토스트 닫기
	};
	
	return (
		// 이 JSX 구조는 Topbar.js의 알림 항목과 완전히 동일합니다.
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