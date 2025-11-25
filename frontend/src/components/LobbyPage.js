import React, { useState, useEffect, useContext, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { RoomContext } from '../context/RoomContext';
import { ChatContext } from '../context/ChatContext';
import { ModalContext } from '../context/ModalContext';
import CreateRoomModal from './CreateRoomModal';
import axiosInstance from '../api/axiosInstance';
import '../styles/LobbyPage.css';

/**
 * @file 채팅 로비 페이지를 구성하는 메인 컴포넌트입니다.
 * 전체 채팅방 목록과 내가 참여한 채팅방 목록을 보여주고,
 * 새로운 채팅방을 생성하거나 기존 채팅방에 입장하는 기능을 제공합니다.
 */

/**
 * 채팅 로비 페이지 컴포넌트입니다.
 * @returns {JSX.Element} LobbyPage 컴포넌트의 JSX.
 */
function LobbyPage() {
    const { user, loading } = useContext(AuthContext);
    const { rawRooms, joinedRooms, joinRoomAndConnect, fetchRooms, setActiveRoomId } = useContext(RoomContext);
    const { usersByRoom } = useContext(ChatContext);
    const { openLoginModal } = useContext(ModalContext);
    const navigate = useNavigate();

    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} */
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    /**
     * API로부터 받은 방 목록(`rawRooms`)과 실시간 사용자 수(`usersByRoom`)를 조합하여
     * 화면에 표시할 최종 방 목록을 계산합니다.
     * @type {import('../context/RoomContext').RawRoom[]}
     */
    const rooms = useMemo(() => {
        return rawRooms.map(room => ({
            ...room,
            connCount: usersByRoom[room.id]?.filter(u => u.conn === 'CONNECT').length || 0,
        }));
    }, [rawRooms, usersByRoom]);

    /**
     * 로비 페이지가 마운트될 때, 현재 활성 방 ID를 초기화하고,
     * 주기적으로 방 목록을 새로고침하는 인터벌을 설정합니다.
     * 언마운트 시 인터벌을 정리합니다.
     */
    useEffect(() => {
        setActiveRoomId(null);
        const intervalId = setInterval(fetchRooms, 3000);
        return () => clearInterval(intervalId);
    }, [setActiveRoomId, fetchRooms]);

    /**
     * 사용자가 특정 채팅방 입장을 시도할 때 호출되는 핸들러입니다.
     * 이미 참여한 방이면 바로 이동하고, 새로운 비공개 방이면 비밀번호를 입력받습니다.
     * @param {import('../context/RoomContext').RawRoom} room - 입장할 채팅방 정보 객체.
     */
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

    /**
     * '새 채팅방 만들기' 모달에서 방 생성을 완료했을 때 호출되는 핸들러입니다.
     * @param {object} roomData - 모달로부터 받은 새로운 방 생성 데이터.
     * @param {string} roomData.roomName - 방 이름.
     * @param {boolean} roomData.isPrivate - 비공개 방 여부.
     * @param {string} roomData.password - 비공개 방의 경우 비밀번호.
     */
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

    /**
     * 현재 사용자가 참여하고 있는 방 목록.
     * @type {import('../context/RoomContext').RawRoom[]}
     */
    const myRooms = rooms.filter(room => room.isMember);
    /**
     * 현재 사용자가 참여하고 있지 않은 방 목록.
     * @type {import('../context/RoomContext').RawRoom[]}
     */
    const otherRooms = rooms.filter(room => !room.isMember);


    return (
        <div className="lobby-container">
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
