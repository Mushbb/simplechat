import React, { createContext, useState, useContext, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from './AuthContext';
import axiosInstance from '../api/axiosInstance';

/**
 * @file 채팅방 목록, 활성 채팅방, 읽지 않은 메시지 상태 등 채팅방과 관련된 전역 상태를 관리하고 제공하는 컨텍스트입니다.
 */

/**
 * @typedef {object} RawRoom
 * @property {number} id
 * @property {string} name
 * @property {string} roomType
 * @property {string} ownerName
 * @property {number} userCount
 * @property {number|null} connCount
 * @property {boolean} isMember
 */

/**
 * @typedef {object} RoomContextType
 * @property {RawRoom[]} rawRooms - API로부터 받은 필터링되지 않은 원본 방 목록.
 * @property {RawRoom[]} joinedRooms - 사용자가 참여하고 있는 방 목록.
 * @property {number|null} activeRoomId - 현재 사용자가 보고 있는 활성 채팅방의 ID.
 * @property {React.Dispatch<React.SetStateAction<number|null>>} setActiveRoomId - 활성 채팅방 ID를 설정하는 함수.
 * @property {Set<number>} unreadRooms - 읽지 않은 메시지가 있는 방들의 ID를 담은 Set.
 * @property {React.Dispatch<React.SetStateAction<Set<number>>>} setUnreadRooms - 읽지 않은 방 Set을 설정하는 함수.
 * @property {Function} joinRoomAndConnect - 사용자가 방에 참여했을 때 로컬 상태를 업데이트하는 함수.
 * @property {(roomId: number) => Promise<void>} exitRoom - 방에서 나가는 함수.
 * @property {(roomId: number) => Promise<void>} deleteRoom - 방을 삭제하는 함수.
 * @property {() => Promise<void>} fetchRooms - 전체 방 목록을 다시 불러오는 함수.
 * @property {Object<number, 'ADMIN'|'MEMBER'>} myRole - 각 방에서의 현재 사용자 역할을 담은 객체.
 * @property {React.Dispatch<React.SetStateAction<object>>} setMyRole - 사용자 역할 상태를 업데이트하는 함수.
 */

/**
 * 채팅방 컨텍스트 객체입니다.
 * @type {React.Context<RoomContextType>}
 */
const RoomContext = createContext();

/**
 * 채팅방 관련 상태와 기능을 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} RoomContext.Provider
 */
function RoomProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const navigate = useNavigate();

    /** @type {[RawRoom[], React.Dispatch<React.SetStateAction<RawRoom[]>>]} */
    const [rawRooms, setRawRooms] = useState([]);
    /** @type {[number|null, React.Dispatch<React.SetStateAction<number|null>>]} */
    const [activeRoomId, setActiveRoomId] = useState(null);
    /** @type {[Set<number>, React.Dispatch<React.SetStateAction<Set<number>>>]} */
    const [unreadRooms, setUnreadRooms] = useState(new Set());
    /** @type {[Object<number, 'ADMIN'|'MEMBER'>, React.Dispatch<React.SetStateAction<object>>]} */
    const [myRole, setMyRole] = useState({});

    /**
     * 서버로부터 전체 채팅방 목록을 가져와 상태를 업데이트합니다.
     */
    const fetchRooms = useCallback(async () => {
        try {
            const response = await axiosInstance.get('/room/list');
            setRawRooms(response.data);
        } catch (error) {
            console.error('Failed to fetch rooms:', error);
        }
    }, []);

    /**
     * 사용자 로그인 상태가 변경되면 방 목록을 가져오거나 초기화합니다.
     */
    useEffect(() => {
        if (user) {
            fetchRooms();
        } else {
            setRawRooms([]);
        }
    }, [user, fetchRooms]);

    /**
     * 사용자가 참여하고 있는 방 목록만 필터링하여 메모이즈합니다.
     * @type {RawRoom[]}
     */
    const joinedRooms = useMemo(() => rawRooms.filter(room => room.isMember), [rawRooms]);

    /**
     * 사용자가 특정 방에 참여했음을 로컬 상태에 반영합니다.
     * 이미 참여한 방이거나 목록에 없는 새로운 방인 경우를 모두 처리합니다.
     * @param {RawRoom} room - 참여할 방 정보 객체.
     */
    const joinRoomAndConnect = useCallback(async (room) => {
        setRawRooms(prevRawRooms => {
            const isAlreadyMember = prevRawRooms.some(r => r.id === room.id && r.isMember);
            if (isAlreadyMember) {
                console.log(`[RoomContext] Already a member of room #${room.id}.`);
                return prevRawRooms;
            }
            const roomExists = prevRawRooms.some(r => r.id === room.id);
            if (roomExists) {
                return prevRawRooms.map(r => r.id === room.id ? { ...r, isMember: true } : r);
            } else {
                return [...prevRawRooms, { ...room, isMember: true }];
            }
        });
    }, []);

    /**
     * 특정 채팅방에서 나갑니다.
     * @param {number} roomId - 나갈 방의 ID.
     */
    const exitRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}/users`);
            setRawRooms(prev => prev.filter(room => room.id !== roomId));
            navigate('/');
        } catch (error) {
            console.error("Failed to exit room:", error);
            alert(error.response?.data?.message || "방에서 나가는 데 실패했습니다.");
        }
    };

    /**
     * 특정 채팅방을 삭제합니다. (방장 권한 필요)
     * @param {number} roomId - 삭제할 방의 ID.
     */
    const deleteRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}`);
            setRawRooms(prev => prev.filter(room => room.id !== roomId));
            navigate('/');
        } catch (error) {
            console.error("Failed to delete room:", error);
            alert(error.response?.data?.message || "방 삭제에 실패했습니다.");
        }
    };

    /**
     * 활성 채팅방이 변경되면 해당 방의 '읽지 않음' 상태를 제거합니다.
     */
    useEffect(() => {
        if (activeRoomId && unreadRooms.has(activeRoomId)) {
            setUnreadRooms(prev => {
                const newSet = new Set(prev);
                newSet.delete(activeRoomId);
                return newSet;
            });
        }
    }, [activeRoomId, unreadRooms]);

    const value = {
        rawRooms,
        joinedRooms,
        activeRoomId,
        setActiveRoomId,
        unreadRooms,
        setUnreadRooms,
        joinRoomAndConnect,
        exitRoom,
        deleteRoom,
        fetchRooms,
        myRole,
        setMyRole,
    };

    return (
        <RoomContext.Provider value={value}>
            {children}
        </RoomContext.Provider>
    );
}

export { RoomContext, RoomProvider };
