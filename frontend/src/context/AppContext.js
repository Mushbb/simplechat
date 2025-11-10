import React from 'react';
import { AuthProvider } from './AuthContext';
import { WebSocketProvider } from './WebSocketContext';
import { NotificationProvider } from './NotificationContext';
import { ModalProvider } from './ModalContext';
import { FriendProvider } from './FriendContext';
import { RoomProvider } from './RoomContext';
import { ChatProvider } from './ChatContext';

// 논리적 순서에 따라 Provider 배열 정의
const providers = [
  AuthProvider,
  RoomProvider,
  FriendProvider,
  WebSocketProvider,
  NotificationProvider,
  ChatProvider,
  ModalProvider,
];

// Provider들을 조합하는 컴포넌트
export const AppContextProvider = ({ children }) => {
  return providers.reduceRight((acc, Provider) => {
    return <Provider>{acc}</Provider>;
  }, children);
};
