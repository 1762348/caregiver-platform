package com.youhu.caregiver.message.service;

import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.message.domain.Message;
import com.youhu.caregiver.message.domain.MessageType;
import com.youhu.caregiver.message.dto.MessageView;
import com.youhu.caregiver.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 站内消息：订单/支付/服务进度的关键节点都会写入 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public void notifyUser(Long userId, MessageType type, String title, String body, String deeplink) {
        Message message = new Message();
        message.setToUserId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setBody(body);
        message.setDeeplink(deeplink);
        message.setIsRead(0);
        messageRepository.save(message);
    }

    @Transactional
    public void notifyCaregiver(Long caregiverId, MessageType type, String title, String body,
                                String deeplink) {
        Message message = new Message();
        message.setToCaregiverId(caregiverId);
        message.setType(type);
        message.setTitle(title);
        message.setBody(body);
        message.setDeeplink(deeplink);
        message.setIsRead(0);
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public PageResult<MessageView> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Message> messages = messageRepository.findByToUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResult.of(messages.map(this::toView));
    }

    @Transactional
    public void markRead(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .filter(item -> userId.equals(item.getToUserId()))
                .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (!Integer.valueOf(1).equals(message.getIsRead())) {
            message.setIsRead(1);
            messageRepository.save(message);
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return messageRepository.markAllReadByUser(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countByToUserIdAndIsRead(userId, 0);
    }

    private MessageView toView(Message message) {
        return new MessageView(message.getId(), message.getTitle(), message.getBody(),
                message.getType(), message.getDeeplink(), message.getIsRead() == 1,
                message.getCreatedAt());
    }
}
