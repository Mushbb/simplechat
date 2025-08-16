import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import CreateRoomModal from './CreateRoomModal';
import axiosInstance from '../api/axiosInstance';

// 간단한 리스트 아이템 스타일
const listItemStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '15px',
  borderBottom: '1px solid #eee',
  listStyle: 'none'
};

const roomInfoStyle = {
  flex: 1,
};

const roomActionsStyle = {
  marginLeft: '20px',
};

function LobbyPage() {
  const [rooms, setRooms] = useState([]);
  const { user, openLoginModal } = useContext(AuthContext);
  const navigate = useNavigate();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const fetchRooms = async () => {
    try {
        const response = await axiosInstance.get('/room/list');
        setRooms(response.data);
    } catch (error) {
        console.error('Failed to fetch rooms:', error);
    }
  };

  // 방 입장 처리 함수 - async/await 추가
  const handleEnterRoom = async (room) => {
    let password = '';
    // 비공개 방일 경우 비밀번호를 입력받습니다.
    if (room.roomType === 'PRIVATE' && !room.isMember) {
      password = prompt('비밀번호를 입력하세요:');
      if (password === null) return;// 사용자가 취소 버튼을 누른 경우
        try {
            const response = await fetch(`/room/${room.id}/users`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ password }), // 비밀번호를 body에 담아 전송
            });

            if (response.ok) {
                console.log(`Successfully entered room ${room.id}`);
                navigate(`/chat/${room.id}`); // 성공 시에만 페이지 이동
            } else {
                const errorData = await response.json();
                alert(errorData.message || '입장에 실패했습니다.');
            }
        } catch (error) {
            console.error('Failed to enter room:', error);
            alert('방 입장에 실패했습니다.');
        }
    }

    // 공개방이거나, 이미 멤버인 비밀방은 바로 입장 요청
      try {
          const response = await axiosInstance.post(`/room/${room.id}/users`, { password: '' }); // 비밀번호는 빈 값으로
          navigate(`/chat/${room.id}`);
      } catch (error) {
          console.error('Failed to enter room:', error);
          alert(error.response?.data?.message || '입장에 실패했습니다.');
      }
  };

  const handleCreateRoom = async (roomData) => {
        try {
            // roomData는 모달에서 받은 { roomName, roomType, password } 객체입니다.
            const response = await axiosInstance.post('/room/create', roomData);
            const newRoomId = response.data;
            setIsCreateModalOpen(false); // 모달 닫기
            alert('새로운 방이 생성되었습니다!');
            fetchRooms(); // 방 목록 새로고침
            // navigate(`/chat/${newRoomId}`); // 생성된 방으로 바로 이동하고 싶다면 주석 해제
        } catch (error) {
            console.error('Failed to create room:', error);
            alert(error.response?.data?.message || '방 생성에 실패했습니다.');
        }
  };

    useEffect(() => {
        // 로비에 처음 들어왔을 때 방 목록을 불러옵니다.
        fetchRooms();
    }, []);

  if (!user) {
    return <h2 style={{padding: '20px'}}>로그인하고 채팅방 목록을 확인하세요.</h2>;
  }

    // ✅ 2. 전체 방 목록을 '내가 참여 중인 방'과 '그 외의 방'으로 분리합니다.
    const myRooms = rooms.filter(room => room.isMember);
    const otherRooms = rooms.filter(room => !room.isMember);

    return (
        <div className="lobby-container">
            {/* ✅ 4. isCreateModalOpen이 true일 때만 모달을 렌더링합니다. */}
            {isCreateModalOpen && <CreateRoomModal onCreate={handleCreateRoom} onClose={() => setIsCreateModalOpen(false)} />}

            <div className="lobby-header">
                <h1>채팅 로비</h1>
                <button
                    className="create-room-btn"
                    onClick={() => {
                        if (user) {
                            setIsCreateModalOpen(true);
                        } else {
                            alert('로그인이 필요합니다.');
                            openLoginModal();
                        }
                    }}
                >
                    새 채팅방 만들기
                </button>
            </div>
            <div className="room-list">
                <h2>내 채팅방</h2>
                <ul>
                    {myRooms.length > 0 ? myRooms.map(room => (
                        <li key={room.id} className="room-list-item my-room" onClick={() => handleEnterRoom(room)}>
                            <div className="room-info">
                                <span className="room-name">
                                    {room.roomType === 'PRIVATE' && <span className="lock-icon">🔒</span>}
                                    {room.name}
                                </span>
                                <span className="room-owner">개설자: {room.ownerName}</span>
                            </div>
                            <div className="room-meta">
                                <span className="room-count">{room.connCount} / {room.userCount}</span>
                                <button className="enter-btn">입장</button>
                            </div>
                        </li>
                    )) : <p className="no-rooms">참여 중인 채팅방이 없습니다.</p>}
                </ul>
            </div>
            {/* --- ✅ 4. '전체 채팅방' 목록 UI (기존 UI 재활용) --- */}
            <div className="room-list">
                <h2>전체 채팅방</h2>
                <ul>
                    {otherRooms.length > 0 ? otherRooms.map(room => (
                        <li key={room.id} className="room-list-item" onClick={() => handleEnterRoom(room)}>
                            <div className="room-info">
                                <span className="room-name">
                                    {room.roomType === 'PRIVATE' && <span className="lock-icon">🔒</span>}
                                    {room.name}
                                </span>
                                <span className="room-owner">개설자: {room.ownerName}</span>
                            </div>
                            <div className="room-meta">
                                <span className="room-count">{room.connCount} / {room.userCount}</span>
                                <button className="enter-btn">입장</button>
                            </div>
                        </li>
                    )) : <p className="no-rooms">입장할 수 있는 다른 채팅방이 없습니다.</p>}
                </ul>
            </div>
        </div>
    );
}

export default LobbyPage;
