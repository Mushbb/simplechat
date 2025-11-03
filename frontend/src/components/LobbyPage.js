import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import CreateRoomModal from './CreateRoomModal';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = axiosInstance.getUri();

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
  const { user, openLoginModal, loading  } = useContext(AuthContext);
    const { setActiveRoomId, initializeChat, usersByRoom, joinRoomAndConnect, joinedRooms } = useContext(ChatContext);
    const navigate = useNavigate();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const fetchRooms = async () => {
    try {
        const response = await axiosInstance.get(`/room/list`);
        setRooms(response.data);
    } catch (error) {
        console.error('Failed to fetch rooms:', error);
    }
  };

  // 방 입장 처리 함수 - async/await 추가
    
    const handleEnterRoom = async (room) => {
        // ✨ 2. "들어가기 전에" 내가 이미 참여한 방인지 확인합니다.
        const isAlreadyMember = joinedRooms.some(joinedRoom => joinedRoom.id === room.id);
        
        if (isAlreadyMember) {
            console.log(`이미 참여한 방 #${room.id} 입니다. 바로 이동합니다.`);
            // 이미 멤버라면, 아무것도 할 필요 없이 그냥 그 방으로 이동만 합니다.
            navigate(`/chat/${room.id}`);
            return; // 함수 종료
        }
        
        // --- 아래는 기존 로직과 동일 (멤버가 아닐 경우에만 실행됨) ---
        let password = '';
        if (room.roomType === 'PRIVATE' && !room.isMember) {
            password = prompt('비밀번호를 입력하세요:');
            if (password === null) {
                return;
            }
        }
        
        try {
            await axiosInstance.post(`/room/${room.id}/users`, { password });
            
            // 이제 joinRoomAndConnect는 중복 걱정 없이 안전하게 호출할 수 있습니다.
            await joinRoomAndConnect(room);
            
            navigate(`/chat/${room.id}`);
            
        } catch (error) {
            console.error('Failed to enter room:', error);
            alert(error.response?.data?.message || '입장에 실패했습니다.');
        }
    };

  const handleCreateRoom = async (roomData) => {
        try {
            const roomRequestData = {
                roomName: roomData.roomName,
                roomType: roomData.isPrivate ? 'PRIVATE' : 'PUBLIC',
                password: roomData.password
            };
            const response = await axiosInstance.post(`/room/create`, roomRequestData);
            const newRoomId = response.data;
            setIsCreateModalOpen(false); // 모달 닫기
            alert('새로운 방이 생성되었습니다!');

            await initializeChat();

            fetchRooms(); // 방 목록 새로고침
            navigate(`/chat/${newRoomId}`); // 생성된 방으로 바로 이동하고 싶다면 주석 해제
        } catch (error) {
            console.error('Failed to create room:', error);
            alert(error.response?.data?.message || '방 생성에 실패했습니다.');
        }
  };

    useEffect(() => {
        // 로비에 처음 들어왔을 때 방 목록을 불러옵니다.
        fetchRooms();
    }, []);

    useEffect(() => {
        // 로비 페이지가 보이면, 활성화된 방이 없다고 Context에 알려줍니다.
        setActiveRoomId(null);

        if (!loading) {
            fetchRooms();
        }
    }, [setActiveRoomId, loading]);

    // ✅ 4. usersByRoom 상태가 변경될 때마다 방 목록을 새로고침하는
    //    useEffect를 새로 추가합니다.
    useEffect(() => {
        // usersByRoom이 비어있지 않다는 것은 웹소켓 연결 및 초기화가
        // 어느정도 진행되었다는 신호이므로, 이 때 목록을 새로고침하면
        // 정확한 인원수를 가져올 확률이 높습니다.
        if (Object.keys(usersByRoom).length > 0) {
            fetchRooms();
        }
    }, [usersByRoom]); // usersByRoom 객체가 바뀔 때마다 실행

  // if (!user) {
  //   return <h2 style={{padding: '20px'}}>로그인하고 채팅방 목록을 확인하세요.</h2>;
  // }

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
                                <span className="room-count online">
                                    ● {room.connCount}
                                </span>
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
                                <span className="room-count online">
                                    ● {room.connCount}
                                </span>
                            </div>
                        </li>
                    )) : <p className="no-rooms">입장할 수 있는 다른 채팅방이 없습니다.</p>}
                </ul>
            </div>
        </div>
    );
}

export default LobbyPage;
