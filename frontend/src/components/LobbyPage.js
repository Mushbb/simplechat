import React, { useState, useEffect, useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { RoomContext } from '../context/RoomContext';
import { ModalContext } from '../context/ModalContext';
import CreateRoomModal from './CreateRoomModal';
import axiosInstance from '../api/axiosInstance';

function LobbyPage() {
    const { user, loading } = useContext(AuthContext);
    const { rooms, joinedRooms, joinRoomAndConnect, fetchRooms, setActiveRoomId } = useContext(RoomContext);
    const { openLoginModal } = useContext(ModalContext);
    const navigate = useNavigate();
    const location = useLocation(); // useLocation 훅 추가

    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    useEffect(() => {
        // 현재 경로가 로비일 때만 데이터를 가져오고, activeRoomId를 null로 설정
        if (location.pathname === '/') {
            setActiveRoomId(null);
            fetchRooms();
        }
    }, [location, fetchRooms, setActiveRoomId]); // location을 의존성 배열에 추가

    const handleEnterRoom = async (room) => {
        const isAlreadyMember = joinedRooms.some(joinedRoom => joinedRoom.id === room.id);
        if (isAlreadyMember) {
            navigate(`/chat/${room.id}`);
            return;
        }

        let password = '';
        if (room.roomType === 'PRIVATE' && !room.isMember) {
            password = prompt('비밀번호를 입력하세요:');
            if (password === null) return;
        }
        
        try {
            await axiosInstance.post(`/room/${room.id}/users`, { password });
            joinRoomAndConnect(room);
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
            setIsCreateModalOpen(false);
            alert('새로운 방이 생성되었습니다!');
            fetchRooms();
            navigate(`/chat/${newRoomId}`);
        } catch (error) {
            console.error('Failed to create room:', error);
            alert(error.response?.data?.message || '방 생성에 실패했습니다.');
        }
    };

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
